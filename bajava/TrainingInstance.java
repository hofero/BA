
package bajava;

public class TrainingInstance implements java.io.Serializable {

//    private long[] notes;
    private int[] notes;
    private double[][] transform;

//    TrainingInstance(long[] notes, double[][] transform) {
    TrainingInstance(int[] notes, double[][] transform) {
        this.notes = notes;
        this.transform = transform;
    }

//    public long[] getNotes() {
//        return notes;
//    }
    public int[] getNotes() {
        return notes;
    }

    public double[][] getTransform() {
        return transform;
    }

}
