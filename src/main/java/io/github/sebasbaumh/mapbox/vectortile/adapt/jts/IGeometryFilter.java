package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import javax.annotation.Nonnull;

import org.locationtech.jts.geom.Geometry;

/**
 * Filter for geometries.
 */
public interface IGeometryFilter
{
	/**
	 * Return true if the value should be accepted (pass), or false if the value should be rejected (fail).
	 * @param geometry input to test
	 * @return true if the value should be accepted (pass), or false if the value should be rejected (fail)
	 * @see Geometry
	 */
	boolean accept(@Nonnull Geometry geometry);
}
