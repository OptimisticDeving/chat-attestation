package dev.optimistic.chatattestation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.net.http.HttpResponse;

@NullMarked
public record KeyManifestDisk(Cache cache, KeyManifest manifest) {
  public record Cache(@Nullable String etag, @Nullable String lastModified) {
    public static final Cache EMPTY = new Cache(null, null);

    public static Cache from(HttpResponse<?> response) {
      final var headers = response.headers();
      final var etag = headers.firstValue("ETag").orElse(null);
      final var lastModified = headers.firstValue("Last-Modified").orElse(null);

      if (etag == null && lastModified == null) return EMPTY;

      // TODO: Handle header values that are very long? Players should only add trusted manifests to their list,
      //  so this shouldn't be a problem.

      return new KeyManifestDisk.Cache(etag, lastModified);
    }
  }
}
