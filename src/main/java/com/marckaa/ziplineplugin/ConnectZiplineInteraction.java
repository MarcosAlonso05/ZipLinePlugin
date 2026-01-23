package com.marckaa.ziplineplugin;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
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



        Player player = (Player) commandBuffer.getComponent(interactionContext.getEntity(), Player.getComponentType());
        ZiplineComponent anchor = (ZiplineComponent) BlockModule.get().getComponent(ZiplineComponent.getComponentType(), world, targetPos.x, targetPos.y, targetPos.z);

        if (itemInHand == null || itemInHand.isEmpty() || !itemInHand.getItemId().equals("Guide_Line")) {
            assert anchor != null;
            if(!anchor.isConnected()){
                assert player != null;
                player.sendMessage(Message.raw("You need a GuideLine to connect the zip line"));
            }
            return;
        }

        if (anchor == null) return;

        GuideLineData toolData = itemInHand.getFromMetadataOrNull("GuideData", GuideLineData.CODEC);
        Inventory inventory = player.getInventory();
        int slot = inventory.getActiveHotbarSlot();

        if (toolData == null || !toolData.hasStartPoint) {
            if (anchor.isConnected()) {
                player.sendMessage(Message.raw("This support is already taken"));
                return;
            }

            toolData = new GuideLineData();
            toolData.x = targetPos.x;
            toolData.y = targetPos.y;
            toolData.z = targetPos.z;
            toolData.hasStartPoint = true;

            ItemStack newItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, toolData);
            inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, newItem);
            player.sendMessage(Message.raw("Point A set. Look for an aligned support."));
        }
        else {
            Vector3i posA = new Vector3i(toolData.x, toolData.y, toolData.z);
            Vector3i posB = targetPos;

            if (posA.equals(posB)) {
                player.sendMessage(Message.raw("Same Block"));
                return;
            }
            if (anchor.isConnected()) {
                player.sendMessage(Message.raw("Taken Support"));
                return;
            }
            ZiplineComponent anchorA = (ZiplineComponent) BlockModule.get().getComponent(ZiplineComponent.getComponentType(), world, posA.x, posA.y, posA.z);
            if (anchorA == null || anchorA.isConnected()) {
                player.sendMessage(Message.raw("Origin occupied or broken"));
                inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null));
                return;
            }

            int heightDiff = Math.abs(posA.y - posB.y);
            if (heightDiff < 5) {
                player.sendMessage(Message.raw("(＃＞＜)  You need at least 5 blocks of unevenness"));
                return;
            }

            double distance = posA.distanceTo(posB);
            if (distance < 10.0) {
                player.sendMessage(Message.raw("(＃＞＜)  Too Close (Min 10)."));
                return;
            }

            if (!isAligned(world, posA, posB)) {
                player.sendMessage(Message.raw("(＃＞＜) Supports are not aligned with their orientation"));
                return;
            }

            int horizontalDist = Math.max(Math.abs(posA.x - posB.x), Math.abs(posA.z - posB.z));

            if (heightDiff < 5) {
                player.sendMessage(Message.raw("(＃＞＜) You need at least 5 blocks of unevenness"));
                return;
            }

            double totalDistance = posA.distanceTo(posB);
            if (totalDistance < 10.0) {
                player.sendMessage(Message.raw("(＃＞＜) Too close (Min 10 total blocks)"));
                return;
            }

            if (horizontalDist <= (heightDiff * 2)) {
                int neededDist = (heightDiff * 2) + 1;
                player.sendMessage(Message.raw("(；￣Д￣)  Too inclined"));
                player.sendMessage(Message.raw("For a height of " + heightDiff + ", you need to separate " + neededDist + " Blocks!!"));
                return;
            }

            anchor.setConnection(posA);
            anchorA.setConnection(posB);

            drawCable(world, posA, posB);

            ItemStack cleanItem = itemInHand.withMetadata("GuideData", GuideLineData.CODEC, null);
            inventory.getHotbar().replaceItemStackInSlot((short)slot, itemInHand, cleanItem);
        }
    }



    private void drawCable(World world, Vector3i p1, Vector3i p2) {
        Vector3i low, high;
        if (p1.y < p2.y) { low = p1; high = p2; }
        else { low = p2; high = p1; }

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

            if (yCur < yNext) {
                blockName = "Zip_Line_Rope3";
            } else if (yCur > yPrev) {
                blockName = "Zip_Line_Rope2";
            } else {
                blockName = "Zip_Line_Rope";
            }

            if (k == 1) {
                blockName += "_Start";
            }
            else if (k == steps - 1) {
                blockName += "_End";
            }

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

        if (isGroupNS) {
            return posA.z == posB.z;
        } else if (isGroupEW) {
            return posA.x == posB.x;
        }
        return false;
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) { }

    public static final BuilderCodec<ConnectZiplineInteraction> CODEC = BuilderCodec.builder(ConnectZiplineInteraction.class, ConnectZiplineInteraction::new, SimpleBlockInteraction.CODEC).build();
}