package com.marckaa.ziplineplugin;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class ZiplineUtils {

    public static @Nullable Vector3i findConnectedAnchor(World world, Vector3i startPos) {
        if (getZiplineComponent(world, startPos) != null) {
            return startPos;
        }

        Set<Vector3i> visited = new HashSet<>();
        Queue<Vector3i> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        int safetyLimit = 500;
        int iterations = 0;

        while (!queue.isEmpty() && iterations < safetyLimit) {
            Vector3i current = queue.poll();
            iterations++;

            ZiplineComponent comp = getZiplineComponent(world, current);
            if (comp != null) {
                return current;
            }

            if (isRopeBlock(world, current)) {
                addNeighbors(current, queue, visited);
            }
        }

        return null;
    }

    private static @Nullable ZiplineComponent getZiplineComponent(World world, Vector3i pos) {
        return (ZiplineComponent) BlockModule.get().getComponent(
                ZiplineComponent.getComponentType(),
                world,
                pos.x, pos.y, pos.z
        );
    }

    private static boolean isRopeBlock(World world, Vector3i pos) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) return false;

        int blockId = chunk.getBlock(pos.x, pos.y, pos.z);
        if (blockId == 0) return false;

        BlockType type = BlockType.getAssetMap().getAsset(blockId);
        if (type == null) return false;

        String id = type.getId();
        return id.startsWith("Zip_Line_Rope");
    }

    private static void addNeighbors(Vector3i current, Queue<Vector3i> queue, Set<Vector3i> visited) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Vector3i neighbor = new Vector3i(current.x + x, current.y + y, current.z + z);
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }
}