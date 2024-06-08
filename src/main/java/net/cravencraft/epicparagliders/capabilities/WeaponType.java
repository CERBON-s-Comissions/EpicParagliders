package net.cravencraft.epicparagliders.capabilities;

import net.cravencraft.epicparagliders.EpicParaglidersAttributes;
import net.cravencraft.epicparagliders.config.ConfigManager;
import net.cravencraft.epicparagliders.config.ServerConfig;
import net.minecraft.world.entity.player.Player;

public enum WeaponType {

    AXE,
    HOE,
    SHOVEL,
    PICKAXE,
    DAGGER,
    SWORD,
    LONGSWORD,
    GREATSWORD,
    SPEAR,
    KNUCKLE,
    TACHI,
    UCHIGATANA;

    /**
     * Gets the stamina multiplier for a given weapon type.
     *
     * @return
     */
    public double getStaminaMultiplier() {
        ServerConfig serverConfig = ConfigManager.SERVER_CONFIG;

        switch (this) {
            case AXE:
            case HOE:
            case SHOVEL:
            case PICKAXE:
                return serverConfig.axeStaminaMultiplier();
            case DAGGER:
                return serverConfig.daggerStaminaMultiplier();
            case SWORD:
                return serverConfig.swordStaminaMultiplier();
            case LONGSWORD:
                return serverConfig.longSwordMultiplier();
            case GREATSWORD:
                return serverConfig.greatSwordStaminaMultiplier();
            case SPEAR:
                return serverConfig.spearStaminaMultiplier();
            case KNUCKLE:
                return serverConfig.knuckleStaminaMultiplier();
            case TACHI:
                return serverConfig.tachiStaminaMultiplier();
            case UCHIGATANA:
                return serverConfig.uchigatanaStaminaMultiplier();
            default:
                return serverConfig.defaultMeleeStaminaMultiplier();
        }
    }

    /**
     * Gets the fixed stamina cost for a given weapon type.
     *
     * @return
     */
    public int getStaminaFixedCost() {
        ServerConfig serverConfig = ConfigManager.SERVER_CONFIG;

        switch (this) {
            case AXE:
            case HOE:
            case SHOVEL:
            case PICKAXE:
                return serverConfig.axeStaminaFixedCost();
            case DAGGER:
                return serverConfig.daggerStaminaFixedCost();
            case SWORD:
                return serverConfig.swordStaminaFixedCost();
            case LONGSWORD:
                return serverConfig.longSwordStaminaFixedCost();
            case GREATSWORD:
                return serverConfig.greatSwordStaminaFixedCost();
            case SPEAR:
                return serverConfig.spearStaminaFixedCost();
            case KNUCKLE:
                return serverConfig.knuckleStaminaFixedCost();
            case TACHI:
                return serverConfig.tachiStaminaFixedCost();
            case UCHIGATANA:
                return serverConfig.uchigatanaStaminaFixedCost();
            default:
                return serverConfig.defaultMeleeStaminaFixedCost();
        }
    }

    /**
     * Gets the attribute for stamina reduction for a specific weapon type.
     *
     * @param player
     * @return
     */
    public double getStaminaReduction(Player player) {

        switch (this) {
            case AXE:
            case HOE:
            case SHOVEL:
            case PICKAXE:
                return player.getAttributeValue(EpicParaglidersAttributes.AXE_STAMINA_REDUCTION.get());
            case DAGGER:
                return player.getAttributeValue(EpicParaglidersAttributes.DAGGER_STAMINA_REDUCTION.get());
            case SWORD:
                return player.getAttributeValue(EpicParaglidersAttributes.SWORD_STAMINA_REDUCTION.get());
            case LONGSWORD:
                return player.getAttributeValue(EpicParaglidersAttributes.LONGSWORD_STAMINA_REDUCTION.get());
            case GREATSWORD:
                return player.getAttributeValue(EpicParaglidersAttributes.GREATSWORD_STAMINA_REDUCTION.get());
            case SPEAR:
                return player.getAttributeValue(EpicParaglidersAttributes.SPEAR_STAMINA_REDUCTION.get());
            case KNUCKLE:
                return player.getAttributeValue(EpicParaglidersAttributes.KNUCKLE_STAMINA_REDUCTION.get());
            case TACHI:
                return player.getAttributeValue(EpicParaglidersAttributes.TACHI_STAMINA_REDUCTION.get());
            case UCHIGATANA:
                return player.getAttributeValue(EpicParaglidersAttributes.UCHIGATANA_STAMINA_REDUCTION.get());
            default:
                return player.getAttributeValue(EpicParaglidersAttributes.DEFAULT_STAMINA_REDUCTION.get());
        }
    }

//    public static WeaponType fromString(String weaponType) {
//        return switch (weaponType) {
//            case "axe" -> AXE;
//            case "hoe" -> HOE;
//            case "shovel" -> SHOVEL;
//            case "pickaxe" -> PICKAXE;
//            case "dagger" -> DAGGER;
//            case "sword" -> SWORD;
//            case "longsword" -> LONGSWORD;
//            case "greatsword" -> GREATSWORD;
//            case "spear" -> SPEAR;
//            case "knuckle" -> KNUCKLE;
//            case "tachi" -> TACHI;
//            case "uchigatana" -> UCHIGATANA;
//            default -> SWORD;
//        };
//    }
}