package dev.optimistic.chatattestation.util;

import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ChatTypeDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public final class Util {
  private static final Pattern EXTRACTOR = Pattern.compile("^(?:<[^<>]*>|[^:]*:) (.*)$");

  private Util() {

  }

  public static @Nullable String extractContent(String fullMsg, ChatType.Bound boundChatType) {
    final var params = boundChatType.chatType().value().chat().parameters();
    // Clear separation between content and sender! We win!
    if (params.contains(ChatTypeDecoration.Parameter.CONTENT) && params.contains(ChatTypeDecoration.Parameter.SENDER)) {
      return fullMsg;
    }

    final var matcher = EXTRACTOR.matcher(fullMsg);
    return matcher.matches() ? matcher.group(1) : null;
  }
}
