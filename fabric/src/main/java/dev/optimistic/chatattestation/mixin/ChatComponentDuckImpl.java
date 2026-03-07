package dev.optimistic.chatattestation.mixin;

import dev.optimistic.chatattestation.duck.ChatComponentDuck;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChatComponent.class)
public abstract class ChatComponentDuckImpl implements ChatComponentDuck {
  @Shadow
  private int chatScrollbarPos;

  @Shadow
  protected abstract void refreshTrimmedMessages();

  @Shadow
  public abstract void scrollChat(int i);

  @Override
  public void chatattestation$refresh() {
    final int currentScrollPos = this.chatScrollbarPos;
    this.refreshTrimmedMessages();
    this.scrollChat(currentScrollPos);
  }
}
