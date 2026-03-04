package dev.optimistic.chatattestation.util;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public final class Util {
  private static final Pattern EXTRACTOR = Pattern.compile("^(?:<[^<>]*>|[^:]*:) (.*)$");
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
    return matcher.matches() ? matcher.group(1) : null;
  }

  public static @Nullable SystemExtraction extractSystemContent(String msg) {
    final var matcher = CHIPMUNK_EXTRACTOR.matcher(msg);
    if (!matcher.matches()) return null;
    return new SystemExtraction(matcher.group(1), matcher.group(2));
  }

  public record SystemExtraction(String sender, String content) {

  }
}
