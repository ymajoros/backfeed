package be.valuya.backfeed.domain.config;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BackFeedConfig {

    private int testToneFrequency;
    @Builder.Default()
    public int noiseProfileDurationMillis = 5_000;
    @Builder.Default()
    public int testToneDurationMillis = 60_000;
}
