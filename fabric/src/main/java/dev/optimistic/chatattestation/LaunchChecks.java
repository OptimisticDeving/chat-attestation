package dev.optimistic.chatattestation;

import net.fabricmc.api.ClientModInitializer;

import static dev.optimistic.chatattestation.util.Constants.LOADER;
import static dev.optimistic.chatattestation.util.Constants.LOGGER;

public final class LaunchChecks implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    final boolean hasModMenu = LOADER.isModLoaded("modmenu");
    final boolean hasClothConfig = LOADER.isModLoaded("cloth-config");

    if (hasModMenu == hasClothConfig) return;
    LOGGER.warn("Both Mod Menu & Cloth Config should be present for the optimal experience, not just one of the two.");
  }
}
