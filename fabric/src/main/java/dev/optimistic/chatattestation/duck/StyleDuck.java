package dev.optimistic.chatattestation.duck;

import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.util.function.Consumer;

public interface StyleDuck {
  void chatattestation$setGuiMessageConsumer(Consumer<GuiMessage> consumer);

  void chatattestation$acceptGuiMessage(GuiMessage message);
}
