/*
 * WorldEdit
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
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
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.patterns.Pattern;

import java.util.*;

/**
 * @author zml2008
 */
public class GravityBrush extends Brush {
    private boolean fullHeight;

    @Override
    public void build(EditSession editSession, Vector pos, Pattern mat, double size) throws MaxChangedBlocksException {
        final BaseBlock air = new BaseBlock(BlockID.AIR, 0);
        final int startY = fullHeight ? editSession.getWorld().getMaxY() : (int)Math.floor(pos.getBlockY() + size);
        for (double x = pos.getBlockX() + size; x > pos.getBlockX() - size; --x) {
            for (int z = (int)Math.floor(pos.getBlockZ() + size); z > (int)Math.floor(pos.getBlockZ() - size); --z) {
                double y = startY;
                final List<BaseBlock> blockTypes = new ArrayList<BaseBlock>();
                for (; y > (int)Math.floor(pos.getBlockY() - size); --y) {
                    final Vector pt = new Vector(x, y, z);
                    final BaseBlock block = editSession.getBlock(pt);
                    if (!block.isAir()) {
                        blockTypes.add(block);
                        editSession.setBlock(pt, air);
                    }
                }
                Vector pt = new Vector(x, y, z);
                Collections.reverse(blockTypes);
                for (int i = 0; i < blockTypes.size();) {
                    if (editSession.getBlock(pt).getType() == BlockID.AIR) {
                        editSession.setBlock(pt, blockTypes.get(i++));
                    }
                    pt = pt.add(0, 1, 0);
                }
            }
        }
    }

    @Override
    public void parseInput(CommandContext args, LocalPlayer player, LocalSession session, WorldEdit we) throws WorldEditException {
        fullHeight = args.hasFlag('h');
    }

    public static class Factory implements BrushFactory {

        @Override
        public Brush createBrush() {
            return new GravityBrush();
        }

        @Override
        public String getPermission() {
            return "worldedit.brush.gravity";
        }
    }
}
