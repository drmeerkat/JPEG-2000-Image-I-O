package FourierTrans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Compression {
//    private double[][] quantMatrix = {
//            {16,11,10,16,24,40,51,61},
//            {12,12,14,19,26,58,60,55},
//            {14,13,16,24,40,57,69,56},
//            {14,17,22,29,51,87,80,62},
//            {18,22,37,56,68,109,103,77},
//            {24,35,55,64,81,104,113,92},
//            {49,64,78,87,103,121,120,101},
//            {72,92,95,98,112,100,103,99}};
    private double[][] quantMatrix = {
        {8,6,5,8,12,20,26,31},
        {6,6,7,10,13,29,30,28},
        {7,7,8,12,20,29,35,28},
        {7,9,11,15,26,44,40,31},
        {9,11,19,28,34,55,52,39},
        {12,18,28,32,41,52,57,46},
        {25,32,39,44,52,61,60,51},
        {36,46,48,49,56,50,52,50}};


    private Map<String, String> acMap = new HashMap<>();
    private Map<String, String> dcMap = new HashMap<>();
    private Map<String, String> reacMap;
    private Map<String, String> redcMap;
    private double[] flatOut;
    private double[][] recImg;
    private double quality;

    public Compression (){
        //TODO: add quality support
        this.quality = 5;
        acMap.put("0/0", "1010");//EOB
        acMap.put("0/1", "00");
        acMap.put("0/2", "01");
        acMap.put("0/3", "100");
        acMap.put("0/4", "1011");
        acMap.put("0/5", "11010");
        acMap.put("0/6", "1111000");
        acMap.put("0/7", "11111000");
        acMap.put("0/8", "1111110110");
        acMap.put("0/9", "1111111110000010");
        acMap.put("0/10", "1111111110000011");
        acMap.put("1/1", "1100");
        acMap.put("1/2", "11011");
        acMap.put("1/3", "1111001");
        acMap.put("1/4", "111110110");
        acMap.put("1/5", "11111110110");
        acMap.put("1/6", "1111111110000100");
        acMap.put("1/7", "1111111110000101");
        acMap.put("1/8", "1111111110000110");
        acMap.put("1/9", "1111111110000111");
        acMap.put("1/10", "1111111110001000");
        acMap.put("2/1", "11100");
        acMap.put("2/2", "11111001");
        acMap.put("2/3", "1111110111");
        acMap.put("2/4", "111111110100");
        acMap.put("2/5", "1111111110001001");
        acMap.put("2/6", "1111111110001010");
        acMap.put("2/7", "1111111110001011");
        acMap.put("2/8", "1111111110001100");
        acMap.put("2/9", "1111111110001101");
        acMap.put("2/10", "1111111110001110");
        acMap.put("3/1", "111010");
        acMap.put("3/2", "111110111");
        acMap.put("3/3", "111111110101");
        acMap.put("3/4", "1111111110001111");
        acMap.put("3/5", "1111111110010000");
        acMap.put("3/6", "1111111110010001");
        acMap.put("3/7", "1111111110010010");
        acMap.put("3/8", "1111111110010011");
        acMap.put("3/9", "1111111110010100");
        acMap.put("3/10", "1111111110010101");
        acMap.put("4/1", "111011");
        acMap.put("4/2", "1111111000");
        acMap.put("4/3", "1111111110010110");
        acMap.put("4/4", "1111111110010111");
        acMap.put("4/5", "1111111110011000");
        acMap.put("4/6", "1111111110011001");
        acMap.put("4/7", "1111111110011010");
        acMap.put("4/8", "1111111110011011");
        acMap.put("4/9", "1111111110011100");
        acMap.put("4/10", "1111111110011101");
        acMap.put("5/1", "1111010");
        acMap.put("5/2", "11111110111");
        acMap.put("5/3", "1111111110011110");
        acMap.put("5/4", "1111111110011111");
        acMap.put("5/5", "1111111110100000");
        acMap.put("5/6", "1111111110100001");
        acMap.put("5/7", "1111111110100010");
        acMap.put("5/8", "1111111110100011");
        acMap.put("5/9", "1111111110100100");
        acMap.put("5/10", "1111111110100101");
        acMap.put("6/1", "1111011");
        acMap.put("6/2", "111111110110");
        acMap.put("6/3", "1111111110100110");
        acMap.put("6/4", "1111111110100111");
        acMap.put("6/5", "1111111110101000");
        acMap.put("6/6", "1111111110101001");
        acMap.put("6/7", "1111111110101010");
        acMap.put("6/8", "1111111110101011");
        acMap.put("6/9", "1111111110101100");
        acMap.put("6/10", "1111111110101101");
        acMap.put("7/1", "11111010");
        acMap.put("7/2", "111111110111");
        acMap.put("7/3", "1111111110101110");
        acMap.put("7/4", "1111111110101111");
        acMap.put("7/5", "1111111110110000");
        acMap.put("7/6", "1111111110110001");
        acMap.put("7/7", "1111111110110010");
        acMap.put("7/8", "1111111110110011");
        acMap.put("7/9", "1111111110110100");
        acMap.put("7/10", "1111111110110101");
        acMap.put("8/1", "111111000");
        acMap.put("8/2", "111111111000000");
        acMap.put("8/3", "1111111110110110");
        acMap.put("8/4", "1111111110110111");
        acMap.put("8/5", "1111111110111000");
        acMap.put("8/6", "1111111110111001");
        acMap.put("8/7", "1111111110111010");
        acMap.put("8/8", "1111111110111011");
        acMap.put("8/9", "1111111110111100");
        acMap.put("8/10", "1111111110111101");
        acMap.put("9/1", "111111001");
        acMap.put("9/2", "1111111110111110");
        acMap.put("9/3", "1111111110111111");
        acMap.put("9/4", "1111111111000000");
        acMap.put("9/5", "1111111111000001");
        acMap.put("9/6", "1111111111000010");
        acMap.put("9/7", "1111111111000011");
        acMap.put("9/8", "1111111111000100");
        acMap.put("9/9", "1111111111000101");
        acMap.put("9/10", "1111111111000110");
        acMap.put("10/1", "111111010");
        acMap.put("10/2", "1111111111000111");
        acMap.put("10/3", "1111111111001000");
        acMap.put("10/4", "1111111111001001");
        acMap.put("10/5", "1111111111001010");
        acMap.put("10/6", "1111111111001011");
        acMap.put("10/7", "1111111111001100");
        acMap.put("10/8", "1111111111001101");
        acMap.put("10/9", "1111111111001110");
        acMap.put("10/10", "1111111111001111");
        acMap.put("11/1", "1111111001");
        acMap.put("11/2", "1111111111010000");
        acMap.put("11/3", "1111111111010001");
        acMap.put("11/4", "1111111111010010");
        acMap.put("11/5", "1111111111010011");
        acMap.put("11/6", "1111111111010100");
        acMap.put("11/7", "1111111111010101");
        acMap.put("11/8", "1111111111010110");
        acMap.put("11/9", "1111111111010111");
        acMap.put("11/10", "1111111111011000");
        acMap.put("12/1", "1111111010");
        acMap.put("12/2", "1111111111011001");
        acMap.put("12/3", "1111111111011010");
        acMap.put("12/4", "1111111111011011");
        acMap.put("12/5", "1111111111011100");
        acMap.put("12/6", "1111111111011101");
        acMap.put("12/7", "1111111111011110");
        acMap.put("12/8", "1111111111011111");
        acMap.put("12/9", "1111111111100000");
        acMap.put("12/10", "1111111111100001");
        acMap.put("13/1", "11111111000");
        acMap.put("13/2", "1111111111100010");
        acMap.put("13/3", "1111111111100011");
        acMap.put("13/4", "1111111111100100");
        acMap.put("13/5", "1111111111100101");
        acMap.put("13/6", "1111111111100110");
        acMap.put("13/7", "1111111111100111");
        acMap.put("13/8", "1111111111101000");
        acMap.put("13/9", "1111111111101001");
        acMap.put("13/10", "1111111111101010");
        acMap.put("14/1", "1111111111101011");
        acMap.put("14/2", "1111111111101100");
        acMap.put("14/3", "1111111111101101");
        acMap.put("14/4", "1111111111101110");
        acMap.put("14/5", "1111111111101111");
        acMap.put("14/6", "1111111111110000");
        acMap.put("14/7", "1111111111110001");
        acMap.put("14/8", "1111111111110010");
        acMap.put("14/9", "1111111111110011");
        acMap.put("14/10", "1111111111110100");
        acMap.put("15/1", "1111111111110101");
        acMap.put("15/2", "1111111111110110");
        acMap.put("15/3", "1111111111110111");
        acMap.put("15/4", "1111111111111000");
        acMap.put("15/5", "1111111111111001");
        acMap.put("15/6", "1111111111111010");
        acMap.put("15/7", "1111111111111011");
        acMap.put("15/8", "1111111111111100");
        acMap.put("15/9", "1111111111111101");
        acMap.put("15/10", "1111111111111110");
        acMap.put("15/0", "11111111001");

        dcMap.put("0", "00");
        dcMap.put("1", "010");
        dcMap.put("2", "011");
        dcMap.put("3", "100");
        dcMap.put("4", "101");
        dcMap.put("5", "110");
        dcMap.put("6", "1110");
        dcMap.put("7", "11110");
        dcMap.put("8", "111110");
        dcMap.put("9", "1111110");
        dcMap.put("10", "11111110");
        dcMap.put("11", "111111110");

        reacMap = acMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        redcMap = dcMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public void setAcMap(Map<String, String> input){
        this.acMap = input;
        this.reacMap = acMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public void setDcMap(Map<String, String> input){
        this.dcMap = input;
        this.redcMap = dcMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    public void setQuantMatrix(double[][] input){
        this.quantMatrix = input;
    }

    public String encode(double[] input, int patchLen){
        int[] cat = new int[input.length], zeroCnt = new int[input.length];
        int tmpZeros = 0, patchCnt = 0, lastValidInd = 0;
        double lastDC = 0;
        String tmpResult = "";
        for (int i=0;i<input.length;i++){
            if (patchCnt == 0){//encode dc component
                String dcCode = Integer.toString(this.catCode((int)input[i]-lastDC));
                tmpResult += String.format("%s%s", dcMap.get(dcCode), this.bitInteger((int)(input[i]-lastDC)));
                lastValidInd = tmpResult.length();
                lastDC = input[i];
                patchCnt++;
                continue;
            }
            if (input[i] == 0){//if ac component is zero
                tmpZeros++;
                if (tmpZeros == 15){
                    tmpResult += acMap.get("15/0");
                    tmpZeros = 0;
                }
            }
            else{// if ac component is non-zero
                cat[i] = this.catCode(input[i]);
                zeroCnt[i] = tmpZeros;
                tmpZeros = 0;
    //            System.out.println(String.format("(%d, %d), %s", zeroCnt[i], cat[i],  this.bitInteger((int)input[i])));
    //            System.out.println(String.format("%s, %s", acMap.get(zeroCnt[i]+"/"+cat[i]),  this.bitInteger((int)input[i])));
                tmpResult += String.format("%s%s", acMap.get(zeroCnt[i]+"/"+cat[i]), this.bitInteger((int)input[i]));
                lastValidInd = tmpResult.length();
            }
            patchCnt++;
            if (patchCnt == patchLen){//finish this patch
                if (lastValidInd != tmpResult.length()){
                    tmpResult = tmpResult.substring(0, lastValidInd);
                }
                tmpZeros = 0;
                tmpResult += "1010";//EOB
                patchCnt = 0;//
                lastValidInd = tmpResult.length();
            }
        }
        return tmpResult;
    }

    public double[] decode(String code, int patchLen, int length){//this length will be the 64 patch cnt
        int lastInd = 0, i = 0, coefCnt = 0;
        String tmpResult = "";
        double[] coef = new double[length];
        int lastDC = 0;
        while (i<=code.length()){//loop over the input
            if (coefCnt == 0 && dcMap.containsValue(code.substring(lastInd,i))){
                int dcCnt = Integer.parseInt(redcMap.get(code.substring(lastInd,i)));
                String coefBinary = code.substring(i, i+dcCnt);//the binary format coef
                int result = strInterger(coefBinary);//parse the coef str to int
                tmpResult += (Integer.toString(result+lastDC)+" ");//save the non-zero coef
                lastDC = result+lastDC;
                lastInd = i+dcCnt;
                i += dcCnt;
                coefCnt++;
                continue;
            }
            else if (coefCnt!=0 && acMap.containsValue(code.substring(lastInd,i))){
                String cat = reacMap.get(code.substring(lastInd,i));// cat will be a str like "0/6" -> zerocnt/cat
                int index = cat.indexOf('/');
                int catCodeCnt = Integer.parseInt(cat.substring(index+1));//the chars after '/' constitute a number
                if (Integer.parseInt(cat.substring(0, index)) != 0) {
                    tmpResult += new String(new char[Integer.parseInt(cat.substring(0, index))]).replace("\0", "0 ");
                    coefCnt += Integer.parseInt(cat.substring(0, index));
                }
                if (catCodeCnt == 0){// 0/0 or 15/0
                    if (Integer.parseInt(cat.substring(0, index)) == 0){//0/0
                        if (coefCnt < patchLen){
                            tmpResult += new String(new char[patchLen- coefCnt]).replace("\0", "0 ");
                            coefCnt = patchLen;
                        }
                    }
                }
                else{// compute the non-zero value part
                    String coefBinary="";
                    coefBinary = code.substring(i, i+catCodeCnt);//the binary format coef
                    int result = strInterger(coefBinary);//parse the coef str to int
                    tmpResult += (Integer.toString(result)+" ");//save the non-zero coef
                    coefCnt++;
                }
                if (coefCnt == patchLen){
                    coefCnt = 0;
                }

                lastInd = i+catCodeCnt;
                i += catCodeCnt;
                continue;
            }
            i++;
        }

        String[] coefArr = tmpResult.split(" ");
        for (int j=0;j<coef.length;j++){
            coef[j] = Integer.parseInt(coefArr[j]);
        }
        return coef;
    }

    public void zigzagFull(double[][] input, boolean filterZero){
        int height = input.length, width = input[0].length;
        this.flatOut = new double[height*width];
        for (int x = 0; x < height/8; x++) {
            for (int y = 0; y < width / 8; y++) {
                //patch x,y from 0,0 -> 63,63
                double[][] patch = new double[8][8];
                for (int i = x * 8; i < x * 8 + 8; i++) {
                    System.arraycopy(input[i], y * 8, patch[i - x * 8], 0, 8);
                }
                double[] tmp = this.zigzagTraversal(patch);
                System.arraycopy(tmp, 0, this.flatOut, x*64*width/8+y*64, tmp.length);
            }
        }
    }

    public void inverseZigzagFull(double[] input, int height, int width){
        this.recImg = new double[height][width];
        for (int x = 0; x < height/8; x++) {
            for (int y = 0; y < width/8; y++) {
                double[] tmp = new double[64];
                System.arraycopy(input, x*64*width/8+y*64, tmp, 0, 64);
                double[][] patch = this.inverseZigzag(tmp, 8, 8);
                for (int i = x * 8; i < x * 8 + 8; i++) {
                    System.arraycopy(patch[i - x * 8], 0, this.recImg[i], y * 8, 8);
                }
            }
        }
    }

    public void quantPatch(double[][] input){
        for (int i=0;i<8;i++){
            for (int j=0;j<8;j++){
                input[i][j] = Math.round(input[i][j]/quantMatrix[i][j]);
            }
        }
    }

    public void inverseQuantPatchLine(double[] input, int row){
        for (int i=0;i<8;i++){
                input[i] *= quantMatrix[row][i];
        }
    }

    public double[] zigzagTraversal(double[][] input){
        int row = input.length, col = input[0].length;
        double[] result = new double[row*col];
        int currentRow = 0, currentCol = 0, cnt=0, scanIter=0;
        boolean islower = false;
        while (cnt<row*col){
            if (!islower && ((currentCol==col-1&&currentRow==0)||(currentCol==0&&currentRow==row-1))){
                islower = true;
            }
            int x = currentCol, y = currentRow;
            if (scanIter%2 == 0) {
                //up right
                while (currentRow >= x && currentCol <= y) {
                    result[cnt++] = input[currentRow--][currentCol++];
                }
                if (currentRow < 0 && !islower){//upper half
                    currentRow++;
                }
                if (currentCol == col){//lower half
                    currentRow += 2;
                    currentCol--;
                }
            }
            else{
                // down left
                while (currentRow <= x && currentCol >= y){
                    result[cnt++] = input[currentRow++][currentCol--];
                }
                if (currentRow == row){//lower half
                    currentRow--;
                    currentCol += 2;
                }
                if (currentCol < 0 && !islower){//upper half
                    currentCol++;
                }
            }
            scanIter++;
        }
        return result;
    }

    public static double[][] inverseZigzag(double[] input, int row, int col){
        double[][] result = new double[row][col];

        boolean islower = false;
        int currentRow = 0, currentCol = 0, cnt=0, scanIter=0;
        while (cnt<row*col){
            int x = currentCol, y = currentRow;
            if (!islower && ((currentCol==col-1&&currentRow==0)||(currentCol==0&&currentRow==row-1))){
                islower = true;
            }
            if (scanIter%2 == 0) {
                //up right
                while (currentRow >= x && currentCol <= y) {
                    result[currentRow--][currentCol++] = input[cnt++];
                }
                if (currentCol == col){//lower half
                    currentRow += 2;
                    currentCol--;
                }
                if (currentRow < 0 && !islower){//upper half
                    currentRow++;
                }
            }
            else{
                // down left
                while (currentRow <= x && currentCol >= y){
                     result[currentRow++][currentCol--] = input[cnt++];
                }
                if (!islower && currentCol < 0){//upper half
                    currentCol++;
                }
                if (currentRow == row){//lower half
                    currentRow--;
                    currentCol += 2;
                }
            }
            scanIter++;
        }
        return result;
    }

    public int catCode(double coef){
        //2^(k-1) -> (2^k) - 1
        if (coef == 0){
            return 0;
        }
        double result = Math.floor(Math.log(Math.abs((double)coef))/Math.log(2.0))+1;
        return (int)result;
    }

    public static String bitInteger(int num){
        if (num > 0){
            return Integer.toBinaryString(num);
        }
        else if (num == 0){
            return "";
        }
        else{
            String tmp = Integer.toBinaryString(-num);
            return tmp.replace('0','x').replace('1','0').replace('x','1');
        }
    }

    public static int strInterger(String binary){
        int result = 0, sign = 1;
        if (binary.indexOf('0') == 0){
            binary = binary.replace('0','x').replace('1','0').replace('x','1');
            sign = -1;
        }
        for (int i=0;i<binary.length();i++){
            result += Integer.parseInt(binary.substring(i, i+1))*Math.pow(2, binary.length()-i-1);
        }
        return sign*result;
    }

    public double[] getFlatOut(){
        return this.flatOut;
    }

    public double[][] getRecImg(){
        return this.recImg;
    }
}

