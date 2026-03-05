package dev.optimistic.chatattestation.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.optimistic.chatattestation.MessagingEntrypointImpl;
import dev.optimistic.chatattestation.config.ConfigurationManager;
import dev.optimistic.chatattestation.crypto.Payload;
import dev.optimistic.chatattestation.crypto.SigningManager;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static dev.optimistic.chatattestation.MessagingEntrypointImpl.CHANNEL_NAME;
import static dev.optimistic.chatattestation.MessagingEntrypointImpl.PAYLOAD_MAP;
import static dev.optimistic.chatattestation.crypto.SigningManager.createHash;
import static io.netty.buffer.Unpooled.buffer;

// Intercept here instead of in ClientPacketListener for compatibility with ChipmunkMod.
// Most compatible method would be via a netty outbound handler, but I don't want to.
@Mixin(Connection.class)
public abstract class ConnectionMixin {
  @WrapMethod(method = "doSendPacket")
  private void onDoSendPacket(
    Packet<?> packet,
    @Nullable ChannelFutureListener channelFutureListener,
    boolean bl,
    Operation<Void> original
  ) {
    if (
      !ConfigurationManager.INSTANCE.config.toggleForSelf
        || (MessagingEntrypointImpl.MESSENGER_INSTANCE == null && ConfigurationManager.INSTANCE.config.disableFallback)
    ) {
      original.call(packet, channelFutureListener, bl);
      return;
    }

    final String cmd;
    if (packet instanceof ServerboundChatCommandPacket(final String command)) {
      cmd = "/" + command;
    } else if (packet instanceof final ServerboundChatCommandSignedPacket signedCommandPacket) {
      cmd = "/" + signedCommandPacket.command();
    } else {
      original.call(packet, channelFutureListener, bl);
      return;
    }

    if (MessagingEntrypointImpl.MESSENGER_INSTANCE == null || ConfigurationManager.INSTANCE.config.forceFallback) {
      original.call(packet, channelFutureListener, bl);
      return;
    }

    final var payload = new ByteArrayOutputStream();
    try {
      Payload.write(cmd.getBytes(StandardCharsets.UTF_8), new DataOutputStream(payload));
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to write payload");
    }

    final var mc = Minecraft.getInstance();
    mc.schedule(() -> {
      final var buf = buffer();
      final var contentHash = createHash(cmd.getBytes(StandardCharsets.UTF_8));
      buf.writeBytes(contentHash);
      buf.writeBytes(payload.toByteArray());
      MessagingEntrypointImpl.MESSENGER_INSTANCE.sendMessage(CHANNEL_NAME, buf.copy());

      final var conn = mc.getConnection();
      if (conn == null) return;
      final var key = new SigningManager.WrappedByteArray(contentHash);

      buf.skipBytes(16);
      PAYLOAD_MAP.put(new MessagingEntrypointImpl.StreamCacheKey(key, conn.getLocalGameProfile().id()), buf.copy());
      PAYLOAD_MAP.put(new MessagingEntrypointImpl.StreamCacheKey(key, Util.NIL_UUID), buf);

      original.call(packet, channelFutureListener, bl);
    });
  }
}
