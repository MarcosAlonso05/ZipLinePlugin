package com.marckaa.ziplineplugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public class ZiplineComponent implements Component<ChunkStore> {

    private Vector3i target;
    private boolean isConnected = false;

    public ZiplineComponent() {}

    public static ComponentType<ChunkStore, ZiplineComponent> getComponentType() {
        return ZiplinePlugin.getInstance().getZiplineComponentType();
    }

    public void setConnection(Vector3i target) {
        this.target = target;
        this.isConnected = true;
    }

    public void setDisconnected() {
        this.target = null;
        this.isConnected = false;
    }

    public Vector3i getTarget() { return target; }
    public boolean isConnected() { return isConnected; }

    public static final BuilderCodec<ZiplineComponent> CODEC = BuilderCodec.builder(ZiplineComponent.class, ZiplineComponent::new)
            .append(new KeyedCodec<>("Target", Vector3i.CODEC), (c, v) -> c.target = v, c -> c.target)
            .add()
            .append(new KeyedCodec<>("IsConnected", Codec.BOOLEAN), (c, v) -> c.isConnected = v, c -> c.isConnected)
            .add()
            .build();

    @Override
    public @Nullable Component<ChunkStore> clone() {
        ZiplineComponent copy = new ZiplineComponent();
        if (this.target != null) {
            copy.target = new Vector3i(this.target.x, this.target.y, this.target.z);
        }
        copy.isConnected = this.isConnected;
        return copy;
    }

    @Override
    public @Nullable Component<ChunkStore> cloneSerializable() {
        return this.clone();
    }
}