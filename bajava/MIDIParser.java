package bajava;

import com.google.common.collect.*;
import com.google.common.io.Files;
import com.sun.media.sound.MidiUtils;

import javax.sound.midi.*;
import java.io.*;
import java.util.ArrayList;
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



    public static ImmutableMultimap<Double, Long> parseChords(byte[] rawmidi) throws Exception {

        InputStream is = new ByteArrayInputStream(rawmidi);
        Sequence sequence = MidiSystem.getSequence(is);


        // time x pitch
//        Multimap<Long, Integer> onEvents  = ArrayListMultimap.create();
//        Multimap<Long, Integer> offEvents = ArrayListMultimap.create();

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
                            offEvents.put(localTime, key);
                        }
                    } else if (sm.getCommand() == NOTE_OFF && key > 0) {
                        offEvents.put(localTime, key);
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



        double prevTick = 0;
        for (Double tick : intervalBounds) {
            if (prevTick == 0) {
                chords.putAll(tick, onEvents.get(tick));
                chordspacked.put(tick,NgramCoder.pack( onEvents.get(tick).toArray(new Integer[onEvents.get(tick).size()]))) ;
            } else {
                ImmutableSet<Integer> prevChord = ImmutableSet.copyOf(chords.get(prevTick));
                ImmutableSet<Integer> restNotes = Sets.difference(prevChord, Sets.newHashSet(offEvents.get(tick))).immutableCopy();
                ImmutableSet<Integer> thisChord = Sets.union(restNotes, Sets.newHashSet(onEvents.get(tick))).immutableCopy();;

                long l = parseLabel( thisChord.toArray(new Integer[thisChord.size()]));
                if (l ==0){
                    l = parseLabel( prevChord.toArray(new Integer[prevChord.size()]));
                }
                chords.putAll(tick, thisChord);
                chordspacked.put(tick,l);

            }
            prevTick = tick;
        }

        ImmutableMultimap<Double, Long> out = ImmutableMultimap.copyOf(chordspacked);
        return out;
    }

    public static ArrayList<ArrayList<Long>> parseAllChords(byte[] rawmidi) throws Exception {

        InputStream is = new ByteArrayInputStream(rawmidi);
        Sequence sequence = MidiSystem.getSequence(is);


        // time x pitch
//        Multimap<Long, Integer> onEvents  = ArrayListMultimap.create();
//        Multimap<Long, Integer> offEvents = ArrayListMultimap.create();

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
                            offEvents.put(localTime, key);
                        }
                    } else if (sm.getCommand() == NOTE_OFF && key > 0) {
                        offEvents.put(localTime, key);
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
        ArrayList<Long> c1 = new ArrayList<Long>();
        ArrayList<Long> c2 = new ArrayList<Long>();
        ArrayList<Long> c3 = new ArrayList<Long>();
        ArrayList<Long> c4 = new ArrayList<Long>();
        ArrayList<Long> c5 = new ArrayList<Long>();
        ArrayList<Long> c6 = new ArrayList<Long>();
        ArrayList<Long> c7 = new ArrayList<Long>();
        ArrayList<Long> c8 = new ArrayList<Long>();
        ArrayList<Long> c9 = new ArrayList<Long>();


        double prevTick = 0;
        for (Double tick : intervalBounds) {
            if (prevTick == 0) {
                chords.putAll(tick, onEvents.get(tick));
                chordspacked.put(tick,NgramCoder.pack( onEvents.get(tick).toArray(new Integer[onEvents.get(tick).size()]))) ;
            } else {
                ImmutableSet<Integer> prevChord = ImmutableSet.copyOf(chords.get(prevTick));
                ImmutableSet<Integer> restNotes = Sets.difference(prevChord, Sets.newHashSet(offEvents.get(tick))).immutableCopy();
                ImmutableSet<Integer> thisChord = Sets.union(restNotes, Sets.newHashSet(onEvents.get(tick))).immutableCopy();
                long l = NgramCoder.pack( thisChord.toArray(new Integer[thisChord.size()]));

                if(thisChord.size() == 2){
                    c2.add(l) ;
                }
                else if(thisChord.size() == 3){
                    c3.add(l) ;
                }
                else if(thisChord.size() == 4){
                    c4.add(l) ;
                }
                else if(thisChord.size() == 5){
                    c5.add(l) ;
                }
                else if(thisChord.size() == 6){
                    c6.add(l) ;
                }
                else if(thisChord.size() == 7){
                    c7.add(l) ;
                }
                else if(thisChord.size() == 8){
                    c8.add(l) ;
                }
                else {
                    c9.add(l) ;
                }

                chords.putAll(tick, thisChord);
                chordspacked.put(tick,l);
            }
            prevTick = tick;
        }

        ArrayList<ArrayList<Long>> out = new ArrayList<ArrayList<Long>>();
        out.add(c1);
        out.add(c2);
        out.add(c3);
        out.add(c4);
        out.add(c5);
        out.add(c6);
        out.add(c7);
        out.add(c8);
        out.add(c9);
        return out;
    }

    public static boolean isLabel(Integer[] chord){
        long l = NgramCoder.pack(chord);

        //Labels einlesen
        String line;
        ArrayList<Long> array = new ArrayList<Long>();
        try{
            FileReader fread = new FileReader("/home/olivia/Dokumente/Bachelorarbeit/Daten/labels.txt");
            BufferedReader in = new BufferedReader(fread);
            for( int i = 0; (line = in.readLine())!=null; i++){
                array.add(Long.parseLong(line));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if(array.contains(l)){
            return true;
        }
        else{
            return false;
        }
    }


    public static long parseLabel(Integer[] chord) {
        long result = 0;
        if(isLabel(chord)){
            result=  NgramCoder.pack(chord);
        }
        else{
            ArrayList<Integer[]> possibleChords = new ArrayList<Integer[]>();
            for(int i = 0; i< chord.length; i ++){
                Integer[] newChord = new Integer[chord.length -1];
                long newLongChord;
                for(int j = 0; j< chord.length; j++){
                    if(i==j){
                        continue;
                    }
                    if(j < i) {
                        newChord[j] = chord[j];
                    }
                    else{
                        newChord[j-1] = chord[j];
                    }

                }
                possibleChords.add(newChord);
            }

            for (Integer[] in : possibleChords){
                if (isLabel(in)) {
                    result =  NgramCoder.pack(in);
                    break;
                }
            }
            if (result == 0){
                for (int i = 0; i< possibleChords.size();i++){
                    if(parseLabel(possibleChords.get(0))!= 0){
                        return  parseLabel(possibleChords.get(i));

                    }
                    else {
                        continue;
                    }
}
            }
        }
        return result;
    }


    public static ArrayList<String> listDir(File dir){
            ArrayList<String> out = new ArrayList<String>();

            File[] files = dir.listFiles();
            if (files!= null){
                for(int i = 0;i<files.length;i++){
                    if(files[i].isDirectory()){
                        listDir(files[i]);
                    }
                    else{
                        out.add(files[i].getAbsolutePath());
                    }
                }
            }
            return out;
        }


    public static void main(String[] args) {


        File dir = new File("/home/olivia/Dokumente/Bachelorarbeit/Daten/Test/kdf/albeniz");
        ArrayList<String> files = listDir(dir);
        for(String s: files){
            File f = new File(s);
            try {
                ImmutableMultimap<Double, Long> chords = parseChords(Files.toByteArray(f));
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

//        long[] longarray = {5868753264417177600L, 4153166305022705664L, 4143670947722821632L,5868753264417177600L, 4153166305022705664L, 4143670947722821632L,5868753264417177600L, 4153166305022705664L, 4143670947722821632L,5868753264417177600L, 4153166305022705664L, 4143670947722821632L,5868753264417177600L, 4153166305022705664L, 4143670947722821632L,5868753264417177600L, 4153166305022705664L, 4143670947722821632L,4971973988617027584L, 5018472678953058304L};


//        for(long l:longarray) {
//           if(){
//               System.out.println("true");
//           }
//            else{
//               System.out.println("false");
//           }
//        }

//        //Alle Midi Dateien durchlaufen und Akkorde in chords speichern
//        File dir = new File("/home/olivia/Dokumente/Bachelorarbeit/Daten/MIDI Daten/kdf");
//        ArrayList<String> files = listDir(dir);
//        ArrayList<ArrayList<Long>> chords = new ArrayList<ArrayList<Long>>();
//        for ( int j = 0; j< 9; j++) chords.add(new ArrayList<Long>());
//        for(String s : files){
//            File f = new File(s);
//            try {
//                ArrayList<ArrayList<Long>> im = parseAllChords(Files.toByteArray(f));
//                for (int i = 1; i< im.size(); i++){
//                    chords.get(i).addAll(im.get(i));
//                }
//
//            } catch (Exception e) {
//                System.out.println(s);
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
//        for( int i = 0; i<=127; i++){
//            Integer[] ii = {i};
//            long l = NgramCoder.pack( ii);
//            chords.get(0).add(l);
//        }
//
//        for(ArrayList<Long> al: chords){
//            //Akkorde zählen und in sorted Chords mit (codierter Akkord, häufigkeit) abspeichern, sortiert aufsteigend nach größe des Akkords
//            int count = 0;
//            Multimap<Long,Integer> sortedChords = TreeMultimap.create() ;
//                for (Long i : al) {
//                    if (sortedChords.containsKey(i)){
//                        continue;
//                    }
//                    for(Long l: al){
//                         if (i.equals(l)){
//                             count = count+1;
//                         }
//                     }
//                    sortedChords.put(i,count);
//                    count = 0;
//                }
//
//            //keys und values invertieren --> häufigste Akkorde sind am Ende
//            Multimap<Integer, Long> inverted = Multimaps.invertFrom(sortedChords,ArrayListMultimap.<Integer, Long>create());
//
//            //Nur Akkorde in Array speichern, mit häufigstem an erster Stelle
//            long[] k = new long[inverted.size()];
//            int iterator =1;
//            for(long l : inverted.values()){
//                k[k.length-iterator]= l;
//                iterator +=1;
//            }
//            iterator = 1;
//
//            //haeufigste Akkorde raussuchen und in labels.txt abspeichern
//            int anzahl;
//            if(k.length<128){
//               anzahl = k.length;
//            }
//            else{
//                anzahl = 128;
//            }
//
//            FileWriter fw = null;
//            try {
//                fw = new FileWriter("/home/olivia/Dokumente/Bachelorarbeit/Daten/labels.txt", true);
//                BufferedWriter bufferedWriter = new BufferedWriter(fw);
//                for (int i = 0; i <anzahl; i++) {
//                    try {
//                        String p = String.valueOf(k[i]);
//                        fw.append(p + "\n");
//
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                bufferedWriter.close();
//                fw.close();
//            } catch (IOException e) {
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//            }
//        }
    }

}
