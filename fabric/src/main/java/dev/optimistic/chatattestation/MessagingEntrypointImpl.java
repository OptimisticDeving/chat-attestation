package dev.optimistic.chatattestation;

import dev.optimistic.chatattestation.config.ConfigurationManager;
import dev.optimistic.chatattestation.crypto.Payload;
import dev.optimistic.chatattestation.crypto.SigningManager;
import io.netty.buffer.ByteBuf;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.fabric.FabricMessagingEntrypoint;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.fabric.FabricMessenger;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.fabric.FabricPayloadReceiver;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jodah.expiringmap.ExpiringMap;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MessagingEntrypointImpl implements FabricMessagingEntrypoint, FabricPayloadReceiver {
  public static final String CHANNEL_NAME = "chat-attestation/v1:stream";
  public static final Map<StreamCacheKey, ByteBuf> PAYLOAD_MAP = ExpiringMap.builder()
    .maxSize(1024)
    .expiration(1, TimeUnit.MINUTES)
    .build();
  public static volatile FabricMessenger MESSENGER_INSTANCE;
  private static volatile MessagingEntrypointImpl INSTANCE;

  private void register() {
    MESSENGER_INSTANCE.receivePayloads(
      CHANNEL_NAME,
      this,
      (short) (Payload.FIXED_PAYLOAD_LENGTH + ConfigurationManager.INSTANCE.config.maxCompressedPayload)
    );
  }

  public static void reregister() {
    if (MESSENGER_INSTANCE == null || INSTANCE == null) return;

    INSTANCE.register();
  }

  @Override
  public void onRegistrationAvailable(@NotNull FabricMessenger messenger) {
    System.out.println("Registration available");
    MESSENGER_INSTANCE = messenger;
    INSTANCE = this;

    this.register();
  }

  @Override
  public void onDeregister() {
    MESSENGER_INSTANCE = null;
    INSTANCE = null;

    PAYLOAD_MAP.clear();
  }

  @Override
  public void onReceivePayload(
    ClientPlayNetworking.@NotNull Context context,
    @NotNull String channelName,
    @NotNull UUID sender,
    @NotNull ByteBuf payload
  ) {
    final byte[] contentHash = new byte[16];
    payload.readBytes(contentHash);

    final var wrapper = new SigningManager.WrappedByteArray(contentHash);
    PAYLOAD_MAP.put(new StreamCacheKey(wrapper, sender), payload.copy());
    PAYLOAD_MAP.put(new StreamCacheKey(wrapper, Util.NIL_UUID), payload.copy());
  }

  // TODO: This won't work very well with vanished players.
  public record StreamCacheKey(SigningManager.WrappedByteArray contentHash, UUID sender) {

  }
}
