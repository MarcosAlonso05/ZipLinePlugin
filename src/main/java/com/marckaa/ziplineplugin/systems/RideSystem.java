package com.marckaa.ziplineplugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.components.RideComponent;

import javax.annotation.Nonnull;

public class RideSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
            RideComponent.getComponentType(),
            TransformComponent.getComponentType(),
            Velocity.getComponentType()
    );

    private static final AnimationSlot ANIM_SLOT = AnimationSlot.Action;

    @Override
    public @Nonnull Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float dt, int entityIndex, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        RideComponent ride = chunk.getComponent(entityIndex, RideComponent.getComponentType());
        TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
        Velocity velocity = chunk.getComponent(entityIndex, Velocity.getComponentType());
        Ref<EntityStore> entityRef = chunk.getReferenceTo(entityIndex);

        if (ride == null || transform == null || velocity == null) return;

        if (!ride.isApproaching() && ride.getSpeed() < ride.getMaxSpeed()) {
            double newSpeed = ride.getSpeed() + (ride.getAcceleration() * dt);
            if (newSpeed > ride.getMaxSpeed()) {
                newSpeed = ride.getMaxSpeed();
            }
            ride.setSpeed(newSpeed);
        }

        Vector3d currentPos = transform.getPosition();
        Vector3d currentTarget;

        if (ride.isApproaching()) {
            currentTarget = ride.getAnchorPos();
        } else {
            currentTarget = ride.getEndPos();
        }

        double distSq = currentPos.distanceSquaredTo(currentTarget);

        if (distSq < 0.25) {
            if (ride.isApproaching()) {
                ride.setApproaching(false);
                commandBuffer.putComponent(entityRef, RideComponent.getComponentType(), ride);
                return;
            } else {
                stopRide(commandBuffer, entityRef, velocity);
                return;
            }
        }

        Vector3d direction = new Vector3d(currentTarget).subtract(currentPos).normalize();
        Vector3d velocityVector = direction.scale(ride.getSpeed());

        velocity.addInstruction(
                velocityVector,
                (VelocityConfig) null,
                ChangeVelocityType.Set
        );
    }

    private void stopRide(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> entity, Velocity velocity) {
        velocity.addInstruction(new Vector3d(0, 0, 0), null, ChangeVelocityType.Set);

        commandBuffer.removeComponent(entity, RideComponent.getComponentType());

        AnimationUtils.playAnimation(entity, ANIM_SLOT, "Idle", true, commandBuffer);
    }
}