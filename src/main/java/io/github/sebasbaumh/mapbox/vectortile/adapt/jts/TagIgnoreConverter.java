package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;

/**
 * Ignores tags, always returns null.
 * @see ITagConverter
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class TagIgnoreConverter implements ITagConverter
{
	@Nullable
	@Override
	public Object toUserData(@Nullable Long id, List<Integer> tags, List<String> keysList,
			List<VectorTile.Tile.Value> valuesList)
	{
		return null;
	}
}
