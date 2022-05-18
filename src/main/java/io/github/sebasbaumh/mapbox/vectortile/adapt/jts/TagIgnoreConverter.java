package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;

import java.util.List;

/**
 * Ignores tags, always returns null.
 *
 * @see ITagConverter
 */
public final class TagIgnoreConverter implements ITagConverter {
    @Override
    public Object toUserData(Long id, List<Integer> tags, List<String> keysList,
                             List<VectorTile.Tile.Value> valuesList) {
        return null;
    }
}
