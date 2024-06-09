package net.cravencraft.epicparagliders.mixins.epicfight.skills.identity;

import net.cravencraft.epicparagliders.capabilities.StaminaOverride;
import net.cravencraft.epicparagliders.config.ConfigManager;
import net.cravencraft.epicparagliders.utils.MathUtils;
import net.minecraft.tags.DamageTypeTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.forge.capability.PlayerMovementProvider;
import tictim.paraglider.impl.movement.PlayerMovement;
import yesman.epicfight.skill.*;
import yesman.epicfight.skill.identity.MeteorSlamSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.entity.eventlistener.HurtEvent;
import yesman.epicfight.world.entity.eventlistener.SkillExecuteEvent;

@Mixin(MeteorSlamSkill.class)
public abstract class MeteorSlamSkillMixin extends Skill {

//    @Shadow(remap = false) @Final private static SkillDataKey<Boolean> PROTECT_NEXT_FALL;

    public MeteorSlamSkillMixin(Builder<? extends Skill> builder) {
        super(builder);
    }

    /**
     * Redirect method to set the resource for Meteor Slam to stamina
     *
     * @param instance
     * @param resource
     * @return
     */
    @Redirect(remap = false, at = @At(value = "INVOKE", target = "Lyesman/epicfight/skill/identity/MeteorSlamSkill$Builder;setResource(Lyesman/epicfight/skill/Skill$Resource;)Lyesman/epicfight/skill/identity/MeteorSlamSkill$Builder;"), method = "createMeteorSlamBuilder")
    private static MeteorSlamSkill.Builder redirectMeteorSlamResource(MeteorSlamSkill.Builder instance, Resource resource) {
        return instance.setResource(Resource.STAMINA);
    }

    /**
     * Cancels the Meteor Slam skill event if the player doesn't have enough stamina left,
     * and sets the consumption amount to be dependent on the weapon being used as well as
     * the server config multiplier parameter.
     *
     * @param container
     * @param event
     * @param ci
     */
    @Inject(cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"), method = "lambda$onInitiate$4")
    private void modifyStaminaSources(SkillContainer container, SkillExecuteEvent event, CallbackInfo ci) {
        ServerPlayerPatch serverPlayerPatch = (ServerPlayerPatch) container.getExecuter();
        PlayerMovement playerMovement = PlayerMovementProvider.of(serverPlayerPatch.getOriginal());
        if (playerMovement.stamina().isDepleted()) {
            ci.cancel();
        }
        else {
            this.consumption = (float) (MathUtils.getAttackStaminaCost(serverPlayerPatch.getOriginal()) * ConfigManager.SERVER_CONFIG.meteorSlamMultiplier());
            ((StaminaOverride) playerMovement.stamina()).performingAction(true);
            ((StaminaOverride) playerMovement.stamina()).setTotalActionStaminaCostServerSide((int) this.consumption);
        }
    }

    /**
     * Sets the fall damage reduction from the Meteor Slam skill to be based on the remaining stamina
     * multiplied by the server config mitigation parameter.
     *
     * @param container
     * @param event
     * @param ci
     */
    @Inject(at = @At("HEAD"), remap = false, cancellable = true, method = "lambda$onInitiate$5")
    private static void modifyFallDamageMitigation(SkillContainer container, HurtEvent.Pre event, CallbackInfo ci) {
        if (event.getDamageSource().is(DamageTypeTags.IS_FALL) && container.getDataManager().getDataValue(SkillDataKeys.PROTECT_NEXT_FALL.get())) {
            float stamina = container.getExecuter().getStamina();
            float damage = event.getAmount();
            int damageReduction = (int) Math.round(damage - (stamina * ConfigManager.SERVER_CONFIG.meteorSlamFallDamageMitigator()));
            event.setAmount(damageReduction);
            event.setCanceled(true);
            container.getDataManager().setData(SkillDataKeys.PROTECT_NEXT_FALL.get(), false);
        }
        ci.cancel();
    }
}