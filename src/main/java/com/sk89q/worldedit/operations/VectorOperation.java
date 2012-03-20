package com.sk89q.worldedit.operations;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;

/**
 * @author zml2008
 */
public abstract class VectorOperation<T> extends Operation<T> {
    protected Vector point;
    private final boolean fixedPoint;

    public VectorOperation(EditSession session) {
        this(session, null);
    }

    public VectorOperation(EditSession session, Vector point) {
        super(session);
        fixedPoint = point != null;
        this.point = point;
    }

    /**
     * Returns the Vector that this operation is centered around
     * @return
     */
    public Vector getPoint() {
        return point;
    }

    protected void updateValues(LocalSession session, LocalPlayer player)
            throws WorldEditException {
        super.updateValues(session, player);
        if (!fixedPoint) {
            point = session.getPlacementPosition(player);
        }
    }

}
