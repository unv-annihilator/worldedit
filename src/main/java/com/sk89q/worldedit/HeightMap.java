// $Id$
/*
 * WorldEditLibrary
 * Copyright (C) 2010 sk89q <http://www.sk89q.com> and contributors
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

package com.sk89q.worldedit;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.filtering.HeightMapFilter;
import com.sk89q.worldedit.operations.RegionOperation;

/**
 * Allows applications of Kernels onto the region's heightmap.
 * Currently only used for smoothing (with a GaussianKernel).
 *
 * @author Grum
 */

public class HeightMap extends RegionOperation<Boolean> {
    private final HeightMapFilter filter;
    private final int iterations;
    private final boolean naturalOnly;

    /**
     * Constructs the HeightMap
     *
     * @param session
     */
    public HeightMap(EditSession session, HeightMapFilter filter, int iterations) {
        this(session, filter, iterations, false);
    }

    /**
     * Constructs the HeightMap
     *
     * @param session
     * @param naturalOnly ignore non-natural blocks
     */
    public HeightMap(EditSession session, HeightMapFilter filter, int iterations, boolean naturalOnly) {
        super(session);
        this.filter = filter;
        this.iterations = iterations;
        this.naturalOnly = naturalOnly;

    }

    /**
     * Apply the filter 'iterations' amount times.
     *
     * @param filter
     * @param iterations
     * @return number of blocks affected
     * @throws MaxChangedBlocksException
     */

    /**
     * Returns the highest solid 'terrain' block which can occur naturally.
     *
     * @param x
     * @param z
     * @param minY minimal height
     * @param maxY maximal height
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        return getHighestTerrainBlock(x, z, minY, maxY, false);
    }

    /**
     * Returns the highest solid 'terrain' block which can occur naturally.
     *
     * @param x
     * @param z
     * @param minY minimal height
     * @param maxY maximal height
     * @param naturalOnly look at natural blocks or all blocks
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY, boolean naturalOnly) {
        for (int y = maxY; y >= minY; --y) {
            Vector pt = new Vector(x, y, z);
            int id = getBlockType(pt);
            if (naturalOnly ? BlockType.isNaturalTerrainBlock(id) : !BlockType.canPassThrough(id)) {
                return y;
            }
        }
        return minY;
    }

    /**
     * Apply a raw heightmap to the region
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException
     */
    @Override
    public Boolean operate(LocalSession session, LocalPlayer player) throws MaxChangedBlocksException {
        int minX = getRegion().getMinimumPoint().getBlockX();
        int minY = getRegion().getMinimumPoint().getBlockY();
        int minZ = getRegion().getMinimumPoint().getBlockZ();
        int maxY = getRegion().getMaximumPoint().getBlockY();


        // Store current heightmap data
        int[] data = new int[getRegion().getWidth() * getRegion().getLength()];
        for (int z = 0; z < getRegion().getHeight(); ++z) {
            for (int x = 0; x < getRegion().getWidth(); ++x) {
                data[z * getRegion().getWidth() + x] = getHighestTerrainBlock(x + minX, z + minZ, minY, maxY, naturalOnly);
            }
        }
        int[] filteredData = filter.filter(data, getRegion().getWidth(), getRegion().getHeight());
        for (int i = 1; i < iterations; ++i) {
            filteredData = filter.filter(filteredData, getRegion().getWidth(), getRegion().getHeight());
        }

        BaseBlock fillerAir = new BaseBlock(BlockID.AIR);

        int blocksChanged = 0;

        // Apply heightmap
        for (int z = 0; z < getRegion().getHeight(); ++z) {
            for (int x = 0; x < getRegion().getWidth(); ++x) {
                int index = z * getRegion().getWidth() + x;
                int curHeight = data[index];

                // Clamp newHeight within the selection area
                int newHeight = Math.min(maxY, filteredData[index]);

                // Offset x,z to be 'real' coordinates
                int X = x + minX;
                int Z = z + minZ;

                // We are keeping the topmost blocks so take that in account for the scale
                double scale = (double) (curHeight - minY) / (double) (newHeight - minZ);

                // Depending on growing or shrinking we need to start at the bottom or top
                if (newHeight > curHeight) {
                    // Set the top block of the column to be the same type (this might go wrong with rounding)
                    BaseBlock existing = getBlock(new Vector(X, curHeight, Z));

                    // Skip water/lava
                    if (existing.getType() != BlockID.WATER && existing.getType() != BlockID.STATIONARY_WATER
                            && existing.getType() != BlockID.LAVA && existing.getType() != BlockID.STATIONARY_LAVA) {
                        setBlock(new Vector(X, newHeight, Z), existing);

                        // Grow -- start from 1 below top replacing airblocks
                        for (int y = newHeight - 1 - minY; y >= 0; --y) {
                            int copyFrom = (int) (y * scale);
                            setBlock(new Vector(X, minY + y, Z), getBlock(new Vector(X, minY + copyFrom, Z)));
                        }
                    }
                } else if (curHeight > newHeight) {
                    // Shrink -- start from bottom
                    for (int y = 0; y < newHeight - minY; ++y) {
                        int copyFrom = (int) (y * scale);
                        setBlock(new Vector(X, minY + y, Z), getBlock(new Vector(X, minY + copyFrom, Z)));
                    }

                    // Set the top block of the column to be the same type
                    // (this could otherwise go wrong with rounding)
                    setBlock(new Vector(X, newHeight, Z), getBlock(new Vector(X, curHeight, Z)));

                    // Fill rest with air
                    for (int y = newHeight + 1; y <= curHeight; ++y) {
                        setBlock(new Vector(X, y, Z), fillerAir);
                    }
                }
            }
        }

        // Drop trees to the floor -- TODO

        return true;
    }
}
