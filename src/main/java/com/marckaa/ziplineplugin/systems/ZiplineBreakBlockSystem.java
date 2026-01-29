package com.marckaa.ziplineplugin.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ZiplineBreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public ZiplineBreakBlockSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int entityIndex,
                       @NonNull ArchetypeChunk<EntityStore> chunk,
                       @NonNull Store<EntityStore> store,
                       @NonNull CommandBuffer<EntityStore> commandBuffer,
                       @NonNull BreakBlockEvent event) {

        World world = ((EntityStore) store.getExternalData()).getWorld();
        Vector3i pos = event.getTargetBlock();
        String brokenBlockId = event.getBlockType().getId();

        // 1. Caso Cuerda
        if (ZiplineUtils.isRopeType(brokenBlockId)) {
            checkNeighborsAndDestroy(world, pos);
        }
        // 2. Caso Soporte
        else {
            ZiplineComponent anchor = ZiplineUtils.getZiplineComponent(world, pos);
            if (anchor != null && anchor.isConnected()) {
                ZiplineUtils.disconnectNetwork(world, anchor, pos);
            }
        }
    }

    private void checkNeighborsAndDestroy(World world, Vector3i center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Vector3i neighbor = new Vector3i(center.x + x, center.y + y, center.z + z);

                    if (ZiplineUtils.isRopeBlock(world, neighbor)) {
                        Vector3i anchorPos = ZiplineUtils.findConnectedAnchor(world, neighbor);

                        if (anchorPos != null) {
                            ZiplineComponent anchor = ZiplineUtils.getZiplineComponent(world, anchorPos);
                            if (anchor != null) {
                                ZiplineUtils.disconnectNetwork(world, anchor, anchorPos);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}