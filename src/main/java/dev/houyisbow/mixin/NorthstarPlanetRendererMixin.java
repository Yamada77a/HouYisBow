package dev.houyisbow.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Protects Northstar's client sky renderer until a local server has loaded its SERVER config. */
@Pseudo
@Mixin(targets = "com.lightning.northstar.planet.PlanetRenderer", remap = false)
abstract class NorthstarPlanetRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/multiplayer/ClientLevel;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/Camera;FFZ)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private static void houyisBow$deferSkyUntilServerConfigLoads(
            ClientLevel level,
            PoseStack pose,
            Camera camera,
            float starBrightness,
            float atmosphereBlend,
            boolean excludeSunAndMoon,
            CallbackInfo callback
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (minecraft.isLocalServer()
                && (server == null || !server.isReady() || !server.isRunning())) {
            callback.cancel();
        }
    }

}
