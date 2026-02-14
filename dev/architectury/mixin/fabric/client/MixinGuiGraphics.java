/*
 * This file is part of architectury.
 * Copyright (C) 2020, 2021, 2022 architectury
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package dev.architectury.mixin.fabric.client;

import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.impl.TooltipEventColorContextImpl;
import dev.architectury.impl.TooltipEventPositionContextImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.class_1799;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_5684;
import net.minecraft.class_8000;

@Mixin(class_332.class)
public abstract class MixinGuiGraphics {
    @Unique
    private static ThreadLocal<TooltipEventPositionContextImpl> tooltipPositionContext = ThreadLocal.withInitial(TooltipEventPositionContextImpl::new);
    
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void preRenderTooltipItem(class_327 font, class_1799 stack, int x, int y, CallbackInfo ci) {
        ClientTooltipEvent.additionalContexts().setItem(stack);
    }
    
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
    private void postRenderTooltipItem(class_327 font, class_1799 stack, int x, int y, CallbackInfo ci) {
        ClientTooltipEvent.additionalContexts().setItem(null);
    }
    
    @Inject(method = "renderTooltipInternal", at = @At("HEAD"), cancellable = true)
    private void renderTooltip(class_327 font, List<? extends class_5684> list, int x, int y, class_8000 positioner, CallbackInfo ci) {
        if (!list.isEmpty()) {
            var colorContext = TooltipEventColorContextImpl.CONTEXT.get();
            colorContext.reset();
            var positionContext = tooltipPositionContext.get();
            positionContext.reset(x, y);
            if (ClientTooltipEvent.RENDER_PRE.invoker().renderTooltip((class_332) (Object) this, list, x, y).isFalse()) {
                ci.cancel();
            } else {
                ClientTooltipEvent.RENDER_MODIFY_COLOR.invoker().renderTooltip((class_332) (Object) this, x, y, colorContext);
                ClientTooltipEvent.RENDER_MODIFY_POSITION.invoker().renderTooltip((class_332) (Object) this, positionContext);
            }
        }
    }
    
    @ModifyVariable(method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    private int modifyTooltipX(int original) {
        return tooltipPositionContext.get().getTooltipX();
    }
    
    @ModifyVariable(method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V",
            at = @At(value = "HEAD"), ordinal = 1, argsOnly = true)
    private int modifyTooltipY(int original) {
        return tooltipPositionContext.get().getTooltipY();
    }
}
