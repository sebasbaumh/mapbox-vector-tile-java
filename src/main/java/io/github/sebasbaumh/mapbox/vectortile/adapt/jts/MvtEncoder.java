package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.locationtech.jts.geom.Geometry;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsLayer;
import io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model.JtsMvt;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerParams;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Convenience class allows easy encoding of a {@link JtsMvt} to bytes.
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public final class MvtEncoder
{
	/**
	 * Encode a {@link JtsMvt} to byte[] ready for writing to a file.
	 * <p>
	 * Uses {@link MvtLayerParams#DEFAULT} and {@link UserDataKeyValueMapConverter} to transform the JtsMvt.
	 * </p>
	 * @param mvt input to encode to bytes
	 * @return bytes ready for writing to a .mvt
	 * @see #encode(JtsMvt, MvtLayerParams, IUserDataConverter)
	 */
	public static byte[] encode(JtsMvt mvt)
	{
		return encodeToTile(mvt, MvtLayerParams.DEFAULT, new UserDataKeyValueMapConverter()).toByteArray();
	}

	/**
	 * Encode a {@link JtsMvt} to byte[] ready for writing to a file.
	 * @param mvt input to encode to bytes
	 * @param mvtLayerParams tile creation parameters
	 * @param userDataConverter converts {@link Geometry#getUserData()} to MVT feature tags
	 * @return bytes ready for writing to a .mvt
	 */
	public static byte[] encode(JtsMvt mvt, MvtLayerParams mvtLayerParams, IUserDataConverter userDataConverter)
	{
		return encodeToTile(mvt, mvtLayerParams, userDataConverter).toByteArray();
	}

	/**
	 * Encode a {@link JtsMvt} to the given {@link OutputStream}.
	 * @param out {@link OutputStream}
	 * @param mvt input to encode to bytes
	 * @param mvtLayerParams tile creation parameters
	 * @param userDataConverter converts {@link Geometry#getUserData()} to MVT feature tags
	 * @throws IOException
	 */
	public static void encodeTo(OutputStream out, JtsMvt mvt, MvtLayerParams mvtLayerParams,
			IUserDataConverter userDataConverter) throws IOException
	{
		encodeToTile(mvt, mvtLayerParams, userDataConverter).writeTo(out);
	}

	/**
	 * Encode a {@link JtsMvt} to byte[] ready for writing to a file.
	 * @param mvt input to encode to bytes
	 * @param mvtLayerParams tile creation parameters
	 * @param userDataConverter converts {@link Geometry#getUserData()} to MVT feature tags
	 * @return {@link io.github.sebasbaumh.mapbox.vectortile.VectorTile.Tile}
	 */
	public static VectorTile.Tile encodeToTile(JtsMvt mvt, MvtLayerParams mvtLayerParams,
			IUserDataConverter userDataConverter)
	{
		// Build MVT containing all layers
		VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
		for (JtsLayer layer : mvt.getLayers())
		{
			// Create MVT layer
			VectorTile.Tile.Layer.Builder layerBuilder = MvtUtil.newLayerBuilder(layer.getName(), mvtLayerParams);
			MvtLayerProps layerProps = new MvtLayerProps();
			JtsAdapter.addFeatures(layerBuilder, layer.getGeometries(), layerProps, userDataConverter);
			MvtUtil.writeProps(layerBuilder, layerProps);

			// Build MVT layer
			tileBuilder.addLayers(layerBuilder.build());
		}
		// Build MVT
		return tileBuilder.build();
	}

}
