package dev.optimistic.chatattestation;

import net.fabricmc.api.ClientModInitializer;

import static dev.optimistic.chatattestation.util.Constants.LOADER;

public final class LaunchChecks implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    final boolean hasModMenu = LOADER.isModLoaded("modmenu");
    final boolean hasClothConfig = LOADER.isModLoaded("cloth-config");

    if (hasModMenu == hasClothConfig) return;
    throw new UnsupportedOperationException("Both modmenu & cloth-config are required, not just one.");
  }
}
