package be.valuya.backfeed.service;

import be.valuya.backfeed.domain.config.BackFeedConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class NoiseDetectionService {

    private static final int BUFFER_SIZE = 2048;
    public static final int RMS_DISPLAY_SCALE = 100;

    private final AudioService audioService = new AudioService();
    private final BackFeedConfig backFeedConfig;

    // the line from which audio data is captured
    private TargetDataLine line;
    private volatile boolean finished;
    private volatile boolean detectionStarted;
    @Getter
    private volatile boolean noiseDetected;
    private List<NoiseListener> noiseListeners = new ArrayList<>();
    private volatile float maxPeak;
    @Getter
    private float rmsThreshold = -1f;


    public void start() {
        try {
            AudioFormat format = audioService.getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = audioService.getTargetDataLine(info);
            line.open(format);

            line.start();

            int bytesPerSample = AudioService.SAMPLE_SIZE_IN_BITS / AudioService.CHANNEL_COUNT / 8;

            byte[] buffer = new byte[BUFFER_SIZE];
            float[] samples = new float[BUFFER_SIZE / bytesPerSample];

            finished = false;
            noiseDetected = false;
            detectionStarted = false;
            line.start();
                maxPeak = 0f;
            for (int b; !finished && (b = line.read(buffer, 0, BUFFER_SIZE)) > -1; ) {

                // convert bytes to samples here
                for (int i = 0, s = 0; i < b; ) {
                    byte b1 = buffer[i++];
                    byte b2 = buffer[i++];
                    samples[s++] = audioService.convertSampleBytesToFloat(b1, b2);
                }

                float rmsSquareSum = 0f;
                float peak = 0f;
                for (float sample : samples) {
                    float abs = Math.abs(sample);
                    peak = Math.max(abs, peak);
                    maxPeak = Math.max(peak, maxPeak);
                    rmsSquareSum += sample * sample;
                }

                float rms = (float) Math.sqrt(rmsSquareSum / samples.length);

                showRms(rms, peak);

                if (detectionStarted && rms > rmsThreshold) {
                    float finalPeak = peak;
                    noiseListeners.forEach(noiseListener -> noiseListener.handleNoiseDetected(finalPeak));
                    noiseDetected = true;
                }
            }
        } catch (LineUnavailableException exception) {
            throw new RuntimeException(exception);
        } finally {
            finish();
        }
    }

    private void showRms(float rms, float peak) {
        int rmsLevel = scaleToDisplay(rms);
        String rmsStr = repeat("*", rmsLevel);

        float actualPeak = Math.max(rms, peak);
        int peakLevel = scaleToDisplay(actualPeak);
        int peakOffset = peakLevel - rmsLevel;
        String peakStr = repeat(" ", peakOffset);

        System.out.printf("%s%s|\r", rmsStr, peakStr);
    }

    private int scaleToDisplay(float rawValue) {
        return (int) (rawValue * RMS_DISPLAY_SCALE);
    }

    private String repeat(String str, int count) {
        return IntStream.range(0, count)
                .mapToObj(starIndex -> str)
                .collect(Collectors.joining());
    }

    /**
     * Closes the target data line to finish capturing and recording
     */
    private void finish() {
        line.stop();
        line.close();
    }

    public void stop() {
        finished = true;
    }

    public void startDetection() {
        detectionStarted = true;
        rmsThreshold = maxPeak;
    }

    public void addNoiseListener(NoiseListener noiseListener) {
        noiseListeners.add(noiseListener);
    }
}
