import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Graph<T, U> {

    private final Map<Integer, T> nodes;

    private final Map<Pair<Integer, Integer>, U> values;

    private final Set<Pair<Integer, Integer>> edges;

    public Graph(final List<T> ts) {
        nodes = new HashMap<>();
        values = new HashMap<>();
        edges = new HashSet<>();

        for (int i = 0; i < ts.size(); ++i) {
            nodes.put(i, ts.get(i));
        }
    }

    /**
     * @return the number of nodes present
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * @return the list of nodes
     */
    public List<T> getNodes() {
        return nodes.values().stream().collect(Collectors.toList());
    }

    /**
     * @return the collection of edges
     */
    public Collection<Pair<Integer, Integer>> getEdges() {
        return edges;
    }

    /**
     * @param target the target object
     * @return the node index of the target object (or -1 if it is not present)
     */
    public int getIndex(final T target) {
        for (int i = 0; i < nodes.size(); ++i) {
            if (target == nodes.get(i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param index the index of the node
     * @return the optional node at that index
     */
    public Optional<T> getNode(final int index) {
        final T node = nodes.get(index);
        if (node == null) return Optional.empty();
        else return Optional.of(node);
    }

    /**
     * Determines whether the graph contains the edge.
     * @param i the first node index
     * @param j the second node index
     * @return whether an edge exists between node i and node j
     */
    public boolean hasEdge(final int i, final int j) {
        return edges.contains(buildEdge(i, j));
    }

    /**
     * Inserts an edge into the graph. Note that this method will not insert
     * reflexive edges, i.e., i must not equal j to be inserted.
     * @param i the first node index
     * @param j the second node index
     * @return whether the edge was successfully inserted
     */
    public boolean addEdge(final int i, final int j, final U value) {
        if (i == j) {
            return false;
        }

        if (i < nodes.size() && j < nodes.size()) {
            final Pair<Integer, Integer> edge = buildEdge(i, j);
            edges.add(edge);
            values.put(edge, value);
            return true;
        }
        return false;
    }

    /**
     * Adds a node to this graph
     * @param target the node to insert
     */
    public void addNode(final T target) {
        nodes.put(nodes.size(), target);
    }

    /**
     * Method to compute the list of neighboring nodes to a specific node.
     * @param index the index of the node whose neighbors are to be retrieved
     * @return the list of neighbors
     */
    public List<Integer> getNeighbors(final int index) {
        return edges.parallelStream()
            .filter(pair -> pair.first == index || pair.second == index)
            .map(pair -> {
                if (pair.first == index) {
                    return pair.second;
                }
                else {
                    return pair.first;
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Retrieves the value associated with the edge between node indices i and j.
     * @param i the first index
     * @param j the second index
     * @return the optional value associated with the edge between i and j
     */
    public Optional<U> getEdgeValue(final int i, final int j) {
        final Pair<Integer, Integer> edge = buildEdge(i, j);
        final U value = values.get(edge);

        if (value == null) return Optional.empty();
        else return Optional.of(value);
    }

    /**
     * @param i the first index
     * @param j the second index
     * @return an edge between i and j
     */
    private Pair<Integer, Integer> buildEdge(final int i, final int j) {
        return new Pair<>(Math.min(i, j), Math.max(i, j));
    }
}
