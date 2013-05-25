package bajava;

import com.google.common.collect.ComparisonChain;

public class MIDIStreamEvent implements Comparable<MIDIStreamEvent> {

    private final short pitch;

    private final short volume;

    private final long tick;

    private final int stream_id;

    public short getPitch() {
        return pitch;
    }

    public short getVolume() {
        return volume;
    }

    public long getTick() {
        return tick;
    }

    public int getStream_id() {
        return stream_id;
    }

    public TYPE getType() {
        return type;
    }

    public static enum TYPE {
        NOTE,
        CONTROL
    }

    private final TYPE type;

    public MIDIStreamEvent(short pitch, short volume, long tick, int stream_id, TYPE type) {
        this.pitch = pitch;
        this.volume = volume;
        this.tick = tick;
        this.stream_id = stream_id;
        this.type = type;
    }

    @Override
    public String toString() {
        return getPitch() + " " + getVolume() + " " + getTick() + " " + getStream_id() + " " + getType();
    }

    @Override
    public int compareTo(MIDIStreamEvent other) {
        return ComparisonChain.start()
                .compare(getTick(), other.getTick())
                .compare(getPitch(), other.getPitch())
                .result();
    }
}






