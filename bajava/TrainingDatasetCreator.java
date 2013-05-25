
package bajava;

import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.common.primitives.Shorts;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class TrainingDatasetCreator {


    private static String MIDI_DATA_ROOT = "";
    private static String OUTPUT_PATH = "";

    private static Map<String, String> audioCQTPaths;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.exit(1);
        }
        MIDI_DATA_ROOT = args[0];
        OUTPUT_PATH = args[1];

        //Liste besserer Zuordnungen (Midi <--> Audio)
        String alignmentListPath = MIDI_DATA_ROOT + "/KDFalignmentQuality.json";
        Type listType = new TypeToken<ArrayList<QualityAlignmentFilter.AlignmentListItem>>() {
        }.getType();
        String listJson = null;

        //gesamte Liste in String umwandeln --> enthaelt alle Zuordnungen
        try {
            listJson = Files.toString(new File(alignmentListPath), Charset.defaultCharset());
            //listJson: [{"id0":"kdf^!!piano-rolls-collection!!^beethoven^beethoven_6080d_sonata_in_f_minor_57_2_3_(nc)smythe.mid","id1":"8gdUWdDVriI","minConfidence":0.5841439688715954},{"id0":"kdf^albeniz^albeniz_mallorca_(c)yogore.mid","id1":"KdSj7blufM8","minConfidence":0.6340314136125654},{"id0":"kdf^albeniz^albeniz_mallorca_(c)yogore.mid","id1":"uKHYNotBhSM","minConfidence":0.7841961852861036},{"id0":"kdf^albeniz^albeniz_mallorca_(c)yogore.mid","id1":"vYHSZVUCJqo","minConfidence":0.717}]
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //Liste deserialisieren und in alignmentList speichern --> enthaelt alle Zuordnungen
        List<QualityAlignmentFilter.AlignmentListItem> alignmentList = new Gson().fromJson(listJson, listType);
        //alignmentList:[[kdf^!!piano-rolls-collection!!^beethoven^beethoven_6080d_sonata_in_f_minor_57_2_3_(nc)smythe.mid, 8gdUWdDVriI, 0.5841439688715954], [kdf^albeniz^albeniz_mallorca_(c)yogore.mid, KdSj7blufM8, 0.6340314136125654], [kdf^albeniz^albeniz_mallorca_(c)yogore.mid, uKHYNotBhSM, 0.7841961852861036], [kdf^albeniz^albeniz_mallorca_(c)yogore.mid, vYHSZVUCJqo, 0.717]]

        //Pfade zu den gefundenen Spektrogrammen {(ID Spektrogramm = Pfad),...}
        audioCQTPaths = getAudioCQTPaths(MIDI_DATA_ROOT + "/cqt");
        //audioCQTPaths: {0GZXD2SuR2w=/home/olivia/Dokumente/Bachelorarbeit/Daten/Test/cqt/0GZXD2SuR2w.aac_44100-16040-335.dat.gz, 0RYhyrWsqv4=/home/olivia/Dokumente/Bachelorarbeit/Daten/Test/cqt/0RYhyrWsqv4.aac_44100-67980-335.dat.gz}

        Multimap<Long, Multiset<Long>> stats = HashMultimap.create();

        int prevStatsSize = 10000;
        int filterLevel = 3;

        for (QualityAlignmentFilter.AlignmentListItem item : alignmentList) {
            String mid = item.getId0();
            String ytId = item.getId1();
            //Pfad zu Zuordnungen (Midi <--> Spektrogramm)
            String alignmentJsonPath = MIDI_DATA_ROOT + "/alignments/" + mid + "_" + ytId + ".json";
            ///vier Pfade: home/olivia/Dokumente/Bachelorarbeit/Daten/Test/alignments/kdf^albeniz^albeniz_mallorca_(c)yogore.mid_KdSj7blufM8.json
            if (item.getMinConfidence() > 0.55) {
                System.out.println("processing " + alignmentJsonPath);
                String jsonString = null;
                try {
                    jsonString = Files.toString(new File(alignmentJsonPath), Charset.defaultCharset());
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                //Fuer jede Zuordnung ein globalAlignment erstellen
                GlobalAlignment globalAlignment = new Gson().fromJson(jsonString, GlobalAlignment.class);
                //[1 local alignments, 1022 points, minConfidence ]
                Multimap<Long, Multiset<Long>> localStats = getStatsForGlobalAlignment(globalAlignment);

                //Zu seltene Noten entfernen
                localStats = filter(localStats, 3);
                //localStats zu stats hinzu
                accumulateStats(stats, localStats);

                if (stats.size() > prevStatsSize * 2) {
                    try {
                        filterLevel += 1;
                        stats = filter(stats, filterLevel);
                        String outName = OUTPUT_PATH + "/audioMidiStats-" + stats.size() + ".obj";
                        System.out.println("storing stats in " + outName);
                        Stopwatch sw = new Stopwatch();
                        sw.start();
                        saveStats(stats, outName);
                        System.out.println("done in " + sw);
                        sw.stop();
                        prevStatsSize = stats.size();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        try {
            Stopwatch sw = new Stopwatch();
            sw.start();
            stats = filter(stats, 3);
            System.out.println(sw);
            sw.reset();
            sw.start();
            saveStats(stats, OUTPUT_PATH + "/audioMidiStats.obj");
            System.out.println(sw);
            sw.reset();
            sw.start();
            sw.stop();
            System.out.println(sw);
        } catch (IOException e) {
            e.printStackTrace();
        }

        printStats(stats);
    }

    private static void accumulateStats(Multimap<Long, Multiset<Long>> stats, Multimap<Long, Multiset<Long>> localStats) {
        for (long peakSignature : localStats.keySet()) {
            if (!stats.containsKey(peakSignature)) {
                stats.putAll(peakSignature, localStats.get(peakSignature));
            } else {
                for (Multiset<Long> activeNoteSignatures : localStats.get(peakSignature)) {
                    assert (activeNoteSignatures.size() == 1);

                    Long activeNoteSignature = activeNoteSignatures.iterator().next();
                    int count = activeNoteSignatures.count(activeNoteSignature);

                    boolean got = false;
                    for (Multiset<Long> msl : stats.get(peakSignature)) {
                        if (msl.contains(activeNoteSignature)) {
                            msl.add(activeNoteSignature, count);
                            got = true;
                        }
                    }
                    if (!got) {
                        Multiset<Long> ms = TreeMultiset.create();
                        ms.add(activeNoteSignature, count);
                        stats.put(peakSignature, ms);
                    }
                }
            }
        }
    }

    private static Multimap<Long, Multiset<Long>> filter(Multimap<Long, Multiset<Long>> stats, int minCount) {
        Multimap<Long, Multiset<Long>> out = HashMultimap.create(stats);
        for (long l : stats.keySet()) {
            Collection<Multiset<Long>> coll = stats.get(l);
            for (Multiset<Long> m : coll) {
                if (m.size() < minCount) out.remove(l, m);
            }
        }
        return out;
    }

    public static void saveStats(Multimap<Long, Multiset<Long>> stats, String path) throws IOException {
        FileOutputStream fos = new FileOutputStream(path);
        GZIPOutputStream gz = new GZIPOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(gz);
        oos.writeObject(stats);
        oos.flush();
        oos.close();
    }


    public static Multimap<Long, Multiset<Long>> getStatsForGlobalAlignment(GlobalAlignment globalAlignment) {

        Multimap<Long, Multiset<Long>> stats = HashMultimap.create();

        //Midi Datei: kdf^albeniz^albeniz_mallorca_(c)yogore.mid
        String uri0 = globalAlignment.getUri0();
        //Id Audio : KdSj7blufM8
        String uri1 = globalAlignment.getUri1();

        //audioCQTPaths.put("UYVT_WdDSLI", MIDI_DATA_ROOT + "/cqt/UYVT_WdDSLI.aac_44100-54308-335.dat.gz");
        //audioCQTPaths.put("UYVT_WdDSLI", OUTPUT_PATH + "/small_44100-1500-335.dat.gz");

        //Pfad zu Audio Datei
        String audioDataPath = audioCQTPaths.get(uri1);

        if (audioDataPath == null) return stats;

        //Abtastrate Spektrogramme
        int sampleRate = getSampleRate(audioDataPath);
        //44100

        //Bytewerte des Spektrogramms
        double[][] audioData = {};
        try {
            audioData = ComputeCQT.load(audioDataPath);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("problem loading " + audioDataPath);
            e.printStackTrace();
            return stats;
        }

        MidiDatabaseHelper mdh = new MidiDatabaseHelper(MIDI_DATA_ROOT, OUTPUT_PATH);
        StreamsContainer sc0 = new StreamsContainer();
        try {
            String realUri0 = uri0.substring(4).replace("^", "/");
            //Midi Datei laden und in StreamsContainer speichern
            sc0 = mdh.loadKDF(realUri0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //sc0.size() = pitches.size() [Ton]
        System.out.println(audioData.length + " " + sc0.size());

        //Die TimeMaps des globalAlignment durchlaufen und in Abschnitte (30 lang) unterteilen --> in Map umwandeln
        //Zuerst in ga: {...localtimeMaps:[[timeMap--> wird zu Key][timeMap--> wird zu Value],...]}
        //Key = Zeit, Value = ...
        int matchLength = 30;
        Map<double[], double[]> ranges = getRanges(globalAlignment, matchLength);


        //Intervallen aus ranges.keySet()(bleibt Key) die gespielten Noten (value) zuordnen --> Noten
        Map<double[], int[]> notes = new HashMap<double[], int[]>();
        for (double[] r : ranges.keySet()) {
            Range ra = Range.closed(r[0], r[r.length - 1]);
            int[] linearized = sc0.getLinearized(ra);
            notes.put(r, linearized);
        }

        //Intervallen aus ranges.values() (wird key ) Daten aus Spektrogramm (value) zuordnen --> Frames
        Map<double[], double[][]> transforms = new HashMap<double[], double[][]>();
        for (double[] r : ranges.values()) {
            int start = (int) (r[0] * sampleRate / 256);
            int stop = (int) (r[r.length - 1] * sampleRate / 256);
            if (start >= 0 && start < audioData.length && stop > start && stop < audioData.length) {
                double[][] d = Arrays.copyOfRange(audioData, start, stop);
                transforms.put(r, d);
            }
        }


        CommonSubsequenceFinder cs = new CommonSubsequenceFinder();
        cs.setPenalty(0.2);
        cs.setMatchingPolicy(CommonSubsequenceFinder.MatchingPolicy.UPPER_SEMITONE);

        List<TrainingInstance> pairs = new ArrayList<TrainingInstance>();

        for (double[] rLeft : ranges.keySet()) {
            double[] rRight = ranges.get(rLeft);

            //Noten und Frames
            int[] nts = notes.get(rLeft);
            double[][] transform = transforms.get(rRight);

            if (transform == null) continue;

            //Frames verkleinern
            double[][] f = PolyphonicPitchEstimator.removeLoudBackground(transform);

            //Trainingsinstanz anlegen
            TrainingInstance ti = new TrainingInstance(nts, transform);
            pairs.add(ti);

            //MIDI daten holen
            StreamsContainer sc00 = sc0.getMIDISubsequenceFromTimeRange(
                    Range.closed(rLeft[0], rLeft[rLeft.length - 1])
            );
            //times Array Zahlen verkleinern
            sc00.trimTime();
            //
            sc00.computeOnsetsOffsets();

            double transformStep = 1d / ComputeCQT.TARGET_FRAME_RATE * 256;

            //Frames durchlaufen
            int frame = 0;
            for (int n = 0; n < rRight.length - 1; n++) {
                double start = rRight[n];
                double stop = rRight[n + 1];
                for (double t = start; t < stop; t += transformStep) {
                    //Klingende Noten aus Frame herausfinden und in activeNoteSignature kodiert abspeichern
                    double tLeft = rLeft[n] + (t - start) * (rLeft[n + 1] - rLeft[n]) / (rRight[n + 1] - rRight[n]);
                    Set<Integer> activeNotes = sc00.getActiveNotes(tLeft - rLeft[0]);
                    Long activeNoteSignature = NgramCoder.pack(activeNotes.toArray(new Integer[]{}));
                    //System.out.println("notes at " + (tLeft - rLeft[0]) + ": " + activeNotes + " "+ activeNoteSignature);
                    if (frame < transform.length) {
                        double[] frameData = f[frame];
                        //selbtenste peaks auswaehlen und sortieren
                        List<Integer> peaks = sortPeaks(frameData);
                        //Peaks als long kodiert in peakSignature speichern
                        //zu stats (Keyset) hinzufuegen falls noch nicht vorhanden
                        //aktiveNoteSignature zu dem passenden Valueset hinzu
                        for (int k = 0; k < 10; k++) {
                            long peakSignature = NgramCoder.pack(peaks.subList(0, k).toArray(new Integer[]{}));
                            if (!stats.containsKey(peakSignature)) {
                                Multiset<Long> ms = TreeMultiset.create();
                                ms.add(activeNoteSignature);
                                stats.put(peakSignature, ms);
                            } else {
                                boolean got = false;
                                for (Multiset<Long> msl : stats.get(peakSignature)) {
                                    if (msl.contains(activeNoteSignature)) {
                                        msl.add(activeNoteSignature);
                                        got = true;
                                    }
                                }
                                if (!got) {
                                    Multiset<Long> ms = TreeMultiset.create();
                                    ms.add(activeNoteSignature);
                                    stats.put(peakSignature, ms);
                                }
                            }
                        }

                        //System.out.println("peaks: " + peaks);
                    } else {
                        //System.out.println("frame " + frame + " out of " + transform.length);
                    }

                    frame++;
                }
            }

        }

        System.out.println("created " + pairs.size() + " training pairs");


        return stats;
    }

    private static int getSampleRate(String audioDataPath) {
        int p = audioDataPath.lastIndexOf("_") + 1;
        return Integer.parseInt(audioDataPath.substring(p, p + 5));
    }

    //Pfade zu allen Spektrogrammen in Ordner root
    private static Map<String, String> getAudioCQTPaths(String root) {
        Map<String, String> out = new HashMap<String, String>();
        for (File f : new File(root).listFiles()) {
            if (f.getName().length() < 11 || !f.getName().endsWith("dat.gz")) continue;
            out.put(f.getName().substring(0, 11), f.getAbsolutePath());
        }
        return out;
    }

    private static void printStats(Multimap<Long, Multiset<Long>> stats) {
        for (long l : Sets.newTreeSet(stats.keySet())) {
            List<Short> s = Shorts.asList(NgramCoder.unpack(l));
            boolean show = s.size() < 10;
            if (show) {
                String d = toString(stats.get(l));
                if (!d.isEmpty()) {
                    System.out.println(s + ": " + d);                   
                }
            }
        }
    }

    private static String toString(Collection<Multiset<Long>> chordCounts) {
        StringBuilder sb = new StringBuilder();
        for (Multiset<Long> ml : chordCounts) {
            for (Long l : Multisets.copyHighestCountFirst(ml).elementSet()) {
                List<Short> li = Shorts.asList(NgramCoder.unpack(l));
                if (ml.count(l) < 10) continue;
                sb.append("[").append(li).append(" x ").append(ml.count(l)).append("], ");
            }
        }
        sb.delete(Math.max(0, sb.length() - 2), sb.length());
        return sb.toString();
    }

    public static List<Integer> sortPeaks(double[] frameData) {
        List<Integer> peaks;
        //Ordnet Peaks nach Haeufigkeit --> map < peak,indices>
        //In frameData 90x-127.0(haeufigstes) --> dann map[0]=127.0 und map.values() von 0 bis 90 die Stellen an denen 127.0 steht
        Multimap<Double, Integer> map = TreeMultimap.create();
        for (int i = 0; i < frameData.length; ++i) {
            map.put(frameData[i], i);
        }
        Collection<Integer> indices = map.values();
        //30 seltenste auswaehlen und normalisieren
        peaks = Lists.reverse(Lists.newArrayList(indices).subList(indices.size() - 30, indices.size()));
        for (int i = 0; i < peaks.size(); i++) {
            peaks.set(i, (peaks.get(i) / 4 + 40));
        }
        return peaks;
    }

    //TimeMaps durchlaufen und jede timeMap in kleinere Abschnitte --> in Map umwandeln: timeMap[0] wird zu key, timeMap[1] zu value
    public static Map<double[], double[]> getRanges(GlobalAlignment ga, final int matchLength) {

        Map<double[], double[]> ranges = new HashMap<double[], double[]>();
        List<double[][]> l = ga.getLocalTimeMaps();
        for (double[][] timeMap : l) {
            double[] left = timeMap[0];
            double[] right = timeMap[1];
            for (int i = 0; i < left.length; i = i + matchLength) {
                double l0 = left[i];
                double l1 = left[Math.min(i + matchLength, left.length - 1)];
                double r0 = right[i];
                double r1 = right[Math.min(i + matchLength, right.length - 1)];
                double[] key = Arrays.copyOfRange(left, i, Math.min(i + matchLength, left.length - 1));
                double[] val = Arrays.copyOfRange(right, i, Math.min(i + matchLength, right.length - 1));
                if (key.length > 0 && val.length > 0)
                    ranges.put(key, val);
            }
        }
        return ranges;
    }

}