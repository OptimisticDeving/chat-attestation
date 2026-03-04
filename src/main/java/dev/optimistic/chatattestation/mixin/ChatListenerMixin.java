package dev.optimistic.chatattestation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.optimistic.chatattestation.crypto.SigningManager;
import dev.optimistic.chatattestation.duck.StyleDuck;
import dev.optimistic.chatattestation.mixin.accessor.ChatComponentAccessor;
import dev.optimistic.chatattestation.util.ChatEncoding;
import dev.optimistic.chatattestation.util.Payload;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.TextReplacementConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;

import static dev.optimistic.chatattestation.util.Constants.*;
import static dev.optimistic.chatattestation.util.Payload.withNonce;

@Mixin(ChatListener.class)
public abstract class ChatListenerMixin {
  @Unique
  private static final GuiMessageTag NO_SUCH_KEY = createTag(
    ChatFormatting.GOLD,
    "No such key could be found - perhaps your client needs to refetch keys or subscribe to a new manifest?"
  );
  @Unique
  private static final GuiMessageTag INVALID_SIGNATURE = createTag(
    ChatFormatting.DARK_RED,
    "Uh oh! Invalid signature! Someone is tampering with their message!"
  );
  @Unique
  private static final GuiMessageTag REUSED_SIGNATURE = createTag(
    ChatFormatting.DARK_RED,
    "Someone re-used this signature. Someone is attempting a replay attack or poorly forging signatures."
  );
  @Unique
  private static final ExecutorService VERIFIER_EXECUTOR = Executors.newSingleThreadExecutor(DAEMON_THREAD_FACTORY);
  @Shadow
  @Final
  private Minecraft minecraft;

  @Unique
  private static GuiMessageTag createTag(ChatFormatting color, String description) {
    return new GuiMessageTag(
      Objects.requireNonNull(color.getColor()),
      null,
      Component.literal(description),
      "chat-attestation"
    );
  }

  @Unique
  private Component injectComponent(String content, Component originalComponent, ChatType.Bound chatType) {
    final var matcher = SIGNED_MESSAGE_PATTERN.matcher(content);
    return matcher.matches() ? originalComponent.copy().withStyle(style -> {
      final var duck = (StyleDuck) (Object) style;
      duck.chatattestation$setGuiMessageConsumer(
        msg -> VERIFIER_EXECUTOR.submit(
          () -> handleCallback(
            msg,
            chatType,
            matcher
          )
        )
      );
      return style;
    }) : originalComponent;
  }

  @Unique
  private void handleCallback(
    @NotNull GuiMessage message,
    ChatType.Bound chatType,
    Matcher contents
  ) {
    final String name = chatType.name().getString();
    final var msg = contents.group(1);
    final var pyl = ChatEncoding.decode(contents.group(2));
    if (pyl == null) return;

    final Payload decodedPayload;

    try {
      decodedPayload = Payload.read(
        msg.getBytes(StandardCharsets.UTF_8),
        new DataInputStream(new ByteArrayInputStream(pyl))
      );
    } catch (Exception e) {
      LOGGER.warn("Failed to decode payload {}", HexFormat.of().formatHex(pyl), e);
      return;
    }

    // TODO: Use same code in signature generation
    final var response =
      SigningManager.INSTANCE.verifyClaim(
        withNonce(decodedPayload.nonce(), decodedPayload.msg()),
        decodedPayload.signature(),
        decodedPayload.key()
      );

    final SigningManager.Response.SignatureValid validSignature;
    GuiMessageTag newTag = null;
    Component newContent = null;
    final var chatComponentAccessor = (ChatComponentAccessor) this.minecraft.gui.getChat();

    try {
      switch (response) {
        case SigningManager.Response.InvalidSignature invalidSignature -> {
          newTag = INVALID_SIGNATURE;
          return;
        }
        case SigningManager.Response.NoSuchKey noSuchKey -> {
          newTag = NO_SUCH_KEY;
          return;
        }
        case SigningManager.Response.ReusedSignature reusedSignature -> {
          newTag = REUSED_SIGNATURE;
          return;
        }
        case SigningManager.Response.SignatureValid valid -> {
          validSignature = valid;
        }
      }

      final var key = validSignature.key();
      final var authenticated = key.manifestUrlToClaims()
        .entrySet()
        .stream()
        .filter(
          entry ->
            entry.getValue()
              .stream()
              .anyMatch(claim -> claim.equalsIgnoreCase(name.trim()))
        )
        .findAny()
        .orElse(null);

      try {
        final var clientAudience = MinecraftClientAudiences.of();

        newContent = clientAudience.asNative(
          clientAudience.asAdventure(message.content()).replaceText(TextReplacementConfig.builder()
            .matchLiteral(contents.group())
            .replacement(decodedPayload.getReplacementMsg())
            .build()
          )
        );
      } catch (IOException e) {
        LOGGER.warn("Failed to decode replacement content", e);
      }

      if (authenticated != null) {
        newTag =
          createTag(
            ChatFormatting.GREEN,
            "Verified by " + URI.create(authenticated.getKey()).getHost()
          );
      } else {
        newTag =
          createTag(
            ChatFormatting.GOLD,
            "The signature was valid and made with a known key," +
              "but we couldn't verify that the key is authorized to use that name." +
              "Permitted usernames for this key: " + Arrays.toString(
              key.manifestUrlToClaims()
                .values()
                .stream()
                .flatMap(Collection::stream).toArray()
            )
          );
      }
    } finally {
      final Component newContentFinal = newContent;
      final GuiMessageTag newTagFinal = newTag;

      this.minecraft.schedule(() -> {
        final var allMessages = chatComponentAccessor.getAllMessages();
        final MutableComponent content = (newContentFinal == null ? message.content() : newContentFinal).copy()
          .withStyle(
            style -> {
              ((StyleDuck) (Object) style).chatattestation$setGuiMessageConsumer(null);
              return style;
            }
          );

        final var clone = new GuiMessage(
          message.addedTime(),
          content,
          message.signature(),
          newTagFinal
        );
        final int idx = allMessages.indexOf(message);
        if (idx == -1) return;
        allMessages.set(allMessages.indexOf(message), clone);
        chatComponentAccessor.invokeRefreshTrimmedMessages();
      });
    }
  }

  @WrapOperation(
    method = "method_45745",
    at = @At(
      value = "INVOKE",
      target =
        "Lnet/minecraft/client/gui/components/ChatComponent;" +
          "addMessage(Lnet/minecraft/network/chat/Component;)V"
    )
  )
  private void onHandleDisguisedChatMessage(
    ChatComponent instance,
    Component component,
    Operation<Void> original,
    @Local(argsOnly = true) ChatType.Bound chatType
  ) {
    original.call(
      instance,
      injectComponent(component.getString(), component, chatType)
    );
  }

  @WrapOperation(
    method = "showMessageToPlayer",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/gui/components/ChatComponent;" +
        "addMessage(Lnet/minecraft/network/chat/Component;" +
        "Lnet/minecraft/network/chat/MessageSignature;" +
        "Lnet/minecraft/client/GuiMessageTag;)V"
    )
  )
  private void showMessageToPlayer$addMessage(
    ChatComponent instance,
    Component component,
    MessageSignature messageSignature,
    GuiMessageTag guiMessageTag,
    Operation<Void> original,
    @Local(argsOnly = true) ChatType.Bound chatType,
    @Local(argsOnly = true) PlayerChatMessage chatMessage
  ) {
    original.call(
      instance,
      injectComponent(chatMessage.signedContent(), component, chatType),
      messageSignature,
      guiMessageTag
    );
  }
}
