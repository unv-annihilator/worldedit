package com.sk89q.worldedit.operations;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.regions.Region;

/**
 * @author zml2008
 */
public abstract class RegionOperation<T> extends Operation<T> {
    protected Region region;

    public RegionOperation(EditSession session) {
        super(session);
    }

    public Region getRegion() {
        if (region == null) {
            throw new IllegalArgumentException("This RegionOperation has not been initialized yet!");
        }
        return region;
    }

    @Override
    protected void updateValues(LocalSession session, LocalPlayer player) throws IncompleteRegionException {
        region = session.getRegionSelector(getWorld()).getRegion();
    }
}
