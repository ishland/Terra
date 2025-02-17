/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.fabric;

import ca.solostudios.strata.Versions;
import ca.solostudios.strata.version.Version;

import com.dfsek.terra.api.event.events.config.ConfigurationLoadEvent;

import com.dfsek.terra.fabric.config.VanillaBiomeProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.api.addon.BaseAddon;
import com.dfsek.terra.api.event.events.config.pack.ConfigPackPostLoadEvent;
import com.dfsek.terra.api.event.events.config.pack.ConfigPackPreLoadEvent;
import com.dfsek.terra.api.event.functional.FunctionalEventHandler;
import com.dfsek.terra.api.world.biome.Biome;
import com.dfsek.terra.fabric.config.PostLoadCompatibilityOptions;
import com.dfsek.terra.fabric.config.PreLoadCompatibilityOptions;
import com.dfsek.terra.fabric.event.BiomeRegistrationEvent;
import com.dfsek.terra.fabric.util.FabricUtil;


public final class FabricAddon implements BaseAddon {
    private static final Version VERSION = Versions.getVersion(1, 0, 0);
    private static final Logger logger = LoggerFactory.getLogger(FabricAddon.class);
    private final PlatformImpl terraFabricPlugin;
    
    public FabricAddon(PlatformImpl terraFabricPlugin) {
        this.terraFabricPlugin = terraFabricPlugin;
    }
    
    @Override
    public void initialize() {
        terraFabricPlugin.getEventManager()
                         .getHandler(FunctionalEventHandler.class)
                         .register(this, ConfigPackPreLoadEvent.class)
                         .then(event -> event.getPack().getContext().put(event.loadTemplate(new PreLoadCompatibilityOptions())))
                         .global();
        
        terraFabricPlugin.getEventManager()
                         .getHandler(FunctionalEventHandler.class)
                         .register(this, ConfigPackPostLoadEvent.class)
                         .then(event -> event.getPack().getContext().put(event.loadTemplate(new PostLoadCompatibilityOptions())))
                         .priority(100)
                         .global();
        
        terraFabricPlugin.getEventManager()
                         .getHandler(FunctionalEventHandler.class)
                         .register(this, BiomeRegistrationEvent.class)
                         .then(event -> {
                             logger.info("Registering biomes...");
                             
                             terraFabricPlugin.getConfigRegistry().forEach(pack -> { // Register all Terra biomes.
                                 pack.getCheckedRegistry(Biome.class)
                                     .forEach((id, biome) -> FabricUtil.registerBiome(biome, pack, event.getRegistryManager(), id));
                             });
                             logger.info("Biomes registered.");
                         })
                         .global();
        
        terraFabricPlugin.getEventManager()
                         .getHandler(FunctionalEventHandler.class)
                         .register(this, ConfigurationLoadEvent.class)
                         .then(event -> {
                             if(event.is(Biome.class)) {
                                event.getLoadedObject(Biome.class).getContext().put(event.load(new VanillaBiomeProperties()));
                             }
                         })
                         .global();
    }
    
    @Override
    public Version getVersion() {
        return VERSION;
    }
    
    @Override
    public String getID() {
        return "terra-fabric";
    }
}
