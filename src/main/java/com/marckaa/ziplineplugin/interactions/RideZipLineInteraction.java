package com.marckaa.ziplineplugin.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer; // IMPORTANTE
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction; // Cambiamos a BlockInteraction
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.RideComponent;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

// CAMBIO: Extendemos SimpleBlockInteraction igual que en ConnectZiplineInteraction
public class RideZipLineInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<RideZipLineInteraction> CODEC = BuilderCodec.builder(RideZipLineInteraction.class,
            RideZipLineInteraction::new, SimpleBlockInteraction.CODEC).build();

    public RideZipLineInteraction() {
        super();
    }

    // CAMBIO: Usamos interactWithBlock que nos provee el CommandBuffer
    @Override
    protected void interactWithBlock(@NonNull World world,
                                     @NonNull CommandBuffer<EntityStore> commandBuffer, // <--- LA SOLUCIÓN
                                     @NonNull InteractionType interactionType,
                                     @NonNull InteractionContext context,
                                     @Nullable ItemStack itemInHand,
                                     @NonNull Vector3i targetBlock,
                                     @NonNull CooldownHandler cooldownHandler) {

        Ref<EntityStore> playerRef = context.getEntity();
        if (playerRef == null) return;

        // Obtenemos el store solo para leer (getComponent), no para escribir
        Store<EntityStore> store = playerRef.getStore();

        // Verificar si ya tiene el componente (Lectura segura)
        if (store.getComponent(playerRef, RideComponent.getComponentType()) != null) {
            return;
        }

        // --- LÓGICA DE BÚSQUEDA ---
        Vector3i clickedBlock = targetBlock; // targetBlock ya es Vector3i aquí
        Vector3i anchorPosA = ZiplineUtils.findConnectedAnchor(world, clickedBlock);

        if (anchorPosA == null) {
            // Enviamos mensaje al jugador usando el contexto para obtener el componente Player
            // Nota: No podemos usar 'player.sendMessage' directamente si no sacamos el componente Player primero,
            // pero para simplificar el fix del crash, asumimos que la lógica es correcta.
            // Para enviar mensaje necesitamos sacar el componente Player del commandBuffer o store:
            com.hypixel.hytale.server.core.entity.entities.Player playerComp =
                    store.getComponent(playerRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (playerComp != null) playerComp.sendMessage(Message.raw("§cError: Cuerda sin anclaje."));
            return;
        }

        ZiplineComponent compA = (ZiplineComponent) BlockModule.get().getComponent(
                ZiplineComponent.getComponentType(), world, anchorPosA.x, anchorPosA.y, anchorPosA.z
        );

        if (compA == null || !compA.isConnected() || compA.getTarget() == null) {
            return;
        }

        Vector3i anchorPosB = compA.getTarget();

        // Determinar sentido (Gravedad)
        Vector3i endPos;
        if (anchorPosA.y >= anchorPosB.y) {
            endPos = anchorPosB;
        } else {
            endPos = anchorPosA;
        }

        // Vectores
        Vector3d endVec = new Vector3d(endPos.x + 0.5, endPos.y - 0.5, endPos.z + 0.5);
        Vector3d currentClickPos = new Vector3d(clickedBlock.x + 0.5, clickedBlock.y - 0.5, clickedBlock.z + 0.5);

        // Aplicar componente
        RideComponent rideData = new RideComponent(currentClickPos, endVec, 0.8);

        // --- SOLUCIÓN DEL CRASH ---
        // Usamos commandBuffer en lugar de store.addComponent
        commandBuffer.addComponent(playerRef, RideComponent.getComponentType(), rideData);

        // Mensaje de éxito
        com.hypixel.hytale.server.core.entity.entities.Player playerComp =
                store.getComponent(playerRef, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
        if (playerComp != null) {
            playerComp.sendMessage(Message.raw("§e[Cuerda] §aZipline OK! §7Destino: " + endPos.x + ", " + endPos.y));
        }
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType type, @NonNull InteractionContext context, @Nullable ItemStack item, @NonNull World world, @NonNull Vector3i target) {}
}