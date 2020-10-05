package com.dfsek.terra.procgen.pixel;

import com.dfsek.terra.procgen.math.Vector2;

import java.util.Set;

public abstract class Polygon {
    public abstract Set<Vector2> getContainedPixels();
}
