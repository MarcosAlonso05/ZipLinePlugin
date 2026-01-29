package com.marckaa.ziplineplugin;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
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

            // Buscamos el otro soporte (el que estaba conectado a este)
            // Nota: Para acceder a los componentes de bloque, usamos BlockModule normalmente,
            // pero aquí solo necesitamos saber si existe para desconectarlo.
            // ZiplineUtils.destroyCableNetwork se encargará de limpiar la cuerda visualmente.

            // 1. Iniciar la destrucción del cable desde el OTRO extremo (targetPos)
            // Hacemos esto porque el soporte actual (ref) acaba de romperse, así que es más seguro
            // empezar a borrar desde el soporte que sigue vivo hacia atrás.
            if (targetPos != null) {
                ZiplineUtils.destroyCableNetwork(world, targetPos);

                // 2. Desconectar lógicamente el otro soporte
                ZiplineComponent otherAnchor = ZiplineUtils.getZiplineComponent(world, targetPos);
                if (otherAnchor != null) {
                    otherAnchor.setDisconnected();
                }
            }
        }
    }
}