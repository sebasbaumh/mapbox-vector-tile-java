package io.github.sebasbaumh.mapbox.vectortile.build;

import java.util.LinkedHashMap;
import java.util.Objects;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Support MVT features that must reference properties by their key and value index.
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class MvtLayerProps
{
	private final LinkedHashMap<String, Integer> keys = new LinkedHashMap<String, Integer>();
	private final LinkedHashMap<Object, Integer> vals = new LinkedHashMap<Object, Integer>();

	/**
	 * Constructs an instance.
	 */
	public MvtLayerProps()
	{
	}

	/**
	 * Add the key and return it's index code. If the key already is present, the previous index code is returned and no
	 * insertion is done.
	 * @param key key to add
	 * @return index of the key
	 */
	public int addKey(String key)
	{
		Objects.requireNonNull(key);
		int nextIndex = keys.size();
		final Integer mapIndex = keys.putIfAbsent(key, nextIndex);
		return mapIndex == null ? nextIndex : mapIndex;
	}

	/**
	 * Add the value and return it's index code. If the value already is present, the previous index code is returned
	 * and no insertion is done. If {@code value} is an unsupported type for encoding in a MVT, then it will not be
	 * added.
	 * @param value value to add
	 * @return index of the value, -1 on unsupported value types
	 * @see MvtUtil#isValidPropValue(Object)
	 */
	public int addValue(Object value)
	{
		Objects.requireNonNull(value);
		if (!MvtUtil.isValidPropValue(value))
		{
			return -1;
		}

		int nextIndex = vals.size();
		final Integer mapIndex = vals.putIfAbsent(value, nextIndex);
		return mapIndex == null ? nextIndex : mapIndex;
	}

	/**
	 * Gets all keys.
	 * @return keys
	 */
	public Iterable<String> getKeys()
	{
		return keys.keySet();
	}

	/**
	 * Gets all values.
	 * @return values
	 */
	public Iterable<Object> getValues()
	{
		return vals.keySet();
	}
}
