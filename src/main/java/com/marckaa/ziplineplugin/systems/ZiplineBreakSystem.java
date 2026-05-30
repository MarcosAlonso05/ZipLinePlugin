package com.marckaa.ziplineplugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.ZiplineComponent;

import javax.annotation.Nonnull;

public class ZiplineBreakSystem extends RefSystem<ChunkStore> {

    @Override
    public @Nonnull Query<ChunkStore> getQuery() {
        return ZiplineComponent.getComponentType();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {

        if (reason == RemoveReason.UNLOAD) {
            return;
        }

        if (store.isShutdown()) {
            return;
        }

        World world = ((ChunkStore) store.getExternalData()).getWorld();

        if (!world.isAlive()) {
            return;
        }

        if (world.getPlayerCount() == 0) {
            return;
        }

        ZiplineComponent brokenAnchor = store.getComponent(ref, ZiplineComponent.getComponentType());

        if (brokenAnchor != null && brokenAnchor.isConnected()) {

            Vector3i targetPos = brokenAnchor.getTarget();

            if (targetPos != null) {
                ZiplineUtils.destroyCableNetwork(world, targetPos);

                ZiplineComponent otherAnchor = ZiplineUtils.getZiplineComponent(world, targetPos);
                if (otherAnchor != null) {
                    otherAnchor.setDisconnected();
                }
            }
        }
    }
}