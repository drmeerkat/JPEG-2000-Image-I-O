package FourierTrans;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {
    private static int width;
    private static int height;
    private static BufferedImage image;

    public static void main(String args[]){
        String imagePath = "./lenatest.jpg";
        readImage(imagePath, true);

        // you can choose fft/dct/fdct/dft for trans1d "type" param
        // and fft/dct(using fdct1d actually)/dft for trans2d

//        experiment1d("dft");
//        experiment1d("fft");
//        experiment1d("dct");
//        experiment1d("fdct");
//        experiment2d("fft");
        experiment2d("dct");
//        zigzagTest();
//        codingTest();
        compressTest();
//        jpegIOTest(imagePath);
    }

    private static void genSmall(){
        double[][] tmp = grayToArray(image, height, width);
        double[][] tmp2 = new double[8][8];
        for (int i=0;i<8;i++){
            System.arraycopy(tmp[i], 256, tmp2[i], 0, 8);
        }
        saveGrayJpg(arrayToGray(tmp2, 8, 8), "small");
    }

    private static void experiment1d(String type){
        double[] test = {1.,2.,3.,4., 1.,2.,3.,4.};
        TransformerRaw trans = new TransformerRaw(type);
        trans.trans1d(test, new double[8], 8, false);
        double[] real = trans.get1dReal();
        double[] img = trans.get1dImg();
        trans.trans1d(real, img, 8, true);
        System.out.println("The "+type+" result is: ");
        for (double i: trans.get1dReal()){
            System.out.println(" "+i);
        }
    }

    private static void experiment2d(String type) {
        TransformerRaw trans = new TransformerRaw(type);

        double[][] rawImage;
        double[][] fftReal;
        double[][] fftImg;
        double[][] ifftReal;

        rawImage = grayToArray(image, height, width);
        // -1^(x+y)*image[x][y]
        if (type.equals("fft"))
            trans.trans2d(centralize(rawImage), new double[height][width], height, width, false);
        else
            trans.trans2d(rawImage, new double[height][width], height, width, false);
        fftReal = trans.get2dReal();
        fftImg = trans.get2dImg();
        trans.trans2d(fftReal, fftImg, height, width, true);
        ifftReal = trans.get2dReal();

        double[][] spectrum = new double[height][width];
        if (type.equals("dct")){
            for (int x = 0; x < height; x++) {
                for (int y = 0; y < width; y++) {
                    spectrum[x][y] = 10*(Math.abs(fftReal[x][y]));
//                    spectrum[x][y] = 10*Math.log(Math.abs(fftReal[x][y]));
                    if (x >= 0 && x < 8
                        && y>= 0 && y < 8){
                        System.out.print(String.format(" %.3f",fftReal[x][y]));
                    }
                }
                if (x >= 0 && x < 8){
                    System.out.println(" ");
                }
            }
        }
        else{
            for (int x = 0; x < height; x++) {
                for (int y = 0; y < width; y++) {
                    spectrum[x][y] = 15*Math.log(Math.sqrt(Math.pow(fftReal[x][y], 2)+Math.pow(fftImg[x][y], 2)));
    //                fftResult[x][y] = (1+Math.random())*fftResult[x][y];//类似负片效果
    //                fftResult[x][y] = (1+Math.random()-0.5)*fftResult[x][y];//类似负片效果
    //                fftResult[x][y] = (1)*fftResult[x][y]+Math.random()*5;

                }
            }
        }
//        showGrayImage(arrayToGray(rawImage), "Raw");
        showGrayImage(arrayToGray(spectrum, height, width), type + " Frequency");
        if (type.equals("dct"))
            showGrayImage(arrayToGray(ifftReal, height, width), type + " Reconstructed");
        else
            showGrayImage(arrayToGray(centralize(ifftReal), height, width), type + " Reconstructed");
//        saveImage(arrayToGray(centralize(ifftResult)), type+"_reconstructed");
//        saveImage(arrayToGray(spectrum), type+"_spectrum");

    }

    private static void compressTest() {
        double[][] rawImage;
        double[][] fftReal;
        double[][] fftImg;
        double[][] ifftReal;
        TransformerRaw trans = new TransformerRaw("jpeg");
        Compression comp = new Compression();
        rawImage = grayToArray(image, height, width);

        //dct
        trans.trans2d(rawImage, new double[height][width], height, width, false);
        fftReal = trans.get2dReal();
        fftImg = trans.get2dImg();

        comp.zigzagFull(fftReal, true);//need to filter zero out
        double[] flat = comp.getFlatOut();
        String encode = comp.encode(flat, 64);
        //TODO: JFIF format I/O
        double[] recflat = comp.decode(encode, 64,width*height);// need to patch zero at last
        comp.inverseZigzagFull(recflat, height, width);
        fftReal = comp.getRecImg();

        trans.trans2d(fftReal, fftImg, height, width, true);
        ifftReal = trans.get2dReal();

        double[][] spectrum = new double[height][width];
        for (int x = 0; x < height; x++) {
            for (int y = 0; y < width; y++) {
                spectrum[x][y] = 10*(Math.abs(fftReal[x][y]));
                    spectrum[x][y] = 10*Math.log(Math.abs(fftReal[x][y]));
                if (x >= 0 && x < 8
                        && y>= 0 && y < 8){
                    System.out.print(String.format(" %.3f",fftReal[x][y]));
                }
            }
            if (x >= 0 && x < 8){
                System.out.println(" ");
            }
        }
        showGrayImage(arrayToGray(rawImage, height, width), "Raw");
        showGrayImage(arrayToGray(spectrum, height, width), "jpeg" + " Frequency");
        showGrayImage(arrayToGray(ifftReal, height, width), "jpeg" + " Reconstructed");

    }

    private static void zigzagTest(){
        Compression comp = new Compression();
        double[][] matrix = {
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}};
        double[] result = comp.zigzagTraversal(matrix);
        for (double i:result){
            System.out.print(i+" ");
        }
        System.out.println(" ");
        double[][] recoverd = comp.inverseZigzag(result, 3, 3);
        for (int i=0;i<3;i++){
            for (int j=0;j<3;j++){
                System.out.print(recoverd[i][j]+" ");
            }
            System.out.println(" ");
        }
    }

    private static void codingTest(){
        boolean pass = true;
        Compression comp = new Compression();
        double[] test =      {-511,57,45,0,0,0,0,23,0, -30,-8,0,0,1,0,0,0,0, -2,12,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0};
        double[] basictest = {-511,57,45,0,0,0,0,23,0,-30,-8,0,0,1,0,0,0,0,0};
        double[] fuck = {0,0,0,0,0,0};
        String result = comp.encode(test, 9);//the coded binary string
        double[] recTest = comp.decode(result, 9, 36);
        for (int i = 0;i<recTest.length;i++){
            System.out.print(String.format("%.0f ",recTest[i]));
            if (recTest[i] != test[i])
                pass = false;
        }
        if (pass)
            System.out.println("\nPass!");
    }

    private static void jpegIOTest(String imagePath){
        JPEGByteIO jpegIO = new JPEGByteIO(imagePath);
        jpegIO.readByte();

        TransformerRaw trans = new TransformerRaw("jpeg");
        Compression comp = new Compression();
        comp.setAcMap(jpegIO.getAC());
        comp.setDcMap(jpegIO.getDC());
        comp.setQuantMatrix(jpegIO.getQuat());

        double[] recflat = comp.decode(jpegIO.getBitImg(), 64, width*height);
        comp.inverseZigzagFull(recflat, height, width);
        double[][] fftReal = comp.getRecImg();
        trans.trans2d(fftReal, new double[height][width], height, width, true);
        double[][] ifftReal = trans.get2dReal();

        showGrayImage(image, "Raw");
        showGrayImage(arrayToGray(ifftReal, height, width), "jpeg" + " Reconstructed");
    }

    private static void readImage(String pathName, boolean isGray) {
        File f;
        try {
            f = new File(pathName);
            image = ImageIO.read(f);
            width = image.getWidth();
            height = image.getHeight();

            System.out.println("width=" + width + ",height=" + height + ".");
            System.out.println("minx=" + image.getMinX() + ",miny=" + image.getMinY() + ".");
            System.out.println("Reading Complete");
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    private static double[][] grayToArray(BufferedImage image, int height, int width){
        if (image.getHeight() != image.getWidth()){
            System.out.println("The input must be a squared image with length of power of 2");
        }
        double[][] result = new double[image.getWidth()][image.getHeight()];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[x][y] = image.getRaster().getPixel(x, y, new double[1])[0];
            }
        }

        return result;
    }

    private static double[][] centralize(double[][] image){
        double[][] result = new double[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result[x][y] = Math.pow(-1, x+y)*image[x][y];
            }
        }

        return result;
    }

    private static BufferedImage arrayToGray(double[][] pixels, int height, int width){
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster er = image.getRaster();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double[] tmp = {pixels[x][y]};
                er.setPixel(x, y, tmp);
            }
        }

        return image;
    }

    private static void showGrayImage(BufferedImage image, String title){
        EventQueue.invokeLater(() -> {
            new Histogram(image, title+" Histogram").display(true);
        });
    }

    private static void saveImage(BufferedImage image, String name){
        try {
            ImageIO.write(image, "png", new File(name + ".png"));

        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

    private static void saveGrayJpg(BufferedImage image, String name){
        try {
            ImageIO.write(image, "jpg", new File(name + ".jpg"));

        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }
}
