package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.LoggerFactory;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsMvt;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Manual test for ensuring linestring geometry does not miss segments on the end.
 */
public class JtsAdapterIssue27Test {

    public static void main(String args[]) throws IOException, ParseException {
        testLineStringMisssingEndSegments();
    }

    public static void testLineStringMisssingEndSegments() throws IOException, ParseException {
        final File wktFile = new File("src/test/resources/wkt/github_issue_27_01_multilinestring.wkt");
        final File outputFile = new File("linestring.mvt");
        final GeometryFactory gf = new GeometryFactory();
        final WKTReader reader = new WKTReader(gf);
        final MvtLayerParams mvtLayerParams = MvtLayerParams.DEFAULT;

        try(FileReader fileReader = new FileReader(wktFile)) {
            final Geometry wktGeom = reader.read(fileReader);
            final Envelope env = new Envelope(545014.05D, 545043.15D, 6867178.74D, 6867219.47D);
            final TileGeomResult tileGeom = JtsAdapter.createTileGeom(wktGeom, env, gf, mvtLayerParams, g -> true);

            final MvtLayerProps mvtLayerProps = new MvtLayerProps();
            final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
            final VectorTile.Tile.Layer.Builder layerBuilder = MvtUtil.newLayerBuilder("myLayerName", mvtLayerParams);
            final List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(tileGeom.mvtGeoms, mvtLayerProps, new UserDataIgnoreConverter());
            layerBuilder.addAllFeatures(features);
            MvtUtil.writeProps(layerBuilder, mvtLayerProps);
            tileBuilder.addLayers(layerBuilder);

            final VectorTile.Tile mvt = tileBuilder.build();
            try {
                Files.write(outputFile.toPath(), mvt.toByteArray());
            } catch (IOException e) {
                LoggerFactory.getLogger(JtsAdapterIssue27Test.class).error(e.getMessage(), e);
            }

            // Examine geometry output, will be a bit screwed but observe line segments are present
            @SuppressWarnings("unused")
			final JtsMvt jtsMvt = MvtReader.loadMvt(outputFile, gf, new TagIgnoreConverter());
        }
    }
}
