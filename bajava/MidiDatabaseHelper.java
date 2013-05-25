
package bajava;

import javax.sound.midi.InvalidMidiDataException;
import java.io.IOException;
import java.io.File;

public class MidiDatabaseHelper {

    private static String MIDI_DATA_ROOT = "";
    private static String INDEX_FILE_ROOT = "";

    public MidiDatabaseHelper(String midiRoot, String indexRoot) {
        MIDI_DATA_ROOT = midiRoot;
        INDEX_FILE_ROOT = indexRoot;
        System.out.println(MIDI_DATA_ROOT + INDEX_FILE_ROOT );
    }

    //Midi laden
    public StreamsContainer loadKDF(String path) throws IOException, InvalidMidiDataException {
        StreamsContainer out = loadMidiFile(MIDI_DATA_ROOT + "/kdf/" + path);
        out.setContentSource(StreamsContainer.ContentSource.MIDI);
        out.setTitle(path);
        return out;
    }

    public static StreamsContainer loadMidiFile(String path) throws IOException, InvalidMidiDataException {
        StreamsContainer sc = new StreamsContainer();
        File f = new File(path);
        sc.setTitle(f.getName());
        sc.addMidi(com.google.common.io.Files.toByteArray(f), f.getName(), 0);
        sc.recomputeLinearized();
        return sc;
    }

}
