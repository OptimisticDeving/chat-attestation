package dev.optimistic.chatattestation.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.List;

import static dev.optimistic.chatattestation.util.Constants.LOADER;

@ConfigSerializable
public final class Configuration {
  public volatile List<String> keyManifestUrls = List.of(
    LOADER.isDevelopmentEnvironment() ?
      "http://localhost:8080/key-manifest-v1.json"
      :
      "https://opt.chipmunk.land/key-manifest-v1.json"
  );
  public volatile boolean toggleForSelf = true;
  public volatile boolean forceFallback = false;
}
