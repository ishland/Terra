/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.fabric.mixin.lifecycle.server;

import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dfsek.terra.api.event.events.platform.PlatformInitializationEvent;
import com.dfsek.terra.fabric.FabricEntryPoint;


@Mixin(Main.class)
public class ServerMainMixin {
    @Inject(method = "main([Ljava/lang/String;)V",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/util/registry/DynamicRegistryManager;create()" +
                              "Lnet/minecraft/util/registry/DynamicRegistryManager$Impl;"))
    private static void injectConstructor(String[] args, CallbackInfo ci) {
        FabricEntryPoint.getPlatform().getEventManager().callEvent(
                new PlatformInitializationEvent()); // Load during MinecraftServer construction, after other mods have registered blocks
        // and stuff
    }
}
