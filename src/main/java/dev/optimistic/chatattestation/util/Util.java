package dev.optimistic.chatattestation.util;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public final class Util {
  private static final Pattern EXTRACTOR = Pattern.compile("^<?([^:<>]*)[>:] (.*)$");
  private static final Pattern CHIPMUNK_EXTRACTOR = Pattern.compile("^\\[.*] ([^›]*) › (.*)$");

  private Util() {

  }

  public static @Nullable String extractDisguisedContent(String fullMsg, ChatType.Bound boundChatType) {
    final var params = boundChatType.chatType().value().chat().parameters();
    // Clear separation between content and sender! We win!
    if (params.contains(ChatTypeDecoration.Parameter.CONTENT) && params.contains(ChatTypeDecoration.Parameter.SENDER)) {
      return fullMsg;
    }

    final var matcher = EXTRACTOR.matcher(fullMsg);
    return matcher.matches() ? matcher.group(2) : null;
  }

  public static @Nullable SystemExtraction extractSystemContent(String msg) {
    final var chipmunkMatcher = CHIPMUNK_EXTRACTOR.matcher(msg);
    if (chipmunkMatcher.matches()) {
      return new SystemExtraction(chipmunkMatcher.group(1), chipmunkMatcher.group(2));
    } else {
      final var regularExtractor = EXTRACTOR.matcher(msg);
      if (!regularExtractor.matches()) return null;
      // TODO: Handle spaces in names.
      final var sender = regularExtractor.group(1).split(" ");
      return new SystemExtraction(sender[sender.length - 1], regularExtractor.group(2));
    }
  }

  public record SystemExtraction(String sender, String content) {

  }
}
