package dev.optimistic.chatattestation;

import dev.optimistic.chatattestation.config.ConfigurationManager;
import dev.optimistic.chatattestation.crypto.Payload;
import dev.optimistic.chatattestation.crypto.SigningManager;
import io.netty.buffer.ByteBuf;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.fabric.FabricMessagingEntrypoint;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.fabric.FabricMessenger;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.fabric.FabricPayloadReceiver;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MessagingEntrypointImpl implements FabricMessagingEntrypoint, FabricPayloadReceiver {
  public static final String CHANNEL_NAME = "chat-attestation/v1:stream";
  public static final Map<SigningManager.WrappedByteArray, ByteBuf> RECENT_PAYLOADS = ExpiringMap.builder()
    .maxSize(1024)
    .expiration(1, TimeUnit.MINUTES)
    .asyncExpirationListener(MessagingEntrypointImpl::onExpire)
    .build();
  public static final Map<UUID, Map<SigningManager.WrappedByteArray, ByteBuf>> ATTRIBUTED_PAYLOADS = ExpiringMap.builder()
    .maxSize(256)
    .expiration(5, TimeUnit.MINUTES)
    .expirationPolicy(ExpirationPolicy.ACCESSED)
    .build();
  public static volatile FabricMessenger MESSENGER_INSTANCE;
  private static volatile MessagingEntrypointImpl INSTANCE;

  public static void registerSelfPayload(
    ClientPacketListener packetListener,
    byte[] contentHash,
    ByteBuf payload
  ) {
    final var key = new SigningManager.WrappedByteArray(contentHash);
    getAttributionMap(packetListener.getLocalGameProfile().id()).put(key, payload.copy());
    RECENT_PAYLOADS.put(key, payload);
  }

  private static Map<SigningManager.WrappedByteArray, ByteBuf> getAttributionMap(UUID sender) {
    return ATTRIBUTED_PAYLOADS.computeIfAbsent(
      sender,
      _ -> ExpiringMap.builder()
        .maxSize(16)
        .expiration(1, TimeUnit.MINUTES)
        .asyncExpirationListener(MessagingEntrypointImpl::onExpire)
        .build()
    );
  }

  private static void onExpire(SigningManager.WrappedByteArray byteArray, ByteBuf buf) {
    buf.release();
  }

  public static void reregister() {
    if (MESSENGER_INSTANCE == null || INSTANCE == null) return;

    INSTANCE.register();
  }

  private void register() {
    MESSENGER_INSTANCE.receivePayloads(
      CHANNEL_NAME,
      this,
      (short) (Payload.FIXED_PAYLOAD_LENGTH + ConfigurationManager.INSTANCE.config.maxCompressedPayload)
    );
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

    RECENT_PAYLOADS.values().forEach(ByteBuf::release);
    RECENT_PAYLOADS.clear();

    ATTRIBUTED_PAYLOADS.values()
      .stream()
      .flatMap(map -> map.values().stream())
      .forEach(ByteBuf::release);
    ATTRIBUTED_PAYLOADS.clear();
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
    RECENT_PAYLOADS.put(wrapper, payload.copy());
    getAttributionMap(sender).put(wrapper, payload.copy());
  }

  // TODO: This won't work very well with vanished players.
  public record StreamCacheKey(SigningManager.WrappedByteArray contentHash, UUID sender) {

  }
}
