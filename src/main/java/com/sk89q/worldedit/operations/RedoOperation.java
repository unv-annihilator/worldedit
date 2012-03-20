package com.sk89q.worldedit.operations;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;

import java.util.Map;

/**
 * @author zml2008
 */
public class RedoOperation extends Operation<Object> {
    private final Operation<?>[] toRedo;
    public RedoOperation(EditSession session, Operation<?>... toRedo) {
        super(session);
        this.toRedo = toRedo;
    }

    @Override
    protected Object operate(LocalSession session, LocalPlayer player) throws WorldEditException {
        for (Operation<?> op : toRedo) {
            for (Map.Entry<BlockVector, BaseBlock> entry : op.current) {
                smartSetBlock(entry.getKey(), entry.getValue());
            }
        }
        flushQueue();
        return null;
    }
}
