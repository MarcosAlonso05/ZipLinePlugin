package com.marckaa.ziplineplugin;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ConnectZiplineInteraction extends SimpleBlockInteraction {

    public ConnectZiplineInteraction() {
        super();
    }

    @Override
    protected void interactWithBlock(@NonNull World world,
                                     @NonNull CommandBuffer<EntityStore> commandBuffer,
                                     @NonNull InteractionType interactionType,
                                     @NonNull InteractionContext interactionContext,
                                     @Nullable ItemStack itemInHand,
                                     @NonNull Vector3i targetPos,
                                     @NonNull CooldownHandler cooldownHandler) {

        if (itemInHand == null || itemInHand.isEmpty() || !itemInHand.getItemId().equals("Guide_Line")) {
            return;
        }

        Player player = (Player) commandBuffer.getComponent(interactionContext.getEntity(), Player.getComponentType());
        ZiplineComponent anchor = (ZiplineComponent) BlockModule.get().getComponent(ZiplineComponent.getComponentType(), world, targetPos.x, targetPos.y, targetPos.z);

        if (anchor == null) return;

        GuideLineData toolData = itemInHand.getFromMetadataOrNull("GuideData", GuideLineData.CODEC);
        Inventory inventory = player.getInventory();
        int slot = inventory.getActiveHotbarSlot();

        if (toolData == null || !toolData.hasStartPoint) {
            if (anchor.isConnected()) {
                player.sendMessage(Message.raw("§cEste soporte ya está ocupado."));
                return;
            }

            toolData = new GuideLineData();
            toolData.x = targetPos.x;
            toolData.y = targetPos.y;
            toolData.z = targetPos.z;
            toolData.hasStartPoint = true;

            ItemStack newItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, toolData);
            inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, newItem);
            player.sendMessage(Message.raw("§aPunto A fijado. Busca un soporte alineado."));
        }
        else {
            Vector3i posA = new Vector3i(toolData.x, toolData.y, toolData.z);
            Vector3i posB = targetPos;

            if (posA.equals(posB)) {
                player.sendMessage(Message.raw("§cMismo bloque."));
                return;
            }
            if (anchor.isConnected()) {
                player.sendMessage(Message.raw("§cDestino ocupado."));
                return;
            }
            ZiplineComponent anchorA = (ZiplineComponent) BlockModule.get().getComponent(ZiplineComponent.getComponentType(), world, posA.x, posA.y, posA.z);
            if (anchorA == null || anchorA.isConnected()) {
                player.sendMessage(Message.raw("§cOrigen ocupado o roto."));
                inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null));
                return;
            }

            int heightDiff = Math.abs(posA.y - posB.y);
            if (heightDiff < 3) {
                player.sendMessage(Message.raw("§cError: Necesitas al menos 3 bloques de desnivel."));
                return;
            }

            double distance = posA.distanceTo(posB);
            if (distance < 6.0) {
                player.sendMessage(Message.raw("§cError: Demasiado cerca (Min 6)."));
                return;
            }

            if (!isAligned(world, posA, posB)) {
                player.sendMessage(Message.raw("§cError: Los soportes no están alineados con su orientación (Eje invertido)."));
                return;
            }

            anchor.setConnection(posA);
            anchorA.setConnection(posB);

            ItemStack cleanItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null);
            inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, cleanItem);
            player.sendMessage(Message.raw("§a¡Tirolina conectada!"));
        }
    }

    private int getBlockRotation(World world, int x, int y, int z) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null || chunk.getBlockChunk() == null) return 0;
        BlockSection section = chunk.getBlockChunk().getSectionAtBlockY(y);
        return section.getRotationIndex(x & 31, y, z & 31);
    }

    // --- LÓGICA INVERTIDA ---
    private boolean isAligned(World world, Vector3i posA, Vector3i posB) {
        int rotationIndex = getBlockRotation(world, posA.x, posA.y, posA.z);

        // Grupo 1: Norte/Sur (Indices 0 y 2)
        boolean isGroupNS = (rotationIndex == 0 || rotationIndex == 2);

        // Grupo 2: Este/Oeste (Indices 1 y 3)
        boolean isGroupEW = (rotationIndex == 1 || rotationIndex == 3);

        if (isGroupNS) {
            // CAMBIO: Antes pedíamos 'posA.x == posB.x' (Cable por Z).
            // AHORA pedimos 'posA.z == posB.z' (Cable por X).
            return posA.z == posB.z;
        }
        else if (isGroupEW) {
            // CAMBIO: Antes pedíamos 'posA.z == posB.z' (Cable por X).
            // AHORA pedimos 'posA.x == posB.x' (Cable por Z).
            return posA.x == posB.x;
        }

        return false;
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) { }

    public static final BuilderCodec<ConnectZiplineInteraction> CODEC = BuilderCodec.builder(ConnectZiplineInteraction.class, ConnectZiplineInteraction::new, SimpleBlockInteraction.CODEC).build();
}