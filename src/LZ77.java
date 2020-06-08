import java.util.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

public class Assignment_2x0 {
    public static void main(String[] args) throws IOException {
        if ( args.length > 0) {         // specify input file
            String inputFile = args[0];
            if (inputFile.contains(".")){
                String ext = inputFile.substring(inputFile.lastIndexOf(".") + 1);  
                String str = "";
                if (ext.equals("png") || ext.equals("jpg")){
                    str = convertToString(inputFile);  
                }else{
                    str = readFile("../data/"+ inputFile, Charset.forName("UTF-8")); 
                }
                int sbSize = 16; //Search buffer size (bits)
                int labSize = 8; //Look-ahead buffer size (bits)

                long startWriteTime = System.nanoTime();
                writeUncompressedAlgorithm(str,"original.bin", 8);
                long totalWriteTime = System.nanoTime() - startWriteTime;
                System.out.println("Write Time Uncompressed: " + (double)totalWriteTime/1000000 + "ms ");

                long startReadTime = System.nanoTime();
                readUncompressedAlgorithm("original.bin");
                long totalReadTime   = System.nanoTime() - startReadTime;
                System.out.println("Read Time Uncompressed: " + (double)totalReadTime/1000000 + "ms ");

                startWriteTime = System.nanoTime();
                writeCompressionAlgorithm(str, "compressed.bin", sbSize, labSize);
                totalWriteTime = System.nanoTime() -startWriteTime;
                System.out.println("Write Time: " + (double)totalWriteTime/1000000 + "ms ");

                startReadTime = System.nanoTime();
                readCompressionAlgorithm("compressed.bin", sbSize, labSize);
                totalReadTime = System.nanoTime() - startReadTime;
                System.out.println("Read Time: " + (double)totalReadTime/1000000 + "ms "); 
            }else{
                System.out.println("Please specify the extension type of the input file.");
                System.exit(1);
            }
        }else{
            System.out.println("Please specify input file.");
            System.exit(1);
        }
    }
    static String readFile(String path, Charset encoding) throws IOException  {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }
    public static String convertToString(String filename) {
        String strImg = "";
        File file = new File("../data/" + filename);
        try (FileInputStream image = new FileInputStream(file)) {
            byte byteImg[] = new byte[(int) file.length()];
            image.read(byteImg);
            strImg = Base64.getEncoder().encodeToString(byteImg);
        } catch (FileNotFoundException e) {
            System.out.println("Image not found: " + e);
        } catch (IOException e) {
            System.out.println("Error reading image: " + e);
        }
        return strImg;
    }
    public static String charToBinary(Character character, int sbSize) {
        char[] binVal = new char[sbSize];
        for (int i =0; i < sbSize; i++){
            binVal[i] = '0';
        }
        int binChar = (int)character;                       //convert char to ascii int
        for (int i = 0; i < sbSize; i++){
            if (( binChar & 1) == 1) {                      //bitwise-AND i.e. 65: 0010 0001 & 0000 0001 = 0000 0001
                binVal[sbSize - 1 - i] = '1';
            }
            binChar >>= 1;                                  //right-shift bits (divide by 2)
        }
        return new String(binVal);
    }
    public static char binaryToChar(String binStr){
        int totVal = 0;
        for (int i = 0; i < 8; i++){
            int bitVal = binStr.charAt(7 -i) - '0';              // ascii val - 48 = 0 or 1;
            totVal += bitVal << i;                              // if one, totVal += 2 * i
        }
        return (char) totVal;
    }
    public static void writeUncompressedAlgorithm(String str, String filename, int sbSize)throws IOException{
        String binStr = "";
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(binStr);
        for (char character : str.toCharArray()){   // convert each char to binary and write to file
            binStr = charToBinary(character, sbSize);
            writer.append(binStr);
        }
        writer.close();
    }
    public static void readUncompressedAlgorithm(String filename) throws IOException{
        String binStr = usingBufferedReader(filename);
        for (int i =0; i < binStr.length(); i +=8){ //convert each 8bit binary to char
            System.out.print(binaryToChar(binStr.substring(i, i+8)));
        }
        System.out.println("");
    }
    public static void writeCompressionAlgorithm(String str, String filename, int sbSize, int labSize)throws IOException{
        ArrayList<LZ77> encLZ77 = encode(str, sbSize, labSize);      // create list of triplets
        String binStr = "";
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(binStr);

        for (LZ77 item: encLZ77){
            // for each triplet, convert ints to char values, add triplet to binary string and write to file.
            binStr = charToBinary((char)item.getDist(), sbSize) + charToBinary((char)item.getLength(), labSize) +charToBinary(item.getNext(), 8);
            writer.append(binStr);
        }
        writer.close();
    }
    public static void readCompressionAlgorithm(String filename, int sbSize, int labSize) throws IOException{
        String binStr = usingBufferedReader(filename);
        ArrayList<LZ77> encLZ77 = new ArrayList<LZ77>();
        for (int i =0; i < binStr.length(); i +=sbSize + labSize + 8){
            int dist = Integer.parseInt(binStr.substring(i, i+sbSize), 2);      // convert binary straight to int
            int length = Integer.parseInt(binStr.substring(i+sbSize, i+sbSize + labSize), 2);
            char character = binaryToChar(binStr.substring(i+ sbSize + labSize, i+sbSize + labSize + 8));            // convert binary to char using ascii value

            encLZ77.add(new LZ77(dist,length,character));                           // add triplet to array
        }
        String decLZ77 = decode(encLZ77);
        System.out.println(decLZ77);

    }
    public static String usingBufferedReader(String filename) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String str;
        String binStr = "";
        while ((str = reader.readLine()) != null){
            binStr += str;
        }
        return binStr;
    }
    public static ArrayList<LZ77> encode(String str, int sbSize, int labSize){
        ArrayList<LZ77> arrLZ77 = new ArrayList<LZ77>();
        String strSB = "";
        for (int i=0; i< str.length(); i++) {
            String strBW = strSB;
            int intLAB = 0;
            String strLAB = "";  
            try{
                while (strBW.contains(strLAB + str.charAt(i + intLAB)) && intLAB < (int)Math.pow(2, labSize) -1&& intLAB < (int)Math.pow(2, sbSize)-1) {
                    strLAB += str.charAt(i + intLAB);
                    strBW += str.charAt(i + intLAB);
                    intLAB += 1;
                }
                i += intLAB;
                int dist;
                if (intLAB > 0){

                    dist = strBW.length() - strBW.substring(0, strBW.length()-1).lastIndexOf(strLAB) - intLAB;

                }else{
                    dist = strBW.length() - strBW.lastIndexOf(strLAB);
                }
                if (arrLZ77.size() == 158){
                    System.out.println("");
                }
                arrLZ77.add(new LZ77(dist, intLAB, str.charAt(i)));
                if (i >= (int)Math.pow(2, sbSize)-2) {
                    strSB = str.substring(i - (int)Math.pow(2, sbSize) +2, i+1);
                } else {
                    strSB = str.substring(0, i+1);
                }
            }catch (StringIndexOutOfBoundsException e){
                arrLZ77.add(new LZ77(strBW.length() - strBW.substring(0, strBW.length()-1).lastIndexOf(strLAB) - intLAB, intLAB, '-'));
                break;
            }
        }
        return arrLZ77;
    }
    public static String decode(ArrayList<LZ77> arrLZ77){
        String strLZ77 = "";
        String repeatedString;
        for(LZ77 item : arrLZ77){   //for each triplet, add repeated string then next char to strLZ77
            if (item.getLength() !=0){
                int startPos = strLZ77.length() - item.getDist();
                int endPos = startPos + item.getLength();

                if (item.getDist() >= item.getLength()){
                    repeatedString = strLZ77.substring(startPos, endPos);
                    strLZ77 += repeatedString;
                }else{
                    repeatedString = strLZ77.substring(startPos, startPos + item.getDist());
                    strLZ77 += repeatedString;
                    for (int i = item.getDist(); i<item.getLength(); i+= item.getDist()){
                        if (i + item.getDist() < item.getLength()){
                            strLZ77 += repeatedString;
                        }else{
                            strLZ77 += strLZ77.substring(startPos, startPos + item.getLength() - i);

                        }
                    }
                }
            }
            strLZ77 += item.getNext();
        }
        if (strLZ77.charAt(strLZ77.length()-1) == '-'){ // remove '-' from end of string if it exists
            strLZ77 = strLZ77.substring(0, strLZ77.length() - 1);
        }
        return strLZ77;
    }
    public static class LZ77 { // class used to store triplets
        int distance, length;
        char next;

        public LZ77(int distance, int length, char next) {
            this.distance = distance;
            this.length = length;
            this.next = next;
        }

        public int getDist() {
            return distance;
        }

        public int getLength() {
            return length;
        }

        public char getNext() {
            return next;
        }
    }
}
