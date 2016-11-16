package com.librato.metrics.reporter;

public class Numbers {
    public static Number getNumberFrom(Object object) {
        if (object instanceof Number) {
            Number number = (Number)object;
            if (isANumber(number)) {
                return number;
            }
        }
        return null;
    }

    public static boolean isANumber(Number number) {
        final double doubleValue = number.doubleValue();
        return !(Double.isNaN(doubleValue) || Double.isInfinite(doubleValue));
    }

}
