package dev.optimistic.chatattestation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.optimistic.chatattestation.config.ConfigurationManager;
import dev.optimistic.chatattestation.util.ChatEncoding;
import dev.optimistic.chatattestation.util.Payload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.minecraft.util.StringUtil.trimChatMessage;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
  @Unique
  private static final Component MSG_TOO_LARGE = Component.literal("Message too large.")
    .withStyle(ChatFormatting.RED);
  @Unique
  private static final ExecutorService SIGN_EXECUTOR = Executors.newSingleThreadExecutor();

  @WrapMethod(method = "sendChat")
  private void onSendChat(
    String string,
    Operation<Void> original
  ) {
    if (!ConfigurationManager.INSTANCE.config.toggleForSelf) {
      original.call(trimChatMessage(string));
      return;
    }

    final var mc = Minecraft.getInstance();

    SIGN_EXECUTOR.submit(() -> {
      final var payload = new ByteArrayOutputStream();
      final boolean compressed;
      try {
        compressed = Payload.write(string.getBytes(StandardCharsets.UTF_8), new DataOutputStream(payload));
      } catch (IOException e) {
        throw new IllegalArgumentException("Failed to write payload");
      }

      final var pyl = ChatEncoding.encode(payload.toByteArray());
      final var msg = "$$" + (compressed ? "" : string) + "$$" + pyl;

      if (msg.length() > 256) {
        mc.schedule(() -> mc.gui.getChat().addMessage(MSG_TOO_LARGE));
        return;
      }

      mc.schedule(() -> original.call(trimChatMessage(msg)));
    });
  }
}
