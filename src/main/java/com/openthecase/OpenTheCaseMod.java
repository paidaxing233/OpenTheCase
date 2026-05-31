package com.openthecase;

import com.openthecase.handler.ContainerCaptureHandler;
import com.openthecase.handler.ItemUseHandler;
import com.openthecase.handler.TooltipHandler;
import com.openthecase.menu.PortableChestMenu;
import com.openthecase.screen.PortableChestScreen;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(OpenTheCaseMod.MODID)
public class OpenTheCaseMod {
    public static final String MODID = "openthecase";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<MenuType<PortableChestMenu>> PORTABLE_CHEST_MENU =
            MENU_TYPES.register("portable_chest", () -> IForgeMenuType.create(PortableChestMenu::new));

    public OpenTheCaseMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        MENU_TYPES.register(modEventBus);

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(new ContainerCaptureHandler());
        MinecraftForge.EVENT_BUS.register(new ItemUseHandler());
        MinecraftForge.EVENT_BUS.register(new TooltipHandler());

        LOGGER.info("OpenTheCase mod loaded!");
    }

    /**
     * Client-side setup handler.
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(PortableChestScreen::register);
        }
    }
}
