package com.sk89q.worldedit.operations;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.regions.CuboidRegion;

/**
 * @author zml2008
 */
public class SetBlocksOperation extends RegionOperation<Object> {
    private final Pattern pattern;

    public SetBlocksOperation(EditSession editSession, Pattern pattern) {
        super(editSession);
        this.pattern = pattern;
    }

    @Override
    protected Object operate(LocalSession session, LocalPlayer player) throws WorldEditException {
        if (getRegion() instanceof CuboidRegion) {
            // Doing this for speed
            Vector min = getRegion().getMinimumPoint();
            Vector max = getRegion().getMaximumPoint();

            int minX = min.getBlockX();
            int minY = min.getBlockY();
            int minZ = min.getBlockZ();
            int maxX = max.getBlockX();
            int maxY = max.getBlockY();
            int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        Vector pt = new Vector(x, y, z);
                        setBlock(pt, pattern);
                    }
                }
            }
        } else {
            for (Vector pt : getRegion()) {
                setBlock(pt, pattern);
            }
        }

        return null;
    }
}
