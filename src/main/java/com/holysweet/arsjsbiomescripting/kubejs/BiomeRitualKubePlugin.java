package com.holysweet.arsjsbiomescripting.kubejs;

import com.holysweet.arsjsbiomescripting.ritual.BiomeRitualDefinition;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.core.RegistryAccess;

import java.util.List;

public final class BiomeRitualKubePlugin implements KubeJSPlugin {
    private static final EventGroup GROUP = EventGroup.of("BiomeRitualEvents");
    private static final EventHandler REGISTER = GROUP.server("register", () -> BiomeRitualRegistration.class);

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GROUP);
    }

    public static List<BiomeRitualDefinition> loadDefinitions(RegistryAccess registries) {
        var event = new BiomeRitualRegistration(registries);
        REGISTER.post(ScriptType.SERVER, event);
        return event.definitions();
    }
}
