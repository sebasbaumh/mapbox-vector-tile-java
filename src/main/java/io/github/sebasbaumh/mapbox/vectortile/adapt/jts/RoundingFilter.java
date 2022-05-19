package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;

/**
 * <p>
 * Round each coordinate value to an integer.
 * </p>
 * <p>
 * Mapbox vector tiles have fixed precision. This filter can be useful for reducing precision to the extent of a MVT.
 * </p>
 */
@NonNullByDefault
public final class RoundingFilter implements CoordinateSequenceFilter
{

	/**
	 * Singleton instance.
	 */
	public static final RoundingFilter INSTANCE = new RoundingFilter();

	/**
	 * Constructs an instance.
	 */
	private RoundingFilter()
	{
	}

	@Override
	public void filter(@SuppressWarnings("null") CoordinateSequence seq, int i)
	{
		seq.setOrdinate(i, 0, Math.round(seq.getOrdinate(i, 0)));
		seq.setOrdinate(i, 1, Math.round(seq.getOrdinate(i, 1)));
	}

	@Override
	public boolean isDone()
	{
		return false;
	}

	@Override
	public boolean isGeometryChanged()
	{
		return true;
	}
}
