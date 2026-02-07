package com.marckaa.ziplineplugin.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.ZiplineComponent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TensorInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<TensorInteraction> CODEC = BuilderCodec.builder(TensorInteraction.class, TensorInteraction::new, SimpleBlockInteraction.CODEC).build();

    public TensorInteraction() {
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

        Player player = (Player) commandBuffer.getComponent(context.getEntity(), Player.getComponentType());
        if (player == null) return;

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
            player.sendMessage(Message.raw("The speed was adjusted to " + newSpeed));
        } else {
            player.sendMessage(Message.raw("Limit reached"));
        }
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) { }
}