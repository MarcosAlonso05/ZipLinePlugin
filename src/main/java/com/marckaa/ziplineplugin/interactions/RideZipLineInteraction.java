package com.marckaa.ziplineplugin.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.RideComponent;
import com.marckaa.ziplineplugin.components.ZiplineComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RideZipLineInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<RideZipLineInteraction> CODEC = BuilderCodec.builder(RideZipLineInteraction.class,
            RideZipLineInteraction::new, SimpleBlockInteraction.CODEC).build();

    private static final String ZIPLINE_ANIMATION = "RideZipLine";
    private static final AnimationSlot ANIM_SLOT = AnimationSlot.Action;

    public RideZipLineInteraction() {
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

        Ref<EntityStore> playerRef = context.getEntity();
        if (playerRef == null) return;

        Store<EntityStore> store = playerRef.getStore();

        RideComponent currentRide = store.getComponent(playerRef, RideComponent.getComponentType());
        if (currentRide != null) {
            commandBuffer.removeComponent(playerRef, RideComponent.getComponentType());

            Velocity velocity = store.getComponent(playerRef, Velocity.getComponentType());
            if (velocity != null) {
                Vector3d direction = new Vector3d(currentRide.getEndPos()).sub(currentRide.getAnchorPos()).normalize();

                double exitSpeed = currentRide.getSpeed() * 0.7;
                Vector3d exitVelocity = direction.mul(exitSpeed);

                exitVelocity.y += 0.2;

                velocity.addInstruction(exitVelocity, (VelocityConfig) null, ChangeVelocityType.Set);
            }

            AnimationUtils.playAnimation(playerRef, ANIM_SLOT, "Idle", true, store);
            return;
        }

        Vector3i clickedBlock = targetBlock;
        Vector3i anchorPosA = ZiplineUtils.findConnectedAnchor(world, clickedBlock);
        if (anchorPosA == null) return;

        ZiplineComponent compA = (ZiplineComponent) BlockModule.get().getComponent(
                ZiplineComponent.getComponentType(), world, anchorPosA.x, anchorPosA.y, anchorPosA.z
        );
        if (compA == null || !compA.isConnected() || compA.getTarget() == null) return;

        double startSpeed = compA.getSpeed();

        Vector3i anchorPosB = compA.getTarget();
        Vector3i endPos;
        if (anchorPosA.y > anchorPosB.y) endPos = anchorPosB;
        else if (anchorPosA.y < anchorPosB.y) endPos = anchorPosA;
        else return;

        Vector3d anchorVec = new Vector3d(clickedBlock.x + 0.5, clickedBlock.y - 1.9, clickedBlock.z + 0.5);
        Vector3d endVec = new Vector3d(endPos.x + 0.5, endPos.y - 1.9, endPos.z + 0.5);

        double acceleration = 7.0;
        double maxSpeed = 40.0;

        RideComponent rideData = new RideComponent(
                anchorVec,
                endVec,
                startSpeed,
                acceleration,
                maxSpeed,
                true
        );
        commandBuffer.putComponent(playerRef, RideComponent.getComponentType(), rideData);

        Velocity velocity = store.getComponent(playerRef, Velocity.getComponentType());
        TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());

        if (velocity != null && playerTransform != null) {
            Vector3d direction = new Vector3d(anchorVec).sub(playerTransform.getPosition()).normalize().mul(startSpeed);
            velocity.addInstruction(direction, (VelocityConfig) null, ChangeVelocityType.Set);
        }

        AnimationUtils.playAnimation(playerRef, ANIM_SLOT, ZIPLINE_ANIMATION, true, store);
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nullable ItemStack itemStack, @Nonnull World world, @Nonnull Vector3i vector3i) {
    }
}