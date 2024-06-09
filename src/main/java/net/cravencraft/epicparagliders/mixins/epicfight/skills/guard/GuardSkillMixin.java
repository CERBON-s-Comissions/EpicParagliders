package net.cravencraft.epicparagliders.mixins.epicfight.skills.guard;

import net.cravencraft.epicparagliders.EpicParaglidersAttributes;
import net.cravencraft.epicparagliders.capabilities.StaminaOverride;
import net.cravencraft.epicparagliders.config.ConfigManager;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.forge.capability.PlayerMovementProvider;
import tictim.paraglider.impl.movement.PlayerMovement;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.HurtEvent;
import yesman.epicfight.world.gamerule.EpicFightGamerules;

@Mixin(GuardSkill.class)
public abstract class GuardSkillMixin extends Skill {
    private float penalty;
    private float impact;
    private PlayerPatch playerPatch;

    public GuardSkillMixin(Builder<? extends Skill> builder) {
        super(builder);
    }

    /**
     * Simply retrieves the 'impact' variable from the 'guard' method to use when modifying
     * the 'blockType'
     */
    @Inject(method = "guard", at = @At("HEAD"), remap = false)
    private void getPlayerPatch(SkillContainer container, CapabilityItem itemCapability, HurtEvent.Pre event, float knockback, float impact, boolean advanced, CallbackInfo ci) {
        this.playerPatch = event.getPlayerPatch();
        this.impact = impact;
    }

    /**
     * Simply retrieves the 'penalty' variable from the 'guard' method to use when modifying
     * the 'blockType'
     */
    @ModifyVariable(method = "guard", at = @At(value = "STORE"), ordinal = 2, remap = false)
    private float getPenalty(float penalty) {
        this.penalty = penalty;
        return penalty;
    }

    /**
     * TODO: Can probably modify the variable here since we can use the consume method now.
     *
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
        PlayerMovement playerMovement = PlayerMovementProvider.of(playerPatch.getOriginal());
        int armorValue = playerMovement.player().getArmorValue();
        double blockMultiplier = Math.round(ConfigManager.SERVER_CONFIG.baseBlockStaminaMultiplier() * playerMovement.player().getAttributeValue(EpicParaglidersAttributes.BLOCK_STAMINA_REDUCTION.get()));

        float poise;
        float weight = this.playerPatch.getWeight();

        // No armor or no block multiplier means that there is no poise.
        // If both are true, then we get the poise based on the player's armor weight and protection value.
        if (weight <= 40.0F) {
            poise = 0.0F;
        }
        else {
            float attenuation = Mth.clamp(this.playerPatch.getOriginal().level().getGameRules().getInt(EpicFightGamerules.WEIGHT_PENALTY), 0, 100) / 100.0F;
            poise = (0.1F * (weight / 40.0F) * (Math.max(armorValue, 0) * 1.5F) * attenuation);
        }

        // Separate variable here solely because it's more readable to me.
        double guardConsumption = getConsumption() + (penalty * blockMultiplier) + (impact * blockMultiplier);
        guardConsumption = (guardConsumption > poise) ? (guardConsumption - poise) : 0;

        ((StaminaOverride) playerMovement.stamina()).performingAction(true);
        ((StaminaOverride) playerMovement.stamina()).setTotalActionStaminaCostServerSide((int) guardConsumption);

        if (playerMovement.stamina().isDepleted()) {
            return GuardSkill.BlockType.GUARD_BREAK;
        }
        else {
            return blockType;
        }
    }
}
