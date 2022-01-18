package com.tectonics.util;

public class Length {

    public static final Length ZERO = new Length(0f);

    private static final float M_PER_KM = 1000f;
    private static final float M_PER_CM = 0.01f; 

    // Customary
    private static final float M_PER_MI = 1609.34f;
    private static final float M_PER_FT = 0.3048f;

    private final float mMeters;

    private Length(final float meters) {
        mMeters = meters;
    }


    public float toMeters() {
        return mMeters;
    }

    public float toKilometers() {
        return mMeters / M_PER_KM;
    }

    public float toCentimeters() {
        return mMeters / M_PER_CM;
    }

    public float toFeet() {
        return mMeters / M_PER_FT;
    }

    public float toMiles() {
        return mMeters / M_PER_MI;
    }



    public Length scale(final float factor) {
        return Length.fromMeters(mMeters * factor);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Length) {
            return mMeters == ((Length) obj).mMeters;
        }
        return false;
    }

    public boolean lessThan(final Length length) {
        return mMeters < length.mMeters;
    }

    public boolean lessThanEquals(final Length length) {
        return mMeters <= length.mMeters;
    }


    public static Length fromMeters(final float meters) {
        return new Length(meters);
    }

    public static Length fromCentimeters(final float cm) {
        return new Length(cm * M_PER_CM);
    }

    public static Length fromKilometers(final float km) {
        return new Length(km * M_PER_KM);
    }

    public static Length fromMiles(final float miles) {
        return new Length(miles * M_PER_MI);
    }

    public static Length fromFeet(final float feet) {
        return new Length(feet * M_PER_FT);
    }


    public static Length sum(final Length...ls) {
        float sum = 0.0f;

        for (final Length l : ls) {
            sum += l.toMeters();
        }

        return fromMeters(sum);
    }

    /**
     * Computes the product in m^ls.length
     * @param ls the lengths to multiply
     * @return the product
     */
    public static float multiply(final Length...ls) {
        float product = 1.0f;

        for (final Length l : ls) {
            product *= l.toMeters();
        }

        return product;
    }

}
