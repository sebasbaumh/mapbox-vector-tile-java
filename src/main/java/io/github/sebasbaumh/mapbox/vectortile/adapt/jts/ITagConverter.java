package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;

/**
 * Process MVT tags and feature id, convert to user data object. The returned user data object may be null.
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public interface ITagConverter
{

	/**
	 * Convert MVT user data to JTS user data object or null.
	 * @param id feature id, may be {@code null}
	 * @param tags MVT feature tags, may be invalid
	 * @param keysList layer key list
	 * @param valuesList layer value list
	 * @return user data object or null
	 */
	@Nullable
	Object toUserData(@Nullable Long id, List<Integer> tags, List<String> keysList,
			List<VectorTile.Tile.Value> valuesList);
}
