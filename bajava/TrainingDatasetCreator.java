package bajava;

import com.google.common.base.Stopwatch;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;

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

        List<TrainingInstance> pairs = new ArrayList<TrainingInstance>();

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
                //Fuer jede Zuordnung ein globalAlignment erstellen (Deserialisierung)
                GlobalAlignment globalAlignment = new Gson().fromJson(jsonString, GlobalAlignment.class);
                //[1 local alignments, 1022 points, minConfidence ]
                System.out.print(globalAlignment.toString());

                //globalAlignment in kleinere Abschnitte (=Trainingspaar) mit (Spektrogramm + Label) zerlegen
                List<TrainingInstance> localPairs = getTrainingInstanceList(globalAlignment);
                //alle TrainingPairs in einem Array speichern
                accumulatePairs(pairs, localPairs);

                if (pairs.size() > prevStatsSize * 2) {
                    try {
                        filterLevel += 1;
                        Stopwatch sw = new Stopwatch();
                        sw.start();
                        System.out.println("done in " + sw);
                        sw.stop();
                        prevStatsSize = pairs.size();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        try {
            Stopwatch sw = new Stopwatch();
            sw.start();
            System.out.println(sw);
            sw.reset();
            sw.start();
            saveTrainingInstance(pairs, OUTPUT_PATH);
            System.out.println(sw);
            sw.reset();
            sw.start();
            sw.stop();
            System.out.println(sw);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Alle Paare in einem Array speichern
    private static void accumulatePairs(List<TrainingInstance> pairs, List<TrainingInstance> localPairs) {
        for (TrainingInstance ti : localPairs) {
            pairs.add(ti);
        }
    }

    //Jedes Trainingspaar in Json Datei speichern
    public static void saveTrainingInstance(List<TrainingInstance> pairs, String path) throws IOException {

        Gson gson = new Gson();
        Type tiType = new TypeToken<TrainingInstance>() {
        }.getType();
        //Neuen Dateinamen in filenames.txt (Auflistung aller Dateien) speichern
        FileWriter fw = new FileWriter(path + "/filenames.txt");

        for (TrainingInstance pair : pairs) {
            try {
                String p = pair.getUri() + "_" + pair.getStarttime() + "-" + pair.getEndtime() + ".json";
                fw.write("out/" + p + "\n");

                //JsonDatei Anlegen
                FileWriter writer = new FileWriter(path + "/out/" + p);
                gson.toJson(pair, tiType, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        fw.close();

    }

    //Die GlobalAlignments (MIDI<-->Spektrogramm) in kleinere Abschnitte Zerlegen und jeden Abschnitt als Trainingspaar abspeichern
    public static List<TrainingInstance> getTrainingInstanceList(GlobalAlignment globalAlignment) {

        List<TrainingInstance> pairs = new ArrayList<TrainingInstance>();
        Multimap<Long, Multiset<Long>> stats = HashMultimap.create();

        //Midi Datei: kdf^albeniz^albeniz_mallorca_(c)yogore.mid
        String uri0 = globalAlignment.getUri0();
        //Id Audio : KdSj7blufM8 (Spektrogramm)
        String uri1 = globalAlignment.getUri1();

        //audioCQTPaths.put("UYVT_WdDSLI", MIDI_DATA_ROOT + "/cqt/UYVT_WdDSLI.aac_44100-54308-335.dat.gz");
        //audioCQTPaths.put("UYVT_WdDSLI", OUTPUT_PATH + "/small_44100-1500-335.dat.gz");

        //Audio Datei: Pfad zu Spektrogramm durch Id herausfinden
        String audioDataPath = audioCQTPaths.get(uri1);

        if (audioDataPath == null) {
            return pairs;
        }

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
            return pairs;
        }

        //Midi Datei laden und Steuerinformationen in StreamsContainer speichern
        MidiDatabaseHelper mdh = new MidiDatabaseHelper(MIDI_DATA_ROOT, OUTPUT_PATH);
        StreamsContainer sc0 = new StreamsContainer();
        ImmutableMultimap<Double, Long> midiparser = null;
        try {
            String realUri0 = uri0.substring(4).replace("^", "/");
            sc0 = mdh.loadKDF(realUri0);
            File f = new File(MIDI_DATA_ROOT + "/kdf/"+realUri0);
            //Herausfinden welche Akkorde zu welchem Zeitpunkt (Akkord,Zeitpunkt) klingen und in midiparser speichern
            midiparser = MIDIParser.parseChords(Files.toByteArray(f));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //sc0.size() = pitches.size() [Ton]
        System.out.println(audioData.length + " " + sc0.size());

        //Die TimeMaps des globalAlignment durchlaufen und in Abschnitte (30 lang) unterteilen --> in Map umwandeln
        //Zuerst in ga: {...localtimeMaps:[[timeMap[0]--> wird zu Key][timeMap[1]--> wird zu Value],...]}
        //Key = ZeitMIDI, Value = ZeitSpektrogramm
        int matchLength = 10;
        Map<double[], double[]> ranges = getRanges(globalAlignment, matchLength);


        //Intervallen aus ranges.keySet()(bleibt Key) die gespielten Noten (value) zuordnen --> Noten
//        Map<double[], int[]> notes = new HashMap<double[], int[]>();
//        for (double[] r : ranges.keySet()) {
//            Range ra = Range.closed(r[0], r[r.length - 1]);
//            int[] linearized = sc0.getLinearized(ra);
//            notes.put(r, linearized);
//        }

        //Intervallen aus ranges.keySet()(=ZeitMIDI,bleibt Key) MIDI-Daten aus MidiParser (value) zuordnen --> Akkorde
        Double[] midiparserDoubleArray = midiparser.keys().toArray(new Double[midiparser.size()]);
        TDoubleArrayList midiparserArrayList = new TDoubleArrayList();
        for (int i = 0; i< midiparserDoubleArray.length; i++){
            midiparserArrayList.add(midiparserDoubleArray[i]);
        }
        Long[] midiparserLongArray = midiparser.values().toArray(new Long[midiparser.size()]);
        long[] mpla = new long[midiparserLongArray.length];
        for (int i = 0; i< midiparserLongArray.length; i++){
            mpla[i]= midiparserLongArray[i];
        }
        Map<double[], long[]> chords = new HashMap<double[], long[]>();
        for(double[] r: ranges.keySet()){
            Range ra = Range.closed(r[0], r[r.length -1]);
            int startIdx =  midiparserArrayList.binarySearch((Double) ra.lowerEndpoint());
            int stopIdx =   midiparserArrayList.binarySearch((Double) ra.upperEndpoint());
            long[] chordsInRange = Arrays.copyOfRange(mpla,startIdx,stopIdx);
            chords.put(r, chordsInRange);
        }


        //Intervallen aus ranges.values() (=ZeitSpektrogramm, wird key ) Daten aus Spektrogramm (value) zuordnen --> Frames
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

        //Zuordnungen ( Zeit MIDI <--> Zeit Spektrogramm  ) durchlaufen und für jeweilige Zeit (Akkorde, Spektrogramm) als Trainingspaar abspeichern
        for (double[] rLeft : ranges.keySet()) {
            double[] rRight = ranges.get(rLeft);

            //Noten und Frames
//            int[] nts = notes.get(rLeft);
//            if (nts.length == 0){
//                continue;
//            }

            //Akkorde in dem Zeitintervall
            long[] chrds = chords.get(rLeft);
            if(chrds.length ==0){
                continue;
            }


            //Spektrogramm in dem Zeitintervall --> Positionen der Top 10 hoechsten Werte
            double[][] transform = transforms.get(rRight);
            if (transform == null) {
                continue;
            }
            for (int i=0; i<transform.length; i++){
                double[] peaks = sortPeaks(transform[i]);
                 transform[i]= peaks;
            }

            //Frames verkleinern
            double[][] f = PolyphonicPitchEstimator.removeLoudBackground(transform);

            //Start- und Endzeit des Abschnitts herausfinden
            StreamsContainer sc00 = sc0.getMIDISubsequenceFromTimeRange(Range.closed(rLeft[0], rLeft[rLeft.length -1]));

            double starttime = sc00.getLinearizedTimes()[0];
            double endtime = sc00.getLinearizedTimes()[sc00.getLinearizedTimes().length-1];

            //Trainingsinstanz anlegen
            TrainingInstance ti = new TrainingInstance(chrds, transform, uri0, starttime, endtime);
            pairs.add(ti);

            double transformStep = 1d / ComputeCQT.TARGET_FRAME_RATE * 256;

        }

        System.out.println("created " + pairs.size() + " training pairs");

        return pairs;
    }

    //Abtastrate
    private static int getSampleRate(String audioDataPath) {
        int p = audioDataPath.lastIndexOf("_") + 1;
        return Integer.parseInt(audioDataPath.substring(p, p + 5));
    }

    //Pfade zu allen Spektrogrammen in Ordner root
    private static Map<String, String> getAudioCQTPaths(String root) {
        Map<String, String> out = new HashMap<String, String>();
        for (File f : new File(root).listFiles()) {
            if (f.getName().length() < 11 || !f.getName().endsWith("dat.gz")) {
                continue;
            }
            out.put(f.getName().substring(0, 11), f.getAbsolutePath());
        }
        return out;
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
                if (key.length > 0 && val.length > 0) {
                    ranges.put(key, val);
                }
            }
        }
        return ranges;
    }

    //top 10 hoechste Werte aus Spektrogramm absteigend sortieren und Position speichern
    public static double[] sortPeaks(double[] frameData) {
        List<Integer> peaks;
        //Ordnet Peaks aufsteigend nach Groesse --> map < peak,indices>
        Multimap<Double, Integer> map = TreeMultimap.create();
        for (int i = 0; i < frameData.length; ++i) {
            map.put(frameData[i], i);
        }
        Collection<Integer> indices = map.values();
        //n hoechste auswaehlen
        int n = 100;
        if(indices.size()> n){
            peaks = Lists.reverse(Lists.newArrayList(indices).subList(indices.size() - n, indices.size()));
        }
        else{
            peaks = Lists.reverse(Lists.newArrayList(indices));
        }
        double[] result = new double[peaks.size()];
        for (int i = 0; i < peaks.size(); i++) {
            result[i]= peaks.get(i);
        }

        return result;
    }
}
