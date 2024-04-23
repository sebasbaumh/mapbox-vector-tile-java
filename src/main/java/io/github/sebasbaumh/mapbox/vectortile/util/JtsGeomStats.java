package io.github.sebasbaumh.mapbox.vectortile.util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.JtsAdapter;

/**
 * Provides feature counts and feature statistics (points and repeated points).
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public final class JtsGeomStats
{
	/**
	 * Number of features per geometry type.
	 */
	public final Map<VectorTile.Tile.GeomType, Integer> featureCounts = new EnumMap<VectorTile.Tile.GeomType, Integer>(
			VectorTile.Tile.GeomType.class);
	/**
	 * Statistics for features.
	 */
	public final List<FeatureStats> featureStats = new ArrayList<FeatureStats>();

	/**
	 * Constructs an instance.
	 */
	private JtsGeomStats()
	{
		for (VectorTile.Tile.GeomType nextGeomType : VectorTile.Tile.GeomType.values())
		{
			featureCounts.put(nextGeomType, 0);
		}
	}

	private static int checkRepeatedPoints2d(LineString lineString)
	{
		int repeatedPoints = 0;

		final CoordinateSequence coordSeq = lineString.getCoordinateSequence();
		Coordinate nextCoord = null;
		Coordinate prevCoord;
		for (int i = 0; i < coordSeq.size(); ++i)
		{
			prevCoord = nextCoord;
			nextCoord = coordSeq.getCoordinate(i);
			if (nextCoord.equals(prevCoord))
			{
				++repeatedPoints;
			}
		}
		return repeatedPoints;
	}

	private static FeatureStats getStats(Geometry geom, VectorTile.Tile.GeomType type)
	{
		switch (type)
		{
			case POINT:
				return pointStats(geom);
			case LINESTRING:
				return lineStats(geom);
			case POLYGON:
				return polyStats(geom);
			default:
				return new FeatureStats();
		}
	}

	/**
	 * Get feature counts and feature statistics (points and repeated points).
	 * @param flatGeomList geometry under analysis
	 * @return the resulting statistics
	 */
	public static JtsGeomStats getStats(Iterable<Geometry> flatGeomList)
	{
		final JtsGeomStats stats = new JtsGeomStats();
		for (Geometry nextGeom : flatGeomList)
		{
			final VectorTile.Tile.GeomType geomType = JtsAdapter.toGeomType(nextGeom);

			// Count features by type
			Integer value = stats.featureCounts.get(geomType);
			value = value == null ? 1 : value + 1;
			stats.featureCounts.put(geomType, value);

			// Get stats per feature
			stats.featureStats.add(getStats(nextGeom, geomType));
		}
		return stats;
	}

	private static FeatureStats lineStats(Geometry geom)
	{
		final FeatureStats featureStats = new FeatureStats();
		for (int i = 0; i < geom.getNumGeometries(); ++i)
		{
			final LineString lineString = (LineString) geom.getGeometryN(i);
			featureStats.totalPts += lineString.getNumPoints();
			featureStats.repeatedPts += checkRepeatedPoints2d(lineString);
		}
		return featureStats;
	}

	private static FeatureStats pointStats(Geometry geom)
	{
		final FeatureStats featureStats = new FeatureStats();
		final HashSet<Point> pointSet = HashSet.newHashSet(geom.getNumPoints());
		featureStats.totalPts = geom.getNumPoints();

		for (int i = 0; i < geom.getNumGeometries(); ++i)
		{
			final Point p = (Point) geom.getGeometryN(i);
			featureStats.repeatedPts += pointSet.add(p) ? 0 : 1;
		}
		return featureStats;
	}

	private static FeatureStats polyStats(Geometry geom)
	{
		final FeatureStats featureStats = new FeatureStats();
		for (int i = 0; i < geom.getNumGeometries(); ++i)
		{
			final Polygon nextPoly = (Polygon) geom.getGeometryN(i);

			// Stats: exterior ring
			final LineString exteriorRing = nextPoly.getExteriorRing();
			featureStats.totalPts += exteriorRing.getNumPoints();
			featureStats.repeatedPts += checkRepeatedPoints2d(exteriorRing);

			// Stats: interior rings
			for (int ringIndex = 0; ringIndex < nextPoly.getNumInteriorRing(); ++ringIndex)
			{

				final LineString nextInteriorRing = nextPoly.getInteriorRingN(ringIndex);
				featureStats.totalPts += nextInteriorRing.getNumPoints();
				featureStats.repeatedPts += checkRepeatedPoints2d(nextInteriorRing);
			}
		}
		return featureStats;
	}

	@Override
	public String toString()
	{
		return "JtsGeomStats{" + "featureCounts=" + featureCounts + ", featureStats=" + featureStats + '}';
	}

	/**
	 * Provides information about points and total points in a feature.
	 */
	public static final class FeatureStats
	{
		/**
		 * Number of repeated points.
		 */
		public int repeatedPts;
		/**
		 * Number of total points.
		 */
		public int totalPts;

		@Override
		public String toString()
		{
			return "FeatureStats{" + "totalPts=" + totalPts + ", repeatedPts=" + repeatedPts + '}';
		}
	}
}
