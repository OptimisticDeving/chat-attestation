package dev.optimistic.chatattestation.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.regex.Pattern;

public final class Constants {
  public static final String MOD_ID = "chat-attestation";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  public static final FabricLoader LOADER = FabricLoader.getInstance();
  public static final Pattern SIGNED_MESSAGE_PATTERN = Pattern.compile("\\$\\$(.*)\\$\\$(.*)$");
  public static final ThreadFactory DAEMON_THREAD_FACTORY = r -> {
    final var t = new Thread(r);
    t.setDaemon(true);
    return t;
  };

  private Constants() {

  }
}
