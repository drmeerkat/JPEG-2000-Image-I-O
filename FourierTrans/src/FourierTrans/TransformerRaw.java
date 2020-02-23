package FourierTrans;

import java.util.Arrays;

public class TransformerRaw {
    private String type;
    private double[] output1dReal;
    private double[] output1dImg;
    private double[][] output2dReal;
    private double[][] output2dImg;

    public TransformerRaw(String type){
        this.type = type;
    }

    public void trans2d(double[][] inputReal, double[][] inputImg, int height, int width, boolean isReverse){
        if (this.type.equals("fft")){
            this.fft2d(inputReal, inputImg, height, width, isReverse);
        }
        else if (this.type.equals("dct")){
            this.dct2d(inputReal, height, width, isReverse, false);
        }
        else if (this.type.equals("dft")){
            this.dft2d(inputReal, inputImg, height, width, isReverse);
        }
        else if (this.type.equals("jpeg")){
            this.dct2d(inputReal, height, width, isReverse, true);
        }
    }

    public void trans1d(double[] inputReal, double[] inputImg, int length, boolean isReverse){
        if (this.type.equals("fft")){
            this.fft1d(inputReal, inputImg, length, isReverse);
        }
        else if (this.type.equals("dct")){
            this.dct1d(inputReal, length, isReverse);
        }
        else if (this.type.equals("fdct")){
            this.fdct1d(inputReal, length, isReverse);
        }
        else if (this.type.equals("dft")){
            this.dft1d(inputReal, inputImg, length, isReverse);
        }
    }

    public void dft1d(double[] inputReal, double[] inputImg, int length, boolean isReverse){
        if (length != inputReal.length || length != inputImg.length){
            throw new IllegalArgumentException("ERROR: Wrong input length.");
        }
        this.output1dReal = new double[length];
        this.output1dImg = new double[length];
        double sumreal = 0;
        double sumimg = 0;
        double angle = 0;
        int sign = 1;
        double scale = 1;
        if (isReverse){
            sign = -1;
            scale = 1/(float)length;
        }
        for (int k=0;k<length;k++){
            sumreal = 0;
            sumimg = 0;
            for (int t=0;t<length;t++){
                angle = 2*Math.PI*t*k/(float)length;
                sumreal += inputReal[t]*Math.cos(angle)+sign*inputImg[t]*Math.sin(angle);
                sumimg += inputImg[t]*Math.cos(angle)-sign*inputReal[t]*Math.sin(angle);
            }
            this.output1dReal[k] = scale*sumreal;
            this.output1dImg[k] = scale*sumimg;
        }
    }

    public void dft2d(double[][] inputReal, double[][] inputImg, int height, int width, boolean isReverse){
        this.output2dReal = new double[height][width];
        this.output2dImg = new double[height][width];
        if (isReverse){
            for (int x=0;x<height;x++){
                for (int y=0;y<width;y++){
                    inputImg[x][y] *= -1;
                }
            }
        }
        for (int x=0;x<height;x++){
            this.dft1d(inputReal[x], inputImg[x], width, false);
            output2dReal[x] = this.output1dReal;
            output2dImg[x] = this.output1dImg;
        }
        double[][] tmpReal = matrixTrans(this.output2dReal, height, width);
        double[][] tmpImg = matrixTrans(this.output2dImg, height, width);
        for (int y=0;y<width;y++){
            this.dft1d(tmpReal[y], tmpImg[y], height, false);
            tmpImg[y] = this.output1dImg;
            tmpReal[y] = this.output1dReal;
        }
        this.output2dReal = matrixTrans(tmpReal, width, height);
        this.output2dImg = matrixTrans(tmpImg, width, height);
        if (isReverse){
            for (int x=0;x<height;x++){
                for (int y=0;y<width;y++){
                    double scale = width*height;
                    this.output2dImg[x][y] /= -scale;
                    this.output2dReal[x][y] /= scale;
                }
            }
        }
    }

    public void fft1d(double[] inputReal, double[] inputImg, int length, boolean isReverse){
        if (length != inputReal.length || length != inputImg.length){
            throw new IllegalArgumentException("ERROR: Wrong input length.");
        }
        if (length % 2 != 0){
            throw new IllegalArgumentException("ERROR: input length is not a power of 2");
        }
        this.output1dImg = new double[length];
        this.output1dReal = new double[length];
        if (isReverse){
            for (int i=0;i<length;i++){
                inputImg[i] = -inputImg[i];
            }
        }
        fft1dIter(inputReal, inputImg, length);
        if (isReverse){
            for (int i = 0; i< length;i++){
                this.output1dReal[i] = this.output1dReal[i]/(float)length;
                this.output1dImg[i] = -this.output1dImg[i]/(float)length;
            }
        }
    }

    public void fft2d(double[][] inputReal, double[][] inputImg, int height, int width, boolean isReverse){
        this.output2dImg = new double[height][width];
        this.output2dReal = new double[height][width];
        if (isReverse){
            for (int x=0;x<height;x++){
                for (int y=0;y<width;y++){
                    inputImg[x][y] = -inputImg[x][y];
                }
            }
        }
        for (int x=0;x<height;x++){
            this.fft1d(inputReal[x], inputImg[x], width, false);
            output2dReal[x] = this.output1dReal;
            output2dImg[x] = this.output1dImg;
        }
        double[][] tmpReal = matrixTrans(this.output2dReal, height, width);
        double[][] tmpImg = matrixTrans(this.output2dImg, height, width);
        for (int y=0;y<width;y++){
            this.fft1d(tmpReal[y], tmpImg[y], height, false);
            tmpImg[y] = this.output1dImg;
            tmpReal[y] = this.output1dReal;
        }
        this.output2dReal = matrixTrans(tmpReal, width, height);
        this.output2dImg = matrixTrans(tmpImg, width, height);
        if (isReverse){
            for (int x=0;x<height;x++){
                for (int y=0;y<width;y++){
                    double scale = width*height;
                    this.output2dReal[x][y] /= scale;
                    this.output2dImg[x][y] /= -scale;
                }
            }
        }
    }

    public void dct1d(double[] inputReal, int length, boolean isReverse){
        this.output1dReal = new double[length];
        double scale = Math.sqrt(2/(double)length);
        double factor = Math.PI/length;
        double angle;

        if (isReverse){
            inputReal[0] /= Math.sqrt(2);
        }
        for (int i=0;i<length;i++){
            for (int j = 0;j<length;j++){
                if (isReverse)
                    angle = factor*(i+0.5)*j;
                else
                    angle = factor*(j+0.5)*i;
                this.output1dReal[i] += inputReal[j]*Math.cos(angle);
            }
            this.output1dReal[i] *= scale;
        }
        if (!isReverse){
            this.output1dReal[0] /= Math.sqrt(2);
        }
    }

    public void fdct1d(double[] inputReal, int length, boolean isReverse){
        // In place scaled calculation
        this.output1dReal = inputReal;
        if (length != inputReal.length){
            throw new IllegalArgumentException("ERROR: Wrong input length.");
        }
        if (length % 2 != 0){
            throw new IllegalArgumentException("ERROR: input length is not a power of 2");
        }
        if (isReverse){
            this.output1dReal[0] /= (Math.sqrt(2));//scale the input x0 when inverse
            fdct1dIterRe(output1dReal, 0, length, new double[length]);
        }
        else{
            fdct1dIter(output1dReal, 0, length, new double[length]);
            this.output1dReal[0] /= (Math.sqrt(2));//scale the output y0 when forward
        }
        for (int i = 0; i < length; i++){
            this.output1dReal[i] *= Math.sqrt(2/(double)length);
        }
    }

    public void dct2d(double[][] inputReal, int height, int width, boolean isReverse, boolean isCompression){
        // deep copy the input
        this.output2dReal = new double[height][width];
        for (int line = 0;line<height;line++){
            this.output2dReal[line] = Arrays.copyOf(inputReal[line], inputReal[line].length);
        }
        //Initialize the quatization object
        Compression comp = new Compression();

        //split the input into 8x8 patch and shift to 0 mean
        if (!isReverse){//forward pass
            for (int x = 0; x < height; x++)
                for (int y = 0; y < width; y++)
                    this.output2dReal[x][y] -= 128;
        }

        for (int x = 0; x < height/8; x++) {
            for (int y = 0; y < width / 8; y++) {
                //patch x,y from 0,0 -> 63,63
                double[][] patch = new double[8][8];
                for (int i = x*8; i < x*8+8; i++) {
                    System.arraycopy(this.output2dReal[i], y*8, patch[i - x*8], 0, 8);
                    if (isCompression && isReverse){
                        comp.inverseQuantPatchLine(patch[i - x*8], i - x*8);
                    }
//                    this.fdct1d(patch[i - x*8], 8, isReverse);
                    this.dct1d(patch[i - x*8], 8, isReverse);
                    System.arraycopy(this.output1dReal, 0, patch[i - x*8], 0, 8);
                }
                patch = matrixTrans(patch, 8, 8);
                for (int j = 0; j < 8; j++) {
//                    this.fdct1d(patch[j], 8, isReverse);
                    this.dct1d(patch[j], 8, isReverse);
                    System.arraycopy(this.output1dReal, 0, patch[j], 0, 8);
                }
                patch = matrixTrans(patch, 8, 8);
                if (isCompression && !isReverse){
                        comp.quantPatch(patch);
                }
                for (int i = x*8; i < x*8+8; i++) {
                    System.arraycopy(patch[i - x*8], 0, this.output2dReal[i], y*8, 8);
                }
            }
        }

        if (isReverse){
            for (int x = 0; x < height; x++)
                for (int y = 0; y < width; y++)
                    this.output2dReal[x][y] += 128;
        }
    }

    public static double[][] matrixTrans(double[][] input, int height, int width){
        // input: matrix with shape height x width
        // output: matrix with shape width x height
        double[][] result = new double[width][height];
        for (int x=0;x<height;x++){
            for (int y=0;y<width;y++){
                result[y][x] = input[x][y];
            }
        }
        return result;
    }

    public double[] get1dReal(){
        return this.output1dReal;
    }

    public double[] get1dImg(){
        return this.output1dImg;
    }

    public double[][] get2dReal(){
        return this.output2dReal;
    }

    public double[][] get2dImg(){
        return this.output2dImg;
    }

    private static void fdct1dIter(double[] vector, int off, int len, double[] temp) {
        // Algorithm by Byeong Gi Lee, 1984. For details, see:
        // See: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.118.3056&rep=rep1&type=pdf#page=34
        if (len == 1)
            return;
        int halfLen = len / 2;
        for (int i = 0; i < halfLen; i++) {
            double x = vector[off + i];
            double y = vector[off + len - 1 - i];
            temp[off + i] = x + y;
            temp[off + i + halfLen] = (x - y) / (Math.cos((i + 0.5) * Math.PI / len) * 2);
        }
        fdct1dIter(temp, off, halfLen, vector);
        fdct1dIter(temp, off + halfLen, halfLen, vector);
        for (int i = 0; i < halfLen - 1; i++) {
            vector[off + i * 2 + 0] = temp[off + i];
            vector[off + i * 2 + 1] = temp[off + i + halfLen] + temp[off + i + halfLen + 1];
        }
        vector[off + len - 2] = temp[off + halfLen - 1];
        vector[off + len - 1] = temp[off + len - 1];
    }

    private static void fdct1dIterRe(double[] vector, int off, int len, double[] temp) {
        // Algorithm by Byeong Gi Lee, 1984. For details, see:
        // https://www.nayuki.io/res/fast-discrete-cosine-transform-algorithms/lee-new-algo-discrete-cosine-transform.pdf
        if (len == 1)
            return;
        int halfLen = len / 2;
        temp[off + 0] = vector[off + 0];
        temp[off + halfLen] = vector[off + 1];
        for (int i = 1; i < halfLen; i++) {
            temp[off + i] = vector[off + i * 2];
            temp[off + i + halfLen] = vector[off + i * 2 - 1] + vector[off + i * 2 + 1];
        }
        fdct1dIterRe(temp, off, halfLen, vector);
        fdct1dIterRe(temp, off + halfLen, halfLen, vector);
        for (int i = 0; i < halfLen; i++) {
            double x = temp[off + i];
            double y = temp[off + i + halfLen] / (Math.cos((i + 0.5) * Math.PI / len) * 2);
            vector[off + i] = x + y;
            vector[off + len - 1 - i] = x - y;
        }
    }

    private void fft1dIter(double[] real, double[] img, int length){
        if (length == 1){
            this.output1dReal = new double[1];
            this.output1dImg = new double[1];
            this.output1dReal[0] = real[0];
            this.output1dImg[0] = img[0];
            return;
        }
        double[] evenReal = new double[length/2];
        double[] oddReal = new double[length/2];
        double[] evenImg = new double[length/2];
        double[] oddImg = new double[length/2];

        for (int i=0;i<length/2;i++){
            evenReal[i]=real[2*i];
            evenImg[i]=img[2*i];
            oddReal[i]=real[2*i+1];
            oddImg[i]=img[2*i+1];
        }

        fft1dIter(evenReal, evenImg, length/2);
        evenReal = this.output1dReal;
        evenImg = this.output1dImg;
        fft1dIter(oddReal, oddImg, length/2);
        oddReal = this.output1dReal;
        oddImg = this.output1dImg;

        this.output1dImg = new double[length];
        this.output1dReal = new double[length];
        double factorReal = 0;
        double factorImg = 0;
        double angle = 0;
        for (int k=0;k<length/2;k++){
            angle = 2*Math.PI*k/(float)length;
            factorReal = oddReal[k]*Math.cos(angle)+oddImg[k]*Math.sin(angle);
            factorImg = -oddReal[k]*Math.sin(angle)+oddImg[k]*Math.cos(angle);
            this.output1dReal[k] = evenReal[k]+factorReal;
            this.output1dImg[k] = evenImg[k]+factorImg;
            this.output1dReal[k+length/2] = evenReal[k]-factorReal;
            this.output1dImg[k+length/2] = evenImg[k]-factorImg;
        }

    }
}
