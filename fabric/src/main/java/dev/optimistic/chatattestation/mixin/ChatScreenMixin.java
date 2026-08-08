package dev.optimistic.chatattestation.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.optimistic.chatattestation.config.Configuration;
import dev.optimistic.chatattestation.config.ConfigurationManager;
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
  private String normalizeChatMessage$trimChatMessage(String message, Operation<String> original) {
    return message;
  }

  @Inject(method = "keyPressed", at = @At("HEAD"))
  private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
    final boolean cannotCompress = this.input.getValue().startsWith("/")
      || ConfigurationManager.INSTANCE.config.disableCompress;

    if (this.originalLen == -1) {
      if (cannotCompress) return;
      this.originalLen = ((EditBoxAccessor) this.input).invokeGetMaxLength();
      this.input.setMaxLength(Integer.MAX_VALUE);
    } else if (cannotCompress) {
      this.input.setMaxLength(this.originalLen);
      this.originalLen = -1;
    }
  }
}
