package net.cravencraft.epicparagliders.mixins.epicfight.client;

import net.cravencraft.epicparagliders.config.ConfigManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPChangePlayerMode;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

@Mixin(LocalPlayerPatch.class)
public abstract class LocalPlayerPatchMixin extends AbstractClientPlayerPatch<LocalPlayer> {

    @Shadow private Minecraft minecraft;

    /**
     * Adds an if statement to check if the useStaminaWheel config is set or if
     * the stamina is being drained. If either are true, then the skill UI will
     * slide up. This fixes an issue where the UI would bug out if the stamina
     * bar was configured to display instead of the stamina wheel, and the user
     * toggled on and off Battle Mode.
     *
     * @param synchronize
     * @param ci
     */
    @Inject(method = "toBattleMode", at = @At("HEAD"), cancellable = true, remap = false)
    private void getStamina(boolean synchronize, CallbackInfo ci) {
        if (this.playerMode != PlayerPatch.PlayerMode.BATTLE) {

            if (ConfigManager.CLIENT_CONFIG.useStaminaWheel() || this.getStamina() == this.getMaxStamina()) {
                ClientEngine.getInstance().renderEngine.upSlideSkillUI();
            }

            if (EpicFightMod.CLIENT_CONFIGS.cameraAutoSwitch.getValue()) {
                this.minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }

            if (synchronize) {
                EpicFightNetworkManager.sendToServer(new CPChangePlayerMode(PlayerPatch.PlayerMode.BATTLE));
            }
        }

        super.toBattleMode(synchronize);
    }

    /**
     * Adds an if statement to check if the useStaminaWheel config is set or if
     * the stamina is being drained. If either are true, then the skill UI will
     * slide down. This fixes an issue where the UI would bug out if the stamina
     * bar was configured to display instead of the stamina wheel, and the user
     * toggled on and off Battle Mode.
     *
     * @param synchronize
     * @param ci
     */
    @Inject(method = "toMiningMode", at = @At("HEAD"), cancellable = true, remap = false)
    public void toMiningMode(boolean synchronize, CallbackInfo ci) {
        if (this.playerMode != PlayerMode.MINING) {

            if (ConfigManager.CLIENT_CONFIG.useStaminaWheel() || this.getStamina() == this.getMaxStamina()) {
                ClientEngine.getInstance().renderEngine.downSlideSkillUI();
            }

            if (EpicFightMod.CLIENT_CONFIGS.cameraAutoSwitch.getValue()) {
                this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            }

            if (synchronize) {
                EpicFightNetworkManager.sendToServer(new CPChangePlayerMode(PlayerMode.MINING));
            }
        }

        super.toMiningMode(synchronize);
    }
}
