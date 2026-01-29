package com.marckaa.ziplineplugin.systems;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.ZiplineUtils;
import com.marckaa.ziplineplugin.components.RideComponent;

import javax.annotation.Nonnull;

public class RideSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
            RideComponent.getComponentType(),
            TransformComponent.getComponentType(),
            Velocity.getComponentType()
    );

    private static final AnimationSlot ANIM_SLOT = AnimationSlot.Action;

    private static final double INERTIA_FACTOR = 0.7;

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

        Vector3d currentPos = transform.getPosition();
        Vector3d currentTarget = ride.isApproaching() ? ride.getAnchorPos() : ride.getEndPos();

        Vector3d direction = new Vector3d(currentTarget).subtract(currentPos).normalize();

        if (!ride.isApproaching()) {
            Vector3d futurePos = new Vector3d(currentPos).addScaled(direction, 1.0);
            World world = ((EntityStore) store.getExternalData()).getWorld();

            if (isPathBlocked(world, futurePos)) {
                Player player = store.getComponent(entityRef, Player.getComponentType());
                stopRide(commandBuffer, entityRef, velocity, ride, direction, false);
                return;
            }
        }

        if (!ride.isApproaching() && ride.getSpeed() < ride.getMaxSpeed()) {
            double newSpeed = ride.getSpeed() + (ride.getAcceleration() * dt);
            if (newSpeed > ride.getMaxSpeed()) newSpeed = ride.getMaxSpeed();
            ride.setSpeed(newSpeed);
        }

        double distSq = currentPos.distanceSquaredTo(currentTarget);
        double threshold = ride.isApproaching() ? 0.25 : 0.5;

        if (distSq < threshold) {
            if (ride.isApproaching()) {
                ride.setApproaching(false);
                commandBuffer.putComponent(entityRef, RideComponent.getComponentType(), ride);
                return;
            } else {
                stopRide(commandBuffer, entityRef, velocity, ride, direction, true);
                return;
            }
        }

        Vector3d velocityVector = direction.scale(ride.getSpeed());
        velocity.addInstruction(velocityVector, (VelocityConfig) null, ChangeVelocityType.Set);
    }

    private boolean isPathBlocked(World world, Vector3d pos) {
        Vector3i feetBlock = pos.toVector3i();
        Vector3i headBlock = new Vector3i(feetBlock.x, feetBlock.y + 1, feetBlock.z);

        if (isSolidObstacle(world, headBlock)) return true;
        if (isSolidObstacle(world, feetBlock)) return true;

        return false;
    }

    private boolean isSolidObstacle(World world, Vector3i pos) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) return false;

        int blockId = chunk.getBlock(pos.x, pos.y, pos.z);
        if (blockId == 0) return false;

        if (ZiplineUtils.isRopeType(BlockType.getAssetMap().getAsset(blockId).getId())) {
            return false;
        }
        return true;
    }

    private void stopRide(CommandBuffer<EntityStore> commandBuffer, Ref<EntityStore> entity, Velocity velocity, RideComponent ride, Vector3d direction, boolean preserveInertia) {

        if (preserveInertia) {
            double exitSpeed = ride.getSpeed() * INERTIA_FACTOR;

            Vector3d exitVector = direction.scale(exitSpeed);

            velocity.addInstruction(exitVector, (VelocityConfig) null, ChangeVelocityType.Set);
        } else {
            velocity.addInstruction(new Vector3d(0, 0, 0), null, ChangeVelocityType.Set);
        }

        commandBuffer.removeComponent(entity, RideComponent.getComponentType());
        AnimationUtils.playAnimation(entity, ANIM_SLOT, "Idle", true, commandBuffer);
    }
}