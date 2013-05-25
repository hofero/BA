package bajava;

import java.util.Arrays;

/*

each point can be explained by other points
each point can explain other points - base frequency
negative explanations are possible - silence is also evidence
we need to sum up all evidence

rules: if a frequency is not sounding itself, it is not a base frequency
if second ot third harmonics are missing, it's not a base frequency

*/
public class PolyphonicPitchEstimator {

    private final static int NUM_HARMONICS = 5;
    private final static int HIGHEST_NOTE = 1;

    public static double[][] removeLoudBackground(double[][] audioData) {
        double[][] out = new double[audioData.length][audioData[0].length];
        int bands = audioData[0].length;
        for (int i = 0; i < audioData.length; i++) {
            double[] average = new double[bands];
            for (int j = 0; j < bands; j++) {
                average[j] = cutOff(Arrays.copyOfRange(audioData[i], Math.max(0, j - 4), Math.min(j + 4, bands)), 0.8);
            }
            for (int j = 0; j < bands; j++) {
                if (audioData[i][j] < average[j]) {
                    out[i][j] = -127;
                } else {
                    out[i][j] = audioData[i][j];
                }
            }
        }
        return out;
    }

    private static double cutOff(double[] a, double part) {
        double[] b = Arrays.copyOf(a, a.length);
        Arrays.sort(b);

        return b[(int) Math.floor(b.length * part)];
    }

}