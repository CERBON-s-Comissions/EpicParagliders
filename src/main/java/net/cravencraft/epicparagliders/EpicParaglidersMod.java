package net.cravencraft.epicparagliders;

import net.cravencraft.epicparagliders.config.ConfigManager;
import net.cravencraft.epicparagliders.gameasset.ExhaustionAnimations;
import net.cravencraft.epicparagliders.network.ModNet;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tictim.paraglider.contents.Contents;

@Mod(EpicParaglidersMod.MOD_ID)
@Mod.EventBusSubscriber(modid = EpicParaglidersMod.MOD_ID, bus = Bus.MOD)
public class EpicParaglidersMod
{
    public static final Logger LOGGER = LogManager.getLogger("EpicParagliders");
    public static final String MOD_ID = "epicparagliders";

    public EpicParaglidersMod()
    {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(ExhaustionAnimations::registerAnimations); //TODO: Can I just use @SubscribeEvent in the original method to bypass this?
        Contents.registerEventHandlers(eventBus);
        EpicParaglidersAttributes.registerEventHandlers(eventBus);
        ModNet.init();

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        ConfigManager.registerConfigs();
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // Attributes for modifying stamina consumption in-game
        event.add(EntityType.PLAYER, EpicParaglidersAttributes.DAGGER_STAMINA_REDUCTION.get());

        // Attributes needed for datapack editing
        event.add(EntityType.PLAYER, EpicParaglidersAttributes.WEAPON_STAMINA_CONSUMPTION.get());
        event.add(EntityType.PLAYER, EpicParaglidersAttributes.WEAPON_TYPE.get());
    }
}
