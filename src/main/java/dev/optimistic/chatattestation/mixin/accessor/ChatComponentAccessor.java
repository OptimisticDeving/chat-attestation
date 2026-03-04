package dev.optimistic.chatattestation.mixin.accessor;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
  @Invoker
  void invokeRefreshTrimmedMessages();

  @Accessor
  List<GuiMessage> getAllMessages();
}
