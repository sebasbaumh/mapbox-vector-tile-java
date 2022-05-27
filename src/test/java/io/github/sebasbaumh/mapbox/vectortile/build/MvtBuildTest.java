package io.github.sebasbaumh.mapbox.vectortile.build;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.junit.Test;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.IGeometryFilter;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.JtsAdapter;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.MvtReader;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.TagKeyValueMapConverter;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.UserDataKeyValueMapConverter;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsLayer;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsMvt;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Test building MVTs.
 */
@SuppressWarnings({ "javadoc", "static-method" })
public final class MvtBuildTest
{

	/**
	 * Do not filter tile geometry.
	 */
	private static final IGeometryFilter ACCEPT_ALL_FILTER = geometry -> true;

	/**
	 * Default MVT parameters.
	 */
	private static final MvtLayerParams DEFAULT_MVT_PARAMS = new MvtLayerParams();

	/**
	 * Generate Geometries with this default specification.
	 */
	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

	/**
	 * Fixed randomization with arbitrary seed value.
	 */
	private static final Random RANDOM = new Random(487125064L);

	private static String TEST_LAYER_NAME = "layerNameHere";

	/**
	 * Example world is 100x100 box.
	 */
	private static final double WORLD_SIZE = 100D;

	private static LineString buildLineString(Random random, int pointCount, GeometryFactory geomFactory)
	{
		final CoordinateSequence coordSeq = getCoordSeq(random, pointCount, geomFactory);
		return new LineString(coordSeq, geomFactory);
	}

	private static MultiPoint buildMultiPoint(Random random, int pointCount, GeometryFactory geomFactory)
	{
		final CoordinateSequence coordSeq = getCoordSeq(random, pointCount, geomFactory);
		return geomFactory.createMultiPoint(coordSeq);
	}

	private static Polygon buildPolygon(Random random, int pointCount, GeometryFactory geomFactory)
	{
		if (pointCount < 3)
		{
			throw new RuntimeException("Need 3 or more points to be a polygon");
		}
		final CoordinateSequence coordSeq = getCoordSeq(random, pointCount, geomFactory);
		final ConvexHull convexHull = new ConvexHull(coordSeq.toCoordinateArray(), geomFactory);
		final Geometry hullGeom = convexHull.getConvexHull();
		return (Polygon) hullGeom;
	}

	private static Point createPoint()
	{
		Coordinate coord = new Coordinate(RANDOM.nextInt(4096), RANDOM.nextInt(4096));
		Point point = GEOMETRY_FACTORY.createPoint(coord);

		Map<String, Object> attributes = new LinkedHashMap<>();
		attributes.put("id", RANDOM.nextDouble());
		attributes.put("name", String.format("name %f : %f", coord.x, coord.y));
		point.setUserData(attributes);

		return point;
	}

	private static VectorTile.Tile encodeMvt(MvtLayerParams mvtParams, Geometry tileGeom)
	{
		// Build MVT
		final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

		// Create MVT layer
		final VectorTile.Tile.Layer.Builder layerBuilder = MvtUtil.newLayerBuilder(TEST_LAYER_NAME, mvtParams);
		final MvtLayerProps layerProps = new MvtLayerProps();

		// MVT tile geometry to MVT features
		JtsAdapter.addFeatures(layerBuilder, tileGeom, layerProps, null);
		MvtUtil.writeProps(layerBuilder, layerProps);

		// Build MVT layer
		final VectorTile.Tile.Layer layer = layerBuilder.build();

		// Add built layer to MVT
		tileBuilder.addLayers(layer);

		/// Build MVT
		return tileBuilder.build();
	}

	private static VectorTile.Tile encodeMvt2(MvtLayerParams mvtParams, Geometry tileGeom)
	{
		// Build MVT
		final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

		// Create MVT layer
		final VectorTile.Tile.Layer.Builder layerBuilder = MvtUtil.newLayerBuilder(TEST_LAYER_NAME, mvtParams);
		final MvtLayerProps layerProps = new MvtLayerProps();

		// MVT tile geometry to MVT features
		JtsAdapter.addFeatures(layerBuilder, tileGeom, layerProps, null);
		MvtUtil.writeProps(layerBuilder, layerProps);

		// Build MVT layer
		final VectorTile.Tile.Layer layer = layerBuilder.build();

		// Add built layer to MVT
		tileBuilder.addLayers(layer);

		/// Build MVT
		return tileBuilder.build();
	}

	private static CoordinateSequence getCoordSeq(Random random, int pointCount, GeometryFactory geomFactory)
	{
		final CoordinateSequence coordSeq = geomFactory.getCoordinateSequenceFactory().create(pointCount, 2);
		for (int i = 0; i < pointCount; ++i)
		{
			final Coordinate coord = coordSeq.getCoordinate(i);
			coord.setOrdinate(0, random.nextDouble() * WORLD_SIZE);
			coord.setOrdinate(1, random.nextDouble() * WORLD_SIZE);
		}
		return coordSeq;
	}

	@Test
	public void testBufferedPolygon() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildPolygon(RANDOM, 200, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final double tileWidth = WORLD_SIZE * .5d;
		final double tileHeight = WORLD_SIZE * .5d;
		final Envelope tileEnvelope = new Envelope(0d, tileWidth, 0d, tileHeight);

		// Build clip envelope - (10 * 2)% buffered area of the tile envelope
		final Envelope clipEnvelope = new Envelope(tileEnvelope);
		final double bufferWidth = tileWidth * .1f;
		final double bufferHeight = tileHeight * .1f;
		clipEnvelope.expandBy(bufferWidth, bufferHeight);

		// Build buffered MVT tile geometry
		Geometry bufferedTileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, clipEnvelope, geomFactory,
				DEFAULT_MVT_PARAMS, ACCEPT_ALL_FILTER);

		// Create MVT layer
		final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, bufferedTileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, bufferedTileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	@Test
	public void testBufferedPolygon2() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildPolygon(RANDOM, 200, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final double tileWidth = WORLD_SIZE * .5d;
		final double tileHeight = WORLD_SIZE * .5d;
		final Envelope tileEnvelope = new Envelope(0d, tileWidth, 0d, tileHeight);

		// Build clip envelope - (10 * 2)% buffered area of the tile envelope
		final Envelope clipEnvelope = new Envelope(tileEnvelope);
		final double bufferWidth = tileWidth * .1f;
		final double bufferHeight = tileHeight * .1f;
		clipEnvelope.expandBy(bufferWidth, bufferHeight);

		// Build buffered MVT tile geometry
		Geometry bufferedTileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, clipEnvelope, geomFactory,
				DEFAULT_MVT_PARAMS, ACCEPT_ALL_FILTER);

		// Create MVT layer
		final VectorTile.Tile mvt = encodeMvt2(DEFAULT_MVT_PARAMS, bufferedTileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, bufferedTileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	@Test
	public void testLines() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildLineString(RANDOM, 10, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

		// Build MVT tile geometry
		Geometry tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory, DEFAULT_MVT_PARAMS,
				ACCEPT_ALL_FILTER);

		// Create MVT layer
		final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, tileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, tileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	@Test
	public void testLines2() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildLineString(RANDOM, 10, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

		// Build MVT tile geometry
		Geometry tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory, DEFAULT_MVT_PARAMS,
				ACCEPT_ALL_FILTER);

		// Create MVT layer
		final VectorTile.Tile mvt = encodeMvt2(DEFAULT_MVT_PARAMS, tileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, tileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	@Test
	public void testPoints() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildMultiPoint(RANDOM, 200, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

		// Build MVT tile geometry
		Geometry tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory, DEFAULT_MVT_PARAMS,
				ACCEPT_ALL_FILTER);

		final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, tileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, tileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	@Test
	public void testPoints2() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildMultiPoint(RANDOM, 200, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

		// Build MVT tile geometry
		Geometry tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory, DEFAULT_MVT_PARAMS,
				ACCEPT_ALL_FILTER);

		final VectorTile.Tile mvt = encodeMvt2(DEFAULT_MVT_PARAMS, tileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, tileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	@Test
	public void testPointsInLayers() throws IOException
	{
		Point point1 = createPoint();
		Point point2 = createPoint();
		Point point3 = createPoint();

		String layer1Name = "Layer 1";
		String layer2Name = "Layer 2";

		byte[] bytes = new MvtWriter.Builder().setLayer(layer1Name).add(point1).add(point2).setLayer(layer2Name)
				.add(point3).build();

		assertNotNull(bytes);

		JtsMvt layers = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		assertNotNull(layers.getLayer(layer1Name));
		assertNotNull(layers.getLayer(layer2Name));

		Collection<Geometry> actualLayer1Geometries = layers.getLayer(layer1Name).getGeometries();
		Collection<Geometry> expectedLayer1Geometries = Arrays.asList(point1, point2);
		assertEquals(expectedLayer1Geometries, actualLayer1Geometries);

		Collection<Geometry> actualLayer2Geometries = layers.getLayer(layer2Name).getGeometries();
		Collection<Geometry> expectedLayer2Geometries = Arrays.asList(point3);
		assertEquals(expectedLayer2Geometries, actualLayer2Geometries);
	}

	@Test
	public void testPolygon() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildPolygon(RANDOM, 200, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

		// Build MVT tile geometry
		Geometry tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory, DEFAULT_MVT_PARAMS,
				ACCEPT_ALL_FILTER);

		// Create MVT layer
		final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, tileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, tileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	@Test
	public void testPolygon2() throws IOException
	{

		// Create input geometry
		final GeometryFactory geomFactory = new GeometryFactory();
		final Geometry inputGeom = buildPolygon(RANDOM, 200, geomFactory);

		// Build tile envelope - 1 quadrant of the world
		final Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

		// Build MVT tile geometry
		Geometry tileGeom = JtsAdapter.createTileGeom(inputGeom, tileEnvelope, geomFactory, DEFAULT_MVT_PARAMS,
				ACCEPT_ALL_FILTER);

		// Create MVT layer
		final VectorTile.Tile mvt = encodeMvt2(DEFAULT_MVT_PARAMS, tileGeom);

		// MVT Bytes
		final byte[] bytes = mvt.toByteArray();

		assertNotNull(bytes);

		JtsMvt expected = new JtsMvt(singletonList(new JtsLayer(TEST_LAYER_NAME, tileGeom)));

		// Load multipolygon z0 tile
		JtsMvt actual = MvtReader.loadMvt(new ByteArrayInputStream(bytes), new GeometryFactory(),
				new TagKeyValueMapConverter());

		// Check that MVT geometries are the same as the ones that were encoded above
		assertEquals(expected, actual);
	}

	private static class MvtWriter
	{

		static class Builder
		{
			// Default MVT parameters
			private static final MvtLayerParams DEFAULT_MVT_PARAMS = new MvtLayerParams();

			private String activeLayer = "default";

			private Map<String, List<Geometry>> layers = new HashMap<>();

			Builder()
			{
			}

			Builder add(Geometry geometry)
			{
				Objects.requireNonNull(geometry);
				getActiveLayer().add(geometry);
				return this;
			}

			byte[] build()
			{
				// Build MVT
				final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

				for (Map.Entry<String, List<Geometry>> layer : layers.entrySet())
				{
					// Layer
					String name = layer.getKey();
					List<Geometry> geometries = layer.getValue();

					// Create MVT layer
					final VectorTile.Tile.Layer.Builder layerBuilder = MvtUtil.newLayerBuilder(name,
							DEFAULT_MVT_PARAMS);

					final MvtLayerProps layerProps = new MvtLayerProps();
					// MVT tile geometry to MVT features
					JtsAdapter.addFeatures(layerBuilder, geometries, layerProps, new UserDataKeyValueMapConverter());
					MvtUtil.writeProps(layerBuilder, layerProps);

					// Build MVT layer
					final VectorTile.Tile.Layer mvtLayer = layerBuilder.build();
					tileBuilder.addLayers(mvtLayer);
				}

				// Build MVT
				return tileBuilder.build().toByteArray();
			}

			private List<Geometry> getActiveLayer()
			{
				boolean isDefined = layers.containsKey(activeLayer);
				if (!isDefined)
				{
					layers.put(activeLayer, new ArrayList<>());
				}
				return layers.get(activeLayer);
			}

			Builder setLayer(String layerName)
			{
				Objects.requireNonNull(layerName);
				activeLayer = layerName;
				return this;
			}
		}
	}
}
