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

package com.dfsek.terra.fabric.util;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import com.dfsek.terra.api.world.biome.PlatformBiome;

import net.minecraft.world.biome.Biome;


public class ProtoPlatformBiome implements PlatformBiome {
    private final Identifier identifier;
    
    private Biome delegate;
    
    public ProtoPlatformBiome(Identifier identifier) {
        this.identifier = identifier;
    }
    
    public net.minecraft.world.biome.Biome get(Registry<net.minecraft.world.biome.Biome> registry) {
        return registry.get(identifier);
    }
    
    @Override
    public Object getHandle() {
        return identifier;
    }
    
    public Biome getDelegate() {
        return delegate;
    }
    
    public void setDelegate(Biome delegate) {
        this.delegate = delegate;
    }
}
