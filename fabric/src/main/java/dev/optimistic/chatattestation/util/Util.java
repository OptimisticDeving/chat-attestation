package dev.optimistic.chatattestation.util;

import dev.optimistic.chatattestation.config.ConfigurationManager;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public final class Util {
  private static final Pattern EXTRACTOR = Pattern.compile("^<?([^:<>]*)[>:] (.*)$");
  private static final Pattern CHIPMUNK_EXTRACTOR = Pattern.compile("^\\[.*] ([^›]*) › (.*)$");
  private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

  private Util() {

  }

  public static @Nullable String extractDisguisedContent(String fullMsg, ChatType.Bound boundChatType) {
    try {
      return extractDisguisedContent0(fullMsg, boundChatType);
    } catch (Throwable ex) {
      LOGGER.warn("Failed to extract disguised content", ex);
      return null;
    }
  }

  private static @Nullable String extractDisguisedContent0(String fullMsg, ChatType.Bound boundChatType) {
    final var params = boundChatType.chatType().value().chat().parameters();
    // Clear separation between content and sender! We win!
    if (params.contains(ChatTypeDecoration.Parameter.CONTENT) && params.contains(ChatTypeDecoration.Parameter.SENDER)) {
      return fullMsg;
    }

    final var matcher = EXTRACTOR.matcher(fullMsg);
    return matcher.matches() ? matcher.group(2) : null;
  }

  private static @Nullable SystemExtraction extractSystemContent0(String msg) {
    final var chipmunkMatcher = CHIPMUNK_EXTRACTOR.matcher(msg);
    if (chipmunkMatcher.matches()) {
      return new SystemExtraction(chipmunkMatcher.group(1), chipmunkMatcher.group(2));
    } else {
      final var regularExtractor = EXTRACTOR.matcher(msg);
      if (!regularExtractor.matches()) return null;
      // TODO: Handle spaces in names.
      final var sender = regularExtractor.group(1).split(" ");
      if (sender.length == 1 && ConfigurationManager.INSTANCE.config.ignoreCspyLike) return null;
      return new SystemExtraction(sender.length == 0 ? "" : sender[sender.length - 1], regularExtractor.group(2));
    }
  }

  public static @Nullable SystemExtraction extractSystemContent(String msg) {
    try {
      return extractSystemContent0(msg);
    } catch (Throwable ex) {
      LOGGER.warn("Failed to extract system content", ex);
      return null;
    }
  }

  public record SystemExtraction(String sender, String content) {

  }
}
