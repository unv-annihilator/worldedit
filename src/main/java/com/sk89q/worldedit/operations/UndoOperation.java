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
public class UndoOperation extends Operation<Object> {
    private final Operation<?>[] toUndo;
    public UndoOperation(EditSession session, Operation<?>... toUndo) {
        super(session);
        this.toUndo = toUndo;
    }

    @Override
    protected Object operate(LocalSession session, LocalPlayer player) throws WorldEditException {
        for (Operation<?> op : toUndo) {
            for (Map.Entry<BlockVector, BaseBlock> entry : op.original) {
                smartSetBlock(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }
}
