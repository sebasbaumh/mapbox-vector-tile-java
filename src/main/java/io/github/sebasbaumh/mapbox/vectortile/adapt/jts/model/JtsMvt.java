package io.github.sebasbaumh.mapbox.vectortile.adapt.jts.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.DefaultLocation;
import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * JTS model of a Mapbox Vector Tile.
 */
@NonNullByDefault({ DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE })
public class JtsMvt
{
	/**
	 * Map layers by name.
	 */
	private final Map<String, JtsLayer> layersByName;

	/**
	 * Create an empty MVT.
	 */
	public JtsMvt()
	{
		this(Collections.emptyList());
	}

	/**
	 * Create a MVT with the provided layers.
	 * @param layers multiple MVT layers
	 */
	public JtsMvt(Collection<JtsLayer> layers)
	{

		// Linked hash map to preserve ordering
		layersByName = new LinkedHashMap<>(layers.size());

		for (JtsLayer nextLayer : layers)
		{
			layersByName.put(nextLayer.getName(), nextLayer);
		}
	}

	/**
	 * Create MVT with single layer.
	 * @param layer single MVT layer
	 */
	public JtsMvt(JtsLayer layer)
	{
		this(Collections.singletonList(layer));
	}

	/**
	 * Create MVT with the provided layers.
	 * @param layers multiple MVT layers
	 */
	public JtsMvt(JtsLayer... layers)
	{
		this(new ArrayList<>(Arrays.asList(layers)));
	}

	@Override
	public boolean equals(@Nullable Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JtsMvt jtsMvt = (JtsMvt) o;
		return layersByName.equals(jtsMvt.layersByName);
	}

	/**
	 * Get the layer by the given name.
	 * @param name layer name
	 * @return layer with matching name, or null if none exists
	 */
	public JtsLayer getLayer(String name)
	{
		return layersByName.get(name);
	}

	/**
	 * Get get all layers within the vector tile.
	 * @return insertion-ordered collection of layers
	 */
	public Collection<JtsLayer> getLayers()
	{
		return layersByName.values();
	}

	/**
	 * Get all layers within the vector tile mapped by name.
	 * @return mapping of layer name to layer
	 */
	public Map<String, JtsLayer> getLayersByName()
	{
		return layersByName;
	}

	@Override
	public int hashCode()
	{
		return layersByName.hashCode();
	}

	@Override
	public String toString()
	{
		return "JtsMvt{" + "layersByName=" + layersByName + '}';
	}
}
