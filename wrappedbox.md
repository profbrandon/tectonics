
# Region

## Member Variables

* `public int mWidth`
* `public int mHeight`
* `public int mArea`

## Constructors

## Methods

### Utility

* `float distance(final Point p1, final Point p2)`
* `float distance(final Vec v1, final Vec v2)`
* `Point wrap(final Point point)`
* `Point wrapSum(final Point...ps)`
* `Vec wrap(final Vec vec)`
* `Optional<Point> getUnwrapped(final BoundingBox box, final Point point)`
* `Set<Point> getNeighbors(final Point point)`
* `Set<Point> get8Neighbors(final Point point)`
* `Set<Point> getCluster(final Point point, final int radius)`
* `Set<Point> getExclusiveNeighbors(final Collection<Point> points)`
* `Set<Point> getNonWrappedDuplicates(final Point point)`
* `Set<Vec> getNonWrappedDuplicates(final Vec vec)`

### Properties

* `outOfBounds(final Point point)`
* `areNeighbors(final Point point1, final Point point2)`
* `pointEquals(final Point point1, final Point point2)`
* `contains(final Collection<Point> points, final Point target)`
* `withinVerticalFrame(final int xMinFrame, final int frameWidth, final Point point)`
* `withinHorizontalFrame(final int yMinFrame, final int frameHeight, final Point point)`
* REWRITE: `withinBoundingBox(final BoundingBox box, final Point point)`
* REWRITE: `boundingBoxesOverlap(final BoundingBox box1, final BoundingBox box2)`

### Accessors

* `int getWidth()`
* `int getHeight()`
* `int getArea()`

## Ideas