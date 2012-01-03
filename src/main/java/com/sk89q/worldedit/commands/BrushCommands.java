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

package com.sk89q.worldedit.commands;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.masks.BlockTypeMask;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.patterns.SingleBlockPattern;
import com.sk89q.worldedit.tools.BrushTool;
import com.sk89q.worldedit.tools.brushes.*;

/**
 * Brush shape commands.
 *
 * @author sk89q
 */
public class BrushCommands {
	private final WorldEdit we;
	
	public BrushCommands(WorldEdit we) {
		this.we = we;
	}

    @Command(
        aliases = {"choose"},
        usage = "[brush]",
        desc = "Binds the selected brush to your currently held item.",
        min = 1,
        max = -1
    )
    public void chooseBrush(CommandContext args, LocalSession session,
            LocalPlayer player, EditSession editSession) throws WorldEditException {
        BrushTool.getBrush(session, player, args, we);
    }

    @Command(
        aliases = {"configure", "config"},
        usage = "<brushopts>",
            desc = "Configure options for brushes.",
        min = 1,
        max = -1
    )
    public void configureBrush(CommandContext args, LocalSession session,
            LocalPlayer player, EditSession editSession) throws WorldEditException {
        session.getBrushTool(player.getItemInHand()).parseInput(args, player, session, we);
    }

    @Command(
        aliases = { "ex", "extinguish" },
        usage = "[radius]",
        desc = "Shortcut fire extinguisher brush",
        min = 0,
        max = 1
    )
    @CommandPermissions("worldedit.brush.ex")
    public void extinguishBrush(CommandContext args, LocalSession session,
            LocalPlayer player, EditSession editSession) throws WorldEditException {
        
        LocalConfiguration config = we.getConfiguration();

        double radius = args.argsLength() > 1 ? args.getDouble(1) : 5;
        if (radius > config.maxBrushRadius) {
            player.printError("Maximum allowed brush radius: "
                    + config.maxBrushRadius);
            return;
        }

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        Pattern fill = new SingleBlockPattern(new BaseBlock(0));
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setMask(new BlockTypeMask(BlockID.FIRE));
        tool.setBrush(new SphereBrush(), "worldedit.brush.ex");

        player.print(String.format("Extinguisher equipped (%.0f).",
                radius));
    }
}
