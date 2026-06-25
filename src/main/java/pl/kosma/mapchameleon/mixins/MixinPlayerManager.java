package pl.kosma.mapchameleon.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import pl.kosma.mapchameleon.MapChameleonMod;

/**
 * Injects into {@link PlayerManager#sendWorldInfo} to push XaeroMap
 * world name whenever a player joins or switches worlds.
 */
@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Inject(
        at = @At("HEAD"),
        method = "sendWorldInfo(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/world/ServerWorld;)V"
    )
    public void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo info) {
        MapChameleonMod.onServerWorldInfo(player);
    }
}
