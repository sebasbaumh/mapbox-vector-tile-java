package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.GeomType;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;
import io.github.sebasbaumh.mapbox.vectortile.util.GeomCmd;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;
import io.github.sebasbaumh.mapbox.vectortile.util.Vec2d;

/**
 * Adapt JTS {@link Geometry} to 'Mapbox Vector Tile' objects.
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public final class JtsAdapter
{
	/**
	 * Adds features for the given geometry to the given layer builder.
	 * @param layerBuilder layer builder to write to
	 * @param geometry JTS geometry to convert
	 * @param layerProps layer properties for tagging features
	 * @param userDataConverter convert {@link Geometry#getUserData()} to MVT feature tags (can be null for no
	 *            converter)
	 */
	public static void addFeatures(VectorTile.Tile.Layer.Builder layerBuilder, Geometry geometry,
			MvtLayerProps layerProps, @Nullable IUserDataConverter userDataConverter)
	{
		// short cut for simple geometries
		if (!(geometry instanceof GeometryCollection))
		{
			VectorTile.Tile.Feature nextFeature = toFeature(geometry, layerProps, userDataConverter);
			if (nextFeature != null)
			{
				layerBuilder.addFeatures(nextFeature);
			}
		}
		else
		{
			// split up the features
			Collection<Geometry> geometries = collectFlatGeometries(geometry);
			addFeatures(layerBuilder, geometries, layerProps, userDataConverter);
		}
	}

	/**
	 * Adds features for the given geometry to the given layer builder.
	 * @param layerBuilder layer builder to write to
	 * @param geometries JTS geometries to convert
	 * @param layerProps layer properties for tagging features
	 * @param userDataConverter convert {@link Geometry#getUserData()} to MVT feature tags (can be null for no
	 *            converter)
	 */
	public static void addFeatures(VectorTile.Tile.Layer.Builder layerBuilder, Iterable<Geometry> geometries,
			MvtLayerProps layerProps, @Nullable IUserDataConverter userDataConverter)
	{
		for (Geometry geom : geometries)
		{
			VectorTile.Tile.Feature nextFeature = toFeature(geom, layerProps, userDataConverter);
			if (nextFeature != null)
			{
				layerBuilder.addFeatures(nextFeature);
			}
		}
	}

	/**
	 * <p>
	 * Recursively convert a {@link Geometry}, which may be an instance of {@link GeometryCollection} with mixed element
	 * types, into a flat list containing only the following {@link Geometry} types:
	 * </p>
	 * <ul>
	 * <li>{@link Point}</li>
	 * <li>{@link LineString}</li>
	 * <li>{@link Polygon}</li>
	 * <li>{@link MultiPoint}</li>
	 * <li>{@link MultiLineString}</li>
	 * <li>{@link MultiPolygon}</li>
	 * </ul>
	 * <p>
	 * WARNING: Any other Geometry types that were not mentioned in the list above will be discarded!
	 * </p>
	 * <p>
	 * Useful for converting a generic geometry into a list of simple MVT-feature-ready geometries.
	 * </p>
	 * @param geom geometry to flatten
	 * @return list of MVT-feature-ready geometries
	 */
	private static Collection<Geometry> collectFlatGeometries(Geometry geom)
	{
		// short cut for simple geometries
		if (geom instanceof Point || geom instanceof MultiPoint || geom instanceof LineString
				|| geom instanceof MultiLineString || geom instanceof Polygon || geom instanceof MultiPolygon)
		{
			return Collections.singleton(geom);
		}
		// collect geometries from geometry collection
		ArrayList<Geometry> singleGeoms = new ArrayList<Geometry>();
		collectFlatGeometries(geom, singleGeoms);
		return singleGeoms;
	}

	/**
	 * Collects all support geometries into a given collection of flat geometries.
	 * @param geom geometry to flatten
	 * @param singleGeoms list of MVT-feature-ready geometries
	 */
	private static void collectFlatGeometries(Geometry geom, Collection<Geometry> singleGeoms)
	{
		if (geom instanceof Point || geom instanceof MultiPoint || geom instanceof LineString
				|| geom instanceof MultiLineString || geom instanceof Polygon || geom instanceof MultiPolygon)
		{
			singleGeoms.add(geom);
		}
		else if (geom instanceof GeometryCollection)
		{
			// Push all child geometries
			int nextGeomCount = geom.getNumGeometries();
			for (int i = 0; i < nextGeomCount; ++i)
			{
				collectFlatGeometries(geom.getGeometryN(i), singleGeoms);
			}
		}
	}

	/**
	 * <p>
	 * Count number of coordinates starting from the end of the coordinate array backwards that match the first
	 * coordinate value.
	 * </p>
	 * <p>
	 * Useful for ensuring self-closing line strings do not repeat the first coordinate.
	 * </p>
	 * @param coords coordinates to check for duplicate points
	 * @return number of duplicate points at the rear of the list
	 */
	private static int countCoordRepeatReverse(Coordinate[] coords)
	{
		int repeatCoords = 0;
		final Coordinate firstCoord = coords[0];
		Coordinate nextCoord;
		for (int i = coords.length - 1; i > 0; --i)
		{
			nextCoord = coords[i];
			if (equalAsInts2d(firstCoord, nextCoord))
			{
				repeatCoords++;
			}
			else
			{
				break;
			}
		}
		return repeatCoords;
	}

	/**
	 * Create geometry clipped and then converted to MVT 'extent' coordinates.
	 * <p>
	 * Uses the same tile and clipping coordinates. May cause rendering issues on boundaries for polygons or line
	 * geometry depending on styling.
	 * </p>
	 * @param geom original 'source' geometry
	 * @param tileEnvelope world coordinate bounds for tile
	 * @param clipEnvelope world coordinates to clip tile by
	 * @param geomFactory creates a geometry for the tile envelope
	 * @param mvtLayerParams specifies vector tile properties
	 * @param filter geometry values that fail filter after transforms are removed (can be null for no filter)
	 * @return tile geometry
	 * @throws TopologyException if a robustness error occurs
	 */
	public static Geometry createTileGeom(Geometry geom, Envelope tileEnvelope, Envelope clipEnvelope,
			GeometryFactory geomFactory, MvtLayerParams mvtLayerParams, @Nullable IGeometryFilter filter)
	{
		double xDiff = tileEnvelope.getWidth();
		double yDiff = tileEnvelope.getHeight();
		double xOffset = -tileEnvelope.getMinX();
		double yOffset = -tileEnvelope.getMinY();

		// build transformation to transform geometry to the tile
		AffineTransformation t = new AffineTransformation();
		// Transform Setup: Shift to 0 as minimum value
		t.translate(xOffset, yOffset);
		// Transform Setup: Scale X and Y to tile extent values, flip Y values
		t.scale(1d / (xDiff / mvtLayerParams.extent), -1d / (yDiff / mvtLayerParams.extent));
		// Transform Setup: Bump Y values to positive quadrant
		t.translate(0d, mvtLayerParams.extent);

		// The area contained in BOTH the 'original geometry', g, AND the 'clip envelope geometry' is the 'tile
		// geometry'
		Geometry clipEnvelopeGeometry = geomFactory.toGeometry(clipEnvelope);

		// work around issues with JTS 1.14 not supporting intersection on a GeometryCollection by collecting all
		// contained geometries
		Collection<Geometry> flatGeometries = collectFlatGeometries(geom);
		// now intersect them with the tile
		ArrayList<Geometry> intersectedGeoms = new ArrayList<Geometry>(flatGeometries.size());
		for (Geometry flatGeom : flatGeometries)
		{
			// AABB intersection culling
			if (clipEnvelope.intersects(flatGeom.getEnvelopeInternal()))
			{
				Geometry nextIntersected = clipEnvelopeGeometry.intersection(flatGeom);
				if (!nextIntersected.isEmpty())
				{
					// copy user data
					nextIntersected.setUserData(flatGeom.getUserData());
					intersectedGeoms.add(nextIntersected);
				}
			}
		}

		ArrayList<Geometry> transformedGeoms = new ArrayList<Geometry>(intersectedGeoms.size());
		// Transform intersected geometry
		Geometry nextTransformGeom;
		for (Geometry nextInterGeom : intersectedGeoms)
		{
			// get user data of base geometry
			Object nextUserData = nextInterGeom.getUserData();
			// transform it to tile
			nextTransformGeom = t.transform(nextInterGeom);
			// round its coordinates to integer (though they are still stored as doubles)
			nextTransformGeom.apply(RoundingFilter.INSTANCE);

			// TODO: Refactor line simplification
			nextTransformGeom = TopologyPreservingSimplifier.simplify(nextTransformGeom, .1d); // Can't use 0d, specify
																								// value < .5d
			// Apply filter on transformed geometry (if any)
			if ((filter == null) || filter.accept(nextTransformGeom))
			{
				// copy user data (if any) and remember transformed geometry
				nextTransformGeom.setUserData(nextUserData);
				transformedGeoms.add(nextTransformGeom);
			}
		}
		// check if there is only a single geometry
		if (transformedGeoms.size() == 1)
		{
			return transformedGeoms.get(0);
		}
		// build a collection out of the transformed geometries
		return new GeometryCollection(transformedGeoms.toArray(new Geometry[transformedGeoms.size()]), geomFactory);
	}

	/**
	 * Create geometry clipped and then converted to MVT 'extent' coordinates.
	 * <p>
	 * Uses the same tile and clipping coordinates. May cause rendering issues on boundaries for polygons or line
	 * geometry depending on styling.
	 * </p>
	 * @param geom original 'source' geometry
	 * @param tileEnvelope world coordinate bounds for tile
	 * @param geomFactory creates a geometry for the tile envelope
	 * @param mvtLayerParams specifies vector tile properties
	 * @param filter geometry values that fail filter after transforms are removed (can be null for no filter)
	 * @return tile geometry
	 */
	public static Geometry createTileGeom(Geometry geom, Envelope tileEnvelope, GeometryFactory geomFactory,
			MvtLayerParams mvtLayerParams, @Nullable IGeometryFilter filter)
	{
		return createTileGeom(geom, tileEnvelope, tileEnvelope, geomFactory, mvtLayerParams, filter);
	}

	/**
	 * Return true if the values of the two {@link Coordinate} are equal when their first and second ordinates are cast
	 * as ints. Ignores 3rd ordinate.
	 * @param a first coordinate to compare
	 * @param b second coordinate to compare
	 * @return true if the values of the two {@link Coordinate} are equal when their first and second ordinates are cast
	 *         as ints
	 */
	private static boolean equalAsInts2d(Coordinate a, Coordinate b)
	{
		return ((int) a.getOrdinate(0)) == ((int) b.getOrdinate(0))
				&& ((int) a.getOrdinate(1)) == ((int) b.getOrdinate(1));
	}

	/**
	 * Get required geometry buffer size for a {@link LineString} or {@link Polygon} geometry.
	 * @param coordCount coordinate count for the geometry
	 * @param closeEnabled whether a 'ClosePath' command should terminate the command list
	 * @return required geometry buffer length
	 */
	private static int geomCmdBuffLenLines(int coordCount, boolean closeEnabled)
	{
		// MoveTo Header, LineTo Header, Optional ClosePath Header, 2 parameters * coordCount
		return 2 + (closeEnabled ? 1 : 0) + (coordCount * 2);
	}

	/**
	 * Get required geometry buffer size for a {@link Point} or {@link MultiPoint} geometry.
	 * @param coordCount coordinate count for the geometry
	 * @return required geometry buffer length
	 */
	private static int geomCmdBuffLenPts(int coordCount)
	{
		// 1 MoveTo Header, 2 parameters * coordCount
		return 1 + (coordCount * 2);
	}

	/**
	 * <p>
	 * Convert a {@link LineString} or {@link Polygon} to a list of MVT geometry drawing commands. A
	 * {@link MultiLineString} or {@link MultiPolygon} can be encoded by calling this method multiple times.
	 * </p>
	 * <p>
	 * See <a href="https://github.com/mapbox/vector-tile-spec">vector-tile-spec</a> for details.
	 * </p>
	 * <p>
	 * WARNING: The value of the {@code cursor} parameter is modified as a result of calling this method.
	 * </p>
	 * @param geom input of type {@link LineString} or {@link Polygon}. Type is NOT checked and expected to be correct.
	 * @param closeEnabled whether a 'ClosePath' command should terminate the command list
	 * @param cursor modified during processing to contain next MVT cursor position
	 * @param minLineToLen minimum allowed length for LineTo command.
	 * @return list of commands
	 */
	private static Collection<Integer> linesToGeomCmds(final Geometry geom, final boolean closeEnabled,
			final Vec2d cursor, final int minLineToLen)
	{
		Coordinate[] geomCoords = geom.getCoordinates();
		// Calculate the geometry coordinate count for processing that supports ignoring repeated final points
		int geomProcCoordCount;
		if (closeEnabled)
		{
			// Check geometry for repeated end points when closing (Polygon rings)
			int repeatEndCoordCount = countCoordRepeatReverse(geomCoords);
			geomProcCoordCount = geomCoords.length - repeatEndCoordCount;
		}
		else
		{
			// No closing (Line strings)
			geomProcCoordCount = geomCoords.length;
		}

		// Guard/Optimization: Not enough geometry coordinates for a line
		if (geomProcCoordCount < 2)
		{
			return Collections.emptyList();
		}

		// Save cursor position if failure creating geometry occurs
		final Vec2d origCursorPos = new Vec2d(cursor);

		/** Tile commands and parameters */
		final ArrayList<Integer> geomCmds = new ArrayList<Integer>(
				geomCmdBuffLenLines(geomProcCoordCount, closeEnabled));

		// Initial coordinate
		Coordinate nextCoord = geomCoords[0];
		/** Holds next MVT coordinate */
		final Vec2d mvtPos = new Vec2d((int) nextCoord.x, (int) nextCoord.y);

		// Encode initial 'MoveTo' command
		geomCmds.add(MvtUtil.geomCmdHdr(GeomCmd.MoveTo, 1));
		moveCursor(cursor, geomCmds, mvtPos);

		/** Index of 'LineTo' 'command header' */
		final int lineToCmdHdrIndex = geomCmds.size();

		// Insert placeholder for 'LineTo' command header
		geomCmds.add(0);

		/** Length of 'LineTo' draw command */
		int lineToLength = 0;

		for (int i = 1; i < geomProcCoordCount; ++i)
		{
			nextCoord = geomCoords[i];
			mvtPos.setX((int) nextCoord.x);
			mvtPos.setY((int) nextCoord.y);

			// Ignore duplicate MVT points in sequence
			if (!cursor.equals(mvtPos))
			{
				++lineToLength;
				moveCursor(cursor, geomCmds, mvtPos);
			}
		}

		if (lineToLength >= minLineToLen && lineToLength <= MvtUtil.GEOM_CMD_HDR_LEN_MAX)
		{
			// Write 'LineTo' 'command header'
			geomCmds.set(lineToCmdHdrIndex, MvtUtil.geomCmdHdr(GeomCmd.LineTo, lineToLength));
			if (closeEnabled)
			{
				geomCmds.add(MvtUtil.CLOSE_PATH_HDR);
			}
			return geomCmds;
		}
		else
		{
			// Revert cursor position
			cursor.set(origCursorPos);

			// Invalid geometry, need at least 1 'LineTo' value to make a Multiline or Polygon
			return Collections.emptyList();
		}
	}

	/**
	 * <p>
	 * Appends {@link MvtUtil#encodeZigZag(int)} of delta in x,y from {@code cursor} to {@code mvtPos} into the
	 * {@code geomCmds} buffer.
	 * </p>
	 * <p>
	 * Afterwards, the {@code cursor} values are changed to match the {@code mvtPos} values.
	 * </p>
	 * @param cursor MVT cursor position
	 * @param geomCmds geometry command list
	 * @param mvtPos next MVT cursor position
	 */
	private static void moveCursor(Vec2d cursor, List<Integer> geomCmds, Vec2d mvtPos)
	{
		// Delta, then zigzag
		geomCmds.add(MvtUtil.encodeZigZag(mvtPos.getX() - cursor.getX()));
		geomCmds.add(MvtUtil.encodeZigZag(mvtPos.getY() - cursor.getY()));
		// store new position
		cursor.set(mvtPos);
	}

	/**
	 * <p>
	 * Convert a {@link Point} or {@link MultiPoint} geometry to a list of MVT geometry drawing commands. See
	 * <a href="https://github.com/mapbox/vector-tile-spec">vector-tile-spec</a> for details.
	 * </p>
	 * <p>
	 * WARNING: The value of the {@code cursor} parameter is modified as a result of calling this method.
	 * </p>
	 * @param geom input of type {@link Point} or {@link MultiPoint}. Type is NOT checked and expected to be correct.
	 * @param cursor modified during processing to contain next MVT cursor position
	 * @return list of commands
	 */
	private static List<Integer> ptsToGeomCmds(final Geometry geom, final Vec2d cursor)
	{
		// Guard: empty geometry coordinates
		Coordinate[] geomCoords = geom.getCoordinates();
		if (geomCoords.length == 0)
		{
			return Collections.emptyList();
		}

		/** Tile commands and parameters */
		ArrayList<Integer> geomCmds = new ArrayList<Integer>(geomCmdBuffLenPts(geomCoords.length));
		/** Length of 'MoveTo' draw command */
		int moveCmdLen = 0;

		// Insert placeholder for 'MoveTo' command header
		geomCmds.add(0);

		for (int i = 0; i < geomCoords.length; ++i)
		{
			Coordinate nextCoord = geomCoords[i];
			Vec2d mvtPos = new Vec2d((int) nextCoord.x, (int) nextCoord.y);

			// Ignore duplicate MVT points
			if (i == 0 || !cursor.equals(mvtPos))
			{
				moveCmdLen++;
				moveCursor(cursor, geomCmds, mvtPos);
			}
		}

		if (moveCmdLen <= MvtUtil.GEOM_CMD_HDR_LEN_MAX)
		{
			// Write 'MoveTo' command header to first index
			geomCmds.set(0, MvtUtil.geomCmdHdr(GeomCmd.MoveTo, moveCmdLen));

			return geomCmds;
		}
		else
		{
			// Invalid geometry, need at least 1 'MoveTo' value to make points
			return Collections.emptyList();
		}
	}

	/**
	 * Create and return a feature from a geometry. Returns null on failure.
	 * @param geom flat geometry (in MVT coordinates) that can be translated to a feature
	 * @param layerProps layer properties for tagging features
	 * @param userDataConverter (can be null for no converter)
	 * @return new tile feature instance, or null on failure
	 */
	@Nullable
	private static VectorTile.Tile.Feature toFeature(Geometry geom, MvtLayerProps layerProps,
			@Nullable IUserDataConverter userDataConverter)
	{
		// Guard: UNKNOWN Geometry
		final VectorTile.Tile.GeomType mvtGeomType = JtsAdapter.toGeomType(geom);
		if (mvtGeomType == VectorTile.Tile.GeomType.UNKNOWN)
		{
			return null;
		}

		final VectorTile.Tile.Feature.Builder featureBuilder = VectorTile.Tile.Feature.newBuilder();
		// should the MVT geometry type be closed with a GeomCmd.ClosePath?
		final boolean mvtClosePath = mvtGeomType == GeomType.POLYGON;
		ArrayList<Integer> mvtGeom = new ArrayList<Integer>();
		Vec2d cursor = new Vec2d();

		featureBuilder.setType(mvtGeomType);

		if (geom instanceof Point || geom instanceof MultiPoint)
		{
			// Encode as MVT point or multipoint
			mvtGeom.addAll(ptsToGeomCmds(geom, cursor));
		}
		else if (geom instanceof LineString || geom instanceof MultiLineString)
		{
			// Encode as MVT linestring or multi-linestring
			for (int i = 0; i < geom.getNumGeometries(); ++i)
			{
				mvtGeom.addAll(linesToGeomCmds(geom.getGeometryN(i), mvtClosePath, cursor, 1));
			}
		}
		else if (geom instanceof MultiPolygon || geom instanceof Polygon)
		{
			// Encode as MVT polygon or multi-polygon
			for (int i = 0; i < geom.getNumGeometries(); ++i)
			{
				final Polygon nextPoly = (Polygon) geom.getGeometryN(i);
				// Add exterior ring
				final LineString exteriorRing = nextPoly.getExteriorRing();

				// Area must be non-zero
				final double exteriorArea = Area.ofRingSigned(exteriorRing.getCoordinates());
				if (((int) Math.round(exteriorArea)) == 0)
				{
					continue;
				}

				// Check CCW Winding (must be positive area in original coordinate system, MVT is positive-y-down, so
				// inequality is flipped)
				// See: https://docs.mapbox.com/vector-tiles/specification/#winding-order
				if (exteriorArea > 0d)
				{
					CoordinateArrays.reverse(exteriorRing.getCoordinates());
				}

				ArrayList<Integer> nextPolyGeom = new ArrayList<Integer>();
				nextPolyGeom.addAll(linesToGeomCmds(exteriorRing, mvtClosePath, cursor, 2));

				boolean valid = true;
				// Add interior rings
				for (int ringIndex = 0; ringIndex < nextPoly.getNumInteriorRing(); ++ringIndex)
				{

					final LineString nextInteriorRing = nextPoly.getInteriorRingN(ringIndex);

					// Area must be non-zero
					final double interiorArea = Area.ofRingSigned(nextInteriorRing.getCoordinates());
					if (((int) Math.round(interiorArea)) == 0)
					{
						continue;
					}

					// Check CW Winding (must be negative area in original coordinate system, MVT is positive-y-down, so
					// inequality is flipped)
					// See: https://docs.mapbox.com/vector-tiles/specification/#winding-order
					if (interiorArea < 0d)
					{
						CoordinateArrays.reverse(nextInteriorRing.getCoordinates());
					}

					// Interior ring area must be < exterior ring area, or entire geometry is invalid
					if (Math.abs(exteriorArea) <= Math.abs(interiorArea))
					{
						valid = false;
						break;
					}

					nextPolyGeom.addAll(linesToGeomCmds(nextInteriorRing, mvtClosePath, cursor, 2));
				}

				if (valid)
				{
					mvtGeom.addAll(nextPolyGeom);
				}
			}
		}

		if (mvtGeom.isEmpty())
		{
			return null;
		}

		featureBuilder.addAllGeometry(mvtGeom);

		// add feature Properties?
		if (userDataConverter != null)
		{
			Object userData = geom.getUserData();
			if (userData != null)
			{
				userDataConverter.addTags(userData, layerProps, featureBuilder);
			}
		}
		return featureBuilder.build();
	}

	/**
	 * <p>
	 * Convert a flat list of JTS {@link Geometry} to a list of vector tile features. The Geometry should be in MVT
	 * coordinates.
	 * </p>
	 * <p>
	 * Each geometry will have its own ID.
	 * </p>
	 * @param geometries list of JTS geometry (in MVT coordinates) to convert
	 * @param layerProps layer properties for tagging features
	 * @param userDataConverter convert {@link Geometry#getUserData()} to MVT feature tags (can be null for no
	 *            converter)
	 * @return features
	 * @see #createTileGeom(Geometry, Envelope, GeometryFactory, MvtLayerParams, IGeometryFilter)
	 * @see #addFeatures(io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.Layer.Builder, Geometry, MvtLayerProps,
	 *      IUserDataConverter)
	 * @deprecated use #addFeatures(io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.Layer.Builder, Geometry,
	 *             MvtLayerProps, IUserDataConverter) instead
	 */
	@Deprecated
	public static Collection<VectorTile.Tile.Feature> toFeatures(Collection<Geometry> geometries,
			MvtLayerProps layerProps, @Nullable IUserDataConverter userDataConverter)
	{
		// check if there is a geometry at all
		Iterator<Geometry> it = geometries.iterator();
		if (!it.hasNext())
		{
			return Collections.emptyList();
		}
		Geometry geomFirst = it.next();
		// check if there is only a single geometry (and no complex collection)
		if (!it.hasNext() && !(geomFirst instanceof GeometryCollection))
		{
			VectorTile.Tile.Feature nextFeature = toFeature(geomFirst, layerProps, userDataConverter);
			if (nextFeature != null)
			{
				return Collections.singleton(nextFeature);
			}
			else
			{
				return Collections.emptyList();
			}
		}
		// ensure to build a list of the correct size as there may be a large number of geometries
		ArrayList<VectorTile.Tile.Feature> features = new ArrayList<VectorTile.Tile.Feature>(
				((Collection<?>) geometries).size());
		for (Geometry nextGeom : geometries)
		{
			VectorTile.Tile.Feature nextFeature = toFeature(nextGeom, layerProps, userDataConverter);
			if (nextFeature != null)
			{
				features.add(nextFeature);
			}
		}
		return features;
	}

	/**
	 * <p>
	 * Convert JTS {@link Geometry} to a list of vector tile features. The Geometry should be in MVT coordinates.
	 * </p>
	 * <p>
	 * Each geometry will have its own ID.
	 * </p>
	 * @param geometry JTS geometry to convert
	 * @param layerProps layer properties for tagging features
	 * @param userDataConverter convert {@link Geometry#getUserData()} to MVT feature tags (can be null for no
	 *            converter)
	 * @return features
	 * @see #createTileGeom(Geometry, Envelope, GeometryFactory, MvtLayerParams, IGeometryFilter)
	 * @see #addFeatures(io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.Layer.Builder, Geometry, MvtLayerProps,
	 *      IUserDataConverter)
	 * @deprecated use #addFeatures(io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.Layer.Builder, Geometry,
	 *             MvtLayerProps, IUserDataConverter) instead
	 */
	@Deprecated
	public static Collection<VectorTile.Tile.Feature> toFeatures(Geometry geometry, MvtLayerProps layerProps,
			@Nullable IUserDataConverter userDataConverter)
	{
		// short cut
		if (!(geometry instanceof GeometryCollection))
		{
			VectorTile.Tile.Feature nextFeature = toFeature(geometry, layerProps, userDataConverter);
			if (nextFeature != null)
			{
				return Collections.singleton(nextFeature);
			}
			else
			{
				return Collections.emptyList();
			}
		}
		// use implementation for multiple features and collect flat geometries upfront
		return toFeatures(collectFlatGeometries(geometry), layerProps, userDataConverter);
	}

	/**
	 * Get the MVT type mapping for the provided JTS Geometry.
	 * @param geometry JTS Geometry to get MVT type for
	 * @return MVT type for the given JTS Geometry, may return
	 *         {@link io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile.GeomType#UNKNOWN}
	 */
	public static VectorTile.Tile.GeomType toGeomType(Geometry geometry)
	{
		if (geometry instanceof Point || geometry instanceof MultiPoint)
		{
			return VectorTile.Tile.GeomType.POINT;
		}
		else if (geometry instanceof LineString || geometry instanceof MultiLineString)
		{
			return VectorTile.Tile.GeomType.LINESTRING;

		}
		else if (geometry instanceof Polygon || geometry instanceof MultiPolygon)
		{
			return VectorTile.Tile.GeomType.POLYGON;
		}
		return VectorTile.Tile.GeomType.UNKNOWN;
	}
}
