package net.cravencraft.epicparagliders.mixins.skills.guard;

import net.cravencraft.epicparagliders.EPModCfg;
import net.cravencraft.epicparagliders.capabilities.PlayerMovementInterface;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.capabilities.PlayerMovement;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.HurtEvent;
import yesman.epicfight.world.gamerule.EpicFightGamerules;

@Mixin(GuardSkill.class)
public abstract class GuardSkillMixin extends Skill {
    //TODO: Cleanup and organize
    private float penalty;
    private float impact;
    private PlayerPatch playerPatch;

    public GuardSkillMixin(Builder<? extends Skill> builder) {
        super(builder);
    }

    @Inject(method = "guard", at = @At("HEAD"), remap = false)
    private void getPlayerPatch(SkillContainer container, CapabilityItem itemCapability, HurtEvent.Pre event, float knockback, float impact, boolean advanced, CallbackInfo ci) {
        this.playerPatch = event.getPlayerPatch();
        this.impact = impact;
    }

    @ModifyVariable(method = "guard", at = @At(value = "STORE"), ordinal = 2, remap = false)
    private float getPenalty(float penalty) {
        this.penalty = penalty;
        return penalty;
    }

    /**
     * Modifies the 'blockType' variable in the 'guard' method of the GuardSkill. This method will
     * take block penalty, weight, and armor value into account when determining the block stamina
     * consumption amount. If the Paragliders stamina system reaches 0, then the blockType will
     * become GUARD_BREAK.
     *
     * @param blockType GUARD, GUARD_BREAK, or ADVANCED_GUARD
     * @return
     */
    @ModifyVariable(method = "guard", at = @At(value = "STORE"), ordinal = 0, remap = false)
    private GuardSkill.BlockType blockType(GuardSkill.BlockType blockType) {
        PlayerMovement playerMovement = PlayerMovement.of(playerPatch.getOriginal());
        float weight = this.playerPatch.getWeight();
        double blockMultiplier = EPModCfg.baseBlockStaminaMultiplier();
        int armorValue = playerMovement.player.getArmorValue();
        float poise;

        if (weight < 40.0F || blockMultiplier < 0.0) {
            poise = 0.0F;
        }
        else {
            float attenuation = Mth.clamp(this.playerPatch.getOriginal().level.getGameRules().getInt(EpicFightGamerules.WEIGHT_PENALTY), 0, 100) / 100.0F;
            poise = (0.1F * (weight / 40.0F) * (Math.max(armorValue, 0) * 1.5F) * attenuation);
        }
        int totalPenalty = (int) (penalty * blockMultiplier);
        int totalImpact = (int) (impact * blockMultiplier);

        int guardConsumption = (int) ((getConsumption() + totalPenalty + totalImpact) - poise);

        ((PlayerMovementInterface) playerMovement).setActionStaminaCostServerSide(guardConsumption);
        ((PlayerMovementInterface) playerMovement).performingActionServerSide(true);

        if (playerMovement.isDepleted()) {
            return GuardSkill.BlockType.GUARD_BREAK;
        }
        else {
            return blockType;
        }
    }
}
