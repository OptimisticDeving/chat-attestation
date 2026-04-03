package dev.optimistic.chatattestation.config.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

public final class FallbackModMenuIntegration implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return FallbackScreen::new;
  }

  private static final class FallbackScreen extends Screen {
    private static final Component TITLE =
      Component.literal("Please install Cloth Config API to configure chat-attestation in game");
    private final Screen parent;
    private final Button back = Button.builder(CommonComponents.GUI_BACK, b -> this.onClose())
      .bounds(0, this.font.lineHeight + 2, 200, 20)
      .build();

    private FallbackScreen(Screen parent) {
      super(TITLE);
      this.parent = parent;
    }

    @Override
    protected void init() {
      this.addRenderableWidget(back);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
      super.render(guiGraphics, i, j, f);

      guiGraphics.drawString(this.font, TITLE, 0, 0, ARGB.white(255));
    }

    @Override
    public void onClose() {
      Minecraft.getInstance().setScreen(this.parent);
    }
  }
}
