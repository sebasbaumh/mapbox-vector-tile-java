package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;

/**
 * Processes a user data object, converts to MVT feature tags.
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public interface IUserDataConverter
{

	/**
	 * <p>
	 * Convert user data to MVT tags. The supplied user data may be null. Implementation should update layerProps and
	 * optionally set the feature id.
	 * </p>
	 * <p>
	 * SIDE EFFECT: The implementation may add tags to featureBuilder, modify layerProps, modify userData.
	 * </p>
	 * @param userData user object may contain values in any format; may be null
	 * @param layerProps properties global to the layer the feature belongs to
	 * @param featureBuilder may be modified to contain additional tags
	 */
	void addTags(@Nullable Object userData, MvtLayerProps layerProps, VectorTile.Tile.Feature.Builder featureBuilder);
}
