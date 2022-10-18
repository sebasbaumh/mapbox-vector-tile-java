package io.github.sebasbaumh.mapbox.vectortile.build;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Immutable parameters collection for Mapbox-Vector-Tile creation.
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class MvtLayerParams
{
	/**
	 * Default resolution of the MVT local coordinate system (4096).
	 */
	public static final int DEFAULT_EXTENT = 4096;
	/**
	 * Default layer parameters..
	 * <p>
	 * Uses defaults:
	 * </p>
	 * <ul>
	 * <li>{@link #getExtent()} = 4096</li>
	 * </ul>
	 */
	public static final MvtLayerParams DEFAULT = new MvtLayerParams(DEFAULT_EXTENT);
	/**
	 * the resolution of the MVT local coordinate system.
	 */
	private final int extent;

	/**
	 * Construct default layer sizing parameters for MVT creation.
	 * @deprecated use {@link #DEFAULT} instead
	 */
	@Deprecated
	public MvtLayerParams()
	{
		this(DEFAULT_EXTENT);
	}

	/**
	 * Construct layer sizing parameters for MVT creation.
	 * @param extent the resolution of the MVT local coordinate system, must be &gt; 0
	 */
	public MvtLayerParams(int extent)
	{
		if (extent <= 0)
		{
			throw new IllegalArgumentException("extent must be > 0");
		}
		this.extent = extent;
	}

	/**
	 * Construct layer sizing parameters for MVT creation.
	 * @param tileSize (unused)
	 * @param extent the resolution of the MVT local coordinate system, must be &gt; 0
	 * @deprecated tileSize is fixed to 256, use {@link #MvtLayerParams(int)} instead
	 */
	@Deprecated
	public MvtLayerParams(@SuppressWarnings("unused") int tileSize, int extent)
	{
		this(extent);
	}

	/**
	 * Gets the resolution of the MVT local coordinate system.
	 * @return the resolution of the MVT local coordinate system.
	 */
	public int getExtent()
	{
		return extent;
	}

	/**
	 * Gets the ratio of tile 'pixel' dimensions to tile extent dimensions.
	 * @return ratio of tile 'pixel' dimensions to tile extent dimensions.
	 */
	public double getRatio()
	{
		// tile size is fixed 256 pixels
		return extent / 256.0;
	}

	/**
	 * Gets the resolution of the tile in pixel coordinates
	 * @return the resolution of the tile in pixel coordinates
	 */
	@SuppressWarnings("static-method")
	public int getTileSize()
	{
		// tile size is fixed 256 pixels
		return 256;
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + " [tileSize=" + getTileSize() + ", extent=" + getExtent() + ", ratio="
				+ getRatio() + "]";
	}

}
