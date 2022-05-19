package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;

/**
 * Filter {@link Polygon} and {@link MultiPolygon} by area or {@link LineString} and {@link MultiLineString} by length.
 * @see IGeometryFilter
 */
@NonNullByDefault
public final class GeomMinSizeFilter implements IGeometryFilter
{
	/**
	 * Minimum area.
	 */
	private final double minArea;

	/**
	 * Minimum length.
	 */
	private final double minLength;

	/**
	 * GeomMinSizeFilter.
	 * @param minArea minimum area required for a {@link Polygon} or {@link MultiPolygon}
	 * @param minLength minimum length required for a {@link LineString} or {@link MultiLineString}
	 */
	public GeomMinSizeFilter(double minArea, double minLength)
	{
		if (minArea < 0.0d)
		{
			throw new IllegalArgumentException("minArea must be >= 0");
		}
		if (minLength < 0.0d)
		{
			throw new IllegalArgumentException("minLength must be >= 0");
		}
		this.minArea = minArea;
		this.minLength = minLength;
	}

	@Override
	public boolean accept(Geometry geometry)
	{
		if ((geometry instanceof Polygon || geometry instanceof MultiPolygon) && geometry.getArea() < minArea)
		{
			return false;

		}
		else if ((geometry instanceof LineString || geometry instanceof MultiLineString)
				&& geometry.getLength() < minLength)
		{
			return false;
		}
		return true;
	}
}
