package dev.optimistic.chatattestation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.optimistic.chatattestation.mixin.accessor.EditBoxAccessor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
  @Shadow
  protected EditBox input;
  @Unique
  private int originalLen = -1;

  @WrapOperation(
    method = "normalizeChatMessage",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/util/StringUtil;trimChatMessage(Ljava/lang/String;)Ljava/lang/String;"
    )
  )
  private String normalizeChatMessage$trimChatMessage(String string, Operation<String> original) {
    return string;
  }

  @Inject(method = "keyPressed", at = @At("HEAD"))
  private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
    final boolean isCmd = this.input.getValue().startsWith("/");
    if (this.originalLen == -1) {
      if (isCmd) return;
      this.originalLen = ((EditBoxAccessor) this.input).invokeGetMaxLength();
      this.input.setMaxLength(Integer.MAX_VALUE);
    } else if (isCmd) {
      this.input.setMaxLength(this.originalLen);
      this.originalLen = -1;
    }
  }
}
