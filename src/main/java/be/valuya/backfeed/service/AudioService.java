package be.valuya.backfeed.service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioService {

    public static final int SAMPLE_RATE = 44100;
    public static final int SAMPLE_SIZE_IN_BITS = 16;
    public static final int CHANNEL_COUNT = 1;
    public static final boolean SIGNED = true;
    public static final boolean BIG_ENDIAN = true;

    private static final String MIXER_SUFFIX = "[default]";
    //    public static final String MIXER_SUFFIX = "[hw:1]";

    public AudioFormat getAudioFormat() {
        return new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNEL_COUNT, SIGNED, BIG_ENDIAN);
    }

    public TargetDataLine getTargetDataLine(DataLine.Info info) throws LineUnavailableException {
        Mixer mixer = getMixer(info);
        Line.Info[] targetLineInfo = mixer.getTargetLineInfo(info);
        return (TargetDataLine) mixer.getLine(targetLineInfo[0]);
    }

    public SourceDataLine getSourceDataLine(DataLine.Info info) throws LineUnavailableException {
        Mixer mixer = getMixer(info);
        Line.Info[] sourceLineInfo = mixer.getSourceLineInfo(info);
        return (SourceDataLine) mixer.getLine(sourceLineInfo[0]);
    }

    public Mixer getMixer(DataLine.Info info) {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            String name = mixerInfo.getName();
            String description = mixerInfo.getDescription();
            System.out.printf("%s - %s", name, description);
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (name.endsWith(MIXER_SUFFIX) && mixer.isLineSupported(info)) {
                System.out.println("... SELECTED");
                return mixer;
            }
            System.out.println();
        }

        throw new IllegalStateException("No appropriate mixer found.");
    }

    public float convertSampleBytesToFloat(byte b1, byte b2) {
        int sampleInt = b1 << 8 | (b2 & 0xFF);
        // normalize to range of +/-1.0f
        return sampleInt / 32768f;
    }

    public void fillSample(byte[] buffer, int offset, double sampleFloat, int bytesPerSample) {
        if (bytesPerSample != 2) {
            throw new IllegalArgumentException("Sample resolution not implemented");
        }
        short sampleShort = (short) (sampleFloat * 32767f);
        buffer[offset + 1] = (byte) sampleShort;
        buffer[offset] = (byte) (sampleShort >> 8);
    }
}
