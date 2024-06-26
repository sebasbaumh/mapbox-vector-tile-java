package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Convert MVT tags list to a {@link Map} of {@link String} to {@link Object}. Tags indices that are out of range of the
 * key or value list are ignored.
 * @see ITagConverter
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class TagKeyValueMapConverter implements ITagConverter
{
	/**
	 * If true, add id to user data object.
	 */
	private final boolean addId;

	/**
	 * The {@link Map} key for the feature id.
	 */
	@Nullable
	private final String idKey;

	/**
	 * If true, return null user data when tags are empty.
	 */
	private final boolean nullIfEmpty;

	/**
	 * Always created user data object, even with empty tags. Ignore feature ids.
	 */
	public TagKeyValueMapConverter()
	{
		this(false);
	}

	/**
	 * Ignore feature ids.
	 * @param nullIfEmpty if true, return null user data when tags are empty
	 */
	public TagKeyValueMapConverter(boolean nullIfEmpty)
	{
		this.nullIfEmpty = nullIfEmpty;
		this.addId = false;
		this.idKey = null;
	}

	/**
	 * Store feature ids using idKey. Id value may be null if not present.
	 * @param nullIfEmpty if true, return null user data when tags are empty
	 * @param idKey key name to use for feature id value
	 */
	public TagKeyValueMapConverter(boolean nullIfEmpty, String idKey)
	{
		Objects.requireNonNull(idKey);

		this.nullIfEmpty = nullIfEmpty;
		this.addId = true;
		this.idKey = idKey;
	}

	@Nullable
	@Override
	public Object toUserData(@Nullable Long id, List<Integer> tags, List<String> keysList,
			List<VectorTile.Tile.Value> valuesList)
	{
		// Guard: empty
		if (nullIfEmpty && tags.isEmpty() && (!addId || id == null))
		{
			return null;
		}

		final Map<String, Object> userData = LinkedHashMap.<String, Object>newLinkedHashMap(((tags.size() + 1) / 2));
		// Add feature properties
		int keyIndex;
		int valIndex;
		boolean valid;

		for (int i = 0; i < tags.size() - 1; i += 2)
		{
			keyIndex = tags.get(i);
			valIndex = tags.get(i + 1);

			valid = keyIndex >= 0 && keyIndex < keysList.size() && valIndex >= 0 && valIndex < valuesList.size();

			if (valid)
			{
				userData.put(keysList.get(keyIndex), MvtUtil.toObject(valuesList.get(valIndex)));
			}
		}

		// Add ID, value may be null
		if (addId)
		{
			userData.put(idKey, id);
		}
		return userData;
	}
}
