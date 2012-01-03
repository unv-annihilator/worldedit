// $Id$
/*
 * WorldEdit
 * Copyright (C) 2010, 2011 sk89q <http://www.sk89q.com>
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

package com.sk89q.worldedit.tools.brushes;

import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.filtering.GaussianKernel;
import com.sk89q.worldedit.filtering.HeightMapFilter;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

public class SmoothBrush extends Brush {
    private int iterations;
    private boolean naturalOnly;
    
    public SmoothBrush() {
        super('i');
    }

    public SmoothBrush(int iterations) {
        this(iterations, false);
    }

    public SmoothBrush(int iterations, boolean naturalOnly) {
        this();
        this.iterations = iterations;
        this.naturalOnly = naturalOnly;
    }

    public void build(EditSession editSession, Vector pos, Pattern mat, double size)
            throws MaxChangedBlocksException {
        double rad = size;
        WorldVector min = new WorldVector(editSession.getWorld(), pos.subtract(rad, rad, rad));
        Vector max = pos.add(rad, rad + 10, rad);
        Region region = new CuboidRegion(editSession.getWorld(), min, max);
        HeightMap heightMap = new HeightMap(editSession, region, naturalOnly);
        HeightMapFilter filter = new HeightMapFilter(new GaussianKernel(5, 1.0));
        heightMap.applyFilter(filter, iterations);
    }

    @Override
    public void parseInput(CommandContext args, LocalPlayer player, LocalSession session, WorldEdit we) throws WorldEditException {
        naturalOnly = args.hasFlag('n');

        if (args.hasFlag('i')) {
            iterations = args.getFlagInteger('i');
        }
    }
}
