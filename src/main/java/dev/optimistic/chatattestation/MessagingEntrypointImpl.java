package dev.optimistic.chatattestation;

import dev.optimistic.chatattestation.crypto.SigningManager;
import io.netty.buffer.ByteBuf;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.MessagingEntrypoint;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.channel.ChannelPayloadReceiver;
import land.chipmunk.code.kaboomstandardsorganization.messaginglib.channel.Messenger;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.jodah.expiringmap.ExpiringMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class MessagingEntrypointImpl implements MessagingEntrypoint, ChannelPayloadReceiver {
  public static final String CHANNEL_NAME = "chat-attestation/v1:stream";
  public static final Map<StreamCacheKey, ByteBuf> PAYLOAD_MAP = ExpiringMap.builder()
    .maxSize(1024)
    .expiration(1, TimeUnit.MINUTES)
    .build();
  //private static final Map<StreamCacheKey, CompletableFuture<ByteBuf>> WAITING_FOR = new ConcurrentHashMap<>();
  public static volatile Messenger MESSENGER_INSTANCE;

  /*public static CompletableFuture<ByteBuf> waitFor(StreamCacheKey key) {
    if (MESSENGER_INSTANCE == null)
      return CompletableFuture.failedFuture(new IllegalStateException("No messenger instance"));

    synchronized (WAITING_FOR) {
      final CompletableFuture<ByteBuf> future = new CompletableFuture<>();
      WAITING_FOR.put(key, future);
      final var existing = PAYLOAD_MAP.remove(key);
      if (existing != null) {
        WAITING_FOR.remove(key);
        future.complete(existing);
      }

      return future.orTimeout(500, TimeUnit.MILLISECONDS).exceptionallyCompose(ex -> {
        synchronized (WAITING_FOR) {
          final var present = WAITING_FOR.get(key);
          if (present == future) WAITING_FOR.remove(key);
        }
        return CompletableFuture.failedFuture(ex);
      });
    }
  }*/

  @Override
  public void onRegistrationAvailable(Messenger messenger) {
    MESSENGER_INSTANCE = messenger;

    MESSENGER_INSTANCE.receivePayloads(CHANNEL_NAME, this);
  }

  @Override
  public void onDeregister() {
    MESSENGER_INSTANCE = null;
    PAYLOAD_MAP.clear();
    //WAITING_FOR.clear();
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

    PAYLOAD_MAP.put(new StreamCacheKey(new SigningManager.WrappedByteArray(contentHash), sender), payload);
  }

  // TODO: This won't work very well with vanished players.
  public record StreamCacheKey(SigningManager.WrappedByteArray contentHash, UUID sender) {

  }
}
