package be.valuya.backfeed.service;

import be.valuya.backfeed.domain.config.BackFeedConfig;
import lombok.RequiredArgsConstructor;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;

@RequiredArgsConstructor
public class BuzzService {

    private final AudioService audioService = new AudioService();
    private final BackFeedConfig backFeedConfig;

    private SourceDataLine line;
    private boolean terminated;

    public void start() {
        try {
            AudioFormat format = audioService.getAudioFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = audioService.getSourceDataLine(info);
            line.open(format, AudioService.SAMPLE_RATE);
            line.start();
            terminated = false;

            while (!terminated) {
                byte[] toneBuffer;
                int frequency = backFeedConfig.getTestToneFrequency();
                if (frequency > 0) {
                    int repetitions = 8; // clicks otherwise, to investigate (rounding errors?)
                    int durationMillis = 1000 / frequency * repetitions;
                    toneBuffer = createSinWaveBuffer(frequency, durationMillis);
                } else {
                    toneBuffer = createWhiteNoiseBuffer(200);
                }
                line.write(toneBuffer, 0, toneBuffer.length);
            }
        } catch (LineUnavailableException exception) {
            throw new RuntimeException(exception);
        } finally {
            finish();
        }
    }

    private void finish() {
        if (line != null) {
            line.drain();
            line.close();
        }
    }

    public void stop() {
        terminated = true;
    }


    public byte[] createSinWaveBuffer(double frequency, int durationMillis) {
        int bytesPerSample = AudioService.SAMPLE_SIZE_IN_BITS / 8;
        int sampleCount = durationMillis * AudioService.SAMPLE_RATE / 1000;
        int bufferSize = sampleCount * bytesPerSample;
        byte[] outputBuffer = new byte[bufferSize];

        double period = (double) AudioService.SAMPLE_RATE / frequency;
        for (int t = 0, offset = 0; t < sampleCount; t++, offset += bytesPerSample) {
            double angle = 2.0 * Math.PI * t / period;
            double sampleFloat = Math.sin(angle);
            audioService.fillSample(outputBuffer, offset, sampleFloat, bytesPerSample);
        }

        return outputBuffer;
    }

    public byte[] createWhiteNoiseBuffer(int durationMillis) {
        int bytesPerSample = AudioService.SAMPLE_SIZE_IN_BITS / 8;
        int sampleCount = durationMillis * AudioService.SAMPLE_RATE / 1000;
        int bufferSize = sampleCount * bytesPerSample;
        byte[] outputBuffer = new byte[bufferSize];

        Random random = new Random();
        random.nextBytes(outputBuffer);

        return outputBuffer;
    }
}
