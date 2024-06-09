package net.cravencraft.epicparagliders.mixins.epicfight.skills.guard;

import net.cravencraft.epicparagliders.EpicParaglidersAttributes;
import net.cravencraft.epicparagliders.capabilities.StaminaOverride;
import net.cravencraft.epicparagliders.config.ConfigManager;
import net.cravencraft.epicparagliders.utils.MathUtils;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tictim.paraglider.forge.capability.PlayerMovementProvider;
import tictim.paraglider.impl.movement.PlayerMovement;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.HurtEvent;
import yesman.epicfight.world.gamerule.EpicFightGamerules;

@Mixin(ParryingSkill.class)
public abstract class ParryingSkillMixin extends GuardSkill {
    private boolean successfulParry;
    private float penalty;
    private float impact;
    private PlayerPatch playerPatch;

    public ParryingSkillMixin(Builder builder) {
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
    private float penalty(float penalty) {
        this.penalty = penalty;
        return penalty;
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @ModifyVariable(method = "guard", at = @At(value = "STORE"), name = "successParrying", remap = false)
    private boolean isSuccessfulParry(boolean successParrying) {
        this.successfulParry = successParrying;
        return successfulParry;
    }

    /**
     * TODO: Bug here that doesn't allow stamina to drain if you successfully parry an attack.
     *       Also, parrying does not give stamina.
     *
     * Modifies the 'stamina' variable in the 'guard' method of the ActiveGuardSkill class.
     * Takes in values retrieved from the above methods that simply store the penalty and impact
     * from the same method to factor into this method's new stamina consumption factor.
     *
     * Calculates the stamina cost based on player armor weight, impact, and block penalty.
     * If active guard is successful (penalty > 0.1), then less stamina is drained
     * (vs. negating all stamina being drained initially), else even more stamina is drained
     * from the block. Making this a high risk high reward system to choose of regular guard.
     *
     * @param blockType Either GUARD_BREAK or ADVANCED_GUARD
     * @return
     */
    @ModifyVariable(method = "guard", at = @At(value = "STORE"), ordinal = 0, remap = false)
    private GuardSkill.BlockType blockType(GuardSkill.BlockType blockType) {
        PlayerMovement playerMovement = PlayerMovementProvider.of(playerPatch.getOriginal());
        StaminaOverride botwStamina = ((StaminaOverride) playerMovement.stamina());

        int guardConsumption;
        int armorValue = playerMovement.player().getArmorValue();

        double blockMultiplier = Math.round(ConfigManager.SERVER_CONFIG.baseBlockStaminaMultiplier() * playerMovement.player().getAttributeValue(EpicParaglidersAttributes.BLOCK_STAMINA_REDUCTION.get()));
        double parryPenaltyMultiplier = ConfigManager.SERVER_CONFIG.parryPenaltyMultiplier();
        double parryPercentModifier = ConfigManager.SERVER_CONFIG.parryPercentModifier() * 0.01;

        float poise;
        float weight = this.playerPatch.getWeight();

        /*
         * If the weight is less than 40 or the bock multiplier is 0, then there will be no player poise.
         * Else, the poise will be a combination of armor value and weight.
         *
         */
        if (weight <= 40.0F || blockMultiplier <= 0.0) {
            poise = 0.0F;
        }
        else {
            float attenuation = Mth.clamp(this.playerPatch.getOriginal().level().getGameRules().getInt(EpicFightGamerules.WEIGHT_PENALTY), 0, 100) / 100.0F;
            poise = (0.1F * (weight / 40.0F) * (Math.max(armorValue, 0) * 1.5F) * attenuation);
        }

        if (this.successfulParry) {
            // If the player is successful with the parry, use one of these formulas depending on if parrying is set to drain stamina in the config.
            if (ConfigManager.SERVER_CONFIG.parryDrain()) {
                guardConsumption = (int) (getConsumption() + (impact * blockMultiplier * (1 - parryPercentModifier)));
            }
            else {
                int trueTotalMissing = (int) (MathUtils.calculateTriangularNumber(botwStamina.getTotalActionStaminaCost()) + (playerMovement.stamina().maxStamina() - playerMovement.stamina().stamina()));
                guardConsumption = -(int) (MathUtils.calculateModifiedTriangularRoot(trueTotalMissing, parryPercentModifier));
            }

        }
        else {
            // If the player is unsuccessful with the parry, use this penalty formula.
            guardConsumption = (int)  (getConsumption() + (penalty * parryPenaltyMultiplier) + (impact * parryPenaltyMultiplier));
        }

        // If the guard consumption is greater than the poise, then subtract poise as well.
        // If the guard consumption is not negative (not gaining stamina) AND LESS OR EQUAL TO poise, then set to 0.
        if (guardConsumption > poise) {
            guardConsumption = (int) (guardConsumption - poise);
        }
        else if (Math.signum(guardConsumption) > 0) {
            guardConsumption = 0;
        }

        botwStamina.setTotalActionStaminaCostServerSide(guardConsumption);
        botwStamina.performingAction(true);

        if (playerMovement.stamina().isDepleted()) {
            return GuardSkill.BlockType.GUARD_BREAK;
        }
        else {
            return blockType;
        }
    }
}