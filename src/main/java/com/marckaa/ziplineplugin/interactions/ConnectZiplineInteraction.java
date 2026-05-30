package com.marckaa.ziplineplugin.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.AnimationSlot;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.AnimationUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.marckaa.ziplineplugin.GuideLineData;
import com.marckaa.ziplineplugin.components.RideComponent;
import com.marckaa.ziplineplugin.components.ZiplineComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConnectZiplineInteraction extends SimpleBlockInteraction {

    private static final String ZIPLINE_ANIMATION = "RideZipLine";
    private static final AnimationSlot ANIM_SLOT = AnimationSlot.Action;

    public ConnectZiplineInteraction() {
        super();
    }

    @Override
    protected void interactWithBlock(@Nonnull World world,
                                     @Nonnull CommandBuffer<EntityStore> commandBuffer,
                                     @Nonnull InteractionType interactionType,
                                     @Nonnull InteractionContext interactionContext,
                                     @Nullable ItemStack itemInHand,
                                     @Nonnull Vector3i targetPos,
                                     @Nonnull CooldownHandler cooldownHandler) {

        Ref<EntityStore> entityRef = interactionContext.getEntity();
        PlayerRef playerRef = (PlayerRef) commandBuffer.getComponent(entityRef, PlayerRef.getComponentType());
        ZiplineComponent anchor = (ZiplineComponent) BlockModule.get().getComponent(ZiplineComponent.getComponentType(), world, targetPos.x, targetPos.y, targetPos.z);

        if (anchor == null) return;

        boolean isHandEmpty = (itemInHand == null || itemInHand.isEmpty());
        boolean isNotGuideLine = isHandEmpty || !itemInHand.getItemId().equals("Guide_Line");

        if (isNotGuideLine) {
            if (commandBuffer.getStore().getComponent(entityRef, RideComponent.getComponentType()) != null) {
                commandBuffer.removeComponent(entityRef, RideComponent.getComponentType());

                Velocity velocity = commandBuffer.getStore().getComponent(entityRef, Velocity.getComponentType());
                if (velocity != null) {
                    velocity.addInstruction(new Vector3d(0, 0.5, 0), null, ChangeVelocityType.Set);
                }

                AnimationUtils.playAnimation(entityRef, ANIM_SLOT, "Idle", true, commandBuffer);
                return;
            }

            if (anchor.isConnected() && anchor.getTarget() != null) {
                Vector3i targetB = anchor.getTarget();
                Vector3i endPos;

                if (targetPos.y > targetB.y) {
                    endPos = targetB;
                } else if (targetPos.y < targetB.y) {
                    if (playerRef != null) playerRef.sendMessage(Message.raw("You're already down"));
                    return;
                } else {
                    return;
                }

                Vector3d anchorVec = new Vector3d(targetPos.x + 0.5, targetPos.y - 2.1, targetPos.z + 0.5);
                Vector3d endVec = new Vector3d(endPos.x + 0.5, endPos.y - 2.1, endPos.z + 0.5);

                double startSpeed = anchor.getSpeed();
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
                commandBuffer.putComponent(entityRef, RideComponent.getComponentType(), rideData);

                Velocity velocity = commandBuffer.getStore().getComponent(entityRef, Velocity.getComponentType());
                TransformComponent transform = commandBuffer.getStore().getComponent(entityRef, TransformComponent.getComponentType());

                if (velocity != null && transform != null) {
                    Vector3d direction = new Vector3d(anchorVec).sub(transform.getPosition()).normalize().mul(startSpeed);
                    velocity.addInstruction(direction, (VelocityConfig) null, ChangeVelocityType.Set);
                }

                AnimationUtils.playAnimation(
                        entityRef,
                        ANIM_SLOT,
                        ZIPLINE_ANIMATION,
                        true,
                        commandBuffer
                );

            } else {
                if (playerRef != null) playerRef.sendMessage(Message.raw("Support disconnected, use a Guide Line"));
            }
            return;
        }

        GuideLineData toolData = itemInHand.getFromMetadataOrNull("GuideData", GuideLineData.CODEC);

        InventoryComponent.Hotbar hotbarComp = commandBuffer.getStore().getComponent(entityRef, InventoryComponent.Hotbar.getComponentType());
        int slot = hotbarComp != null ? hotbarComp.getActiveSlot() : -1;
        ItemContainer hotbarContainer = hotbarComp != null ? hotbarComp.getInventory() : null;

        if (toolData == null || !toolData.hasStartPoint) {
            if (anchor.isConnected()) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("This support is already taken"));
                return;
            }

            toolData = new GuideLineData();
            toolData.x = targetPos.x;
            toolData.y = targetPos.y;
            toolData.z = targetPos.z;
            toolData.hasStartPoint = true;

            ItemStack newItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, toolData);
            if (hotbarContainer != null && slot != -1) {
                hotbarContainer.replaceItemStackInSlot((short)slot, itemInHand, newItem);
            }
            if (playerRef != null) playerRef.sendMessage(Message.raw("Point A set."));
        }
        else {
            Vector3i posA = new Vector3i(toolData.x, toolData.y, toolData.z);
            Vector3i posB = targetPos;

            if (posA.equals(posB)) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("Same Block"));
                return;
            }
            if (anchor.isConnected()) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("Taken Support"));
                return;
            }
            ZiplineComponent anchorA = (ZiplineComponent) BlockModule.get().getComponent(ZiplineComponent.getComponentType(), world, posA.x, posA.y, posA.z);
            if (anchorA == null || anchorA.isConnected()) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("Origin occupied or broken"));
                if (hotbarContainer != null && slot != -1) {
                    hotbarContainer.replaceItemStackInSlot((short)slot, itemInHand, itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null));
                }
                return;
            }

            int heightDiff = Math.abs(posA.y - posB.y);
            if (heightDiff < 5) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("(＃＞＜)  Min 5 blocks unevenness"));
                return;
            }

            double distance = posA.distance(posB);
            if (distance < 10.0) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("(＃＞＜)  Too Close (Min 10)."));
                return;
            }

            if (!isAligned(world, posA, posB)) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("(＃＞＜) Not aligned"));
                return;
            }

            int horizontalDist = Math.max(Math.abs(posA.x - posB.x), Math.abs(posA.z - posB.z));
            if (horizontalDist <= (heightDiff * 2)) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("(；￣Д￣)  Too inclined"));
                return;
            }

            int requiredAmount = (int) Math.ceil(posA.distance(posB) * 0.8);
            String materialId = "Ingredient_Fibre";

            CombinedItemContainer container = InventoryComponent.getCombined(commandBuffer, entityRef, InventoryComponent.HOTBAR_FIRST);
            int available = container.countItemStacks(stack -> stack.getItemId().equals(materialId));

            if (available < requiredAmount) {
                if (playerRef != null) playerRef.sendMessage(Message.raw("You don't have enough fiber. You need: " + requiredAmount));
                if (hotbarContainer != null && slot != -1) {
                    hotbarContainer.replaceItemStackInSlot((short)slot, itemInHand, itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null));
                }
                return;
            }

            container.removeItemStack(new ItemStack(materialId, requiredAmount));

            anchor.setConnection(posA);
            anchorA.setConnection(posB);

            drawCable(world, posA, posB);

            ItemStack cleanItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null);
            if (hotbarContainer != null && slot != -1) {
                hotbarContainer.replaceItemStackInSlot((short)slot, itemInHand, cleanItem);
            }
        }
    }

    private void drawCable(World world, Vector3i p1, Vector3i p2) {
        Vector3i low = new Vector3i();
        Vector3i high = new Vector3i();
        if (p1.y < p2.y) { low.set(p1); high.set(p2); }
        else { low.set(p2); high.set(p1); }

        int dx = high.x - low.x;
        int dy = high.y - low.y;
        int dz = high.z - low.z;

        int steps = Math.max(Math.abs(dx), Math.abs(dz));
        if (steps == 0) return;

        int rotation;
        if (Math.abs(dx) > Math.abs(dz)) {
            rotation = (dx > 0) ? 3 : 1;
        } else {
            rotation = (dz > 0) ? 2 : 0;
        }

        for (int k = 1; k < steps; k++) {
            int curX = low.x + (dx * k / steps);
            int curZ = low.z + (dz * k / steps);
            int yPrev = low.y + (dy * (k - 1) / steps);
            int yCur  = low.y + (dy * k / steps);
            int yNext = low.y + (dy * (k + 1) / steps);

            if (curX == high.x && yCur == high.y && curZ == high.z) break;

            String blockName;
            if (yCur < yNext) blockName = "Zip_Line_Rope3";
            else if (yCur > yPrev) blockName = "Zip_Line_Rope2";
            else blockName = "Zip_Line_Rope";

            if (k == 1) blockName += "_Start";
            else if (k == steps - 1) blockName += "_End";

            setBlock(world, curX, yCur, curZ, blockName, rotation);
        }
    }

    private void setBlock(World world, int x, int y, int z, String blockAssetName, int rotation) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null || chunk.getBlockChunk() == null) return;
        BlockSection section = chunk.getBlockChunk().getSectionAtBlockY(y);
        if (section == null) return;

        int blockId = BlockType.getBlockIdOrUnknown(blockAssetName, "ErrorBlock", new Object[0]);
        if (blockId == -1) return;

        section.set(x & 31, y & 31, z & 31, blockId, rotation, 0);
    }

    private int getBlockRotation(World world, int x, int y, int z) {
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(x, z));
        if (chunk == null || chunk.getBlockChunk() == null) return 0;
        BlockSection section = chunk.getBlockChunk().getSectionAtBlockY(y);
        return section.getRotationIndex(x & 31, y, z & 31);
    }

    private boolean isAligned(World world, Vector3i posA, Vector3i posB) {
        int rotationIndex = getBlockRotation(world, posA.x, posA.y, posA.z);
        boolean isGroupNS = (rotationIndex == 0 || rotationIndex == 2);
        boolean isGroupEW = (rotationIndex == 1 || rotationIndex == 3);

        if (isGroupNS) return posA.z == posB.z;
        else if (isGroupEW) return posA.x == posB.x;
        return false;
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nullable ItemStack itemStack, @Nonnull World world, @Nonnull Vector3i vector3i) { }

    public static final BuilderCodec<ConnectZiplineInteraction> CODEC = BuilderCodec.builder(ConnectZiplineInteraction.class, ConnectZiplineInteraction::new, SimpleBlockInteraction.CODEC).build();
}