package be.valuya.backfeed.service;

import be.valuya.backfeed.domain.config.BackFeedConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BackFeedService {

    private final BackFeedConfig backFeedConfig;
    private BuzzService buzzService;
    private NoiseDetectionService noiseDetectionService;
    @Getter
    private volatile boolean noiseDetected;

    public void start() throws InterruptedException {
        try {
            noiseDetectionService = new NoiseDetectionService(backFeedConfig);
            buzzService = new BuzzService(backFeedConfig);

            Thread noiseDetectionThread = new Thread(noiseDetectionService::start);

            int noiseProfileDurationMillis = backFeedConfig.getNoiseProfileDurationMillis();
            noiseDetectionThread.start();
            System.out.println("Started profiling background noise...\n");
            Thread.sleep(noiseProfileDurationMillis);

            noiseDetectionService.addNoiseListener(this::handleNoiseDetected);
            noiseDetectionService.startDetection();
            float rmsThreshold = noiseDetectionService.getRmsThreshold();
            System.out.printf("Starting noise detection, level set to %f\n", rmsThreshold);

            int testToneDurationMillis = backFeedConfig.getTestToneDurationMillis();
            long startTime = System.currentTimeMillis();
            long endTime = startTime + testToneDurationMillis;
            Thread buzzThread = new Thread(buzzService::start);
            buzzThread.start();
            while (!noiseDetected && System.currentTimeMillis() < endTime) {
                Thread.sleep(100);
            }
            if (!noiseDetected) {
                System.out.println("Finished without detecting noise.");
            }
        } finally {
            stop();
        }
    }

    private void handleNoiseDetected(float peak) {
        if (!noiseDetected) {
            float rmsThreshold = noiseDetectionService.getRmsThreshold();
            float factor = peak / rmsThreshold;
            System.out.printf("Noise detected, level = %f > %f by a factor of %f; shutting down.\n", peak, rmsThreshold, factor);
        }
        this.noiseDetected = true;
    }

    private void stop() {
        noiseDetectionService.stop();
        buzzService.stop();
    }
}
