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
	 * Default layer parameters created using {@link #MvtLayerParams()}.
	 */
	public static final MvtLayerParams DEFAULT = new MvtLayerParams();

	/**
	 * the resolution of the MVT local coordinate system.
	 */
	public final int extent;

	/**
	 * ratio of tile 'pixel' dimensions to tile extent dimensions.
	 */
	public final float ratio;

	/**
	 * the resolution of the tile in 'pixel' dimensions.
	 */
	public final int tileSize;

	/**
	 * Construct default layer sizing parameters for MVT creation.
	 * <p>
	 * Uses defaults:
	 * </p>
	 * <ul>
	 * <li>{@link #tileSize} = 256</li>
	 * <li>{@link #extent} = 4096</li>
	 * </ul>
	 * @see #MvtLayerParams(int, int)
	 */
	public MvtLayerParams()
	{
		this(256, 4096);
	}

	/**
	 * Construct layer sizing parameters for MVT creation.
	 * @param tileSize the resolution of the tile in pixel coordinates, must be &gt; 0
	 * @param extent the resolution of the MVT local coordinate system, must be &gt; 0
	 */
	public MvtLayerParams(int tileSize, int extent)
	{
		if (tileSize <= 0)
		{
			throw new IllegalArgumentException("tileSize must be > 0");
		}

		if (extent <= 0)
		{
			throw new IllegalArgumentException("extent must be > 0");
		}

		this.tileSize = tileSize;
		this.extent = extent;
		this.ratio = extent / (float) tileSize;
	}

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + " [tileSize=" + tileSize + ", extent=" + extent + ", ratio=" + ratio
				+ "]";
	}

}
