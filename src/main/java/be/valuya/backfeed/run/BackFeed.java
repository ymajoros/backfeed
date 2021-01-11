package be.valuya.backfeed.run;

import be.valuya.backfeed.domain.config.BackFeedConfig;
import be.valuya.backfeed.service.BackFeedService;

public class BackFeed {

    public static void main(String[] args) throws InterruptedException {
        BackFeedConfig backFeedConfig = BackFeedConfig.builder()
                .testToneFrequency(-1)
                .build();
        BackFeedService backFeedService = new BackFeedService(backFeedConfig);
        backFeedService.start();
    }
}
