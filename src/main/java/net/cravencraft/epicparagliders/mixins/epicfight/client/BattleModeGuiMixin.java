package net.cravencraft.epicparagliders.mixins.epicfight.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.cravencraft.epicparagliders.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.config.Cfg;
import tictim.paraglider.forge.capability.PlayerMovementProvider;
import tictim.paraglider.impl.movement.ClientPlayerMovement;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.gui.BattleModeGui;
import yesman.epicfight.client.gui.EntityIndicator;
import yesman.epicfight.client.gui.ModIngameGui;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.config.EpicFightOptions;

@Mixin(BattleModeGui.class)
public abstract class BattleModeGuiMixin extends ModIngameGui {
    @Shadow(remap = false) private int sliding;
    @Shadow(remap = false) private boolean slidingToggle;
    @Shadow(remap = false) @Final private EpicFightOptions config;

    private int maxPossibleStamina = Cfg.get().maxStamina();
    private boolean decreaseOpacity;
    private float modifiedOpacity;
    private float ratio;

    public BattleModeGuiMixin() {
        super();
    }


    /**
     * Simply retrieves the blue color change ratio (based on the player's current stamina).
     * Uses this value to modify the GUI in the method below.
     *
     * @param ratio
     * @return
     */
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(method = "renderGui", at = @At("STORE"), name = "ratio", remap = false)
    private float getRatio(float ratio) {
        this.ratio = ratio;
        return ratio;
    }

    /**
     *  Will not render the Epic Fight stamina bar unless the server config is set to do so.
     *
     * @param maxStamina
     * @return
     */
    @ModifyVariable(method = "renderGui", at = @At("STORE"), ordinal = 1, remap = false)
    private float removeGui(float maxStamina) {
        if (!ConfigManager.CLIENT_CONFIG.useStaminaWheel()) {
            return maxStamina;
        }
        else {
            return 0.0F;
        }

    }

    /**
     * Displays ONLY the stamina bar even if the player is not in Battle Mode (only if the config is set to display
     * via the InGameStaminaWheelRendererMixin class). Is needed because the player can potentially drain
     * stamina from actions like running and swimming now.
     */
    @Inject(at = @At("HEAD"), method = "renderGui", remap = false, cancellable = true)
    private void testMethod(LocalPlayerPatch playerPatch, GuiGraphics guiGraphics, float partialTicks, CallbackInfo ci) {
        if (!playerPatch.isBattleMode()) {
            if (!playerPatch.getOriginal().isAlive() || playerPatch.getOriginal().getVehicle() != null) {
                return;
            }
            if (this.sliding <= 28) {
                if (this.sliding > 0) {
                    if (this.slidingToggle) {
                        this.sliding -= 2;
                    } else {
                        this.sliding += 2;
                    }
                }

                Window sr = Minecraft.getInstance().getWindow();
                int width = sr.getGuiScaledWidth();
                int height = sr.getGuiScaledHeight();
                boolean depthTestEnabled = GL11.glGetBoolean(2929);
                boolean blendEnabled = GL11.glGetBoolean(3042);
                if (depthTestEnabled) {
                    RenderSystem.disableDepthTest();
                }

                if (!blendEnabled) {
                    RenderSystem.enableBlend();
                }

                PoseStack poseStack = guiGraphics.pose();
                poseStack.pushPose();
                poseStack.setIdentity();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                float maxStamina = playerPatch.getMaxStamina();
                float stamina = playerPatch.getStamina();
                float ratio;
                if (maxStamina > 0.0F && stamina < maxStamina) {
                    Vec2i pos = this.config.getStaminaPosition(width, height);
                    float prevStamina = playerPatch.getPrevStamina();
                    ratio = (prevStamina + (stamina - prevStamina) * partialTicks) / maxStamina;
                    poseStack.pushPose();
                    poseStack.translate(0.0F, (float)this.sliding, 0.0F);
                    this.setModifiedOpacity(1.0F, ratio, 0.25F, 1.0F);
                    guiGraphics.blit(EntityIndicator.BATTLE_ICON, pos.x, pos.y, this.getDisplayStamina(118), 4, 2.0F, 38.0F, 237, 9, 255, 255);
                    guiGraphics.blit(EntityIndicator.BATTLE_ICON, pos.x, pos.y, this.getDisplayStamina((int) (118*ratio)), 4, 2.0F, 47.0F, (int)(237.0F * ratio), 9, 255, 255);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    poseStack.popPose();
                }
            }
            ci.cancel();
        }
    }

    /**
     * Adjusts the stamina bar's length to be relative to the max stamina a user has vs. the max
     * stamina possible to achieve.
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V", ordinal = 0), method = "renderGui")
    private void modifyStaminaBarSize(GuiGraphics guiGraphics, ResourceLocation p_282034_, int p_283671_, int p_282377_, int p_282058_, int p_281939_, float p_282285_, float p_283199_, int p_282186_, int p_282322_, int p_282481_, int p_281887_) {
        guiGraphics.blit(p_282034_, p_283671_, p_282377_, this.getDisplayStamina(p_282058_), p_281939_, p_282285_, p_283199_, p_282186_, p_282322_, p_282481_, p_281887_);
    }

    /**
     * Adjusts the stamina bar's dynamic length to be relative to the max stamina a user has vs. the max
     * stamina possible to achieve.
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V", ordinal = 1), method = "renderGui")
    private void modifyDynamicStaminaBar(GuiGraphics guiGraphics, ResourceLocation p_282034_, int p_283671_, int p_282377_, int p_282058_, int p_281939_, float p_282285_, float p_283199_, int p_282186_, int p_282322_, int p_282481_, int p_281887_) {
        guiGraphics.blit(p_282034_, p_283671_, p_282377_, this.getDisplayStamina(p_282058_), p_281939_, p_282285_, p_283199_, p_282186_, p_282322_, p_282481_, p_281887_);
    }

    /**
     * Modifies the Epic Fight stamina bar. It will now turn red and the opacity will decrease whenever
     * the player's stamina is depleted.
     *
     * @param red
     * @param blue
     * @param green
     * @param opacity
     */
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0),
            method = "renderGui")
    private void modifyStaminaBar(float red, float blue, float green, float opacity) {
        this.setModifiedOpacity(red, this.ratio, green, opacity);
    }

    private int getDisplayStamina(int originalStamina) {
        return (int) (originalStamina / (this.maxPossibleStamina / ClientEngine.getInstance().getPlayerPatch().getMaxStamina()));
    }

    /**
     * Helper method to display the stamina bar as blinking red if the player is out of stamina.
     *
     * @param red
     * @param blue
     * @param green
     * @param opacity
     */
    private void setModifiedOpacity(float red, float blue, float green, float opacity) {
        ClientPlayerMovement playerMovement = (ClientPlayerMovement) PlayerMovementProvider.of(Minecraft.getInstance().player);

        if (playerMovement.stamina().isDepleted()) {
            if (decreaseOpacity) {
                modifiedOpacity -= 0.05F;
            }
            else {
                modifiedOpacity += 0.05f;
            }

            if (modifiedOpacity >= 1.0F) {
                decreaseOpacity = true;
            }
            else if(modifiedOpacity <= 0.0F) {
                decreaseOpacity = false;
            }

            RenderSystem.setShaderColor(red, 0.0F, 0.0F, modifiedOpacity);
        }
        else {
            modifiedOpacity = opacity;
            RenderSystem.setShaderColor(red, blue, 0.25F, opacity);
        }
    }
}