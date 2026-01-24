package com.marckaa.ziplineplugin.components;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

public class RideComponent implements Component<EntityStore> {

    private final Vector3d targetPosition;
    private final double speed;

    public RideComponent(Vector3d targetBlockPos, double speed) {
        this.targetPosition = targetBlockPos.add(0.5, 0.5, 0.5);
        this.speed = speed;
    }

    public Vector3d getTargetPosition() {
        return targetPosition;
    }

    public double getSpeed() {
        return speed;
    }

    @Override
    public @Nullable Component<EntityStore> clone() {
        return null;
    }
}
