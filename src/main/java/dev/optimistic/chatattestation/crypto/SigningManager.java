package dev.optimistic.chatattestation.crypto;

import com.google.common.primitives.Longs;
import com.google.gson.reflect.TypeToken;
import dev.optimistic.chatattestation.KeyManifest;
import dev.optimistic.chatattestation.config.ConfigurationManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.math.ec.rfc8032.Ed25519;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import static dev.optimistic.chatattestation.util.Constants.*;

public final class SigningManager {
  public static final SigningManager INSTANCE = new SigningManager();
  public final byte[] selfHash;
  private final ExecutorService requestExecutor = Executors.newCachedThreadPool();
  private final StampedLock lock = new StampedLock();
  private final Map<String, KeyManifest> manifests;
  private final Map<WrappedByteArray, LoadedKey> hashToKey = new HashMap<>();
  private final Long2ObjectAVLTreeMap<Set<WrappedByteArray>> usedSignatureMap = new Long2ObjectAVLTreeMap<>();
  private final Path cachePath;
  private final Ed25519PrivateKeyParameters selfKey;

  private SigningManager() {
    final var gameDir = LOADER.getGameDir();
    this.cachePath = gameDir.resolve("data").resolve(MOD_ID + "-cache.json");

    final var cacheParent = this.cachePath.getParent();
    final boolean orphan = Files.notExists(cacheParent);

    try {
      if (orphan) Files.createDirectories(cacheParent);

      if (orphan || Files.notExists(this.cachePath)) {
        this.manifests = new HashMap<>();
      } else {
        this.manifests = GSON.fromJson(
          Files.newBufferedReader(this.cachePath),
          new TypeToken<>() {

          }
        );
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to initialize cache", e);
    }

    final var keyPath = gameDir.resolve("chat-attestation.key");

    if (Files.notExists(keyPath)) {
      try {
        this.selfKey = new Ed25519PrivateKeyParameters(SecureRandom.getInstanceStrong());
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalArgumentException("JVM does not provide strong secure random impl", e);
      }

      try {
        Files.write(keyPath, this.selfKey.getEncoded());
      } catch (IOException e) {
        throw new IllegalStateException("Failed to save private key", e);
      }
    } else {
      try {
        this.selfKey = new Ed25519PrivateKeyParameters(Files.readAllBytes(keyPath));
      } catch (IOException e) {
        throw new IllegalArgumentException("Failed to load key from file", e);
      }
    }

    final var encodedPublicKey = this.selfKey.generatePublicKey().getEncoded();
    this.selfHash = createHash(encodedPublicKey);
    LOGGER.info(
      "Encoded public key: {}",
      Base64.getEncoder().encodeToString(encodedPublicKey)
    );

    @SuppressWarnings("resource") final var scheduler = Executors.newSingleThreadScheduledExecutor(DAEMON_THREAD_FACTORY);
    scheduler.scheduleAtFixedRate(this::refetchKeys, 0, 1, TimeUnit.MINUTES);
    scheduler.scheduleAtFixedRate(this::gcOldPeriods, 0, 15, TimeUnit.SECONDS);
  }

  public static void init() {

  }

  private static byte[] createHash(byte[] input) {
    final MessageDigest messageDigest;
    try {
      messageDigest = MessageDigest.getInstance("SHA3-224");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JVM does not have SHA3-224 support", e);
    }

    return Arrays.copyOfRange(messageDigest.digest(input), 0, 16);
  }

  private static long getCurrentPeriod() {
    return System.currentTimeMillis() / 5_000;
  }

  private static byte[] withPeriod(byte[] msg) {
    final byte[] withPeriod = new byte[8 + msg.length];
    System.arraycopy(Longs.toByteArray(getCurrentPeriod()), 0, withPeriod, 0, 8);
    System.arraycopy(msg, 0, withPeriod, 8, msg.length);
    return withPeriod;
  }

  private CompletableFuture<KeyManifest> refetchManifest(String url) {
    return CompletableFuture.supplyAsync(() -> {
      try (final var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
        final var request = client.send(
          HttpRequest.newBuilder(URI.create(url)).GET().build(),
          HttpResponse.BodyHandlers.ofString()
        );

        return GSON.fromJson(request.body(), KeyManifest.class);
      } catch (Exception e) {
        throw new CompletionException("Failed to fetch manifest", e);
      }
    }, this.requestExecutor);
  }

  public void refetchKeys() {
    final var futures = ConfigurationManager.INSTANCE.config.keyManifestUrls.stream()
      .collect(Collectors.toMap(url -> url, this::refetchManifest));

    for (final var futureEntry : futures.entrySet()) {
      final String manifest = futureEntry.getKey();

      try {
        this.manifests.put(manifest, futureEntry.getValue().join());
      } catch (Exception e) {
        LOGGER.warn("Fetching {} failed, cache will be used if present", manifest, e);
      }
    }

    try (final var writer = Files.newBufferedWriter(this.cachePath)) {
      GSON.toJson(this.manifests, writer);
      writer.flush();
    } catch (IOException e) {
      LOGGER.warn("Failed to save manifest cache", e);
    }

    this.reloadKeys();
  }

  private void reloadKeys() {
    final long writeLock = this.lock.writeLock();

    try {
      this.hashToKey.clear();

      for (final var entry : this.manifests.entrySet()) {
        final var manifestUrl = entry.getKey();

        for (final var keyToClaim : entry.getValue().keyToClaims().entrySet()) {
          final var rawKey = Base64.getDecoder()
            .decode(keyToClaim.getKey());
          final var loadedKey = this.hashToKey.computeIfAbsent(
            new WrappedByteArray(
              createHash(rawKey)
            ),
            k -> new LoadedKey(
              new Ed25519PublicKeyParameters(rawKey),
              new HashMap<>()
            )
          );

          final var claims = loadedKey.manifestUrlToClaims.computeIfAbsent(manifestUrl, k -> new HashSet<>());
          claims.addAll(keyToClaim.getValue());
        }
      }
    } finally {
      this.lock.unlockWrite(writeLock);
    }
  }

  // Can only be called by one thread at a time.
  public Response verifyClaim(byte[] msg, byte[] sig, byte[] key) {
    synchronized (this.usedSignatureMap) {
      final var usedSignatures = this.usedSignatureMap.computeIfAbsent(getCurrentPeriod(), k -> new HashSet<>());
      final var wrappedSig = new WrappedByteArray(sig);
      if (usedSignatures.contains(wrappedSig)) return Response.ReusedSignature.INSTANCE;
      final var wrappedKey = new WrappedByteArray(key);
      final var loadedKey = this.hashToKey.get(wrappedKey);
      if (loadedKey == null) return Response.NoSuchKey.INSTANCE;
      msg = withPeriod(msg);

      if (!loadedKey.bc.verify(Ed25519.Algorithm.Ed25519, null, msg, 0, msg.length, sig, 0))
        return Response.InvalidSignature.INSTANCE;

      usedSignatures.add(wrappedSig);
      return new Response.SignatureValid(loadedKey);
    }
  }

  private void gcOldPeriods() {
    synchronized (this.usedSignatureMap) {
      final long currentPeriod = getCurrentPeriod();
      final var iterator = this.usedSignatureMap.sequencedEntrySet().iterator();
      int count = 0;

      while (iterator.hasNext()) {
        if (iterator.next().getKey() >= currentPeriod) continue;
        count++;
        iterator.remove();
      }

      if (LOADER.isDevelopmentEnvironment()) LOGGER.info("Removed {} old entries", count);
    }
  }

  public byte[] createSignature(byte[] msg) {
    msg = withPeriod(msg);
    final byte[] signature = new byte[64];
    this.selfKey.sign(Ed25519.Algorithm.Ed25519, null, msg, 0, msg.length, signature, 0);
    return signature;
  }

  public sealed interface Response {
    final class NoSuchKey implements Response {
      public static final NoSuchKey INSTANCE = new NoSuchKey();

      private NoSuchKey() {

      }
    }

    final class InvalidSignature implements Response {
      public static final InvalidSignature INSTANCE = new InvalidSignature();

      private InvalidSignature() {

      }
    }

    final class ReusedSignature implements Response {
      public static final ReusedSignature INSTANCE = new ReusedSignature();

      private ReusedSignature() {

      }
    }

    record SignatureValid(LoadedKey key) implements Response {

    }
  }

  public static final class WrappedByteArray {
    private final byte[] arr;

    public WrappedByteArray(byte @Unmodifiable [] arr) {
      this.arr = arr;
    }

    @Override
    public String toString() {
      return HexFormat.of().formatHex(this.arr);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(this.arr);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof final WrappedByteArray wrappedByteArray && Arrays.equals(wrappedByteArray.arr, this.arr);
    }
  }


  public record LoadedKey(Ed25519PublicKeyParameters bc, Map<String, Set<String>> manifestUrlToClaims) {

  }
}
