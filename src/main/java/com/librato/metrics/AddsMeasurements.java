package com.librato.metrics;

/**
 * Something that accepts measurements. Used to test that certain measurements are generated correctly.
 */
interface AddsMeasurements {

    void addMeasurement(Measurement measurement);

}
