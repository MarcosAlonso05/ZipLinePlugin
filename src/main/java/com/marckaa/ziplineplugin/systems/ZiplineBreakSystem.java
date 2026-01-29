package com.marckaa.ziplineplugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import org.jspecify.annotations.NonNull;

public class ZiplineBreakSystem extends RefSystem<ChunkStore> {

    @Override
    public @NonNull Query<ChunkStore> getQuery() {
        return ZiplineComponent.getComponentType();
    }

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason reason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason reason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {

        if (reason == RemoveReason.UNLOAD) {
            return;
        }

        ZiplineComponent brokenAnchor = store.getComponent(ref, ZiplineComponent.getComponentType());

        if (brokenAnchor != null && brokenAnchor.isConnected()) {

            Vector3i targetPos = brokenAnchor.getTarget();
            World world = ((ChunkStore) store.getExternalData()).getWorld();

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