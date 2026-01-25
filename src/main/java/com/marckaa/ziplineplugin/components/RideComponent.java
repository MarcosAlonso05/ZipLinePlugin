package com.marckaa.ziplineplugin.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplinePlugin;
import org.jspecify.annotations.Nullable;

public class RideComponent implements Component<EntityStore> {

    // Quitamos 'final' para que el Codec pueda asignar valores
    private Vector3d startPos;
    private Vector3d endPos;
    private double speed;

    // Constructor vacío OBLIGATORIO para el BuilderCodec
    public RideComponent() {}

    // Constructor normal para cuando lo creamos nosotros por código
    public RideComponent(Vector3d startPos, Vector3d endPos, double speed) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.speed = speed;
    }

    public static ComponentType<EntityStore, RideComponent> getComponentType() {
        return ZiplinePlugin.getInstance().getRideComponentType();
    }

    public Vector3d getStartPos() { return startPos; }
    public Vector3d getEndPos() { return endPos; }
    public double getSpeed() { return speed; }

    // --- CODEC CORREGIDO ---
    public static final BuilderCodec<RideComponent> CODEC = BuilderCodec.builder(RideComponent.class, RideComponent::new)
            .append(new KeyedCodec<>("Start", Vector3d.CODEC), (c, v) -> c.startPos = v, c -> c.startPos)
            .add()
            .append(new KeyedCodec<>("End", Vector3d.CODEC), (c, v) -> c.endPos = v, c -> c.endPos)
            .add()
            .append(new KeyedCodec<>("Speed", Codec.DOUBLE), (c, v) -> c.speed = v, c -> c.speed)
            .add()
            .build();

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new RideComponent(startPos, endPos, speed);
    }
}