
import java.util.List;
import java.util.stream.Collectors;

import java.awt.Point;

public class TerrainGeneration {
    
    /**
     * Precondition:  Assumes neighbors is non-empty
     * @param point the point in global coordinates
     */
    public static void fillEmptyPoint(final Point point, final WrappedBox wrappedBox, final List<Region> neighbors) {
        final Region selected = Util.randomElement(neighbors);
        
        final BoundingBox selectedBox = selected.getBoundingBox();

        // Neighbors in local coordinates
        final List<Point> neighborPoints = wrappedBox.getNeighbors(point)
            .stream()
            .filter(selected::containsGlobal)
            .map(neighbor -> 
                Vec.sum(
                    Vec.extend(wrappedBox.getUnwrapped(selectedBox, neighbor).get()),
                    selected.getPosition().negate()).truncate())
            .collect(Collectors.toList());

        final float averageThickness = Util.averageComputedValue(neighborPoints, neighbor -> {
            return selected.getChunkAt(neighbor.x, neighbor.y).get().getThickness().toKilometers();
        });

        if (true || averageThickness <= 1.0f) {
            final Chunk chunk = new Chunk();
            chunk.deposit(new Chunk.Layer(Chunk.RockType.BASALT, averageThickness * 0.9f));

            final Point bordering = wrappedBox.getNonWrappedDuplicates(point)
                .stream()
                .filter(duplicate -> selectedBox.nextTo(duplicate))
                .map(duplicate -> Vec.sum(Vec.extend(duplicate), selected.getPosition().negate()).truncate())
                .collect(Collectors.toList()).get(0);

            selected.setChunk(bordering.x, bordering.y, chunk);
        }
    }
}
