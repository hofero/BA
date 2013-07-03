
package bajava;

public class TrainingInstance implements java.io.Serializable {

//    private int[] notes;
    private long[] chords;
    private double[][] transform;
    private String uri;
    private double starttime;
    private double endtime;

    TrainingInstance(long[] chords, double[][] transform, String uri, double starttime, double endtime) {
//        this.notes = notes;
        this.chords = chords;
        this.transform = transform;
        this.uri = uri;
        this.starttime = starttime;
        this.endtime = endtime;
    }

//    public int[] getNotes() {
//        return notes;
//    }

    public long[] getChords() {
        return chords;
    }

    public double[][] getTransform() {
        return transform;
    }

    public String getUri() {
        return uri;
    }

    public double getStarttime() {
        return starttime;
    }

    public double getEndtime() {
        return endtime;
    }
}
