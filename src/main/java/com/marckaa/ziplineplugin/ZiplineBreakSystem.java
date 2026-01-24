package com.marckaa.ziplineplugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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

        ZiplineComponent brokenAnchor = store.getComponent(ref, ZiplineComponent.getComponentType());

        if (brokenAnchor != null && brokenAnchor.isConnected()) {

            Vector3i targetPos = brokenAnchor.getTarget();
            World world = ((ChunkStore) store.getExternalData()).getWorld();

            Ref<ChunkStore> targetRef = BlockModule.getBlockEntity(world, targetPos.x, targetPos.y, targetPos.z);

            if (targetRef != null && targetRef.isValid()) {
                ZiplineComponent otherAnchor = store.getComponent(targetRef, ZiplineComponent.getComponentType());

                if (otherAnchor != null) {
                    otherAnchor.setDisconnected();
                }
            }
        }
    }
}