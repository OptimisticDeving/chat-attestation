package dev.optimistic.chatattestation.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

import static dev.optimistic.chatattestation.util.Constants.LOADER;

@ConfigSerializable
public final class Configuration {
  private static final String DEFAULT_MANIFEST = "https://opt.chipmunk.land/key-manifest-v1.json";

  public volatile List<String> keyManifestUrls =
    LOADER.isDevelopmentEnvironment() ?
      List.of("http://localhost:8080/key-manifest-v1.json", DEFAULT_MANIFEST)
      :
      List.of(DEFAULT_MANIFEST);

  public volatile boolean toggleForSelf = true;
  public volatile boolean forceFallback = false;
  public volatile boolean disableFallback = true;
  public volatile int chatMsgTrunc = 2048;
  public volatile int maxCompressedPayload = this.chatMsgTrunc * 4;
}
