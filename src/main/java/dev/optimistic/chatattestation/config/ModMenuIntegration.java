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
  private static final Component REMOTE = Component.literal("Remote");
  private static final Component LOCAL = Component.literal("Local");
  private static final Component TOGGLE_FOR_SELF = Component.literal("Sign own messages");
  private static final Component FORCE_FALLBACK = Component.literal("Force fallback encoding");
  private static final Component TOOLTIP =
    Component.literal(
      "An operator can assign keys to any username they like. Ensure that you trust them. " +
        "Key origin will be shown when hovering over the tag of a chat message."
    ).withStyle(ChatFormatting.RED);

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
            .setTooltip(TOOLTIP)
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
            .setSaveConsumer(newValue -> {
              ConfigurationManager.INSTANCE.config.forceFallback = newValue;
              ConfigurationManager.INSTANCE.save();
            })
            .build()
        );

      return builder.build();
    };
  }
}
