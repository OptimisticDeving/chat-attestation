package dev.optimistic.chatattestation.mixin;

import dev.optimistic.chatattestation.duck.StyleDuck;
import net.minecraft.client.GuiMessage;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Consumer;

@Mixin(Style.class)
public abstract class StyleDuckImplMixin implements StyleDuck {
  @Unique
  private Consumer<GuiMessage> messageConsumer;

  @Override
  public void chatattestation$setGuiMessageConsumer(Consumer<GuiMessage> consumer) {
    this.messageConsumer = consumer;
  }

  @Override
  public void chatattestation$acceptGuiMessage(GuiMessage message) {
    if (this.messageConsumer == null) return;
    this.messageConsumer.accept(message);
  }
}
