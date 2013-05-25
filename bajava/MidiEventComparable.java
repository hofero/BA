package bajava;

import com.google.common.collect.ComparisonChain;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

public class MidiEventComparable extends MidiEvent implements Comparable<MidiEventComparable> {

    public short pitch;
    public long time;

    public MidiEventComparable(MidiEvent event) {
        super(event.getMessage(), event.getTick());
        MidiMessage message = event.getMessage();
        if (message instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) message;
            this.pitch = (short) sm.getData1();
            this.time = event.getTick();
        }
    }

    @Override
    public int compareTo(MidiEventComparable other) {
        return ComparisonChain.start()
                .compare(time, other.time)
                .compare(pitch, other.pitch)
                .result();
    }
}