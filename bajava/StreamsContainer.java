
package bajava;

import com.google.common.collect.*;


import com.sun.media.sound.MidiUtils;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

import javax.sound.midi.*;
import java.io.*;
import java.util.*;

public class StreamsContainer implements Serializable {

    private static final long serialVersionUID = 7728233125050386258L;
    final int NOTE_ON = 0x90;
    final int NOTE_OFF = 0x80;

    private final TShortArrayList pitches = new TShortArrayList();
    private final TShortArrayList volumes = new TShortArrayList();
    private final TLongArrayList ticks = new TLongArrayList();
    private final TDoubleArrayList times = new TDoubleArrayList();
    private final TIntArrayList streamIds = new TIntArrayList();
    private final TShortArrayList types = new TShortArrayList();
    private final TShortArrayList pedals = new TShortArrayList();

    private final TIntArrayList linearizedToOrig = new TIntArrayList();
    private final TIntArrayList linearizedList = new TIntArrayList();

    private final TIntIntHashMap streamOffsets = new TIntIntHashMap();
    private final TreeMap<Integer, String> streamTitles = Maps.newTreeMap();

    TreeMultimap<Integer, Double> onsets = null, offsets = null;

    public int[] linearized;

    private double prevLastTime = 0;
    private String title;

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static enum ContentSource {
        DEFAULT,
        OMR,
        MIDI,
        AUDIO
    }

    private ContentSource contentSource = ContentSource.DEFAULT;


    public void addMidi(byte[] rawMidi, String title, int streamId) throws InvalidMidiDataException, IOException {
        InputStream is = new ByteArrayInputStream(rawMidi);
        Sequence sequence = MidiSystem.getSequence(is);
        addSequence(streamId, title, sequence);
    }

    public void addSequence(int streamId, String title, Sequence sequence) {

        int n = pitches.size();
        int prevSize = n;

        if (streamOffsets.containsKey(streamId)) {
            throw new IllegalArgumentException("already added a stream with this id");
        }
        streamOffsets.put(streamId, n);
        streamTitles.put(streamId, title);

        Multimap<Long, MidiEventComparable> m = TreeMultimap.create();
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEventComparable event = new MidiEventComparable(track.get(i));
                m.put(event.getTick(), event);
            }
        }

        double maxTime = 0;
        MidiUtils.TempoCache tempoCache = new MidiUtils.TempoCache();
        for (long t : Sets.newTreeSet(m.keySet())) {
            for (MidiEvent event : m.get(t)) {
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    int key = sm.getData1();
                    long tick = event.getTick();
                    double localTime = MidiUtils.tick2microsecond(sequence, tick, tempoCache) / 1000000f;
                    maxTime = Math.max(localTime, maxTime);
                    if (sm.getCommand() == NOTE_ON || (sm.getCommand() == NOTE_OFF && key > 0)) {
                        pitches.add((short) key);
                        ticks.add(tick);
                        times.add(localTime + prevLastTime);
                        streamIds.add(streamId);
                        types.add((short) 144);
                        pedals.add((short) 0);

                        if (sm.getCommand() == NOTE_ON) {
                            int velocity = sm.getData2();
                            volumes.add((short) velocity);
                            if (velocity > 0) {
                                linearizedList.add((short) key);
                                linearizedToOrig.add(n);
                            }
                        } else {
                            volumes.add((short) 0);
                        }

                        n++;
                    }
                }
            }
        }
        prevLastTime += maxTime;

        // in case no events were added and the stream was empty
        if (n == prevSize) {
            streamOffsets.remove(streamId);
            streamTitles.remove(streamId);
        }

    }


    public double[] getLinearizedTimes() {
        double[] out = new double[linearizedList.size()];
        int n = 0;
        for (int i : linearizedToOrig.toArray()) {
            out[n++] = times.get(i);
        }
        return out;
    }


    public StreamsContainer getMIDISubsequenceFromTimeRange(Range range) {
        return getMIDISubsequenceTimeRange(range, true);
    }

    private StreamsContainer getMIDISubsequenceTimeRange(Range range, boolean withinRange) {
        StreamsContainer out = new StreamsContainer();

        int n = 0;
        int streamIdPrev = 0;
        out.prevLastTime = prevLastTime;
        out.title = title;
        for (int o = 0; o < linearized.length; o++) {

            double time = times.get(linearizedToOrig.get(o));

            if ((!withinRange && range.contains(time)) || (withinRange && !range.contains(time))) continue;

            int first = linearizedToOrig.get(o);
            int last;
            if (o >= linearizedToOrig.size() - 1) {
                last = pitches.size();
            } else {
                last = linearizedToOrig.get(o + 1);
            }

            out.linearizedList.add(pitches.get(first));
            out.linearizedToOrig.add(n);

            for (int i = first; i < last; i++) {
                out.pitches.add(pitches.get(i));
                out.ticks.add(ticks.get(i));
                out.times.add(times.get(i));
                out.streamIds.add(streamIds.get(i));
                out.types.add(types.get(i));
                out.pedals.add(pedals.get(i));
                out.volumes.add(volumes.get(i));

                if (streamIds.get(i) != streamIdPrev || n == 0) {
                    out.streamOffsets.put(streamIds.get(i), streamOffsets.get(streamIds.get(i)));
                    out.streamTitles.put(streamIds.get(i), streamTitles.get(streamIds.get(i)));
                }
                streamIdPrev = streamIds.get(i);

                n++;
            }
        }
        out.recomputeLinearized();
        return out;
    }

    public StreamsContainer trimTime() {
        if (times.isEmpty()) return this;
        double startTime = times.get(0);
        for (int i = 0; i < times.size(); i++) {
            times.set(i, times.get(i) - startTime);
        }
        return this;
    }

    public int[] getLinearized(Range<Double> timeRange) {
        Range<Integer> r = getOffsets(timeRange);
        return Arrays.copyOfRange(linearized, r.lowerEndpoint(), r.upperEndpoint());
    }

    public Range<Integer> getOffsets(Range<Double> timeRange) {
        int startIdx = times.binarySearch(timeRange.lowerEndpoint());
        int stopIdx = times.binarySearch(timeRange.upperEndpoint());
        startIdx = startIdx < 0 ? -startIdx - 1 : startIdx;
        stopIdx = stopIdx < 0 ? -stopIdx - 1 : stopIdx;
        int startIdxLin = linearizedToOrig.binarySearch(startIdx);
        int stopIdxLin = linearizedToOrig.binarySearch(stopIdx);
        startIdxLin = startIdxLin < 0 ? -startIdxLin - 1 : startIdxLin;
        stopIdxLin = stopIdxLin < 0 ? -stopIdxLin - 1 : stopIdxLin;
        return Range.closed(startIdxLin, stopIdxLin);
    }

    public Set<Integer> getActiveNotes(double time) {

        if (onsets == null || offsets == null)
            throw new IllegalStateException("on- and offsets not computed yet");

        Set<Integer> out = new TreeSet<Integer>();
        for (int pitch : onsets.keySet()) {
            try {
                SortedSet<Double> prevOnsets = onsets.get(pitch).headSet(time + 0.001f);
                if (prevOnsets.isEmpty()) continue;
                double lastOnsetTime = prevOnsets.last();

                SortedSet<Double> followingOffsets = offsets.get(pitch).tailSet(lastOnsetTime);
                if (followingOffsets.isEmpty()) {
                    out.add(pitch);
                    continue;
                }
                double offsetTime = followingOffsets.first();
                if (offsetTime >= time) out.add(pitch);
            } catch (Exception e) {
                // no onset or offset for this pitch, bad data
                e.printStackTrace();
            }
        }
        return out;
    }

    public void recomputeLinearized() {
        linearized = linearizedList.toArray();

    }

    public int size() {
        return pitches.size();
    }

    public ContentSource getContentSource() {
        return contentSource;
    }

    public void setContentSource(ContentSource contentSource) {
        this.contentSource = contentSource;
    }

    public Map<Integer, Double> getStreamTimes() {
        Map<Integer, Double> out = Maps.newTreeMap();
        for (int i : streamOffsets.keys()) {
            double time = times.get(streamOffsets.get(i));
            out.put(i, time);
        }
        return out;
    }




}