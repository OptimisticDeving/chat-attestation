package dev.optimistic.chatattestation.config.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import static dev.optimistic.chatattestation.util.Constants.LOADER;

public final class ModMenuIntegration implements ModMenuApi {
  private final ModMenuApi delegate = LOADER.isModLoaded("cloth-config") ?
    new PresentModMenuIntegration() : new FallbackModMenuIntegration();

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return this.delegate.getModConfigScreenFactory();
  }
}
