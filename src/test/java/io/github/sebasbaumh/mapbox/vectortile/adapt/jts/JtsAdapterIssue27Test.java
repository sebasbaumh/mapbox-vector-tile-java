package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.LoggerFactory;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Manual test for ensuring linestring geometry does not miss segments on the end.
 */
@SuppressWarnings({ "javadoc" })
public class JtsAdapterIssue27Test
{
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testLineStringMisssingEndSegments() throws IOException, ParseException
	{
		final File wktFile = new File("src/test/resources/wkt/github_issue_27_01_multilinestring.wkt");
		final File outputFile = tempFolder.newFile();
		final GeometryFactory gf = new GeometryFactory();
		final WKTReader reader = new WKTReader(gf);
		final MvtLayerParams mvtLayerParams = MvtLayerParams.DEFAULT;

		try (FileReader fileReader = new FileReader(wktFile))
		{
			final Geometry wktGeom = reader.read(fileReader);
			final Envelope env = new Envelope(545014.05D, 545043.15D, 6867178.74D, 6867219.47D);
			Geometry tileGeom = JtsAdapter.createTileGeom(wktGeom, env, gf, mvtLayerParams, g -> true);

			final MvtLayerProps mvtLayerProps = new MvtLayerProps();
			final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
			final VectorTile.Tile.Layer.Builder layerBuilder = MvtUtil.newLayerBuilder("myLayerName", mvtLayerParams);
			JtsAdapter.addFeatures(layerBuilder, tileGeom, mvtLayerProps, null);
			MvtUtil.writeProps(layerBuilder, mvtLayerProps);
			tileBuilder.addLayers(layerBuilder);

			final VectorTile.Tile mvt = tileBuilder.build();
			try
			{
				Files.write(outputFile.toPath(), mvt.toByteArray());
			}
			catch (IOException e)
			{
				LoggerFactory.getLogger(JtsAdapterIssue27Test.class).error(e.getMessage(), e);
			}

			MvtReader.loadMvt(outputFile, gf, null);
		}
	}
}
