
package bajava;

import java.io.*;
import java.util.zip.GZIPInputStream;

//Spektrogramm durchlaufen und Bytewerte in double [][] speichern
public class ComputeCQT {

    public static final int TARGET_FRAME_RATE = 22050;

    public static double[][] load(String inPath) throws IOException {
        return load(inPath, Integer.MAX_VALUE);
    }

    public static double[][] load(String inPath, int maxFrames) throws IOException {

        FileInputStream fos = new FileInputStream(inPath);
        GZIPInputStream gzout = new GZIPInputStream(fos);
        DataInputStream daos = new DataInputStream(gzout);

        int idx = inPath.indexOf(".aac_"); //64
        String[] a = inPath.substring(idx + 5).split("-");
        int frameRate = Integer.parseInt(a[0]); // 44100
        int frames = Integer.parseInt(a[1]); //54856
        int bands = Integer.parseInt(a[2].split(".dat")[0]); // 335

        double[][] data3 = new double[Math.min(frames, maxFrames)][bands];
        
        int n = 0;
        while (daos.available() > 0 && n < frames * bands) {
            byte b = daos.readByte();
            //n = j*data.length + i
            int j = n / frames;
            int i = n % frames;
            n++;
            if (i >= maxFrames) continue;
            data3[i * TARGET_FRAME_RATE / frameRate][j] = b;
        }
        daos.close();
        return data3;
    }

}