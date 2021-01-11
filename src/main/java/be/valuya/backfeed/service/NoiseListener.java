package be.valuya.backfeed.service;

@FunctionalInterface
public interface NoiseListener {

    void handleNoiseDetected(float level);
}
