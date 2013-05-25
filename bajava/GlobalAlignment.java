
package bajava;

import bajava.StreamsContainer.ContentSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//Zuordnungen MIDI <--> Spektrogramme
public class GlobalAlignment {
    //MIDI Datei
    private String uri0;
    //Spektrogramm ID
    private String uri1;
    //MIDI
    private StreamsContainer.ContentSource source0;
    //Audio
    private StreamsContainer.ContentSource source1;
    //Abschnitt - Zeit
    private Map<Integer, Double> streamTimes0;
    private Map<Integer, Double> streamTimes1;
    //
    private final List<double[][]> localTimeMaps;

    private final List<double[]> confidences;

    private int points;
    private int segments;

    public GlobalAlignment(StreamsContainer sc0, StreamsContainer sc1){
        localTimeMaps = new ArrayList<double[][]>();
        confidences = new ArrayList<double[]>();
        this.setUri0(sc0.getTitle());
        this.setUri1(sc1.getTitle());
        this.setSource0(sc0.getContentSource());
        this.setSource1(sc1.getContentSource());
        streamTimes0 = sc0.getStreamTimes();
        streamTimes1 = sc1.getStreamTimes();
    }


    //Getter und Setter
    public String getUri0() {
        return uri0;
    }

    public String getUri1() {
        return uri1;
    }

    public void setUri0(String uri0) {
        this.uri0 = uri0;
    }

    public void setUri1(String uri1) {
        this.uri1 = uri1;
    }

    public void setSource0(ContentSource source0) {
        this.source0 = source0;
    }

    public void setSource1(ContentSource source1) {
        this.source1 = source1;
    }
    
    public List<double[][]> getLocalTimeMaps() {
        return localTimeMaps;
    }

    public String toString() {
        return "[" + localTimeMaps.size() + " local alignments, " + points + " points, minConfidence " + "]";
   }
}
