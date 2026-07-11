package pl.kosma.mapchameleon.mixin;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.kosma.mapchameleon.network.WorldNameHandler;

/**
 * Hooks into PlayerManager.sendWorldInfo to push XaeroMap
 * world name when a player switches worlds or respawns.
 */
@Mixin(PlayerManager.class)
public class MixinPlayerManager {
    @Inject(
        at = @At("HEAD"),
        method = "sendWorldInfo(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/world/ServerWorld;)V"
    )
    public void onSendWorldInfo(ServerPlayerEntity player, ServerWorld world, CallbackInfo info) {
        int worldId = Math.abs(world.getRegistryKey().getValue().hashCode());
        WorldNameHandler.sendXaeroWorldName(player, worldId);
    }
}
