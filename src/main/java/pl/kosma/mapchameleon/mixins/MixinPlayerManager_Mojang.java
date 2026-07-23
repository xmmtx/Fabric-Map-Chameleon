package pl.kosma.mapchameleon.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import pl.kosma.mapchameleon.MapChameleonMod_Mojang;

/**
 * Injects into {@link PlayerList#sendLevelInfo} to push XaeroMap
 * world name whenever a player joins or switches worlds.
 *
 * <p>Uses Mojang official mappings (MC 26.x).</p>
 */
@Mixin(PlayerList.class)
public class MixinPlayerManager_Mojang {
    @Inject(
        at = @At("HEAD"),
        method = "sendLevelInfo(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerLevel;)V"
    )
    public void onSendLevelInfo(ServerPlayer player, ServerLevel world, CallbackInfo info) {
        MapChameleonMod_Mojang.onServerWorldInfo(player);
    }
}
