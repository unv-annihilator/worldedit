// $Id$
/*
 * WorldEdit
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.worldedit.operations;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bags.BlockBagException;
import com.sk89q.worldedit.bags.UnplaceableBlockException;
import com.sk89q.worldedit.blocks.*;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.regions.Region;

import java.util.*;
import java.util.Vector;

/**
 * Represents a WorldEdit operation.
 *
 * @author sk89q
 */
public abstract class Operation<T> {

    /**
     * Stores the original blocks before modification.
     */
    protected DoubleArrayList<BlockVector, BaseBlock> original =
            new DoubleArrayList<BlockVector, BaseBlock>(true);

    /**
     * Stores the current blocks.
     */
    protected DoubleArrayList<BlockVector, BaseBlock> current =
            new DoubleArrayList<BlockVector, BaseBlock>(false);

    /**
     * Blocks that should be placed before last.
     */
    protected DoubleArrayList<BlockVector, BaseBlock> queueAfter =
            new DoubleArrayList<BlockVector, BaseBlock>(false);

    /**
     * Blocks that should be placed last.
     */
    protected DoubleArrayList<BlockVector, BaseBlock> queueLast =
            new DoubleArrayList<BlockVector, BaseBlock>(false);

    /**
     * Blocks that should be placed after all other blocks.
     */
    protected DoubleArrayList<BlockVector, BaseBlock> queueFinal =
            new DoubleArrayList<BlockVector, BaseBlock>(false);

    /**
     * List of missing blocks;
     */
    private Set<Integer> missingBlocks = new HashSet<Integer>();

    private final EditSession editSession;

    private final LocalWorld world;

    public Operation(EditSession session) {
        this.editSession = session;
        this.world = editSession.getWorld();
    }

    public EditSession getEditSession() {
        return editSession;
    }

    /**
     * Get the number of blocks changed, including repeated block changes.
     *
     * @return
     */
    public int getBlockChangeCount() {
        return original.size();
    }

    public LocalWorld getWorld() {
        return world;
    }

    /**
     * Sets a block without changing history.
     *
     * @param pt
     * @param block
     * @return Whether the block changed
     */
    protected boolean rawSetBlock(com.sk89q.worldedit.Vector pt, BaseBlock block) {
        final int y = pt.getBlockY();
        final int type = block.getType();
        if (y < 0 || y > world.getMaxY()) {
            return false;
        }

        world.checkLoadedChunk(pt);

        // No invalid blocks
        if (!world.isValidBlockType(type)) {
            return false;
        }

        if (editSession.getMask() != null) {
            if (!editSession.getMask().matches(this, pt)) {
                return false;
            }
        }

        final int existing = world.getBlockType(pt);

        // Clear the container block so that it doesn't drop items
        if (BlockType.isContainerBlock(existing) && editSession.getBlockBag() == null) {
            world.clearContainerBlockContents(pt);
            // Ice turns until water so this has to be done first
        } else if (existing == BlockID.ICE) {
            world.setBlockType(pt, BlockID.AIR);
        }

        if (editSession.getBlockBag() != null) {
            if (type > 0) {
                try {
                    editSession.getBlockBag().fetchPlacedBlock(type, 0);
                } catch (UnplaceableBlockException e) {
                    return false;
                } catch (BlockBagException e) {
                    missingBlocks.add(type);
                    return false;
                }
            }

            if (existing > 0) {
                try {
                    editSession.getBlockBag().storeDroppedBlock(existing, world.getBlockData(pt));
                } catch (BlockBagException e) {
                }
            }
        }

        final boolean result;

        if (world.usesBlockData(type)) {
            if (editSession.hasFastMode()) {
                result = world.setTypeIdAndDataFast(pt, type, block.getData() > -1 ? block.getData() : 0);
            } else {
                result = world.setTypeIdAndData(pt, type, block.getData() > -1 ? block.getData() : 0);
            }
        } else {
            if (editSession.hasFastMode()) {
                result = world.setBlockTypeFast(pt, type);
            } else {
                result = world.setBlockType(pt, type);
            }
        }
        //System.out.println(pt + "" +result);

        if (type != 0) {
            if (block instanceof ContainerBlock) {
                if (editSession.getBlockBag() == null) {
                    world.copyToWorld(pt, block);
                }
            } else if (block instanceof TileEntityBlock) {
                world.copyToWorld(pt, block);
            }
        }
        return result;
    }

    /**
     * Sets the block at position x, y, z with a block type. If queue mode is
     * enabled, blocks may not be actually set in world until flushQueue() is
     * called.
     *
     * @param pt
     * @param block
     * @return Whether the block changed -- not entirely dependable
     * @throws MaxChangedBlocksException
     */
    protected boolean setBlock(com.sk89q.worldedit.Vector pt, BaseBlock block)
            throws MaxChangedBlocksException {
        BlockVector blockPt = pt.toBlockVector();

        // if (!original.containsKey(blockPt)) {
        original.put(blockPt, getBlock(pt));

        if (editSession.getBlockChangeLimit() != -1 && original.size() > editSession.getBlockChangeLimit()) {
            throw new MaxChangedBlocksException(editSession.getBlockChangeLimit());
        }
        // }

        current.put(pt.toBlockVector(), block);

        return smartSetBlock(pt, block);
    }

    /**
     * Insert a contrived block change into the history.
     *
     * @param pt
     * @param existing
     * @param block
     */
    protected void rememberChange(com.sk89q.worldedit.Vector pt, BaseBlock existing, BaseBlock block) {
        BlockVector blockPt = pt.toBlockVector();

        original.put(blockPt, existing);
        current.put(pt.toBlockVector(), block);
    }

    /**
     * Set a block with a pattern.
     *
     * @param pt
     * @param pat
     * @return Whether the block changed -- not entirely dependable
     * @throws MaxChangedBlocksException
     */
    protected boolean setBlock(com.sk89q.worldedit.Vector pt, Pattern pat)
            throws MaxChangedBlocksException {
        return setBlock(pt, pat.next(pt));
    }

    /**
     * Set a block only if there's no block already there.
     *
     * @param pt
     * @param block
     * @return if block was changed
     * @throws MaxChangedBlocksException
     */
    public boolean setBlockIfAir(com.sk89q.worldedit.Vector pt, BaseBlock block)
            throws MaxChangedBlocksException {
        if (!getBlock(pt).isAir()) {
            return false;
        } else {
            return setBlock(pt, block);
        }
    }

    /**
     * Set a block by chance.
     *
     * @param pos
     * @param block
     * @param c 0-1 chance
     * @return whether a block was changed
     * @throws MaxChangedBlocksException
     */
    public boolean setChanceBlockIfAir(com.sk89q.worldedit.Vector pos, BaseBlock block, double c)
            throws MaxChangedBlocksException {
        if (Math.random() <= c) {
            return setBlockIfAir(pos, block);
        }
        return false;
    }

    /**
     * Actually set the block. Will use queue.
     *
     * @param pt
     * @param block
     * @return
     */
    protected boolean smartSetBlock(com.sk89q.worldedit.Vector pt, BaseBlock block) {
        if (editSession.isQueueEnabled()) {
            if (BlockType.shouldPlaceLast(block.getType())) {
                // Place torches, etc. last
                queueLast.put(pt.toBlockVector(), block);
                return !(getBlockType(pt) == block.getType() && getBlockData(pt) == block.getData());
            } else if (BlockType.shouldPlaceFinal(block.getType())) {
                // Place signs, reed, etc even later
                queueFinal.put(pt.toBlockVector(), block);
                return !(getBlockType(pt) == block.getType() && getBlockData(pt) == block.getData());
            } else if (BlockType.shouldPlaceLast(getBlockType(pt))) {
                // Destroy torches, etc. first
                rawSetBlock(pt, new BaseBlock(BlockID.AIR));
            } else {
                queueAfter.put(pt.toBlockVector(), block);
                return !(getBlockType(pt) == block.getType() && getBlockData(pt) == block.getData());
            }
        }

        return rawSetBlock(pt, block);
    }

    /**
     * Gets the block type at a position x, y, z.
     *
     * @param pt
     * @return Block type
     */
    protected BaseBlock getBlock(com.sk89q.worldedit.Vector pt) {
        // In the case of the queue, the block may have not actually been
        // changed yet
        if (editSession.isQueueEnabled()) {
            /*
             * BlockVector blockPt = pt.toBlockVector();
             *
             * if (current.containsKey(blockPt)) { return current.get(blockPt);
             * }
             */
        }

        return rawGetBlock(pt);
    }

    /**
     * Gets the block type at a position x, y, z.
     *
     * @param pt
     * @return Block type
     */
    protected int getBlockType(com.sk89q.worldedit.Vector pt) {
        // In the case of the queue, the block may have not actually been
        // changed yet
        if (editSession.isQueueEnabled()) {
            /*
             * BlockVector blockPt = pt.toBlockVector();
             *
             * if (current.containsKey(blockPt)) { return current.get(blockPt);
             * }
             */
        }

        return world.getBlockType(pt);
    }

    protected int getBlockData(com.sk89q.worldedit.Vector pt) {
        // In the case of the queue, the block may have not actually been
        // changed yet
        if (editSession.isQueueEnabled()) {
            /*
             * BlockVector blockPt = pt.toBlockVector();
             *
             * if (current.containsKey(blockPt)) { return current.get(blockPt);
             * }
             */
        }

        return world.getBlockData(pt);
    }

    /**
     * Gets the block type at a position x, y, z.
     *
     * @param pt
     * @return BaseBlock
     */
    protected BaseBlock rawGetBlock(com.sk89q.worldedit.Vector pt) {
        world.checkLoadedChunk(pt);

        int type = world.getBlockType(pt);
        int data = world.getBlockData(pt);

        switch (type) {
            case BlockID.WALL_SIGN:
            case BlockID.SIGN_POST: {
                SignBlock block = new SignBlock(type, data);
                world.copyFromWorld(pt, block);
                return block;
            }

            case BlockID.CHEST: {
                ChestBlock block = new ChestBlock(data);
                world.copyFromWorld(pt, block);
                return block;
            }

            case BlockID.FURNACE:
            case BlockID.BURNING_FURNACE: {
                FurnaceBlock block = new FurnaceBlock(type, data);
                world.copyFromWorld(pt, block);
                return block;
            }

            case BlockID.DISPENSER: {
                DispenserBlock block = new DispenserBlock(data);
                world.copyFromWorld(pt, block);
                return block;
            }

            case BlockID.MOB_SPAWNER: {
                MobSpawnerBlock block = new MobSpawnerBlock(data);
                world.copyFromWorld(pt, block);
                return block;
            }

            case BlockID.NOTE_BLOCK: {
                NoteBlock block = new NoteBlock(data);
                world.copyFromWorld(pt, block);
                return block;
            }

            default:
                return new BaseBlock(type, data);
        }
    }

    /**
     * Finish off the queue.
     */
    public void flushQueue() {
        if (!editSession.isQueueEnabled()) {
            return;
        }

        final Set<BlockVector2D> dirtyChunks = new HashSet<BlockVector2D>();

        for (Map.Entry<BlockVector, BaseBlock> entry : queueAfter) {
            BlockVector pt = entry.getKey();
            rawSetBlock(pt, entry.getValue());

            // TODO: use ChunkStore.toChunk(pt) after optimizing it.
            if (editSession.hasFastMode()) {
                dirtyChunks.add(new BlockVector2D(pt.getBlockX() >> 4, pt.getBlockZ() >> 4));
            }
        }

        // We don't want to place these blocks if other blocks were missing
        // because it might cause the items to drop
        if (editSession.getBlockBag() == null || missingBlocks.size() == 0) {
            for (Map.Entry<BlockVector, BaseBlock> entry : queueLast) {
                BlockVector pt = entry.getKey();
                rawSetBlock(pt, entry.getValue());

                // TODO: use ChunkStore.toChunk(pt) after optimizing it.
                if (editSession.hasFastMode()) {
                    dirtyChunks.add(new BlockVector2D(pt.getBlockX() >> 4, pt.getBlockZ() >> 4));
                }
            }

            final Set<BlockVector> blocks = new HashSet<BlockVector>();
            final Map<BlockVector, BaseBlock> blockTypes = new HashMap<BlockVector, BaseBlock>();
            for (Map.Entry<BlockVector, BaseBlock> entry : queueFinal) {
                final BlockVector pt = entry.getKey();
                blocks.add(pt);
                blockTypes.put(pt, entry.getValue());
            }

            while (!blocks.isEmpty()) {
                BlockVector current = blocks.iterator().next();
                if (!blocks.contains(current)) {
                    continue;
                }

                final Deque<BlockVector> walked = new LinkedList<BlockVector>();

                while (true) {
                    walked.addFirst(current);

                    assert(blockTypes.containsKey(current));

                    final BaseBlock baseBlock = blockTypes.get(current);

                    final int type = baseBlock.getType();
                    final int data = baseBlock.getData();

                    switch (type) {
                        case BlockID.WOODEN_DOOR:
                        case BlockID.IRON_DOOR:
                            if ((data & 0x8) == 0) {
                                // Deal with lower door halves being attached to the floor AND the upper half
                                BlockVector upperBlock = current.add(0, 1, 0).toBlockVector();
                                if (blocks.contains(upperBlock) && !walked.contains(upperBlock)) {
                                    walked.addFirst(upperBlock);
                                }
                            }
                    }

                    final PlayerDirection attachment = BlockType.getAttachment(type, data);
                    if (attachment == null) {
                        // Block is not attached to anything => we can place it
                        break;
                    }

                    current = current.add(attachment.vector()).toBlockVector();

                    if (!blocks.contains(current)) {
                        // We ran outside the remaing set => assume we can place blocks on this
                        break;
                    }

                    if (walked.contains(current)) {
                        // Cycle detected => This will most likely go wrong, but there's nothing we can do about it.
                        break;
                    }
                }

                for (BlockVector pt : walked) {
                    rawSetBlock(pt, blockTypes.get(pt));
                    blocks.remove(pt);

                    // TODO: use ChunkStore.toChunk(pt) after optimizing it.
                    if (editSession.hasFastMode()) {
                        dirtyChunks.add(new BlockVector2D(pt.getBlockX() >> 4, pt.getBlockZ() >> 4));
                    }
                }
            }
        }

        if (!dirtyChunks.isEmpty()) world.fixAfterFastMode(dirtyChunks);

        queueAfter.clear();
        queueLast.clear();
        queueFinal.clear();
    }

    public T run(LocalSession session, LocalPlayer player) throws WorldEditException {
        T ret = operate(session, player);
        editSession.rememberOperation(this);
        return ret;
    }

    protected void updateValues(LocalSession session, LocalPlayer player)
            throws WorldEditException {}

    protected abstract T operate(LocalSession session,
            LocalPlayer player) throws WorldEditException;
}
