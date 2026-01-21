package com.marckaa.ziplineplugin;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
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

        // Validamos item
        if (itemInHand == null || itemInHand.isEmpty() || !itemInHand.getItemId().equals("Guide_Line")) {
            return;
        }

        Player player = (Player) commandBuffer.getComponent(interactionContext.getEntity(), Player.getComponentType());

        // Obtener el bloque
        ZiplineComponent anchor = (ZiplineComponent) BlockModule.get().getComponent(ZiplineComponent.getComponentType(), world, targetPos.x, targetPos.y, targetPos.z);

        if (anchor == null) return;

        GuideLineData toolData = itemInHand.getFromMetadataOrNull("GuideData", GuideLineData.CODEC);

        Inventory inventory = player.getInventory();
        int slot = inventory.getActiveHotbarSlot();

        // CASO A: Primer clic (Guardar)
        if (toolData == null || !toolData.hasStartPoint) {
            toolData = new GuideLineData();
            toolData.x = targetPos.x;
            toolData.y = targetPos.y;
            toolData.z = targetPos.z;
            toolData.hasStartPoint = true;

            ItemStack newItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, toolData);

            inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, newItem);
            player.sendMessage(Message.raw("Punto A fijado en: " + targetPos));
        }
        // CASO B: Segundo clic (Conectar)
        else {
            if (toolData.x == targetPos.x && toolData.y == targetPos.y && toolData.z == targetPos.z) {
                player.sendMessage(Message.raw("¡No puedes conectar la tirolina al mismo bloque!"));
                return;
            }

            Vector3i startPos = new Vector3i(toolData.x, toolData.y, toolData.z);
            anchor.setConnection(startPos);

            // CORRECCIÓN FINAL AQUÍ:
            // Usamos 'withMetadata' pasando 'null' como dato.
            // Según el código fuente de ItemStack, esto ejecuta 'clonedMeta.remove(key)'.
            ItemStack cleanItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null);

            inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, cleanItem);
            player.sendMessage(Message.raw("¡Tirolina conectada entre " + startPos + " y " + targetPos + "!"));
        }
    }

    // Dejar vacío, es necesario para compilar
    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) {
    }

    public static final BuilderCodec<ConnectZiplineInteraction> CODEC = BuilderCodec.builder(ConnectZiplineInteraction.class, ConnectZiplineInteraction::new, SimpleBlockInteraction.CODEC).build();
}