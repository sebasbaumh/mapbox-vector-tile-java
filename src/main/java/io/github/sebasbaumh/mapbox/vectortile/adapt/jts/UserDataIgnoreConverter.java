package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;

/**
 * Ignores user data, does not take any action.
 *
 * @see IUserDataConverter
 */
public final class UserDataIgnoreConverter implements IUserDataConverter {
    @Override
    public void addTags(Object userData, MvtLayerProps layerProps,
                        VectorTile.Tile.Feature.Builder featureBuilder) {
    }
}
