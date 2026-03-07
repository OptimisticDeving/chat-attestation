package dev.optimistic.chatattestation;

import dev.optimistic.chatattestation.crypto.SigningManager;
import io.netty.buffer.ByteBuf;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.common.channel.ChannelPayloadReceiver;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.common.channel.Messenger;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.fabric.FabricMessagingEntrypoint;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jodah.expiringmap.ExpiringMap;
import net.minecraft.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MessagingEntrypointImpl implements FabricMessagingEntrypoint, ChannelPayloadReceiver<ClientPlayNetworking.Context> {
  public static final String CHANNEL_NAME = "chat-attestation/v1:stream";
  public static final Map<StreamCacheKey, ByteBuf> PAYLOAD_MAP = ExpiringMap.builder()
    .maxSize(1024)
    .expiration(1, TimeUnit.MINUTES)
    .build();
  public static volatile Messenger<ClientPlayNetworking.Context> MESSENGER_INSTANCE;

  @Override
  public void onRegistrationAvailable(@NotNull Messenger<ClientPlayNetworking.Context> messenger) {
    MESSENGER_INSTANCE = messenger;

    MESSENGER_INSTANCE.receivePayloads(CHANNEL_NAME, this);
  }

  @Override
  public void onDeregister() {
    MESSENGER_INSTANCE = null;
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
