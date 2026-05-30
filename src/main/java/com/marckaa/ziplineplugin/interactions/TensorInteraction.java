package com.marckaa.ziplineplugin.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.ZiplineComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TensorInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<TensorInteraction> CODEC = BuilderCodec.builder(TensorInteraction.class, TensorInteraction::new, SimpleBlockInteraction.CODEC).build();

    public TensorInteraction() {
        super();
    }

    @Override
    protected void interactWithBlock(@Nonnull World world,
                                     @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType interactionType,
                                     @Nonnull InteractionContext context,
                                     @Nullable ItemStack itemInHand,
                                     @Nonnull Vector3i targetBlock,
                                     @Nonnull CooldownHandler cooldownHandler) {

        Ref<EntityStore> entityRef = context.getEntity();
        PlayerRef playerRef = (PlayerRef) commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());

        ZiplineComponent anchor = ZiplineUtils.getZiplineComponent(world, targetBlock);

        if (anchor == null && ZiplineUtils.isRopeBlock(world, targetBlock)) {
            Vector3i anchorPos = ZiplineUtils.findConnectedAnchor(world, targetBlock);
            if (anchorPos != null) {
                anchor = ZiplineUtils.getZiplineComponent(world, anchorPos);
            }
        }

        if (anchor == null) {
            return;
        }

        double currentSpeed = anchor.getSpeed();
        double newSpeed = currentSpeed;

        if (interactionType == InteractionType.Primary) {
            newSpeed -= 1.0;
            if (newSpeed < 5.0) newSpeed = 5.0;
        }
        else if (interactionType == InteractionType.Secondary) {
            newSpeed += 1.0;
            if (newSpeed > 30.0) newSpeed = 30.0;
        }

        if (newSpeed != currentSpeed) {
            anchor.setSpeed(newSpeed);
            if (playerRef != null) playerRef.sendMessage(Message.raw("The speed was adjusted to " + newSpeed));
        } else {
            if (playerRef != null) playerRef.sendMessage(Message.raw("Limit reached"));
        }
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nullable ItemStack itemStack, @Nonnull World world, @Nonnull Vector3i vector3i) { }
}