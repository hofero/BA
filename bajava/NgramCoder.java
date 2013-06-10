
package bajava;

import java.util.Arrays;

//Mehrere Integer Werte zu einem Longwert codieren
public class NgramCoder {
    public static long pack(Integer[] a){
        long out = 0;
        for (int i = 0; i<Math.min(9, a.length);i++){
            out += (long) a[i]<<7*(8-i);
        }
        return out;
    }

    public static short[] unpack(long l){
        short[] a = new short[9];
        int nonNullIndex = -1;
        for(int i = 0; i<9; i++){
            a[i]= (short)((l>>>7*(8-i))&0x7f);
            if(a[i]!=0)nonNullIndex = i;
        }
        return Arrays.copyOfRange(a, 0,nonNullIndex +1);
    }
    
}
