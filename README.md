# MapBox Vector Tile - Java

[![CodeQL](https://github.com/sebasbaumh/mapbox-vector-tile-java/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/sebasbaumh/mapbox-vector-tile-java/actions/workflows/codeql-analysis.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sebasbaumh/mapbox-vector-tile-java)](https://search.maven.org/artifact/io.github.sebasbaumh/mapbox-vector-tile-java)
[![javadoc](https://javadoc.io/badge2/io.github.sebasbaumh/mapbox-vector-tile-java/javadoc.svg)](https://javadoc.io/doc/io.github.sebasbaumh/mapbox-vector-tile-java/latest/index.html)
[![License](https://img.shields.io/github/license/sebasbaumh/mapbox-vector-tile-java.svg)](https://github.com/sebasbaumh/mapbox-vector-tile-java/blob/master/LICENSE)

[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=sebasbaumh_mapbox-vector-tile-java&metric=ncloc)](https://sonarcloud.io/dashboard?id=sebasbaumh_mapbox-vector-tile-java)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=sebasbaumh_mapbox-vector-tile-java&metric=security_rating)](https://sonarcloud.io/dashboard?id=sebasbaumh_mapbox-vector-tile-java)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=sebasbaumh_mapbox-vector-tile-java&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=sebasbaumh_mapbox-vector-tile-java)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=sebasbaumh_mapbox-vector-tile-java&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=sebasbaumh_mapbox-vector-tile-java)

This project allows encoding and decoding of MapBox Vector Tiles (MVT).  
*It is originally based on [mapbox-vector-tile-java](https://github.com/wdtinc/mapbox-vector-tile-java), which is unfortunately [discontinued](https://github.com/wdtinc/mapbox-vector-tile-java/issues/45#issuecomment-1126996294) and I want to thank its authors here for their work.*

## Project goals and improvements:
* protoc generated model for Mapbox Vector Tiles v2.1.
* Provides MVT encoding through use of the Java Topology Suite (JTS).
* All dependencies were upgraded to their latest version (including JTS)
* Pull requests from the original source were integrated ([52](https://github.com/wdtinc/mapbox-vector-tile-java/pull/52) and [53](https://github.com/wdtinc/mapbox-vector-tile-java/pull/53))
* Clean up of the code and optimizations (use null annotations and streamline flow)
* Support for JDK 11+
* The license is still [Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

### See also:

 * https://www.mapbox.com/vector-tiles/specification/ for overview of the MVT spec.
 * https://github.com/mapbox/vector-tile-spec/tree/master/2.1 for details on the MVT spec.
 * https://developers.google.com/protocol-buffers/ for details on protoc.
 * https://projects.eclipse.org/projects/locationtech.jts for details on JTS.


## How to use it ##
### Maven
There is a Maven artifact in the official Maven repository, so just add this to your Maven POM:

```xml
<dependency>
	<groupId>io.github.sebasbaumh</groupId>
	<artifactId>mapbox-vector-tile-java</artifactId>
	<version>22.2.0</version>
</dependency>
```

### Gradle

```
compile 'io.github.sebasbaumh:mapbox-vector-tile-java:22.2.0'
```

## Overview

The version reflects the year of the release, e.g. `22.0.0` is a version released in 2022.

### Changes from to the original [mapbox-vector-tile-java](https://github.com/wdtinc/mapbox-vector-tile-java)

The API differs a bit from [mapbox-vector-tile-java](https://github.com/wdtinc/mapbox-vector-tile-java) with the main point being a different namespace (`io.github.sebasbaumh.mapbox.vectortile`) as publishing a project to Maven Central requires to own that namespace.

Especially [`JtsAdapter`](https://github.com/sebasbaumh/mapbox-vector-tile-java/blob/main/src/main/java/io/github/sebasbaumh/mapbox/vectortile/adapt/jts/JtsAdapter.java) has been reworked and optimized. Usually you can just move from `addAllFeatures`/`toFeatures` to `addFeatures` instead.

There are also some changes in the class structure, so make sure check your existing code for errors or deprecation warnings. For converters and filters it is now possible to use `null` values to use none/ignore them.

## Usage

### Encoding vector tiles

Example for encoding a JTS geometry to a vector tile as `byte[]`:
```java
// prepare helper classes
GeometryFactory geomFactory = new GeometryFactory();
MvtLayerProps mvtLayerProps = new MvtLayerProps();
MvtLayerParams mvtLayerParams = new MvtLayerParams();
VectorTile.Tile.Layer.Builder mvtLayerBuilder = MvtUtil.newLayerBuilder("test", mvtLayerParams);

// build tile envelope - 1 quadrant of the world
Envelope tileEnvelope = new Envelope(0d, WORLD_SIZE * .5d, 0d, WORLD_SIZE * .5d);

// this is the geometry
org.locationtech.jts.geom.Point point = geomFactory.createPoint(new Coordinate(12,45));
// encode the geometry
org.locationtech.jts.geom.Geometry tileGeom = JtsAdapter.createTileGeom(               point, tileEnvelope, geomFactory, mvtLayerParams, null);
// add it to the layer builder
mvtLayerBuilder.addAllFeatures(JtsAdapter.toFeatures(tileGeom, mvtLayerProps, null));

// finish writing of features
MvtUtil.writeProps(mvtLayerBuilder, mvtLayerProps);
VectorTile.Tile.Builder mvtBuilder = VectorTile.Tile.newBuilder();
mvtBuilder.addLayers(mvtLayerBuilder.build());
// build the vector tile (here as byte array)
byte[] mvtData = mvtBuilder.build().toByteArray();
```

---
*The following content is based on the original code in [mapbox-vector-tile-java](https://github.com/wdtinc/mapbox-vector-tile-java) and may not be fully up to date. It will be reworked step by step.*

Contents

- [Overview](#overview)
    - [Reading MVTs](#reading-mvts)
    - [Building and Writing MVTs](#building-and-writing-mvts)
    - [Buffering Polygons Beyond MVT Extent](#buffering-polygons-beyond-mvt-extent)
- [Examples](#examples)
- [Generate VectorTile class using .proto](#how-to-generate-vectortile-class-using-vector_tile.proto)
- [Known Issues](#known-issues)

## Overview

### Reading MVTs

Per-tile geometry conversion overview:

![Image of Geometry Conversion Overview](docs/mvt_read_flow.png)

Use MvtReader.loadMvt() to load MVT data from a path or input stream
into JTS geometry. The TagKeyValueMapConverter instance will convert
MVT feature tags to a Map with primitive values. The map will be
stored as a JTS geometry user data object within the Geometry.

The JtsMvt object wraps the JTS Geometry with MVT layer information
and structure.

```java
GeometryFactory geomFactory = new GeometryFactory();

JtsMvt jtsMvt = MvtReader.loadMvt(
        Paths.get("path/to/your.mvt"),
        geomFactory,
        new TagKeyValueMapConverter());


// Allow negative-area exterior rings with classifier
// (recommended for Mapbox compatibility)
JtsMvt jtsMvt = MvtReader.loadMvt(
        Paths.get("path/to/your.mvt"),
        geomFactory,
        new TagKeyValueMapConverter(),
        MvtReader.RING_CLASSIFIER_V1);
```

### Building and Writing MVTs

Per-layer geometry conversion overview:

![Image of Geometry Conversion Overview](docs/mvt_build_flow.png)

#### 1) Create or Load JTS Geometry

Create or load any JTS Geometry that will be included in the MVT. The Geometries are assumed
to be in the global/world units for your target projection. Example: meters for EPSG:3857.

#### 2) Create Tiled JTS Geometry in MVT Coordinates

Create tiled JTS geometry with JtsAdapter#createTileGeom(). MVTs currently
do not support feature collections so any JTS geometry collections will be flattened
to a single level. A TileGeomResult will contain the world/global intersection
geometry from clipping as well as the actual MVT geometry that uses
tile extent coordinates. The intersection geometry can be used for hierarchical
processing, while the extent geometry is intended to be encoded as the tile geometry.
Keep in mind that MVTs use local 'screen coordinates' with inverted y-axis compared with cartesian.

```java
IGeometryFilter acceptAllGeomFilter = geometry -> true;
Envelope tileEnvelope = new Envelope(0d, 100d, 0d, 100d); // TODO: Your tile extent here
MvtLayerParams layerParams = new MvtLayerParams(); // Default extent

TileGeomResult tileGeom = JtsAdapter.createTileGeom(
        jtsGeom, // Your geometry
        tileEnvelope,
        geomFactory,
        layerParams,
        acceptAllGeomFilter);
```

JavaDoc for JtsAdapter.createTileGeom()

```java
/**
 * Create geometry clipped and then converted to MVT 'extent' coordinates. Result
 * contains both clipped geometry (intersection) and transformed geometry for encoding to MVT.
 *
 * <p>Uses the same tile and clipping coordinates. May cause rendering issues on boundaries for polygons
 * or line geometry depending on styling.</p>
 *
 * @param g original 'source' geometry
 * @param tileEnvelope world coordinate bounds for tile
 * @param geomFactory creates a geometry for the tile envelope
 * @param mvtLayerParams specifies vector tile properties
 * @param filter geometry values that fail filter after transforms are removed
 * @return tile geometry result
 * @see TileGeomResult
 */
public static TileGeomResult createTileGeom(Geometry g,
                                            Envelope tileEnvelope,
                                            GeometryFactory geomFactory,
                                            MvtLayerParams mvtLayerParams,
                                            IGeometryFilter filter)
```


#### 3) Create MVT Builder, Layers, and Features

After creating a tile's geometry in step 2, it is ready to be encoded in a MVT protobuf.

Note: Applications can perform step 2 multiple times to place geometry in separate MVT layers.

Create the VectorTile.Tile.Builder responsible for the MVT protobuf
byte array. This is the top-level object for writing the MVT:

```java
VectorTile.Tile.Builder tileBuilder = VectorTile.Tile.newBuilder();
```

Create an empty layer for the MVT using the MvtLayerBuild#newLayerBuilder() utility function:

```java
VectorTile.Tile.Layer.Builder layerBuilder = MvtLayerBuild.newLayerBuilder("myLayerName", layerParams);
```

MVT JTS Geometry from step 2 need to be converted to MVT features.

MvtLayerProps is a supporting class for building MVT layer
key/value dictionaries. A user data converter will take JTS Geometry
user data (preserved during MVT tile geometry conversion) and convert it to MVT tags:

```java
MvtLayerProps layerProps = new MvtLayerProps();
IUserDataConverter userDataConverter = new UserDataKeyValueMapConverter();
List<VectorTile.Tile.Feature> features = JtsAdapter.toFeatures(tileGeom.mvtGeoms, layerProps, userDataConverter);
```

Use MvtLayerBuild#writeProps() utility function after JtsAdapter#toFeatures() to add the key/value dictionary to the
MVT layer:

```java
MvtLayerBuild.writeProps(layerBuilder, layerProps);
```

#### 4) Write MVT

This example writes the MVT protobuf byte array to an output file.

```java
VectorTile.Tile mvt = tileBuilder.build();
try {
    Files.write(path, mvt.toByteArray());
} catch (IOException e) {
    logger.error(e.getMessage(), e);
}
```

### Buffering Polygons Beyond MVT Extent

For polygon geometry that will be styled with outlines, it is recommended that
the clipping area be larger than the tile extent area. This can be handled like
the example in MvtBuildTest#testBufferedPolygon(). Code example:

```java
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
final TileGeomResult bufferedTileGeom = JtsAdapter.createTileGeom(
        JtsAdapter.flatFeatureList(inputGeom),
        tileEnvelope, clipEnvelope, geomFactory,
        DEFAULT_MVT_PARAMS, ACCEPT_ALL_FILTER);

// Create MVT layer
final VectorTile.Tile mvt = encodeMvt(DEFAULT_MVT_PARAMS, bufferedTileGeom);
```

## Examples

See [tests](https://github.com/sebasbaumh/mapbox-vector-tile-java/tree/main/src/test/java/io/github/sebasbaumh/mapbox/vectortile).

## How to generate VectorTile class using vector_tile.proto

If vector_tile.proto is changed in the specification, VectorTile may need to be regenerated.

Command `protoc` version should be the same version as the POM.xml dependency.

protoc --java_out=src/main/java src/main/resources/vector_tile.proto

#### Extra .proto config

These options were added to the .proto file:

 * syntax = "proto2";
 * option java_package = "io.github.sebasbaumh.mapbox.vectortile";
 * option java_outer_classname = "VectorTile";

## Known Issues

 * Creating tile geometry with non-simple line strings that self-cross in many places will be 'noded' by JTS during an intersection operation. This results in ugly output.
 * Invalid or non-simple geometry may not work correctly with JTS operations when creating tile geometry.
