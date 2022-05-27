package io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.geom.Geometry;

import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * <p>
 * JTS model of a Mapbox Vector Tile (MVT) layer.
 * </p>
 * <p>
 * A layer contains a subset of all geographic geometries in the tile.
 * </p>
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class JtsLayer
{

	private final int extent;
	private final Collection<Geometry> geometries;
	private final String name;

	/**
	 * Create an empty JTS layer.
	 * @param name layer name
	 * @throws IllegalArgumentException when {@code name} is null
	 */
	public JtsLayer(String name)
	{
		this(name, new ArrayList<Geometry>(0), MvtLayerParams.DEFAULT.extent);
	}

	/**
	 * Create a JTS layer with geometries.
	 * @param name layer name
	 * @param geometries
	 * @throws IllegalArgumentException when {@code name} or {@code geometries} are null
	 */
	public JtsLayer(String name, Collection<Geometry> geometries)
	{
		this(name, geometries, MvtLayerParams.DEFAULT.extent);
	}

	/**
	 * Create a JTS layer with geometries.
	 * @param name layer name
	 * @param geometries
	 * @param extent
	 * @throws IllegalArgumentException when {@code name} or {@code geometries} are null or {@code extent} is less than
	 *             or equal to 0
	 */
	public JtsLayer(String name, Collection<Geometry> geometries, int extent)
	{
		if (extent <= 0)
		{
			throw new IllegalArgumentException("extent is less than or equal to 0");
		}
		this.name = name;
		this.geometries = geometries;
		this.extent = extent;
	}

	/**
	 * Create a JTS layer with geometries.
	 * @param name layer name
	 * @param geom
	 * @throws IllegalArgumentException when {@code name} or {@code geometries} are null
	 */
	public JtsLayer(String name, Geometry geom)
	{
		this(name, Collections.singleton(geom), MvtLayerParams.DEFAULT.extent);
	}

	@Override
	public boolean equals(@Nullable Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JtsLayer layer = (JtsLayer) o;
		return (this.extent == layer.getExtent()) && name.equals(layer.name)
				&& MvtUtil.equalsIterable(this.geometries, layer.geometries);
	}

	/**
	 * Get the layer extent.
	 * @return extent of the layer
	 */
	public int getExtent()
	{
		return extent;
	}

	/**
	 * Get a read-only collection of geometry.
	 * @return unmodifiable collection of geometry.
	 */
	public Collection<Geometry> getGeometries()
	{
		return geometries;
	}

	/**
	 * Get the layer name.
	 * @return name of the layer
	 */
	public String getName()
	{
		return name;
	}

	@Override
	public int hashCode()
	{
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + extent;
		result = 31 * result + (geometries != null ? geometries.hashCode() : 0);
		return result;
	}

	@Override
	public String toString()
	{
		return "Layer{" + "name='" + name + '\'' + ", geometries=" + geometries + ", extent=" + extent + '}';
	}
}
