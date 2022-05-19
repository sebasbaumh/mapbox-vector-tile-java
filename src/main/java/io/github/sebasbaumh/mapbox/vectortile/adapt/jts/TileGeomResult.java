package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;

/**
 * Processing result containing intersection geometry and MVT geometry.
 * @see JtsAdapter#createTileGeom(Geometry, Envelope, GeometryFactory, MvtLayerParams, IGeometryFilter)
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public final class TileGeomResult
{

	/**
	 * Intersection geometry (projection units and coordinates).
	 */
	public final List<Geometry> intGeoms;

	/**
	 * Geometry in MVT coordinates (tile extent units, screen coordinates).
	 */
	public final List<Geometry> mvtGeoms;

	/**
	 * Create TileGeomResult, which contains the intersection of geometry and MVT geometry.
	 * @param intGeoms geometry intersecting tile
	 * @param mvtGeoms geometry for MVT
	 * @throws NullPointerException if intGeoms or mvtGeoms are null
	 */
	public TileGeomResult(List<Geometry> intGeoms, List<Geometry> mvtGeoms)
	{
		// FIX
		Objects.requireNonNull(intGeoms);
		Objects.requireNonNull(mvtGeoms);
		this.intGeoms = intGeoms;
		this.mvtGeoms = mvtGeoms;
	}
}
