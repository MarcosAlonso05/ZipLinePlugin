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

    private Vector3d anchorPos;
    private Vector3d endPos;
    private double speed;
    private boolean isApproaching;

    public RideComponent() {}

    public RideComponent(Vector3d anchorPos, Vector3d endPos, double speed, boolean isApproaching) {
        this.anchorPos = anchorPos;
        this.endPos = endPos;
        this.speed = speed;
        this.isApproaching = isApproaching;
    }

    public static ComponentType<EntityStore, RideComponent> getComponentType() {
        return ZiplinePlugin.getInstance().getRideComponentType();
    }

    public Vector3d getAnchorPos() { return anchorPos; }
    public Vector3d getEndPos() { return endPos; }
    public double getSpeed() { return speed; }
    public boolean isApproaching() { return isApproaching; }

    public void setApproaching(boolean approaching) {
        this.isApproaching = approaching;
    }

    public static final BuilderCodec<RideComponent> CODEC = BuilderCodec.builder(RideComponent.class, RideComponent::new)
            .append(new KeyedCodec<>("Anchor", Vector3d.CODEC), (c, v) -> c.anchorPos = v, c -> c.anchorPos).add()
            .append(new KeyedCodec<>("End", Vector3d.CODEC), (c, v) -> c.endPos = v, c -> c.endPos).add()
            .append(new KeyedCodec<>("Speed", Codec.DOUBLE), (c, v) -> c.speed = v, c -> c.speed).add()
            .append(new KeyedCodec<>("IsAppr", Codec.BOOLEAN), (c, v) -> c.isApproaching = v, c -> c.isApproaching).add()
            .build();

    @Override
    public @Nullable Component<EntityStore> clone() {
        return new RideComponent(anchorPos, endPos, speed, isApproaching);
    }
}