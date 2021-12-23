
import java.util.Collection;
import java.util.Stack;

public class Chunk {
    public static final Length WIDTH_IN_KM = Length.fromKilometers(0.5f);

    /**
     * Enumeration for the types of rock available for a chunk
     */
    public static enum RockType {
        SEDIMENT(1500f),
        SEDIMENTARY(2400f),
        METAMORPHIC(3000f),
        IGNEOUS(2900f);

        /**
         * Density in kg m^-3
         */
        public final float mDensity;

        /**
         * @param density The density in kg m^-3 of the rock type.
         */
        RockType(final float density) {
            mDensity = density;
        }
    
        /**
         * @return a random rock type
         */
        public static RockType randomRockType() {
            final RockType[] rocks = RockType.values();
            return rocks[(int) (Math.random() * rocks.length)];
        }
    }

    /**
     * Class to represent a layer of rock
     */
    public static class Layer {
        public final RockType mRockType;
        public final Length mThickness;

        /**
         * Builds a layer with the specified rock type and thickness in meters
         * @param rockType the rock type for this layer
         * @param thicknessMeters the thickness in meters
         */
        public Layer(final RockType rockType, final float thicknessMeters) {
            mRockType = rockType;
            mThickness = Length.fromMeters(thicknessMeters);
        }

        /**
         * @return the thickness in meters
         */
        public Length getThickness() {
            return mThickness;
        }

        /**
         * @param layers the layers to compute the total thickness of
         * @return the total thickness in meters
         */
        public static Length totalThickness(final Collection<Layer> layers) {
            float totalMeters = 0.0f;

            for (final Layer layer : layers) {
                totalMeters += layer.getThickness().toMeters();
            }

            return Length.fromMeters(totalMeters);
        }
    }

    /**
     * The layers
     */
    private Stack<Layer> mLayers = new Stack<Layer>();

    /**
     * @param depositionLayer the layer to deposit
     */
    public void deposit(final Layer depositionLayer) {
        this.mLayers.push(depositionLayer);
    }

    /**
     * @return the type of rock present at the top of the chunk
     */
    public RockType getTopRockType() {
        return mLayers.peek().mRockType;
    }

    /**
     * @return The vertical thickness of the chunk
     */
    public Length getThickness() {
        return Layer.totalThickness(mLayers);
    }

    /**
     * @return the mass of the chunk in kg
     */
    public float getMass() {
        float totalMass = 0.0f;

        for (Layer layer : mLayers) {
            float volume = Length.multiply(layer.getThickness(), WIDTH_IN_KM, WIDTH_IN_KM);
            totalMass += layer.mRockType.mDensity * volume;
        }

        return totalMass;
    }

    /**
     * @return the density of the chunk in kg m^-3
     */
    public float getDensity() {
        final float mass = getMass();
        final float totalVolume = Length.multiply(getThickness(), WIDTH_IN_KM, WIDTH_IN_KM);

        return mass / totalVolume;
    }


    /**
     * @param chunk the chunk to determine the depth it sinks
     * @param mantleDensity the mantle density in kg m^-3
     * @return how deep the chunk is sunk below the "top" of the mantle
     */
    public static Length depthSunk(final Chunk chunk, final float mantleDensity) {
        return Length.fromMeters(chunk.getThickness().toMeters() * chunk.getDensity() / mantleDensity);
    }
}
