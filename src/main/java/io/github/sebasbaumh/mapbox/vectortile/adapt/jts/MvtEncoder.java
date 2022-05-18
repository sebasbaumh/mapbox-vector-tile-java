package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import org.locationtech.jts.geom.Geometry;
import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsLayer;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsMvt;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerBuild;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;

import java.util.Collection;
import java.util.List;

/**
 * Convenience class allows easy encoding of a {@link JtsMvt} to bytes.
 */
public final class MvtEncoder {

    /**
     * Encode a {@link JtsMvt} to byte[] ready for writing to a file.
     *
     * <p>Uses {@link MvtLayerParams#DEFAULT} and {@link UserDataKeyValueMapConverter} to transform the JtsMvt.</p>
     *
     * @param mvt input to encode to bytes
     * @return bytes ready for writing to a .mvt
     * @see #encode(JtsMvt, MvtLayerParams, IUserDataConverter)
     */
    public static byte[] encode(JtsMvt mvt) {
        return encode(mvt, MvtLayerParams.DEFAULT, new UserDataKeyValueMapConverter());
    }

    /**
     * Encode a {@link JtsMvt} to byte[] ready for writing to a file.
     *
     * @param mvt input to encode to bytes
     * @param mvtLayerParams tile creation parameters
     * @param userDataConverter converts {@link Geometry#userData} to MVT feature tags
     * @return bytes ready for writing to a .mvt
     */
    public static byte[] encode(JtsMvt mvt, MvtLayerParams mvtLayerParams, IUserDataConverter userDataConverter) {

        // Build MVT
        final VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();

        for(JtsLayer layer : mvt.getLayers()) {
            final Collection<Geometry> layerGeoms = layer.getGeometries();

            // Create MVT layer
            final VectorTile.Tile.Layer.Builder layerBuilder =
                    MvtLayerBuild.newLayerBuilder(layer.getName(), mvtLayerParams);
            final MvtLayerProps layerProps = new MvtLayerProps();

            // MVT tile geometry to MVT features
            final List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(
                    layerGeoms, layerProps, userDataConverter);
            layerBuilder.addAllFeatures(features);
            MvtLayerBuild.writeProps(layerBuilder, layerProps);

            // Build MVT layer
            final VectorTile.Tile.Layer vtl = layerBuilder.build();
            tileBuilder.addLayers(vtl);
        }

        // Build MVT
        return tileBuilder.build().toByteArray();
    }
}
