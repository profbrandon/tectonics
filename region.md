
# Region

## Static Enumerations

## Member Variables

* `int mWidth`
* `int mHeight`
* `Vec mPosition`
* `Vec mVelocity`
* `List<List<Optional<Chunk>>> mChunks`
* `float[][] mHeightMap`

## Constructors



## Methods

### Utility

* `Point toLocal(final Point global)`
* `Point toGlobal(final Point local)`

### Properties

* `isMinimumSize()`
* `isRoughlyConvex()`
* `contains(final int x, final int y)`
* `contains(final Point local)`
* `containsGlobal(final int x, final int y)`
* `containsGlobal(final Point global)`
* `neighbors(final Point local)`
* `neighborsGlobal(final Point global)`
* `onBoundary(final int x, final int y)`
* `onBoundary(final Point local)`


### Accessors

* `int getWidth()`
* `int getHeight()`
* `Vec getPosition()`
* `Vec getVelocity()`
* `Vec getCentroid()`
* `Boolean[][] toBooleanArray()`
* `Optional<Chunk> getChunkAt(final int x, final int y)`
* `Optional<Chunk> getChunkAt(final Point local)`
* `float getDepthAt(final int x, final int y)`
* `float getDepthAt(final Point local)`
* `float getElevationAt(final int x, final int y)`
* `float getElevationAt(final Point local)`
* `Pair<Float, Float> getElevationRange()`
* `List<Point> getPoints()`
* `List<Point> getGlobalPoints()`
* `List<Point> getBoundary()`
* `List<Point> getGlobalBoundary()`
* TODO: `List<Point> getNeighbors()`
* TODO: `List<Point> getGlobalNeighbors()`
* `BoundingBox getBoundingBox()`

### Modifiers

* `void setChunk(final int x, final int y, final Chunk chunk)`
* `void setChunk(final Point local, final Chunk chunk)`
* `void removeChunk(final int x, final int y)`
* `void removeChunk(final Point local)`
* `void setDepthAt(final int x, final int y, final float height)`
* `void setDepthAt(final Point local, final float height)`
* `void setPosition(final Vec position)`
* `void setVelocity(final Vec velocity)`
* `void lift(final float dz)`
* REWRITE: `void resize(final int x0, final int y0, final int x1, final int y0)`
* `float reEvaluateHeightMap(final float mantleDensity)`
* `Point refit()`
* `List<Region> partition()`
* `List<Region> divide()`


## Static Methods

* `Region buildRegion(final List<Pair<Point, Chunk>> chunkPairs, final Vec position)`
* `Optional<Pair<Boolean[][], Point> importRegionShapeFromPNG(final String filename)`
* `boolean exportRegionShapeToPNG(final String filePath, final Region region)`



## Ideas

* Replace `resize()` and `refit()` calls with `buildRegion()`. This would need an additional method `void overwrite(final Region region)`. This would also need an additional method `List<Pair<Point, Chunk>> getChunkPairs()`.

* Memoize calls to methods like `getCentroid()`. This would require a boolean `mStateChanged`. Maybe there should be multiple flags for different types of modification (e.g. `lift()` doesn't modify the output of `getCentroid()`)