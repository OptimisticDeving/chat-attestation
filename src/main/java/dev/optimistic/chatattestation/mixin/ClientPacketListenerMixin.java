package dev.optimistic.chatattestation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.optimistic.chatattestation.MessagingEntrypointImpl;
import dev.optimistic.chatattestation.config.ConfigurationManager;
import dev.optimistic.chatattestation.crypto.Payload;
import dev.optimistic.chatattestation.crypto.SigningManager;
import dev.optimistic.chatattestation.util.ChatEncoding;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static dev.optimistic.chatattestation.MessagingEntrypointImpl.CHANNEL_NAME;
import static dev.optimistic.chatattestation.MessagingEntrypointImpl.PAYLOAD_MAP;
import static dev.optimistic.chatattestation.crypto.SigningManager.createHash;
import static dev.optimistic.chatattestation.util.Constants.MESSAGE_LIMIT;
import static dev.optimistic.chatattestation.util.Constants.SIGN_EXECUTOR;
import static io.netty.buffer.Unpooled.buffer;
import static net.minecraft.util.StringUtil.trimChatMessage;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
  @Unique
  private static final Component MSG_TOO_LARGE = Component.literal("Message too large.")
    .withStyle(ChatFormatting.RED);

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

      final String msg;
      if (MessagingEntrypointImpl.MESSENGER_INSTANCE == null || ConfigurationManager.INSTANCE.config.forceFallback) {
        final var pyl = ChatEncoding.encode(payload.toByteArray());
        msg = "$$" + (compressed ? "" : string) + "$$" + pyl;

        if (msg.length() > MESSAGE_LIMIT) {
          mc.schedule(() -> mc.gui.getChat().addMessage(MSG_TOO_LARGE));
          return;
        }
      } else {
        final var buf = buffer();
        msg = string.length() > MESSAGE_LIMIT ? "$$" : string;

        final var contentHash = createHash(msg.getBytes(StandardCharsets.UTF_8));
        buf.writeBytes(contentHash);
        buf.writeBytes(payload.toByteArray());
        MessagingEntrypointImpl.MESSENGER_INSTANCE.sendMessage(CHANNEL_NAME, buf.copy());

        final var conn = mc.getConnection();
        if (conn == null) return;
        final var key = new SigningManager.WrappedByteArray(contentHash);

        buf.skipBytes(16);
        PAYLOAD_MAP.put(new MessagingEntrypointImpl.StreamCacheKey(key, conn.getLocalGameProfile().id()), buf.copy());
        PAYLOAD_MAP.put(new MessagingEntrypointImpl.StreamCacheKey(key, Util.NIL_UUID), buf);
      }

      mc.schedule(() -> original.call(trimChatMessage(msg)));
    });
  }
}
