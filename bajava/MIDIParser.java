package bajava;

import com.google.common.collect.*;
import com.sun.media.sound.MidiUtils;

import javax.sound.midi.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;


public class MIDIParser
{
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    private static double prevLastTime = 0;

    public static ImmutableMap<String, String> INDEX_PARSERS = new ImmutableMap.Builder<String, String>()
            .put("singleNoteAffine", "parseTrackwise")
            .put("singleNoteAbsolute", "parseTrackwise")
            .put("chordAffine", "parseChords")
            .put("chordAbsolute", "parseChords")
            .put("chordAffineTrackwise", "parseChordsTrackwise")
            .put("chordAbsoluteTrackwise", "parseChordsTrackwise")
            .put("chAfTrT", "parseChordsTrackwise")
            .put("chAbTrT", "parseChordsTrackwise")
            .put("pureRhythm", "parseChordsTrackwise")
            .build();


    //Herausfinden welche Akkorde in dem Zeitintervall zu welchem Zeitpunkt klingen und zu Label kodieren  --> in Map speichern (Label, Zeitpunkt)
    public static ImmutableMultimap<Double, Long> parseChords(byte[] rawmidi) throws Exception {

        InputStream is = new ByteArrayInputStream(rawmidi);
        Sequence sequence = MidiSystem.getSequence(is);

        //Onsets und Offsets in Map mit <Zeit,Note> speichern
        Multimap<Double, Integer> onEvents  = ArrayListMultimap.create();
        Multimap<Double, Integer> offEvents = ArrayListMultimap.create();

        // accumulating note on and off events over all voices in a common timeline
        MidiUtils.TempoCache tempoCache = new MidiUtils.TempoCache();
        double maxTime = 0;
        for (Track track : sequence.getTracks()) {
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                MidiMessage message = event.getMessage();
                if (message instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) message;
                    int key = sm.getData1();;
                    long time = event.getTick();
                    double localTime = MidiUtils.tick2microsecond(sequence, time, tempoCache) / 1000000f;
                    maxTime = Math.max(localTime, maxTime);
                    if (sm.getCommand() == NOTE_ON) {
                        int velocity = sm.getData2();
                        if (key > 0 && velocity > 0) {
                            onEvents.put(localTime+ prevLastTime, key);
                        }
                        else if (key > 0 && velocity == 0) {
                            offEvents.put(localTime+prevLastTime, key);
                        }
                    } else if (sm.getCommand() == NOTE_OFF && key > 0) {
                        offEvents.put(localTime+prevLastTime, key);
                    }
                }
            }
        }  prevLastTime += maxTime;


        // time points at which notes are turned on or off - chord boundaries
        Set<Double> intervalBounds = ImmutableSortedSet.copyOf(
                Sets.union(onEvents.keySet(), offEvents.keySet())
                        .immutableCopy());
        Multimap<Double, Integer> chords = TreeMultimap.create();
        Multimap<Double, Long> chordspacked = TreeMultimap.create();
        Multiset<Integer> activeNotes = HashMultiset.create();


        //Anhand von den Zeitintervallen, zu denen die Toene klingen Herausfinden welche Toene gleichzeitig klingen --> Zu Akkorden kodieren (nur wenn sie in Liste) und mit Zeit speichern
        double prevTick = 0;
        for (Double tick : intervalBounds) {
            if (prevTick == 0) {
                chords.putAll(tick, onEvents.get(tick));
            } else {
                ImmutableSet<Integer> prevChord = ImmutableSet.copyOf(chords.get(prevTick));
                ImmutableSet<Integer> restNotes = Sets.difference(prevChord, Sets.newHashSet(offEvents.get(tick))).immutableCopy();
                ImmutableSet<Integer> thisChord = Sets.union(restNotes, Sets.newHashSet(onEvents.get(tick))).immutableCopy();;
                chords.putAll(tick, thisChord);
            }
            prevTick = tick;
        }

        //Akkorde kodieren und abspeichern
        for(double d : chords.keySet()){
            long l = parseThisChord(chords.get(d));
            chordspacked.put(d,l);
        }
        for(double d: chordspacked.keySet()){
        }
        ImmutableMultimap<Double, Long> out = ImmutableMultimap.copyOf(chordspacked);
        return out;
    }

    //Prueft ob Akkord in Liste der bekannten Akkorde enthalten
    public static long checkChord(long chord){
        ArrayList<Long> chordsKnown = new ArrayList<Long>();
        String line;
        try {
            FileReader fr = new FileReader("/home/olivia/Dokumente/Bachelorarbeit/Daten/labels.txt");
            BufferedReader in = new BufferedReader(fr);
            try {
                for ( int i = 0; (line = in.readLine())!= null; i++)  {
                  chordsKnown.add(Long.parseLong(line));
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if (chordsKnown.contains(chord)){
            return chord;
        }
        else{
            return 0;
        }
    }

    //Akkord zu bekanntem Akkord kodieren
    public static long parseThisChord(Collection<Integer> chords){
        //Wenn Akkord bekannt zurueckgeben
        long l = NgramCoder.pack( chords.toArray(new Integer[chords.size()]));
        if(checkChord(l)!= 0){
            return l;
        }
        //Wenn nicht alle Akkorde mit einem Ton weniger ausprobieren
        else{
            for (int i = 1; i<= chords.size(); i++) {
                ArrayList<Integer> newchord = new ArrayList<Integer>(chords);
                newchord.remove(newchord.toArray()[chords.size() - i]) ;
                long l2 = NgramCoder.pack( newchord.toArray(new Integer[newchord.size()]));
                if(checkChord(l2)!= 0){
                    return l2;
                }
            }
        }
        //Wenn immer noch nicht Funktion mit einem Ton weniger aufrufen
        ArrayList<Integer> newchord = new ArrayList<Integer>(chords);
        newchord.remove(newchord.toArray()[chords.size() - 1]) ;
        long l3 = parseThisChord(newchord);
        if(l3 != 0){
            return l3;
        }
        return 0;
    }

    //Alle Files auflisten
    public static ArrayList<String> listDir(File dir){
            ArrayList<String> out = new ArrayList<String>();
            File[] files = dir.listFiles();
            if (files!= null){
                for(int i = 0;i<files.length;i++){
                    if(files[i].isDirectory()){
                        for(String s: listDir(files[i])){
                             out.add(s);
                        }
                    }
                    else{
                        out.add(files[i].getAbsolutePath());
                    }
                }
            }
            return out;
        }

    public static void main(String[] args) {

//        //Liste der Zuordnungen durchlaufen, alle Akkorde raussuchen und Kodiert in allchordspacked abspeichern (Datei)
//        //Liste besserer Zuordnungen (Midi <--> Audio)
//        String alignmentListPath = "/home/olivia/Dokumente/Bachelorarbeit/Daten/MIDI Daten/KDFalignmentQuality.json";
//        Type listType = new TypeToken<ArrayList<QualityAlignmentFilter.AlignmentListItem>>() {
//        }.getType();
//        String listJson = null;
//        //gesamte Liste in String umwandeln --> enthaelt alle Zuordnungen
//        try {
//            listJson = Files.toString(new File(alignmentListPath), Charset.defaultCharset());
//            //listJson: [{"id0":"kdf^!!piano-rolls-collection!!^beethoven^beethoven_6080d_sonata_in_f_minor_57_2_3_(nc)smythe.mid","id1":"8gdUWdDVriI","minConfidence":0.5841439688715954},{"id0":"kdf^albeniz^albeniz_mallorca_(c)yogore.mid","id1":"KdSj7blufM8","minConfidence":0.6340314136125654},{"id0":"kdf^albeniz^albeniz_mallorca_(c)yogore.mid","id1":"uKHYNotBhSM","minConfidence":0.7841961852861036},{"id0":"kdf^albeniz^albeniz_mallorca_(c)yogore.mid","id1":"vYHSZVUCJqo","minConfidence":0.717}]
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//        //Liste deserialisieren und in alignmentList speichern --> enthaelt alle Zuordnungen
//        List<QualityAlignmentFilter.AlignmentListItem> alignmentList = new Gson().fromJson(listJson, listType);
//        System.out.println(alignmentList.size());
//       //Akkorde Suchen und Kodiert abspeichern
//        ArrayList<Long> allchordspacked = new ArrayList<Long>();
//        int zahl= 1;
//        File dir = new File("/home/olivia/Dokumente/Bachelorarbeit/Daten/MIDI Daten/allchordspacked.txt");
//        ArrayList<String> files = listDir(dir);
//          //chords = (Zeit,Toene)
//        for(QualityAlignmentFilter.AlignmentListItem al: alignmentList){
//        File dir = new File("/home/olivia/Dokumente/Bachelorarbeit/Daten/Test/kdf/albeniz");
//        ArrayList<String> files = listDir(dir);
//        for(String s: files){
//            System.out.println(zahl);
//            String uri0 = al.getId0();
//            String realUri0 = uri0.substring(4).replace("^", "/");
//            File f = new File("/home/olivia/Dokumente/Bachelorarbeit/Daten/MIDI Daten/kdf/"+realUri0);
//            File f = new File(s);
//            try {
//                byte[] rawmidi =  Files.toByteArray(f);
//                InputStream is = new ByteArrayInputStream(rawmidi);
//                Sequence sequence = MidiSystem.getSequence(is);
//
//                //Onsets und Offsets in Map mit <Zeit,Note> speichern
//                Multimap<Double, Integer> onEvents  = ArrayListMultimap.create();
//                Multimap<Double, Integer> offEvents = ArrayListMultimap.create();
//
//
//                // accumulating note on and off events over all voices in a common timeline
//                MidiUtils.TempoCache tempoCache = new MidiUtils.TempoCache();
//                double maxTime = 0;
//                prevLastTime = 0;
//                for (Track track : sequence.getTracks()) {
//                    for (int i = 0; i < track.size(); i++) {
//                        MidiEvent event = track.get(i);
//                        MidiMessage message = event.getMessage();
//                        if (message instanceof ShortMessage) {
//                            ShortMessage sm = (ShortMessage) message;
//                            int key = sm.getData1();;
//                            long time = event.getTick();
//                            double localTime = MidiUtils.tick2microsecond(sequence, time, tempoCache) / 1000000f;
//                            maxTime = Math.max(localTime, maxTime);
//                            if (sm.getCommand() == NOTE_ON) {
//                                int velocity = sm.getData2();
//                                if (key > 0 && velocity > 0) {
//                                    onEvents.put(localTime+ prevLastTime, key);
//                                }
//                                else if (key > 0 && velocity == 0) {
//                                    offEvents.put(localTime+prevLastTime, key);
//                                }
//                            } else if (sm.getCommand() == NOTE_OFF && key > 0) {
//                                offEvents.put(localTime+prevLastTime, key);
//                            }
//                        }
//                    }
//
//
//
//                }  prevLastTime += maxTime;
//
//                //Ausrechnen welche Toene von gleichzeitig Klingen
//                Set<Double> intervalBounds = ImmutableSortedSet.copyOf(
//                        Sets.union(onEvents.keySet(), offEvents.keySet())
//                                .immutableCopy());
//                Multimap<Double, Integer> chords = TreeMultimap.create();
//                double prevTick = 0;
//                for (Double tick : intervalBounds) {
//                    if (prevTick == 0) {
//                        chords.putAll(tick, onEvents.get(tick));
//                    } else {
//                        ImmutableSet<Integer> prevChord = ImmutableSet.copyOf(chords.get(prevTick));
//                        ImmutableSet<Integer> restNotes = Sets.difference(prevChord, Sets.newHashSet(offEvents.get(tick))).immutableCopy();
//                        ImmutableSet<Integer> thisChord = Sets.union(restNotes, Sets.newHashSet(onEvents.get(tick))).immutableCopy();;
//                        chords.putAll(tick, thisChord);
//                    }
//                    prevTick = tick;
//                }
//                //Akkorde kodieren und abspeichern
//                Multimap<Double, Long> chordspacked = TreeMultimap.create();
//                for(double d : chords.keySet()){
//                    long l = NgramCoder.pack( chords.get(d).toArray(new Integer[chords.get(d).size()]));
//                    chordspacked.put(d,l);
//                    allchordspacked.add(l);
//                }
//                zahl++;
//
//            } catch (Exception e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//
//        //Alle Akkorde in Datei Abspeichern
//        FileWriter fw = null;
//        try {
//            fw = new FileWriter("/home/olivia/Dokumente/Bachelorarbeit/Daten/allchordspacked.txt", true);
//            BufferedWriter bufferedWriter = new BufferedWriter(fw);
////            for (int i = 0; i <1000; i++) {
//            for ( int i = 0; i< allchordspacked.size(); i++){
//                try {
//                    String p = String.valueOf(k[i]);
////                    String p = String.valueOf(allchordspacked.get(i));
//                    fw.append(p + "\n");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            bufferedWriter.close();
//            fw.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//        //Labels einlesen
//        String line;
//        ArrayList<Long> allchordspacked = new ArrayList<Long>();
//        try{
//            FileReader fread = new FileReader("/home/olivia/Dokumente/Bachelorarbeit/Daten/allchordspacked.txt");
//            BufferedReader in = new BufferedReader(fread);
//            for( int i = 0; (line = in.readLine())!=null; i++){
//                allchordspacked.add(Long.parseLong(line));
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//
//            //Akkorde zählen und in sorted Chords mit (codierter Akkord, häufigkeit) abspeichern
//            int count = 0;
//            //int durchlauf  = 0;
//            Multimap<Long,Integer> sortedChords = TreeMultimap.create() ;
//            for (Long i : allchordspacked) {
//                //durchlauf++;
//                //if((durchlauf % 1000) == 0) {
//                //    System.out.println(durchlauf);
//                //}
//                if (sortedChords.containsKey(i)){
//                    continue;
//                }
//                for(Long l : allchordspacked){
//                    if (i.equals(l)){
//                        count = count+1;
//                    }
//                }
//                sortedChords.put(i,count);
//                count = 0;
//            }
//        //inverted = (Häufigkeit, Liste der Akkorde die so oft vorkommen)
//        TreeMultimap<Integer, Long> inverted = Multimaps.invertFrom(sortedChords,TreeMultimap.<Integer, Long>create());
//
//        //1000 Häufigste Akkorde in Array speichern, mit häufigstem an erster Stelle
//        long[] k = new long[1000];
//        int iterator =1;
//        for(int i = 0; i<1000; i++){
//            k[i]= Long.parseLong(inverted.values().toArray()[inverted.values().size()-(i+1)].toString());
//        }
//        iterator = 1;
//        //Häufigste Akkorde in Datei schreiben
//        FileWriter fw = null;
//        try {
//            fw = new FileWriter("/home/olivia/Dokumente/Bachelorarbeit/Daten/labels.txt", true);
//            BufferedWriter bufferedWriter = new BufferedWriter(fw);
//            for (int i = 0; i <1000; i++) {
//                try {
//                    String p = String.valueOf(k[i]);
//                         fw.append(p + "\n");
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            bufferedWriter.close();
//            fw.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }

    }

}
