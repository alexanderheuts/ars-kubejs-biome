package com.holysweet.arsjsbiomescripting;

import com.hollingsworth.arsnouveau.api.registry.RitualRegistry;
import com.holysweet.arsjsbiomescripting.ritual.ConfiguredBiomeRitual;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ArsNouveauKubeJSBiomeScripting.MODID)
public class ArsNouveauKubeJSBiomeScripting {
    public static final String MODID = "arsjsbiomescripting";

    public ArsNouveauKubeJSBiomeScripting(IEventBus modBus) {
        modBus.addListener(this::registerRitual);
    }

    private void registerRitual(FMLCommonSetupEvent event) {
        // The shared ritual is reconstructed by Ars, while its inputs are defined by scripts.
        event.enqueueWork(() -> RitualRegistry.registerRitual(new ConfiguredBiomeRitual()));
    }
}
