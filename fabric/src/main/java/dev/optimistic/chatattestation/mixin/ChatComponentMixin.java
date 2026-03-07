package dev.optimistic.chatattestation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.optimistic.chatattestation.duck.StyleDuck;
import dev.optimistic.chatattestation.util.ListeningList;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
  @Shadow
  @Final
  @Mutable
  private List<GuiMessage> allMessages;

  @WrapOperation(
    method = "<init>",
    at = @At(
      value = "FIELD",
      target = "Lnet/minecraft/client/gui/components/ChatComponent;allMessages:Ljava/util/List;",
      opcode = Opcodes.PUTFIELD
    )
  )
  private void init$setAllMessages(
    ChatComponent instance,
    List<GuiMessage> value,
    Operation<Void> original
  ) {
    // Instead of injecting into the public add chat message API, we create a wrapper around the allMessages list.
    // If a mod bypasses the public API with an access widener/accessor mixin, we will know that the message has been added even so.
    original.call(instance, new ListeningList<>(message ->
      ((StyleDuck) (Object) message.content().getStyle()).chatattestation$acceptGuiMessage(message), value)
    );
  }
}
