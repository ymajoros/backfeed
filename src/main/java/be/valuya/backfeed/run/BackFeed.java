package be.valuya.backfeed.run;

import be.valuya.backfeed.domain.config.BackFeedConfig;
import be.valuya.backfeed.service.BuzzService;
import be.valuya.backfeed.service.NoiseDetectionService;

public class BackFeed {

    public static final int DURATION_MILLIS = 60_000;

    /**
     * Entry to run the program
     */
    public static void main(String[] args) throws InterruptedException {
        BackFeedConfig backFeedConfig = BackFeedConfig.builder()
                .testToneFrequency(200)
                .build();
        NoiseDetectionService noiseDetectionService = new NoiseDetectionService(backFeedConfig);
        BuzzService buzzService = new BuzzService(backFeedConfig);

        Thread audioRecordingThread = new Thread(noiseDetectionService::start);
        Thread buzzThread = new Thread(buzzService::start);

        audioRecordingThread.start();
        buzzThread.start();

        Thread.sleep(DURATION_MILLIS);
        noiseDetectionService.stop();
        buzzService.stop();
    }
}
