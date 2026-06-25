package pl.kosma.mapchameleon.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import pl.kosma.mapchameleon.MapChameleonMod_Legacy;

/**
 * Legacy mixin for MC 1.20–1.20.4.
 * Identical injection point but calls the legacy entrypoint.
 */
@Mixin(PlayerManager.class)
public class MixinPlayerManager_Legacy {
    @Inject(
        at = @At("HEAD"),
        method = "sendWorldInfo(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/world/ServerWorld;)V"
    )
    public void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo info) {
        MapChameleonMod_Legacy.onServerWorldInfo(player);
    }
}
