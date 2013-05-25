package bajava;

public class QualityAlignmentFilter {

    //Liste besserer Zuordnungen (MIDI <--> Audio) 
    public static class AlignmentListItem{
        //MIDI
        private final String id0;
        //Audio (.aac) ID
        private final String id1;
        //uebereinstimmung
        private final double minConfidence;

        public AlignmentListItem(String u0, String u1, double mc){
            id0 = u0;
            id1 = u1;
            minConfidence = mc;
        }

        //Getter und Setter
        public String getId0(){
            return id0;
        }

        public String getId1() {
            return id1;
        }

        public double getMinConfidence() {
            return minConfidence;
        }

        @Override
        public String toString() {
            return "[" + id0 + ", "+ id1 + ", " + minConfidence + "]";
        }
    }

}
