package FourierTrans;

import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JPEGByteIO {

    private enum SectionType{
        TEM,
        SOF0, SOF1, SOF2, SOF3, SOF5, SOF6, SOF7, SOF9, SOF10, SOF11, SOF13, SOF14, SOF15,
        DHT, JPG, DAC,
        RST0, RST1, RST2, RST3, RST4, RST5, RST6, RST7,
        SOI, EOI, SOS, DQT, DNL, DRI, DHP, EXP,
        APP0, APP15, JPG0, JPG13, COM, NOP,
    };

    static HashMap<Integer, SectionType> mapSectionType = new HashMap<Integer, SectionType>();

    static {
        mapSectionType.put(0x01, SectionType.TEM);
        mapSectionType.put(0xc0, SectionType.SOF0);//start of frame
        mapSectionType.put(0xc1, SectionType.SOF1);//dito

        mapSectionType.put(0xc2, SectionType.SOF2);//unsupported
        mapSectionType.put(0xc3, SectionType.SOF3);

        mapSectionType.put(0xc5, SectionType.SOF5);
        mapSectionType.put(0xc6, SectionType.SOF6);
        mapSectionType.put(0xc7, SectionType.SOF7);

        mapSectionType.put(0xc9, SectionType.SOF9);
        mapSectionType.put(0xca, SectionType.SOF10);
        mapSectionType.put(0xcb, SectionType.SOF11);

        mapSectionType.put(0xcd, SectionType.SOF13);
        mapSectionType.put(0xce, SectionType.SOF14);
        mapSectionType.put(0xcf, SectionType.SOF15);

        mapSectionType.put(0xc4, SectionType.DHT);//Huffman Tabel
        mapSectionType.put(0xc8, SectionType.JPG);//decoding error
        mapSectionType.put(0xcc, SectionType.DAC);

        mapSectionType.put(0xd0, SectionType.RST0);//used for resync, ignored
        mapSectionType.put(0xd1, SectionType.RST1);
        mapSectionType.put(0xd2, SectionType.RST2);
        mapSectionType.put(0xd3, SectionType.RST3);
        mapSectionType.put(0xd4, SectionType.RST4);
        mapSectionType.put(0xd5, SectionType.RST5);
        mapSectionType.put(0xd6, SectionType.RST6);
        mapSectionType.put(0xd7, SectionType.RST7);

        mapSectionType.put(0xd8, SectionType.SOI);//start of Image
        mapSectionType.put(0xd9, SectionType.EOI);//end of Image
        mapSectionType.put(0xda, SectionType.SOS);//Start of Scan
        mapSectionType.put(0xdb, SectionType.DQT);//Quantization table
        mapSectionType.put(0xdc, SectionType.DNL);
        mapSectionType.put(0xdd, SectionType.DRI);//Restart Interval
        mapSectionType.put(0xde, SectionType.DHP);
        mapSectionType.put(0xdf, SectionType.EXP);

        mapSectionType.put(0xe0, SectionType.APP0);//JFIF APP0 segment marker
        mapSectionType.put(0xef, SectionType.APP15);

        mapSectionType.put(0xf0, SectionType.JPG0);
        mapSectionType.put(0xfd, SectionType.JPG13);
        mapSectionType.put(0xfe, SectionType.COM);//comment

        mapSectionType.put(0xff, SectionType.NOP);
    };

    private class holderAPP0{
        private int length;
        private byte unitDense;
        private byte[] xDensity = new byte[2];
        private byte[] yDensity = new byte[2];
        private byte thumbnailWidth;
        private byte thumbnailHeight;
        private byte[] thumbnailBytes;
    }

    private class holderDQT{
        private int length;
        private byte qtInfo;
        private byte[] data;
        private int cntQT;
        private int precision;
        private double[][] quant = new double[8][8];

        private void getInfo(){
            this.cntQT = (qtInfo & 0xf) + 1;
            this.precision = (((qtInfo >> 4) & 0xf) == 0)? 0 : 1;//8 bit or 16 bit
        }

        private void getQuat(){
            double[] input = new double[this.data.length];
            for (int i=0;i< input.length;i++){
                input[i] = this.data[i] & 0xff;
            }
            this.quant = Compression.inverseZigzag(input, 8, 8);
        }

        public String toString(){
            return String.format("qtINFO: 0x%02x precison:%d cntQT:%d",qtInfo, precision, cntQT);
        }
    }

    private class holderSOF0{
        private int length;
        private byte dataPrecision;//usually 8 bits/sample
        private byte[] height = new byte[2];
        private byte[] width = new byte[2];
        private int numContents;// 1 for gray scale, 3/4 for YCbCr,YIQ / CMYK
        private byte[] componentData;
        private int[] componentID;
        private int[] verSampFactors;
        private int[] horSampFactors;
        private int[] quantNum;

        private void initialComponent(){
            componentData = new byte[numContents*3];
            componentID = new int[numContents];
            verSampFactors = new int[numContents];
            horSampFactors = new int[numContents];
            quantNum = new int[numContents];
        }
    }

    private class holderDHT{
        private int length;
        private byte htINFO;
        private int htNum, htType, cntCode;
        private byte[] numSym;
        private byte[] symbols;
        private Map<String, String> map = new HashMap<>();
        private Map<String, String> reMap;

        private void getInfo(){
            assert ((this.htINFO >> 5) & 0x7) == 0;
            this.htNum = this.htINFO & 0xf;
            this.htType = (this.htINFO >> 4) & 0x1;
        }

        private int cntSymbols(){
            int sum = 0;
            for (int i = 0;i<this.numSym.length;i++){
                sum += (this.numSym[i] & 0xff);
            }
            assert sum <= 256;
            this.cntCode = sum;
            return sum;
        }

        private void constructMap(){
            //depend on this dht's type, 0 for dc, 1 for ac
            int code = 0, cnt = 0, lastLen = 0;
            String tmpCode, finalCode, acCode="";
            for (int i=0;i<this.numSym.length;i++){
                // there is numSym[i] codes with length (i+1)
                for (int j=0;j<this.numSym[i];j++){
                    // patch leading zero and afterwards zeros
                    tmpCode = this.forPatch(Compression.bitInteger(code), lastLen - Compression.bitInteger(code).length());
                    finalCode = this.patch(tmpCode, (i+1)-tmpCode.length());
                    lastLen = finalCode.length();
                    if (this.htType == 0){//dcmap
                        this.map.put(Integer.toString(this.symbols[cnt] & 0xff), finalCode);
                    }
                    else{//acmap
                        acCode = Integer.toString((this.symbols[cnt] >> 4) & 0xf);
                        acCode += "/";
                        acCode += Integer.toString(this.symbols[cnt] & 0xf);
                        this.map.put(acCode, finalCode);
                    }
                    code = this.strPositiveInterger(finalCode);
                    code++;
                    cnt++;
                }
            }
            this.reMap = this.map.entrySet().stream()
                                 .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        }

        private int strPositiveInterger(String binary){
            int result = 0;
            for (int i=0;i<binary.length();i++){
                result += Integer.parseInt(binary.substring(i, i+1))*Math.pow(2, binary.length()-i-1);
            }
            return result;
        }

        private String patch(String input, int numZero){
            if (numZero <= 0){
                return input;
            }
            return input + new String(new char[numZero]).replace("\0", "0");
        }

        private String forPatch(String input, int numZero){
            if (numZero <= 0){
                return input;
            }
            return new String(new char[numZero]).replace("\0", "0") + input;
        }
    }

    private class holderSOS{
        private int length;
        private int scanCnt;
        private byte[] compINFO;
        private int[] compID;
        private int[] compDC;
        private int[] compAC;

        private void getInfo(){
            compID = new int[this.scanCnt];
            compDC = new int[this.scanCnt];
            compAC = new int[this.scanCnt];
            for (int i=0;i<this.scanCnt;i++){
                compID[i] = this.compINFO[i*2];// (1=Y, 2=Cb, 3=Cr, 4=I, 5=Q),
                compAC[i] = this.compINFO[i*2+1] & 0xf;
                compDC[i] = (this.compINFO[i*2+1] >> 4) & 0xf;
            }
        }
    }
    private double[][] image;
    private double width, height;
    private byte[] rawData, compressedByteImg;
    private int curPtr;
    private String bitImg;

    private holderAPP0 app = new holderAPP0();
    private holderSOF0 sof = new holderSOF0();
    private ArrayList<holderDQT> arrdqt = new ArrayList<>();
    private ArrayList<holderDHT> arrdht = new ArrayList<>();
    private holderSOS sos = new holderSOS();

    public JPEGByteIO(String imgPath){
        this.rawData = read(imgPath);
        this.curPtr = 0;
        this.bitImg = "";
    }

    public Map getDC(){
        return this.arrdht.get(0).map;
    }

    public Map getAC(){
        return this.arrdht.get(1).map;
    }

    public double[][] getQuat(){
        return this.arrdqt.get(0).quant;
    }

    public String getBitImg(){
        return this.bitImg;
    }

    public void readByte(){
        SectionType type;
        boolean beginScan = false;
        while (this.available() > 1){
            // The remaining byte cnt at the loop begining must be a multiple of 2
            if (beginScan){
                this.compressedByteImg = this.getBytes(this.rawData.length - this.curPtr - 2);
                for (int i=0;i<this.compressedByteImg.length;i++){
                    this.bitImg += Integer.toBinaryString(this.compressedByteImg[i] & 0xff);
                }
            }
            byte firstByte = this.nextByte();
            byte secByte = this.nextByte();
            type = this.getSectionType(firstByte, secByte);
//            System.out.println(type);
            switch (type){
                case SOI:
                    System.out.println("Start of Image!");
                    break;
                case APP0:
                    this.readAPP0();
                    break;
                case DQT:
                    this.readDQT();
                    break;
                case SOF0:
                    this.readSOF0();
                    break;
                case DHT:
                    this.readDHT();
                    break;
                case SOS:
                    beginScan = true;
                    this.readSOS();
                    break;
                case EOI:
                    System.out.println("End of Image!");
                    break;
                case NOP:
                    continue;
                case SOF2:
                    jumpOverSection();
                    break;
                case DRI:
                    jumpOverSection();
                    break;
                case COM:
                    jumpOverSection();
                    break;
                default:
                    jumpOverSection();
                    break;
            }
        }
    }

    private void readSOS(){
        this.sos.length = this.getSectionLen(this.nextByte(), this.nextByte());
        this.sos.scanCnt = this.nextByte();
        this.sos.compINFO = this.getBytes(this.sos.scanCnt*2);
        this.sos.getInfo();
        this.getBytes(3);//skip over three bytes
    }

    private void readDHT(){
        holderDHT dht = new holderDHT();
        dht.length = this.getSectionLen(this.nextByte(), this.nextByte());
        dht.htINFO = this.nextByte();
        dht.numSym = this.getBytes(16);
        dht.getInfo();
        dht.symbols = this.getBytes(dht.cntSymbols());
        dht.constructMap();
        this.arrdht.add(dht);
    }

    private void readSOF0(){
        this.sof.length = this.getSectionLen(this.nextByte(), this.nextByte());
        this.sof.dataPrecision = this.nextByte();
        this.sof.height = this.getBytes(2);
        this.sof.width = this.getBytes(2);
        this.sof.numContents = this.nextByte();
        this.sof.initialComponent();
        this.sof.componentData = this.getBytes(this.sof.numContents*3);
        for (int i = 0;i<this.sof.numContents;i++){
            this.sof.componentID[i] = this.sof.componentData[i*3];
            this.sof.verSampFactors[i] = 0xf & this.sof.componentData[i*3+1];
            this.sof.horSampFactors[i] = 0xf & (this.sof.componentData[i*3+1] >> 4);
            this.sof.quantNum[i] = this.sof.componentData[i*3+2];
        }

    }

    private void readDQT(){
        holderDQT dqt = new holderDQT();
        dqt.length = this.getSectionLen(this.nextByte(), this.nextByte());
        dqt.qtInfo = this.nextByte();
        dqt.getInfo();
        dqt.data = this.getBytes(64 * (dqt.precision+1));
        dqt.getQuat();
        System.out.println("DQT readin\n"+dqt);
        this.arrdqt.add(dqt);
    }

    private void readAPP0(){
        this.app.length = this.getSectionLen(this.nextByte(), this.nextByte());
        byte[] identifier = this.getBytes(5);
        byte tmp;
        if (identifier[0]==0x4a
                &&identifier[1]==0x46
                &&identifier[2]==0x49
                &&identifier[3]==0x46
                &&identifier[4]==0x00
                &&this.app.length>=16){
            System.out.println("This is a JFIF segment");
        }
        else{
            System.out.println("This isn't a JFIF segment");
            this.backwards(7);
            this.jumpOverSection();
        }
        if (this.nextByte() != 0x01){

        }
        tmp = this.nextByte();
        if (tmp < 0 && tmp > 2){

        }
        this.app.unitDense = this.nextByte();
        this.app.xDensity = this.getBytes(2);
        this.app.yDensity = this.getBytes(2);
        this.app.thumbnailWidth = this.nextByte();
        this.app.thumbnailHeight = this.nextByte();
        this.app.thumbnailBytes = this.getBytes(this.app.thumbnailHeight*this.app.thumbnailWidth*3);
    }

    private byte nextByte(){
        if (this.curPtr < this.rawData.length){
            return this.rawData[this.curPtr++];
        }
        else{
            return -1;
        }
    }

    private byte[] getBytes(int num){
        byte[] result = new byte[1];
        if (num != 0 && this.curPtr + num <= this.rawData.length){
            result = Arrays.copyOfRange(this.rawData, this.curPtr, num+this.curPtr);
            this.curPtr += num;
        }
        return result;
    }

    private void backwards(int num){
        if (this.curPtr - num >= 0){
            this.curPtr -= num;
        }
        //else do nothing
    }

    private int available(){
        return this.rawData.length - this.curPtr;
    }

    private void jumpOverSection(){
        int len = (this.getSectionLen(this.nextByte(), this.nextByte()) - 2);
        this.curPtr += len;
    }

    private SectionType getSectionType(byte firstByte, byte secByte){
        if (0xff != (firstByte&0xff) || !mapSectionType.containsKey(secByte&0xff)){
            return SectionType.NOP;
        }
        return mapSectionType.get(secByte&0xff);
    }

    private int getSectionLen(byte firstByte, byte secByte){
        int result = (((firstByte << 8)&0xffff)+(secByte&0xff)) & 0xffff;
        return result;
    }

    static public byte[] read(String bFile){
        byte[] result = new byte[1];
        try{
            result = Files.readAllBytes(Paths.get(bFile));
        }catch (Exception e){
            System.out.println("IO Exception: "+e);
        }
        return result;
    }

    public byte[] getRawData(){
        return this.rawData;
    }
}
