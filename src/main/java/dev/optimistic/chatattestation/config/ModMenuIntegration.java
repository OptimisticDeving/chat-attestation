package dev.optimistic.chatattestation.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.optimistic.chatattestation.crypto.SigningManager;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class ModMenuIntegration implements ModMenuApi {
  private static final Component TITLE = Component.literal("Chat Attestation Config");
  private static final Component MANIFEST_LIST = Component.literal("Key Manifests");
  private static final Component MANIFEST_LIST_TOOLTIP =
    Component.literal(
      "An operator can assign keys to any username they like. Ensure that you trust them. " +
        "Key origin will be shown when hovering over the tag of a chat message."
    ).withStyle(ChatFormatting.RED);
  private static final Component REMOTE = Component.literal("Remote");
  private static final Component LOCAL = Component.literal("Local");
  private static final Component TOGGLE_FOR_SELF = Component.literal("Sign own messages");
  private static final Component TOGGLE_FOR_SELF_TOOLTIP = Component.literal(
    "Controls sending of signatures for messages/commands." +
      " You will still be able to receive & verify signatures of other players."
  );
  private static final Component FORCE_FALLBACK = Component.literal("Force fallback encoding");
  private static final Component FORCE_FALLBACK_TOOLTIP = Component.literal(
    "Forces the fallback (in-line chat encoding) to be used," +
      " even in cases where Extras messaging is present on the server." +
      " Note that Disable Fallback must be off for this to come into effect."
  );
  private static final Component CHAT_TRUNC = Component.literal("Chat message max characters");
  private static final Component CHAT_TRUNC_TOOLTIP = Component.literal(
    "Maximum length for decompressed chat messages before truncation." +
      " This does not limit how big the decompressed payload can be. "
  );
  private static final Component MAX_COMPRESSED_PAYLOAD_LEN = Component.literal("Max compressed payload length");
  private static final Component MAX_COMPRESSED_PAYLOAD_TOOLTIP = Component.literal(
    "The maximum decompressed payload before halting." +
      " It is recommended to set this to *4 the max characters count." +
      " A malicious user could easily compress a huge amount of bytes into a very small compressed payload," +
      " so this option is necessary to avoid attacks like those."
  );
  private static final Component DISABLE_FALLBACK = Component.literal("Disable automatic fallback");
  private static final Component DISABLE_FALLBACK_TOOLTIP = Component.literal(
    "Completely disables fallback chat encoding." +
      " This is enabled by default due to user complaints about the unreadability for users who do not have the"
      + " mod installed."
  );
  private static final Component FORCE_COMPRESSION = Component.literal("Force compression for all messages");

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> {
      final var builder = ConfigBuilder.create()
        .setParentScreen(parent)
        .setTitle(TITLE);

      builder.getOrCreateCategory(REMOTE)
        .addEntry(
          builder
            .entryBuilder()
            .startStrList(MANIFEST_LIST, ConfigurationManager.INSTANCE.config.keyManifestUrls)
            .setTooltip(MANIFEST_LIST_TOOLTIP)
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.keyManifestUrls = newValue;
              SigningManager.INSTANCE.refetchKeys();
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        );

      builder.getOrCreateCategory(LOCAL)
        .addEntry(
          builder
            .entryBuilder()
            .startBooleanToggle(TOGGLE_FOR_SELF, ConfigurationManager.INSTANCE.config.toggleForSelf)
            .setTooltip(TOGGLE_FOR_SELF_TOOLTIP)
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.toggleForSelf = newValue;
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        )
        .addEntry(
          builder
            .entryBuilder()
            .startBooleanToggle(FORCE_FALLBACK, ConfigurationManager.INSTANCE.config.forceFallback)
            .setTooltip(FORCE_FALLBACK_TOOLTIP)
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.forceFallback = newValue;
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        )
        .addEntry(
          builder
            .entryBuilder()
            .startIntField(CHAT_TRUNC, ConfigurationManager.INSTANCE.config.chatMsgTrunc)
            .setTooltip(CHAT_TRUNC_TOOLTIP)
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.chatMsgTrunc = newValue;
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        )
        .addEntry(
          builder
            .entryBuilder()
            .startIntField(MAX_COMPRESSED_PAYLOAD_LEN, ConfigurationManager.INSTANCE.config.maxCompressedPayload)
            .setTooltip(MAX_COMPRESSED_PAYLOAD_TOOLTIP)
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.maxCompressedPayload = newValue;
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        )
        .addEntry(
          builder
            .entryBuilder()
            .startBooleanToggle(DISABLE_FALLBACK, ConfigurationManager.INSTANCE.config.disableFallback)
            .setTooltip(DISABLE_FALLBACK_TOOLTIP)
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.disableFallback = newValue;
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        ).addEntry(
          builder
            .entryBuilder()
            .startBooleanToggle(FORCE_COMPRESSION, ConfigurationManager.INSTANCE.config.forceCompress)
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.forceCompress = newValue;
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        );

      return builder.build();
    };
  }
}
