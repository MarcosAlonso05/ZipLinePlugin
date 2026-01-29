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

    public static void disconnectNetwork(World world, ZiplineComponent anchorA, Vector3i posA) {
        if (anchorA == null) return;

        Vector3i posB = anchorA.getTarget();

        anchorA.setDisconnected();

        if (posB != null) {
            ZiplineComponent anchorB = getZiplineComponent(world, posB);
            if (anchorB != null) {
                anchorB.setDisconnected();
                destroyCableNetwork(world, posB);
            }
        }

        destroyCableNetwork(world, posA);
    }

    public static void destroyCableNetwork(World world, Vector3i startPoint) {
        Set<Vector3i> visited = new HashSet<>();
        Queue<Vector3i> queue = new LinkedList<>();

        addRopeNeighbors(world, startPoint, queue, visited);

        int safetyLimit = 2000;
        int iterations = 0;

        while (!queue.isEmpty() && iterations < safetyLimit) {
            Vector3i current = queue.poll();
            iterations++;

            world.setBlock(current.x, current.y, current.z, "Empty");

            addRopeNeighbors(world, current, queue, visited);
        }
    }

    private static void addRopeNeighbors(World world, Vector3i center, Queue<Vector3i> queue, Set<Vector3i> visited) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Vector3i neighbor = new Vector3i(center.x + x, center.y + y, center.z + z);

                    if (visited.contains(neighbor)) continue;

                    if (isRopeBlock(world, neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }


    public static boolean isRopeType(String blockId) {
        return blockId != null && blockId.startsWith("Zip_Line_Rope");
    }

    public static boolean isRopeBlock(World world, Vector3i pos) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) return false;

        int blockId = chunk.getBlock(pos.x, pos.y, pos.z);
        if (blockId == 0) return false;

        BlockType type = BlockType.getAssetMap().getAsset(blockId);
        return type != null && isRopeType(type.getId());
    }

    public static @Nullable Vector3i findConnectedAnchor(World world, Vector3i startPos) {
        if (getZiplineComponent(world, startPos) != null) {
            return startPos;
        }

        Set<Vector3i> visited = new HashSet<>();
        Queue<Vector3i> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        int safetyLimit = 1000;
        int iterations = 0;

        while (!queue.isEmpty() && iterations < safetyLimit) {
            Vector3i current = queue.poll();
            iterations++;

            ZiplineComponent comp = getZiplineComponent(world, current);
            if (comp != null) {
                return current;
            }

            if (isRopeBlock(world, current)) {
                addNeighborsSimple(current, queue, visited);
            }
        }
        return null;
    }

    public static @Nullable ZiplineComponent getZiplineComponent(World world, Vector3i pos) {
        return (ZiplineComponent) BlockModule.get().getComponent(
                ZiplineComponent.getComponentType(),
                world,
                pos.x, pos.y, pos.z
        );
    }

    private static void addNeighborsSimple(Vector3i current, Queue<Vector3i> queue, Set<Vector3i> visited) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    Vector3i n = new Vector3i(current.x + x, current.y + y, current.z + z);
                    if (!visited.contains(n)) {
                        visited.add(n);
                        queue.add(n);
                    }
                }
            }
        }
    }
}