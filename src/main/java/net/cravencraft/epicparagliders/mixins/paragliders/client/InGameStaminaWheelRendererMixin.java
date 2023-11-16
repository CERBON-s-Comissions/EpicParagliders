package net.cravencraft.epicparagliders.mixins.paragliders.client;

import net.cravencraft.epicparagliders.config.ConfigManager;
import net.cravencraft.epicparagliders.capabilities.PlayerMovementInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.ModCfg;
import tictim.paraglider.capabilities.PlayerMovement;
import tictim.paraglider.capabilities.PlayerState;
import tictim.paraglider.client.InGameStaminaWheelRenderer;
import tictim.paraglider.client.StaminaWheelConstants;
import tictim.paraglider.client.StaminaWheelRenderer;
import tictim.paraglider.utils.Color;
import yesman.epicfight.client.ClientEngine;

import static tictim.paraglider.client.StaminaWheelConstants.*;

@Mixin(InGameStaminaWheelRenderer.class)
public abstract class InGameStaminaWheelRendererMixin extends StaminaWheelRenderer {

    private boolean staminaBarShowing;
    @Shadow private int prevStamina;

    @Shadow private long fullTime;

    /**
     * Overrides Paraglider's stamina wheel, and adds checks for Epic Fight actions within it.
     * Will only display if the use_stamina_wheel server config is set to true.
     *
     * @param h
     * @param ci
     */
    @Inject(method = "makeWheel", at = @At(value = "HEAD"), remap=false, cancellable = true)
    public void makeWheel(PlayerMovement h, CallbackInfo ci) {
        if (ConfigManager.CLIENT_CONFIG.useStaminaWheel()) {
            int totalActionStaminaCost = ((PlayerMovementInterface) h).getTotalActionStaminaCost();
            int stamina = h.getStamina();

            int maxStamina = h.getMaxStamina();
            if (stamina >= maxStamina) {
                long time = System.currentTimeMillis();
                long timeDiff;
                if (this.prevStamina != stamina) {
                    this.prevStamina = stamina;
                    this.fullTime = time;
                    timeDiff = 0L;
                } else {
                    timeDiff = time - this.fullTime;
                }

                Color color = StaminaWheelConstants.getGlowAndFadeColor(timeDiff);
                if (color.alpha <= 0.0F) {
                    return;
                }

                WheelLevel[] var9 = WheelLevel.values();
                int var10 = var9.length;

                for(int var11 = 0; var11 < var10; ++var11) {
                    WheelLevel t = var9[var11];
                    this.addWheel(t, 0.0, t.getProportion(stamina), color);
                }
            } else {
                this.prevStamina = stamina;
                Color color = StaminaWheelConstants.DEPLETED_1.blend(StaminaWheelConstants.DEPLETED_2, StaminaWheelConstants.cycle(System.currentTimeMillis(), h.isDepleted() ? 600L : 300L));
                Color gainStam = Color.of(2, 2, 150).blend(Color.of(2, 150, 255), cycle(System.currentTimeMillis(), h.isDepleted() ? DEPLETED_BLINK : BLINK));
                PlayerState state = h.getState();
                int stateChange = (state.isConsume()) ? state.change() - totalActionStaminaCost : -totalActionStaminaCost;
                WheelLevel[] var14 = WheelLevel.values();
                int var7 = var14.length;

                for(int var15 = 0; var15 < var7; ++var15) {
                    WheelLevel t = var14[var15];
                    this.addWheel(t, 0.0, t.getProportion(maxStamina), StaminaWheelConstants.EMPTY);
                    if (h.isDepleted()) {
                        this.addWheel(t, 0.0, t.getProportion(stamina), color);
                    } else {
                        this.addWheel(t, 0.0, t.getProportion(stamina), StaminaWheelConstants.IDLE);
                        if (((state.isConsume()
                                && (state.isParagliding() ? ModCfg.paraglidingConsumesStamina() : ModCfg.runningConsumesStamina())))
                                || totalActionStaminaCost > 0) {
                            this.addWheel(t, t.getProportion(stamina + stateChange * 10), t.getProportion(stamina), color);
                        }
                        if (totalActionStaminaCost < 0) {
                            int totalProportion = stamina + (-totalActionStaminaCost) * 10;
                            // If the total proportion is greater than the max stamina, just set it to the missing stamina.
                            if (totalProportion > h.getMaxStamina()) {
                                totalProportion = stamina + (h.getMaxStamina() - stamina);
                            }

                            addWheel(t,  t.getProportion(stamina), t.getProportion(totalProportion), gainStam);
                        }
                    }
                }
            }
        }
        else if (!ClientEngine.getInstance().getPlayerPatch().isBattleMode()) {

            if (h.getStamina() == h.getMaxStamina() && staminaBarShowing == true) {
                ClientEngine.getInstance().renderEngine.downSlideSkillUI();
                staminaBarShowing = false;
            }
            else if (h.getStamina() < h.getMaxStamina() && staminaBarShowing == false) {
                ClientEngine.getInstance().renderEngine.upSlideSkillUI();
                staminaBarShowing = true;
            }
        }
        ci.cancel();
    }
}
