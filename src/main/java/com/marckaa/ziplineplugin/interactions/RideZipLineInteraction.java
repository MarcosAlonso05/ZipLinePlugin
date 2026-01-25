package com.marckaa.ziplineplugin.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.RideComponent;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class RideZipLineInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<RideZipLineInteraction> CODEC = BuilderCodec.builder(RideZipLineInteraction.class,
            RideZipLineInteraction::new, SimpleBlockInteraction.CODEC).build();

    public RideZipLineInteraction() {
        super();
    }

    @Override
    protected void interactWithBlock(@NonNull World world,
                                     @NonNull CommandBuffer<EntityStore> commandBuffer,
                                     @NonNull InteractionType interactionType,
                                     @NonNull InteractionContext context,
                                     @Nullable ItemStack itemInHand,
                                     @NonNull Vector3i targetBlock,
                                     @NonNull CooldownHandler cooldownHandler) {

        Ref<EntityStore> playerRef = context.getEntity();
        if (playerRef == null) return;
        Store<EntityStore> store = playerRef.getStore();

        Player playerComp = store.getComponent(playerRef, Player.getComponentType());

        if (store.getComponent(playerRef, RideComponent.getComponentType()) != null) {
            commandBuffer.removeComponent(playerRef, RideComponent.getComponentType());
            return;
        }

        Vector3i clickedBlock = targetBlock;
        Vector3i anchorPosA = ZiplineUtils.findConnectedAnchor(world, clickedBlock);

        if (anchorPosA == null) {
            if (playerComp != null) playerComp.sendMessage(Message.raw("Rope without anchor"));
            return;
        }

        ZiplineComponent compA = (ZiplineComponent) BlockModule.get().getComponent(
                ZiplineComponent.getComponentType(), world, anchorPosA.x, anchorPosA.y, anchorPosA.z
        );

        if (compA == null || !compA.isConnected() || compA.getTarget() == null) {
            return;
        }

        Vector3i anchorPosB = compA.getTarget();

        Vector3i endPos;
        if (anchorPosA.y >= anchorPosB.y) {
            endPos = anchorPosB;
        } else {
            endPos = anchorPosA;
        }

        Vector3d endVec = new Vector3d(endPos.x + 0.5, endPos.y - 0.5, endPos.z + 0.5);
        Vector3d currentClickPos = new Vector3d(clickedBlock.x + 0.5, clickedBlock.y - 0.5, clickedBlock.z + 0.5);

        RideComponent rideData = new RideComponent(currentClickPos, endVec, 0.8);

        commandBuffer.putComponent(playerRef, RideComponent.getComponentType(), rideData);

        if (playerComp != null) {
            playerComp.sendMessage(Message.raw("§e[Cuerda] §aZipline OK! §7Destino: " + endPos.x + ", " + endPos.y));
        }
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType type, @NonNull InteractionContext context, @Nullable ItemStack item, @NonNull World world, @NonNull Vector3i target) {}
}