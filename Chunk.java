
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.awt.Color;

public class Chunk {
    public static final Length WIDTH_IN_KM = Length.fromKilometers(1.0f);

    /**
     * Enumeration for the different classes of rock
     */
    public static enum RockClass {
        SEDIMENT,
        SEDIMENTARY,
        METAMORPHIC,
        IGNEOUS,
        MAGMA
    }

    /**
     * Enumeration for the types of rock available for a chunk
     */
    public static enum RockType {
        
        // MAGMA
        FELSIC(2400f, Float.POSITIVE_INFINITY, new Color(255, 128, 0), RockClass.MAGMA),
        MAFIC(2700f, Float.POSITIVE_INFINITY, new Color(255, 150, 0), RockClass.MAGMA),

        // IGNEOUS:
        RHYOLITE(2500f, 0f, new Color(250, 210, 160), RockClass.IGNEOUS),
        GRANITE(2650f, 0f, new Color(250, 175, 125), RockClass.IGNEOUS),
        BASALT(3000f, 0f, new Color(50, 50, 50), RockClass.IGNEOUS),
        GABBRO(3100f, 0f, new Color(50, 60, 40), RockClass.IGNEOUS),

        // METAMORPHIC:
        GNEISS(2800f, Float.POSITIVE_INFINITY, new Color(200, 170, 140), RockClass.METAMORPHIC),
        SCHIST(2900f, Float.POSITIVE_INFINITY, new Color(120, 130, 130), RockClass.METAMORPHIC),
        SLATE(2800f, Float.POSITIVE_INFINITY, new Color(60, 60, 50), RockClass.METAMORPHIC),
        QUARTZITE(2700f, Float.POSITIVE_INFINITY, new Color(255, 140, 100), RockClass.METAMORPHIC),
        METACONGLOMERATE(2700f, Float.POSITIVE_INFINITY, new Color(130, 115, 80), RockClass.METAMORPHIC),

        // SEDIMENTARY:
        SHALE(2300f, 0f, new Color(70, 70, 60), RockClass.SEDIMENTARY),
        SANDSTONE(2400f, 0f, new Color(240, 180, 100), RockClass.SEDIMENTARY),
        CONGLOMERATE(2400f, 0f, new Color(175, 160, 125), RockClass.SEDIMENTARY),

        // SEDIMENT:
        GRAVEL(1400f, 0f, new Color(115, 110, 100), RockClass.SEDIMENT),
        SAND(1500f, 0f, new Color(230, 200, 130), RockClass.SEDIMENT),
        CLAY(1600f, 0f, new Color(200, 100, 50), RockClass.SEDIMENT);


        /**
         * Density in kg m^-3
         */
        public final float mDensity;
        
        /**
         * The maximumm pressure it can handle before changing in kg m^2
         */
        public final float mMaxPressure;

        /**
         * Rock color
         */
        public final Color mColor;

        /**
         * The rock's class
         */
        public final RockClass mClass;

        /**
         * @param density The density in kg m^-3 of the rock type.
         */
        RockType(final float density, final float pressure, final Color color, final RockClass rockClass) {
            mDensity = density;
            mMaxPressure = pressure;
            mColor = color;
            mClass = rockClass;
        }
    
        /**
         * @return a random rock type
         */
        public static RockType randomRockType() {
            final RockType[] rocks = RockType.values();
            return rocks[(int) (Math.random() * rocks.length)];
        }

        /**
         * @param input the rock to melt
         * @return the magma produced via melting
         */
        public RockType melt(final RockType input) {
            switch(input) {
                case GABBRO:
                case BASALT:
                case SCHIST:
                    return MAFIC;

                default:
                    return FELSIC;
            }
        }
    
        /**
         * @param input the rock to get eroded elements from
         * @return the types of rock produced from erosion
         */
        public List<RockType> erode(final RockType input) {
            final List<RockType> outputs = new ArrayList<>();

            switch(input) {
                case MAFIC:
                case FELSIC:
                    break;

                case GRAVEL:
                    outputs.add(GRAVEL);
                    outputs.add(SAND);
                    break;

                case SAND:
                    outputs.add(SAND);
                case CLAY:
                    outputs.add(CLAY);
                    break;

                default:
                    outputs.add(GRAVEL);
            }

            return outputs;
        }

        /**
         * @param input the rock to transform
         * @return the transformed rock
         */
        public RockType transform(final RockType input) {
            switch(input) {
                case FELSIC:
                case MAFIC:
                    return null;

                case RHYOLITE:
                case GRANITE:
                    return GNEISS;

                case GABBRO:
                case BASALT:
                    return SCHIST;

                case SHALE:
                    return SLATE;

                case SANDSTONE:
                    return QUARTZITE;

                case CONGLOMERATE:
                    return METACONGLOMERATE;

                case CLAY:
                    return SHALE;

                case SAND:
                    return SANDSTONE;

                case GRAVEL:
                    return CONGLOMERATE;

                default:
                    return input;
            }
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
     * @return a list representation of the layers
     */
    public List<Layer> getLayers() {
        if (mLayers.isEmpty()) return new ArrayList<>();
        else return mLayers.stream().collect(Collectors.toList());
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
