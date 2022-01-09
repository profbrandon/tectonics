
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
* `List<Pair<Point, Chunk>> getChunkPairs()`
* `List<Point> getPoints()`
* `List<Point> getGlobalPoints()`
* `List<Point> getBoundary()`
* `List<Point> getGlobalBoundary()`
* `List<Point> getNeighbors()`
* `List<Point> getGlobalNeighbors()`
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
* `void overwrite(final Region region)` - Should only be applied to regions with no reference
* `float reEvaluateHeightMap(final float mantleDensity)`
* `List<Region> partition()`
* `List<Region> divide()`
<br>
Note: Although `partition` and `divide` don't directly modify a region, the child regions they create will have the ability to modify the parent indirectly through the chunks.


## Static Methods

* `Region buildRegion(final List<Pair<Point, Chunk>> chunkPairs, final Vec position)`
* `Optional<Pair<Boolean[][], Point> importRegionShapeFromPNG(final String filename)`
* `boolean exportRegionShapeToPNG(final String filePath, final Region region)`



## Ideas

* Memoize calls to methods like `getCentroid()`. The architecture of this would require several new methods. For example, for calls that change which chunks are present, we can call `invalidateChunks()` and for calls that change the heighmap, we can call `invalidateHeightMap()`. The methods of return type `T` can then be memoized with private `Optional<T>` values.
    + The methods that can be memoized like this are:
        - `getCentroid`
        - `toBooleanArray`
        - `getElevationRange`
        - `getChunkPairs`
        - `getPoints`
        - `getGlobalPoints`
        - `getBoundary`
        - `getGlobalBoundary`
        - `getNeighbors`
        - `getGlobalNeighbors`
        - `getBoundingBox`
    <br>
    + And methods that cause invalidation are:
        - `setChunk`
        - `removeChunk`
        - `setDepthAt`
        - `overwrite`
        - `reEvalutateHeightMap`

    Note that the method `lift` invalidates `getElevationRange`, however, since it uniformly lifts the terrain, we can just recompute the elevation simply by adding the lift value to the previously computed elevation extrema.