package com.sk89q.worldedit.operations;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.patterns.Pattern;

import java.util.HashSet;
import java.util.Stack;

/**
 * @author zml2008
 */
public class FillXZOperation extends VectorOperation<Object> {
    private final Pattern pattern;
    private final double radiusSq;
    private final int depth;
    private final boolean recursive;

    public FillXZOperation(EditSession session, Pattern pattern,
                           double radius, int depth, boolean recursive) {
        super(session);
        this.pattern = pattern;
        this.radiusSq = radius * radius;
        this.depth = depth;
        this.recursive = recursive;
    }

    /**
     * Recursively fills a block and below until it hits another block.
     *
     * @param x
     * @param cy
     * @param z
     * @param pattern
     * @param minY
     * @throws MaxChangedBlocksException
     * @return the number of affected blocks
     */
    private void fillY(int x, int cy, int z, Pattern pattern, int minY)
            throws MaxChangedBlocksException {

        for (int y = cy; y >= minY; --y) {
            Vector pt = new Vector(x, y, z);

            if (getBlock(pt).isAir()) {
                setBlock(pt, pattern);
            } else {
                break;
            }
        }
    }

    @Override
    protected Object operate(LocalSession session, LocalPlayer player) throws WorldEditException {
        int originX = getPoint().getBlockX();
        int originY = getPoint().getBlockY();
        int originZ = getPoint().getBlockZ();

        HashSet<BlockVector> visited = new HashSet<BlockVector>();
        Stack<BlockVector> queue = new Stack<BlockVector>();

        queue.push(new BlockVector(originX, originY, originZ));

        while (!queue.empty()) {
            BlockVector pt = queue.pop();
            int cx = pt.getBlockX();
            int cy = pt.getBlockY();
            int cz = pt.getBlockZ();

            if (cy < 0 || cy > originY || visited.contains(pt)) {
                continue;
            }

            visited.add(pt);

            if (recursive) {
                if (getPoint().distanceSq(pt) > radiusSq) {
                    continue;
                }

                if (getBlock(pt).isAir()) {
                    setBlock(pt, pattern);
                } else {
                    continue;
                }

                queue.push(new BlockVector(cx, cy - 1, cz));
                queue.push(new BlockVector(cx, cy + 1, cz));
            } else {
                double distSq = Math.pow(originX - cx, 2)
                        + Math.pow(originZ - cz, 2);
                int minY = originY - depth + 1;

                if (distSq > radiusSq) {
                    continue;
                }

                if (getBlock(pt).isAir()) {
                    fillY(cx, originY, cz, pattern, minY);
                } else {
                    continue;
                }
            }

            queue.push(new BlockVector(cx + 1, cy, cz));
            queue.push(new BlockVector(cx - 1, cy, cz));
            queue.push(new BlockVector(cx, cy, cz + 1));
            queue.push(new BlockVector(cx, cy, cz - 1));
        }

        return null;
    }
}
