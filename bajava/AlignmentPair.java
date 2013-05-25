
package bajava;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class AlignmentPair {

    private StreamsContainer sc0;
    private StreamsContainer sc1;

    private double[] leftTime = {}; // linearized times
    private double[] rightTime = {};

    private final TIntArrayList left = new TIntArrayList();
    private final TIntArrayList right = new TIntArrayList();

    private TIntDoubleMap rightToLeft = new TIntDoubleHashMap();
    private TIntDoubleMap leftToRight = new TIntDoubleHashMap();

    private double rightToLeftBegin = Integer.MAX_VALUE;
    private double rightToLeftEnd = 0;
    private double leftToRightBegin = Integer.MAX_VALUE;
    private double leftToRightEnd = 0;

    private int totalPairs = 0;
    private final TIntArrayList dists = new TIntArrayList();

    private final TIntArrayList leftDists = new TIntArrayList();
    private final TIntArrayList rightDists = new TIntArrayList();

    private double[] indicator = new double[]{};

    protected AlignmentPair() {
    }

    public AlignmentPair(StreamsContainer sc0, StreamsContainer sc1) {
        this.setLeftContainer(sc0);
        this.setRightContainer(sc1);
        leftTime = sc0.getLinearizedTimes();
        rightTime = sc1.getLinearizedTimes();
    }

    public void setLeftContainer(StreamsContainer sc0) {
        this.sc0 = sc0;
    }

    public void setRightContainer(StreamsContainer sc1) {
        this.sc1 = sc1;
    }
}
