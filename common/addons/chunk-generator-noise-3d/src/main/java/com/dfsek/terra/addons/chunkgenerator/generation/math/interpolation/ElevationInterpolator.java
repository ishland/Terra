/*
 * Copyright (c) 2020-2021 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.dfsek.terra.addons.chunkgenerator.generation.math.interpolation;

import com.dfsek.terra.addons.chunkgenerator.config.noise.BiomeNoiseProperties;
import com.dfsek.terra.api.util.MathUtil;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;

import java.util.HashSet;
import java.util.Set;


public class ElevationInterpolator {
    private final double[][] values = new double[18][18];
    
    public ElevationInterpolator(long seed, int chunkX, int chunkZ, BiomeProvider provider, int smooth) {
        int xOrigin = chunkX << 4;
        int zOrigin = chunkZ << 4;
        
        BiomeNoiseProperties[][] gens = new BiomeNoiseProperties[18 + 2 * smooth][18 + 2 * smooth];
        
        // Precompute generators.
        for(int x = -1 - smooth; x <= 16 + smooth; x++) {
            for(int z = -1 - smooth; z <= 16 + smooth; z++) {
                gens[x + 1 + smooth][z + 1 + smooth] = provider.getBiome(xOrigin + x, zOrigin + z, seed).getContext().get(
                        BiomeNoiseProperties.class);
            }
        }
        
        for(int x = -1; x <= 16; x++) {
            for(int z = -1; z <= 16; z++) {
                double noise = 0;
                double div = 0;
                
                BiomeNoiseProperties center = gens[x + 1 + smooth][z + 1 + smooth];
                boolean same = true;
    
                for(int xi = -smooth; xi <= smooth; xi++) {
                    for(int zi = -smooth; zi <= smooth; zi++) {
                        if(gens[x + 1 + smooth + xi][z + 1 + smooth + zi] != center) { // test referential equality because thats all we need to know
                            same = false;
                            break;
                        }
                    }
                }
                
                if(same) {
                    values[x + 1][z + 1] = center.elevation().noise(seed, xOrigin + x, zOrigin + z); // no weighting needed!
                } else {
                    for(int xi = -smooth; xi <= smooth; xi++) {
                        for(int zi = -smooth; zi <= smooth; zi++) {
                            BiomeNoiseProperties gen = gens[x + 1 + smooth + xi][z + 1 + smooth + zi];
                            noise += gen.elevation().noise(seed, xOrigin + x, zOrigin + z) * gen.elevationWeight();
                            div += gen.elevationWeight();
                        }
                    }
                    values[x + 1][z + 1] = noise / div;
                }
            }
        }
    }
    
    public double getElevation(int x, int z) {
        return values[x + 1][z + 1];
    }
}
