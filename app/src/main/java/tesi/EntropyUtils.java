package tesi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class EntropyUtils {
    public static List<Double> defaultRandomEntropies = generateRandomEntropies(40);

    public static double calculateEntropy(byte[] data) {
        // Count the frequency of every byte and map it
        Map<Byte, Integer> byteCountMap = new HashMap<>();

        for (byte b : data) {
            byteCountMap.put(b, byteCountMap.getOrDefault(b, 0) + 1);
        }

        // Calculate entropy
        double entropy = 0.0;
        int totalBytes = data.length;
        
        for (int count : byteCountMap.values()) {
            double probability = (double) count / totalBytes;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        
        return entropy;
    }

    public static List<Double> calcArrayEntropies(byte[] data, int size){
        List<Double> entropies = new ArrayList<Double>();
        for(int i = 8; i <= size; i += 8){
            byte[] fragment = Arrays.copyOf(data, i);
            double entropy = calculateEntropy(fragment);
            entropies.add(entropy);
        }

        return entropies;
    }

    public static List<Double> generateRandomEntropies(int size){
        byte[] rBytes = new byte[size];
        
        try {
            SecureRandom.getInstanceStrong().nextBytes(rBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return calcArrayEntropies(rBytes, size);
    }

    public static double calcTrapezoidArea(double y1, double y2){
        return y1 * y2 * 4;
    }

    public static double calcCurveArea(List<Double> entropies){
        if(entropies.size() < 2){
            System.err.println("not enough entropies");
            return 0;
        }

        double area = 0;
        for (int i = 0; i < entropies.size() - 1; i++) {
            area += calcTrapezoidArea(entropies.get(i), entropies.get(i + 1)); 
        }

/*         System.out.println("area1");
        System.out.println(area); */

        return area;
    }

    
    public static List<Double> readFileEntropies(Path path) throws IOException {
        if(!Files.isRegularFile(path)){
            throw new IOException(path + " is not a regular file");
            //return new ArrayList<Double>();
        }

        InputStream stream = Files.newInputStream(path);
        byte[] fileStart = stream.readNBytes(40);
        stream.close();

        List<Double> entropies = calcArrayEntropies(fileStart, 40);
        return entropies;

    }

    public static List<Double> offsetRandom(List<Double> entropies, List<Double> randomEntropies){
        return subtractEntropiesList(entropies, randomEntropies);
    }

    
    public static List<Double> subtractEntropiesList(List<Double> ents1, List<Double> ents2){
        List<Double> offset = new ArrayList<Double>();

        if (ents1.size() != ents2.size()){
            System.err.println("the lists have different sizes!");
            System.out.println("ents1: " + Arrays.toString(ents1.toArray()));
            System.out.println("ents2: " + Arrays.toString(ents2.toArray()));
            return null;
        }
        
        Iterator<Double> e1Iterator = ents1.iterator();
        Iterator<Double> e2Iterator = ents2.iterator();

        while (e1Iterator.hasNext() && e2Iterator.hasNext()) {
            offset.add(e1Iterator.next() - e2Iterator.next());
        }

        /*
        System.out.println("ent");
        System.out.println(ents1);
        System.out.println(offset);
        System.out.println(ents2);
        System.out.println("ent");*/

        return offset;
    }

    public static double trapezoidRule(List<Double> entropies){
        double result = 0;

        if (entropies.size() == 0) {
            System.err.println("wtf");
            return 0;
        }

        // add f(a) and f(b)
        double fa = entropies.get(0);
        double fb = entropies.get(entropies.size() - 1);
        result += (fa + fb);

        // 2*sum of every f(x) excluding borders
        for (int i = 1; i < entropies.size() - 1; i++) {
            result += entropies.get(i) * 2;
        }

        // result now must be multiplied for h/2, but h=8 so multiply by 4 directly
        return result * 4;
    }

    public static double calcCurveArea2(List<Double> entropies){
        //System.out.println(entropies);
        double result = 0;

        if (entropies.size() == 0) {
            System.err.println("wtf");
            return -19;
        }

        double fa = entropies.get(0);
        double fb = entropies.get(entropies.size() - 1);
        result += (fa + fb);
        //System.out.println(result);
        //System.out.println("^fa fb");

        for (int i = 1; i < entropies.size() - 1; i++) {
            result += entropies.get(i) * 2;
        }

        return result * 4;
    }


    public static double calcPathArea(Path path) throws IOException{
        return calcPathArea(path, defaultRandomEntropies);
    }



    public static double calcPathArea(Path path, List<Double> randomEntropies) throws IOException {
        long result = 0;
        List<Double> ents;
        //try {
            ents = readFileEntropies(path);
            ents = offsetRandom(randomEntropies, ents);

            
            result = (long) calcCurveArea2(ents);
            //long result2 = (long) calcCurveArea2(ents);
            

            //result = result2;
            //System.out.println("res");
            //System.out.println(result);
            //System.out.println(result2);
            //System.out.println("res");
        /*} catch (IOException e) {
            e.printStackTrace();
        }*/
        
        return result;
    }
}
