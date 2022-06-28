package io.github.sebasbaumh.mapbox.vectortile.util;

import java.util.Iterator;
import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;

/**
 * Class for helper functions.
 * @author sbaumhekel
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public final class MvtUtil
{
	/**
	 * encoded 'command header' integer for {@link GeomCmd#CLOSE_PATH}.
	 */
	public static final int CLOSE_PATH_HDR = geomCmdHdr(GeomCmd.CLOSE_PATH, 1);
	/**
	 * Maximum allowed 'command header' length value.
	 */
	public static final int GEOM_CMD_HDR_LEN_MAX = (int) (Math.pow(2, 29) - 1);

	// prevent instantiating this class
	@Deprecated
	private MvtUtil()
	{
	}

	/**
	 * See: <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">Google Protocol Buffers
	 * Docs</a>
	 * @param n zig-zag encoded integer to decode
	 * @return decoded integer
	 */
	public static int decodeZigZag(int n)
	{
		return (n >> 1) ^ (-(n & 1));
	}

	/**
	 * See: <a href="https://developers.google.com/protocol-buffers/docs/encoding#types">Google Protocol Buffers
	 * Docs</a>
	 * @param n integer to encode
	 * @return zig-zag encoded integer
	 */
	public static int encodeZigZag(int n)
	{
		return (n << 1) ^ (n >> 31);
	}

	/**
	 * Checks, if the given {@link Iterable}s contain the same elements (in the same order).
	 * @param la {@link Iterable} (can be null)
	 * @param lb {@link Iterable} (can be null)
	 * @return true on success, else false
	 */
	@SuppressWarnings("unlikely-arg-type")
	public static <T, U> boolean equalsIterable(@Nullable Iterable<T> la, @Nullable Iterable<U> lb)
	{
		// check same instance
		if (la == lb)
		{
			return true;
		}
		// iterables are different instances, so none of them should be null to proceed
		if ((la == null) || (lb == null))
		{
			return false;
		}
		// walk through items
		Iterator<T> it = la.iterator();
		Iterator<U> it2 = lb.iterator();
		while (it.hasNext() && it2.hasNext())
		{
			// check items
			if (!Objects.equals(it.next(), it2.next()))
			{
				return false;
			}
		}
		// make sure there are no more items
		return !it.hasNext() && !it2.hasNext();
	}

	/**
	 * <p>
	 * Encodes a 'command header' with the first 3 LSB as the command id, the remaining bits as the command length. See
	 * the vector-tile-spec for details.
	 * </p>
	 * @param cmd command to execute
	 * @param length how many times the command is repeated
	 * @return encoded 'command header' integer
	 */
	public static int geomCmdHdr(GeomCmd cmd, int length)
	{
		return (cmd.getCmdId() & 0x7) | (length << 3);
	}

	/**
	 * Get the id component from the 'command header' integer, then find the {@link GeomCmd} with a matching id.
	 * @param cmdHdr encoded 'command header' integer
	 * @return command with matching id, or null if a match could not be made
	 */
	@Nullable
	public static GeomCmd getGeomCmd(int cmdHdr)
	{
		final int cmdId = getGeomCmdId(cmdHdr);
		return GeomCmd.fromId(cmdId);
	}

	/**
	 * Get the id component from the 'command header' integer.
	 * @param cmdHdr encoded 'command header' integer
	 * @return command id
	 */
	public static int getGeomCmdId(int cmdHdr)
	{
		return cmdHdr & 0x7;
	}

	/**
	 * Get the length component from the 'command header' integer.
	 * @param cmdHdr encoded 'command header' integer
	 * @return command length
	 */
	public static int getGeomCmdLength(int cmdHdr)
	{
		return cmdHdr >> 3;
	}

	/**
	 * Check if {@code value} is valid for encoding as a MVT layer property value.
	 * @param value target to check
	 * @return true is the object is a type that is supported by MVT
	 */
	public static boolean isValidPropValue(Object value)
	{
		return (value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof Float
				|| value instanceof Double || value instanceof String);
	}

	/**
	 * Create a new {@link io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.Layer.Builder} instance with
	 * initialized version, name, and extent metadata.
	 * @param layerName name of the layer
	 * @param mvtLayerParams tile creation parameters
	 * @return new layer builder instance with initialized metadata.
	 */
	public static VectorTile.Tile.Layer.Builder newLayerBuilder(String layerName, MvtLayerParams mvtLayerParams)
	{
		VectorTile.Tile.Layer.Builder layerBuilder = VectorTile.Tile.Layer.newBuilder();
		layerBuilder.setVersion(2);
		layerBuilder.setName(layerName);
		layerBuilder.setExtent(mvtLayerParams.extent);
		return layerBuilder;
	}

	/**
	 * Covert an {@link Object} to a new {@link io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.Value} instance.
	 * @param value target for conversion
	 * @return new instance with String or primitive value set
	 */
	public static VectorTile.Tile.Value toMvtValue(Object value)
	{
		final VectorTile.Tile.Value.Builder tileValue = VectorTile.Tile.Value.newBuilder();

		if (value instanceof Boolean)
		{
			tileValue.setBoolValue((Boolean) value);
		}
		else if (value instanceof Integer)
		{
			tileValue.setSintValue((Integer) value);
		}
		else if (value instanceof Long)
		{
			tileValue.setSintValue((Long) value);
		}
		else if (value instanceof Float)
		{
			tileValue.setFloatValue((Float) value);
		}
		else if (value instanceof Double)
		{
			tileValue.setDoubleValue((Double) value);
		}
		else if (value instanceof String)
		{
			tileValue.setStringValue((String) value);
		}
		return tileValue.build();
	}

	/**
	 * Convert {@link io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.Value} to String or boxed primitive object.
	 * @param value target for conversion
	 * @return String or boxed primitive
	 */
	@Nullable
	public static Object toObject(VectorTile.Tile.Value value)
	{
		if (value.hasDoubleValue())
		{
			return value.getDoubleValue();
		}
		else if (value.hasFloatValue())
		{
			return value.getFloatValue();
		}
		else if (value.hasIntValue())
		{
			return value.getIntValue();
		}
		else if (value.hasBoolValue())
		{
			return value.getBoolValue();
		}
		else if (value.hasStringValue())
		{
			return value.getStringValue();
		}
		else if (value.hasSintValue())
		{
			return value.getSintValue();

		}
		else if (value.hasUintValue())
		{
			return value.getUintValue();
		}
		return null;
	}

	/**
	 * Modifies {@code layerBuilder} to contain properties from {@code layerProps}.
	 * @param layerBuilder layer builder to write to
	 * @param layerProps properties to write
	 */
	public static void writeProps(VectorTile.Tile.Layer.Builder layerBuilder, MvtLayerProps layerProps)
	{
		// Add keys
		layerBuilder.addAllKeys(layerProps.getKeys());
		// Add values
		for (Object val : layerProps.getValues())
		{
			layerBuilder.addValues(toMvtValue(val));
		}
	}

}
