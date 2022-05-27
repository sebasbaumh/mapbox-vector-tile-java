package io.github.sebasbaumh.mapbox.vectortile.adapt.jts;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

import io.github.sebasbaumh.mapbox.vectortile.VectorTile;
import io.github.sebasbaumh.mapbox.vectortile.build.MvtLayerProps;

/**
 * Ignores user data, does not take any action.
 * @see IUserDataConverter
 * @deprecated use a <code>null</code> value for {@link IUserDataConverter} parameters instead
 */
@Deprecated
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class UserDataIgnoreConverter implements IUserDataConverter
{
	@Override
	public void addTags(Object userData, MvtLayerProps layerProps, VectorTile.Tile.Feature.Builder featureBuilder)
	{
	}
}
