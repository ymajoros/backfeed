package be.valuya.backfeed.service;

import be.valuya.backfeed.domain.config.BackFeedConfig;
import lombok.RequiredArgsConstructor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class NoiseDetectionService {

    private static final float RMS_THRESHOLD = 0.005f;
    private static final int BUFFER_SIZE = 2048;
    public static final int RMS_DISPLAY_SCALE = 100;

    private AudioService audioService = new AudioService();
    private final BackFeedConfig backFeedConfig;

    // the line from which audio data is captured
    private TargetDataLine line;
    private volatile boolean finished;
    private boolean noiseDetected;


    public void start() {
        try {
            AudioFormat format = audioService.getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = audioService.getTargetDataLine(info);
            line.open(format);

            System.out.println("Start capturing...");
            line.start();

            int bytesPerSample = AudioService.SAMPLE_SIZE_IN_BITS / AudioService.CHANNEL_COUNT / 8;

            byte[] buffer = new byte[BUFFER_SIZE];
            float[] samples = new float[BUFFER_SIZE / bytesPerSample];

            finished = false;
            noiseDetected = false;
            line.start();
            for (int b; !finished && (b = line.read(buffer, 0, BUFFER_SIZE)) > -1; ) {

                // convert bytes to samples here
                for (int i = 0, s = 0; i < b; ) {
                    byte b1 = buffer[i++];
                    byte b2 = buffer[i++];
                    samples[s++] = audioService.convertSampleBytesToFloat(b1, b2);
                }

                float rms = 0f;
                float peak = 0f;
                for (float sample : samples) {
                    float abs = Math.abs(sample);
                    peak = Math.max(abs, peak);
                    rms += sample * sample;
                }

                rms = (float) Math.sqrt(rms / samples.length);

                showRms(rms, peak);

                if (rms > RMS_THRESHOLD) {
                    if (!noiseDetected) {
                        System.out.println("Noise detected!");
                    }
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
        if (rawValue < 0.005) {
            return 0;
        }
        return (int) (Math.sqrt(rawValue) * RMS_DISPLAY_SCALE);
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
        System.out.println("Finished");
    }

    public void stop() {
        finished = true;
    }
}
