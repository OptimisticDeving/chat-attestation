package dev.optimistic.chatattestation;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Set;

public record KeyManifest(@SerializedName("key_to_claims") Map<String, Set<String>> keyToClaims) {

}
