package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.LoggerFactory;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsLayer;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsMvt;
import io.github.sebasbaumh.mapbox.vectortile.util.GeomCmd;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;
import io.github.sebasbaumh.mapbox.vectortile.util.Vec2d;

/**
 * Load Mapbox Vector Tiles (MVT) to JTS {@link Geometry}. Feature tags may be converted to user data via
 * {@link ITagConverter}.
 * @see JtsMvt
 * @see JtsLayer
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public final class MvtReader
{
	private static final int MIN_LINE_STRING_LEN = 6; // MoveTo,1 + LineTo,1
	private static final int MIN_POLYGON_LEN = 9; // MoveTo,1 + LineTo,2 + ClosePath
	/**
	 * Area for surveyor formula may be positive or negative for exterior rings. Mimics Mapbox parsers supporting V1.
	 */
	public static final RingClassifier RING_CLASSIFIER_V1 = new PolyRingClassifierV1();
	/**
	 * Area from surveyor formula must be positive for exterior rings. Obeys V2.1 spec.
	 */
	public static final RingClassifier RING_CLASSIFIER_V2_1 = new PolyRingClassifierV2_1();
	/**
	 * Default ring classifier when it is not specified.
	 */
	private static final RingClassifier RING_CLASSIFIER_DEFAULT = RING_CLASSIFIER_V1;

	// prevent instantiating this class
	@Deprecated
	private MvtReader()
	{
	}

	/**
	 * Convenience method for loading MVT from file. See
	 * {@link #loadMvt(InputStream, GeometryFactory, ITagConverter, RingClassifier)}. Uses {@link #RING_CLASSIFIER_V2_1}
	 * for forming Polygons and MultiPolygons.
	 * @param file path to the MVT
	 * @param geomFactory allows for JTS geometry creation
	 * @param tagConverter converts MVT feature tags to JTS user data object (can be null for no converter)
	 * @return JTS MVT with geometry in MVT coordinates
	 * @throws IOException failure reading MVT from path
	 * @see #loadMvt(InputStream, GeometryFactory, ITagConverter, RingClassifier)
	 * @see Geometry
	 * @see Geometry#getUserData()
	 * @see RingClassifier
	 */
	public static JtsMvt loadMvt(File file, GeometryFactory geomFactory, @Nullable ITagConverter tagConverter)
			throws IOException
	{
		return loadMvt(file, geomFactory, tagConverter, RING_CLASSIFIER_DEFAULT);
	}

	/**
	 * Convenience method for loading MVT from file. See
	 * {@link #loadMvt(InputStream, GeometryFactory, ITagConverter, RingClassifier)}.
	 * @param file path to the MVT
	 * @param geomFactory allows for JTS geometry creation
	 * @param tagConverter converts MVT feature tags to JTS user data object (can be null for no converter)
	 * @param ringClassifier determines how rings are parsed into Polygons and MultiPolygons
	 * @return JTS MVT with geometry in MVT coordinates
	 * @throws IOException failure reading MVT from path
	 * @see #loadMvt(InputStream, GeometryFactory, ITagConverter, RingClassifier)
	 * @see Geometry
	 * @see Geometry#getUserData()
	 * @see RingClassifier
	 */
	public static JtsMvt loadMvt(File file, GeometryFactory geomFactory, @Nullable ITagConverter tagConverter,
			RingClassifier ringClassifier) throws IOException
	{
		final JtsMvt jtsMvt;

		try (final InputStream is = new FileInputStream(file))
		{
			jtsMvt = loadMvt(is, geomFactory, tagConverter, ringClassifier);
		}

		return jtsMvt;
	}

	/**
	 * Load an MVT to JTS geometries using coordinates. Uses {@code tagConverter} to create user data from feature
	 * properties.
	 * @param is stream with MVT data
	 * @param geomFactory allows for JTS geometry creation
	 * @param tagConverter converts MVT feature tags to JTS user data object (can be null for no converter)
	 * @return JTS MVT with geometry in MVT coordinates
	 * @throws IOException failure reading MVT from stream
	 * @see Geometry
	 * @see Geometry#getUserData()
	 * @see RingClassifier
	 */
	public static JtsMvt loadMvt(InputStream is, GeometryFactory geomFactory, @Nullable ITagConverter tagConverter)
			throws IOException
	{
		return loadMvt(is, geomFactory, tagConverter, RING_CLASSIFIER_DEFAULT);
	}

	/**
	 * Load an MVT to JTS geometries using coordinates. Uses {@code tagConverter} to create user data from feature
	 * properties.
	 * @param is stream with MVT data
	 * @param geomFactory allows for JTS geometry creation
	 * @param tagConverter converts MVT feature tags to JTS user data object (can be null for no converter)
	 * @param ringClassifier determines how rings are parsed into Polygons and MultiPolygons
	 * @return JTS MVT with geometry in MVT coordinates
	 * @throws IOException failure reading MVT from stream
	 * @see Geometry
	 * @see Geometry#getUserData()
	 * @see RingClassifier
	 */
	public static JtsMvt loadMvt(InputStream is, GeometryFactory geomFactory, @Nullable ITagConverter tagConverter,
			RingClassifier ringClassifier) throws IOException
	{

		final VectorTile.Tile mvt = VectorTile.Tile.parseFrom(is);
		final Vec2d cursor = new Vec2d();
		final List<JtsLayer> jtsLayers = new ArrayList<>(mvt.getLayersList().size());

		for (VectorTile.Tile.Layer nextLayer : mvt.getLayersList())
		{

			final List<String> keysList = nextLayer.getKeysList();
			final List<VectorTile.Tile.Value> valuesList = nextLayer.getValuesList();
			final List<Geometry> layerGeoms = new ArrayList<>(nextLayer.getFeaturesList().size());

			for (VectorTile.Tile.Feature nextFeature : nextLayer.getFeaturesList())
			{

				final Long id = nextFeature.hasId() ? nextFeature.getId() : null;

				final VectorTile.Tile.GeomType geomType = nextFeature.getType();

				if (geomType == VectorTile.Tile.GeomType.UNKNOWN)
				{
					continue;
				}

				final List<Integer> geomCmds = nextFeature.getGeometryList();
				cursor.set(0, 0);
				final Geometry nextGeom = readGeometry(geomCmds, geomType, geomFactory, cursor, ringClassifier);
				if (nextGeom != null)
				{
					if (tagConverter != null)
					{
						nextGeom.setUserData(
								tagConverter.toUserData(id, nextFeature.getTagsList(), keysList, valuesList));
					}
					layerGeoms.add(nextGeom);
				}
			}

			jtsLayers.add(new JtsLayer(nextLayer.getName(), layerGeoms, nextLayer.getExtent()));
		}

		return new JtsMvt(jtsLayers);
	}

	@Nullable
	private static Geometry readGeometry(List<Integer> geomCmds, VectorTile.Tile.GeomType geomType,
			GeometryFactory geomFactory, Vec2d cursor, RingClassifier ringClassifier)
	{
		Geometry result = null;

		switch (geomType)
		{
			case POINT:
				result = readPoints(geomFactory, geomCmds, cursor);
				break;
			case LINESTRING:
				result = readLines(geomFactory, geomCmds, cursor);
				break;
			case POLYGON:
				result = readPolys(geomFactory, geomCmds, cursor, ringClassifier);
				break;
			default:
				LoggerFactory.getLogger(MvtReader.class).error("readGeometry(): Unhandled geometry type [{}]",
						geomType);
		}

		return result;
	}

	/**
	 * Create {@link LineString} or {@link MultiLineString} from MVT geometry drawing commands.
	 * @param geomFactory creates JTS geometry
	 * @param geomCmds contains MVT geometry commands
	 * @param cursor contains current MVT extent position
	 * @return JTS geometry or null on failure
	 */
	@Nullable
	private static Geometry readLines(GeometryFactory geomFactory, List<Integer> geomCmds, Vec2d cursor)
	{

		// Guard: must have header
		if (geomCmds.isEmpty())
		{
			return null;
		}

		/** Geometry command index */
		int i = 0;

		int cmdHdr;
		int cmdLength;
		GeomCmd cmd;
		List<LineString> geoms = new ArrayList<>(1);
		CoordinateSequence nextCoordSeq;

		while (i <= geomCmds.size() - MIN_LINE_STRING_LEN)
		{

			// --------------------------------------------
			// Expected: MoveTo command of length 1
			// --------------------------------------------

			// Read command header
			cmdHdr = geomCmds.get(i++);
			cmdLength = MvtUtil.getGeomCmdLength(cmdHdr);
			cmd = MvtUtil.getGeomCmd(cmdHdr);

			// Guard: command type and length
			if (cmd != GeomCmd.MOVE_TO || cmdLength != 1)
			{
				break;
			}

			// Update cursor position with relative move
			cursor.add(MvtUtil.decodeZigZag(geomCmds.get(i++)), MvtUtil.decodeZigZag(geomCmds.get(i++)));

			// --------------------------------------------
			// Expected: LineTo command of length > 0
			// --------------------------------------------

			// Read command header
			cmdHdr = geomCmds.get(i++);
			cmdLength = MvtUtil.getGeomCmdLength(cmdHdr);
			cmd = MvtUtil.getGeomCmd(cmdHdr);

			// Guard: command type and length
			// Guard: header data length unsupported by geometry command buffer
			// (require at least (1 value * 2 params) + current_index)
			if (cmd != GeomCmd.LINE_TO || cmdLength < 1
					|| ((cmdLength * GeomCmd.LINE_TO.getParamCount()) + i > geomCmds.size()))
			{
				break;
			}

			nextCoordSeq = geomFactory.getCoordinateSequenceFactory().create(1 + cmdLength, 2);

			// Set first point from MoveTo command
			nextCoordSeq.setOrdinate(0, 0, cursor.getX());
			nextCoordSeq.setOrdinate(0, 1, cursor.getY());

			// Set remaining points from LineTo command
			for (int lineToIndex = 0; lineToIndex < cmdLength; ++lineToIndex)
			{

				// Update cursor position with relative line delta
				cursor.add(MvtUtil.decodeZigZag(geomCmds.get(i++)), MvtUtil.decodeZigZag(geomCmds.get(i++)));

				nextCoordSeq.setOrdinate(lineToIndex + 1, 0, cursor.getX());
				nextCoordSeq.setOrdinate(lineToIndex + 1, 1, cursor.getY());
			}

			geoms.add(geomFactory.createLineString(nextCoordSeq));
		}

		return geoms.size() == 1 ? geoms.get(0) : geomFactory
				.createMultiLineString(geoms.toArray(new LineString[geoms.size()]));
	}

	/**
	 * Create {@link Point} or {@link MultiPoint} from MVT geometry drawing commands.
	 * @param geomFactory creates JTS geometry
	 * @param geomCmds contains MVT geometry commands
	 * @param cursor contains current MVT extent position
	 * @return JTS geometry or null on failure
	 */
	@Nullable
	private static Geometry readPoints(GeometryFactory geomFactory, List<Integer> geomCmds, Vec2d cursor)
	{

		// Guard: must have header
		if (geomCmds.isEmpty())
		{
			return null;
		}

		/** Geometry command index */
		int i = 0;

		// Read command header
		final int cmdHdr = geomCmds.get(i++);
		final int cmdLength = MvtUtil.getGeomCmdLength(cmdHdr);
		final GeomCmd cmd = MvtUtil.getGeomCmd(cmdHdr);

		// Guard: command type
		// Guard: minimum command length
		if ((cmd != GeomCmd.MOVE_TO) || (cmdLength < 1))
		{
			return null;
		}

		// Guard: header data unsupported by geometry command buffer
		// (require header and at least 1 value * 2 params)
		int requiredgeomCmdsLength = cmdLength * GeomCmd.MOVE_TO.getParamCount() + 1;
		if (requiredgeomCmdsLength > geomCmds.size())
		{
			return null;
		}
		if (requiredgeomCmdsLength < geomCmds.size())
		{
			geomCmds = geomCmds.subList(0, requiredgeomCmdsLength); // ignore extra commands... should it return null
																	// instead?
		}

		final CoordinateSequence coordSeq = geomFactory.getCoordinateSequenceFactory().create(cmdLength, 2);
		int coordIndex = 0;

		while (i < geomCmds.size() - 1)
		{
			cursor.add(MvtUtil.decodeZigZag(geomCmds.get(i++)), MvtUtil.decodeZigZag(geomCmds.get(i++)));

			coordSeq.setOrdinate(coordIndex, 0, cursor.getX());
			coordSeq.setOrdinate(coordIndex, 1, cursor.getY());
			coordIndex++;
		}

		return coordSeq.size() == 1 ? geomFactory.createPoint(coordSeq) : geomFactory.createMultiPoint(coordSeq);
	}

	/**
	 * Create {@link Polygon} or {@link MultiPolygon} from MVT geometry drawing commands.
	 * @param geomFactory creates JTS geometry
	 * @param geomCmds contains MVT geometry commands
	 * @param cursor contains current MVT extent position
	 * @param ringClassifier
	 * @return JTS geometry or null on failure
	 */
	@Nullable
	private static Geometry readPolys(GeometryFactory geomFactory, List<Integer> geomCmds, Vec2d cursor,
			RingClassifier ringClassifier)
	{

		// Guard: must have header
		if (geomCmds.isEmpty())
		{
			return null;
		}

		/** Geometry command index */
		int i = 0;

		int cmdHdr;
		int cmdLength;
		GeomCmd cmd;
		List<LinearRing> rings = new ArrayList<>(1);
		CoordinateSequence nextCoordSeq;

		while (i <= geomCmds.size() - MIN_POLYGON_LEN)
		{

			// --------------------------------------------
			// Expected: MoveTo command of length 1
			// --------------------------------------------

			// Read command header
			cmdHdr = geomCmds.get(i++);
			cmdLength = MvtUtil.getGeomCmdLength(cmdHdr);
			cmd = MvtUtil.getGeomCmd(cmdHdr);

			// Guard: command type and length
			if (cmd != GeomCmd.MOVE_TO || cmdLength != 1)
			{
				break;
			}

			// Update cursor position with relative move
			cursor.add(MvtUtil.decodeZigZag(geomCmds.get(i++)), MvtUtil.decodeZigZag(geomCmds.get(i++)));

			// --------------------------------------------
			// Expected: LineTo command of length > 1
			// --------------------------------------------

			// Read command header
			cmdHdr = geomCmds.get(i++);
			cmdLength = MvtUtil.getGeomCmdLength(cmdHdr);
			cmd = MvtUtil.getGeomCmd(cmdHdr);

			// Guard: command type and length
			// Guard: header data length unsupported by geometry command buffer
			// (require at least (2 values * 2 params) + (current index 'i') + (1 for ClosePath))
			if (cmd != GeomCmd.LINE_TO || cmdLength < 2
					|| ((cmdLength * GeomCmd.LINE_TO.getParamCount()) + i + 1 > geomCmds.size()))
			{
				break;
			}

			nextCoordSeq = geomFactory.getCoordinateSequenceFactory().create(2 + cmdLength, 2);

			// Set first point from MoveTo command
			nextCoordSeq.setOrdinate(0, 0, cursor.getX());
			nextCoordSeq.setOrdinate(0, 1, cursor.getY());

			// Set remaining points from LineTo command
			for (int lineToIndex = 0; lineToIndex < cmdLength; ++lineToIndex)
			{

				// Update cursor position with relative line delta
				cursor.add(MvtUtil.decodeZigZag(geomCmds.get(i++)), MvtUtil.decodeZigZag(geomCmds.get(i++)));

				nextCoordSeq.setOrdinate(lineToIndex + 1, 0, cursor.getX());
				nextCoordSeq.setOrdinate(lineToIndex + 1, 1, cursor.getY());
			}

			// --------------------------------------------
			// Expected: ClosePath command of length 1
			// --------------------------------------------

			// Read command header
			cmdHdr = geomCmds.get(i++);
			cmdLength = MvtUtil.getGeomCmdLength(cmdHdr);
			cmd = MvtUtil.getGeomCmd(cmdHdr);

			if (cmd != GeomCmd.CLOSE_PATH || cmdLength != 1)
			{
				break;
			}

			// Set last point from ClosePath command
			nextCoordSeq.setOrdinate(nextCoordSeq.size() - 1, 0, nextCoordSeq.getOrdinate(0, 0));
			nextCoordSeq.setOrdinate(nextCoordSeq.size() - 1, 1, nextCoordSeq.getOrdinate(0, 1));

			rings.add(geomFactory.createLinearRing(nextCoordSeq));
		}

		// Classify rings
		final List<Polygon> polygons = ringClassifier.classifyRings(rings, geomFactory);
		if (polygons.isEmpty())
		{
			return null;

		}
		else if (polygons.size() == 1)
		{
			return polygons.get(0);

		}
		else
		{
			return geomFactory.createMultiPolygon(polygons.toArray(new Polygon[polygons.size()]));
		}
	}

	/**
	 * Area for surveyor formula may be positive or negative for exterior rings. Mimics Mapbox parsers supporting V1.
	 * The outer ring winding order is established by the first polygon ring.
	 * @see Area#ofRingSigned(Coordinate[])
	 */
	private static final class PolyRingClassifierV1 implements RingClassifier
	{

		@Override
		public List<Polygon> classifyRings(List<LinearRing> rings, GeometryFactory geomFactory)
		{
			final List<Polygon> polygons = new ArrayList<>();
			final List<LinearRing> holes = new ArrayList<>();

			double outerArea = 0d;
			LinearRing outerPoly = null;

			for (LinearRing r : rings)
			{

				// Area.ofRingSigned() area is positive if the ring is oriented CW, negative if the
				// ring is oriented CCW, and zero if the ring is degenerate or flat
				double area = Area.ofRingSigned(r.getCoordinates());

				if (!r.isRing() || (area == 0d))
				{
					continue; // zero-area
				}

				// Outer ring winding order established by first polygon ring
				// If first ring (no outer) or next ring winding order matches outer ring winding order...
				if (outerPoly == null || (outerArea < 0 == area < 0))
				{

					// Outer
					if (outerPoly != null)
					{
						polygons.add(geomFactory.createPolygon(outerPoly, holes.toArray(new LinearRing[holes.size()])));
						holes.clear();
					}

					outerPoly = r;
					outerArea = area;

				}
				else
				{

					// Hole
					if (Math.abs(outerArea) < Math.abs(area))
					{
						continue; // Holes must have less area, could probably be handled in a isSimple() check
					}

					holes.add(r);
				}
			}

			if (outerPoly != null)
			{
				polygons.add(geomFactory.createPolygon(outerPoly, holes.toArray(new LinearRing[holes.size()])));
			}

			return polygons;
		}
	}

	/**
	 * Area from surveyor formula must be positive for exterior rings (but area check is flipped because MVT is Y-DOWN).
	 * Obeys V2.1 spec.
	 * @see Area#ofRingSigned(Coordinate[])
	 */
	private static final class PolyRingClassifierV2_1 implements RingClassifier
	{

		@Override
		public List<Polygon> classifyRings(List<LinearRing> rings, GeometryFactory geomFactory)
		{
			final List<Polygon> polygons = new ArrayList<>();
			final List<LinearRing> holes = new ArrayList<>();

			double outerArea = 0d;
			LinearRing outerPoly = null;

			for (LinearRing r : rings)
			{

				// Area.ofRingSigned() area is positive if the ring is oriented CW, negative if the
				// ring is oriented CCW, and zero if the ring is degenerate or flat
				double area = Area.ofRingSigned(r.getCoordinates());

				if (!r.isRing() || (area == 0d))
				{
					continue; // zero-area
				}

				if (area < 0d)
				{
					if (outerPoly != null)
					{
						polygons.add(geomFactory.createPolygon(outerPoly, holes.toArray(new LinearRing[holes.size()])));
						holes.clear();
					}

					// Neg (in Y-DOWN MVT) --> Pos CW, Outer
					outerPoly = r;
					outerArea = area;

				}
				else
				{

					if (Math.abs(outerArea) < Math.abs(area))
					{
						continue; // Holes must have less area, could probably be handled in a isSimple() check
					}

					// Pos (in Y-DOWN MVT) --> Neg CCW, Hole
					holes.add(r);
				}
			}

			if (outerPoly != null)
			{
				polygons.add(geomFactory.createPolygon(outerPoly, holes.toArray(new LinearRing[holes.size()])));
			}

			return polygons;
		}
	}

	/**
	 * Classifies Polygon and MultiPolygon rings.
	 */
	public interface RingClassifier
	{

		/**
		 * <p>
		 * Classify a list of rings into polygons using surveyor formula.
		 * </p>
		 * <p>
		 * Zero-area polygons are removed.
		 * </p>
		 * @param rings linear rings to classify into polygons
		 * @param geomFactory creates JTS geometry
		 * @return polygons from classified rings
		 */
		List<Polygon> classifyRings(List<LinearRing> rings, GeometryFactory geomFactory);
	}
}