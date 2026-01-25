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

    /**
     * Busca un anclaje (bloque con ZiplineComponent) conectado a la red de cuerdas.
     * @param world El mundo.
     * @param startPos La posición del bloque (cuerda o soporte) donde se hizo clic.
     * @return La posición del anclaje encontrado, o null si no encuentra nada o la cuerda está rota.
     */
    public static @Nullable Vector3i findConnectedAnchor(World world, Vector3i startPos) {
        // Si el bloque clicado YA es un anclaje, lo devolvemos directamente
        if (getZiplineComponent(world, startPos) != null) {
            return startPos;
        }

        // Si no, iniciamos una búsqueda por los vecinos
        Set<Vector3i> visited = new HashSet<>();
        Queue<Vector3i> queue = new LinkedList<>();

        queue.add(startPos);
        visited.add(startPos);

        int safetyLimit = 500; // Límite para evitar bucles infinitos o lagazos masivos
        int iterations = 0;

        while (!queue.isEmpty() && iterations < safetyLimit) {
            Vector3i current = queue.poll();
            iterations++;

            // 1. Miramos si este bloque es un anclaje
            ZiplineComponent comp = getZiplineComponent(world, current);
            if (comp != null) {
                return current; // ¡Encontrado!
            }

            // 2. Si no es anclaje, verificamos si es una cuerda válida
            if (isRopeBlock(world, current)) {
                // Añadimos los vecinos a la cola de búsqueda
                addNeighbors(current, queue, visited);
            }
        }

        return null; // No se encontró ningún anclaje conectado
    }

    /**
     * Helper para obtener el componente de forma segura
     */
    private static @Nullable ZiplineComponent getZiplineComponent(World world, Vector3i pos) {
        return (ZiplineComponent) BlockModule.get().getComponent(
                ZiplineComponent.getComponentType(),
                world,
                pos.x, pos.y, pos.z
        );
    }

    /**
     * Verifica si el bloque es una cuerda basándose en su ID.
     */
    private static boolean isRopeBlock(World world, Vector3i pos) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) return false;

        int blockId = chunk.getBlock(pos.x, pos.y, pos.z);
        if (blockId == 0) return false;

        BlockType type = BlockType.getAssetMap().getAsset(blockId);
        if (type == null) return false;

        // Aquí comparamos con los nombres que usaste en ConnectZiplineInteraction
        String id = type.getId();
        return id.startsWith("Zip_Line_Rope");
    }

    /**
     * Añade los 26 vecinos posibles a la cola (si no han sido visitados).
     */
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