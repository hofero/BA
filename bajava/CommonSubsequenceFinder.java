
package bajava;

import com.google.common.collect.*;

public class CommonSubsequenceFinder {

    private AlignmentPair alignmentPair;
    private double penalty = 0.4;
    private MatchingPolicy matchingPolicy = MatchingPolicy.EXACT;
    private double[] leftBoost;
    private double[] rightBoost;
    private int firstLeftOffset;
    private int firstRightOffset;
    private int lastLeftOffset;
    private int lastRightOffset;
    private final Multimap<Integer, Integer> excludedPairs = HashMultimap.create();
    private double diagonalPenalty = 0;


    public enum MatchingPolicy implements MatchingPolicyInterface {

        EXACT(new MatchingPolicyInterface() {
            public boolean equal(Integer a, Integer b) {
                return a.equals(b);
            }
        }),
        SEMITONE(new MatchingPolicyInterface() {
            public boolean equal(Integer a, Integer b) {
                return Math.abs(a - b) <= 1;
            }
        }),
        UPPER_SEMITONE(new MatchingPolicyInterface() {
            public boolean equal(Integer a, Integer b) {
                return a - b == 0 || a - b == 1;
            }
        }),
        LOWER_SEMITONE(new MatchingPolicyInterface() {
            public boolean equal(Integer a, Integer b) {
                return a - b == 0 || a - b == -1;
            }
        }),
        OCTAVE(new MatchingPolicyInterface() {
            public boolean equal(Integer a, Integer b) {
                return (a - b) % 12 == 0;
            }
        }),
        OCTAVE_SEMITONE(new MatchingPolicyInterface() {
            public boolean equal(Integer a, Integer b) {
                return (a - b) % 12 <= 1 || (a - b) % 12 >= 11;
            }
        });
        private final MatchingPolicyInterface mp;

        MatchingPolicy(MatchingPolicyInterface mp) {
            this.mp = mp;
        }

        @Override
        public boolean equal(Integer a, Integer b) {
            return mp.equal(a, b);
        }
    }

    public interface MatchingPolicyInterface {
        public boolean equal(Integer a, Integer b);
    }

    public static enum AlignmentDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT
    }

    public void setPenalty(double p) {
        penalty = p;
    }

    public void setMatchingPolicy(MatchingPolicy policy) {
        matchingPolicy = policy;
    }


}
