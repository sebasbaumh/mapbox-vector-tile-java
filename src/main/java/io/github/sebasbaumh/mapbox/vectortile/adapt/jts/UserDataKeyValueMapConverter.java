package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;

/**
 * Convert simple user data {@link Map} where the keys are {@link String} and values are {@link Object}. Supports
 * converting a specific map key to a user id. If the key to user id conversion fails, the error occurs silently and the
 * id is discarded.
 * @see IUserDataConverter
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class UserDataKeyValueMapConverter implements IUserDataConverter
{
	/**
	 * The {@link Map} key for the feature id.
	 */
	private final String idKey;

	/**
	 * If true, set feature id from user data.
	 */
	private final boolean setId;

	/**
	 * Does not set feature id.
	 */
	public UserDataKeyValueMapConverter()
	{
		this.setId = false;
		this.idKey = null;
	}

	/**
	 * Tries to set feature id using provided user data {@link Map} key.
	 * @param idKey user data {@link Map} key for getting id value.
	 */
	public UserDataKeyValueMapConverter(String idKey)
	{
		Objects.requireNonNull(idKey);
		this.setId = true;
		this.idKey = idKey;
	}

	@Override
	public void addTags(Object userData, MvtLayerProps layerProps, VectorTile.Tile.Feature.Builder featureBuilder)
	{
		try
		{
			@SuppressWarnings("unchecked")
			final Map<String, Object> userDataMap = (Map<String, Object>) userData;

			for (Map.Entry<String, Object> e : userDataMap.entrySet())
			{
				final String key = e.getKey();
				final Object value = e.getValue();

				if (key != null && value != null)
				{
					final int valueIndex = layerProps.addValue(value);

					if (valueIndex >= 0)
					{
						featureBuilder.addTags(layerProps.addKey(key));
						featureBuilder.addTags(valueIndex);
					}
				}
			}

			// Set feature id value
			if (setId)
			{
				final Object idValue = userDataMap.get(idKey);
				if (idValue != null)
				{
					if (idValue instanceof Long || idValue instanceof Integer || idValue instanceof Float
							|| idValue instanceof Double || idValue instanceof Byte || idValue instanceof Short)
					{
						featureBuilder.setId((long) idValue);
					}
					else if (idValue instanceof String)
					{
						try
						{
							featureBuilder.setId(Long.parseLong((String) idValue));
						}
						catch (NumberFormatException expected)
						{}
					}
				}
			}
		}
		catch (ClassCastException ex)
		{
			throw new IllegalArgumentException("unsupported user data: " + userData, ex);
		}
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + " [setId=" + setId + ", idKey=" + idKey + "]";
	}

}
