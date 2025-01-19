/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package tesi;

import java.io.IOException;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static tesi.EntropyUtils.*;

public class App {

    static ArrayList<Path> writeLocations = new ArrayList<>();
    public static String home = System.getProperty("user.home");

    /**
     * delete output files before execution to avoid adding the data on top of previous data
     */
    public static void deleteWriteFiles() {
        Path p;

        p = Paths.get(System.getProperty("user.dir"), "output.txt");
        writeLocations.add(p);

        p = Paths.get(System.getProperty("user.home"), "output.txt");
        writeLocations.add(p);

        // p = Paths.get("C:", "output.txt");
        // writeLocations.add(p);

        p = Paths.get("C:", "AsdrubalePaolo", "output.txt");
        writeLocations.add(p);

        for (Path path : writeLocations) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void analyzeFile(Path path) {
        double area;
        try {
            area = calcPathArea(path);
            Path parent = path.getParent();
            Path txt = parent.resolve("output.txt");
            String output = path + " has area of " + area + "\n";
            System.out.println("writing in " + txt);
            Files.writeString(txt, output, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            // uncomment this to reset those files, for example for a complete recalculation
            // Files.writeString(txt, "", StandardOpenOption.WRITE,
            // StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void analyzeFiles() {
        Path base = Paths.get(home, "Desktop\\files\\data");

        try (Stream<Path> walk = Files.walk(base)) {
            walk.filter(Files::isRegularFile)
                    .forEach(f -> analyzeFile(f));
        } catch (Exception e) {
            e.printStackTrace();
            ;
        }
    }


    public static long simpleListener(Set<Path> deleted, Set<Path> created, Set<Path> modified) {
        // for every file compute the area and if the area is < 16 add ((16 - area)/2)
        // to the water to add (the return value)
        long result = 0;

        Set<Path> combined = new LinkedHashSet<Path>();

        combined.addAll(created);
        combined.addAll(modified);

        for (Path path : combined) {
            try {
                double area = calcPathArea(path);
                if (area < 16) {
                    result += 16 - area;

                }
            } catch (Exception e) {
                // do not update result: area cannot be calculated (maybe deleted file)
            }
        }

        result = result / 2;

        return result;
    }

    public static PathHandler genSimpleHandler(String loc) throws IOException {
        // look only in path home/loc, recursively
        Path target = Paths.get(home, loc);
        // the bucket has a capacity of 200 and leaks 30 every 15 seconds
        PathHandler handler = new PathHandler(Collections.singleton(target), new SimpleTimeBucket(200, 30, 15 * 1000));
        handler.name = loc;
        handler.setListener(App::simpleListener);
        return handler;
    }

    public static PathHandler genFileHandler(String loc) throws IOException {
        Path target = Paths.get(home, loc);
        // the bucket has a capacity of 300 and leaks 35 every 100 files processed
        PathHandler handler = new PathHandler(Collections.singleton(target), new SimpleFileBucket(300, 35, 100));
        handler.name = loc;

        handler.setListener((Set<Path> deleted, Set<Path> created, Set<Path> modified) -> {
            // this listener adds 1 for every file with an area < 5
            long result = 0;
            Set<Path> combined = new LinkedHashSet<Path>();

            // combined.addAll(deleted);
            combined.addAll(created);
            combined.addAll(modified);

            for (Path path : combined) {
                try {
                    double area = calcPathArea(path);
                    if (area < 5) {
                        result += 1;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return result;
        });

        return handler;
    }

    public static void main(String[] args) throws IOException, InterruptedException, NoSuchAlgorithmException {
        deleteWriteFiles();

        ArrayList<PathHandler> handlers = new ArrayList<>();
        handlers.add(genSimpleHandler("Desktop"));
        handlers.add(genSimpleHandler("Documents"));
        handlers.add(genSimpleHandler("Pictures"));
        handlers.add(genSimpleHandler("Videos"));
        //handlers.add(genSimpleHandler("Appdata"));
        handlers.add(genFileHandler("Downloads"));
     
        for (PathHandler pathHandler : handlers) {
            new Thread(pathHandler::run).start();
        }

        Thread.sleep(((long) (15 * 60 * 1000)));

        for (PathHandler pathHandler : handlers) {
            pathHandler.shutdown();
        }

        // sleep a few seconds to wait everything stopped
        Thread.sleep(((long) (5 * 1000)));


        // print the level at which the handlers finished
        for (PathHandler pathHandler : handlers) {
            var water = pathHandler.bucket.getWater();
            String msg = pathHandler.name + " finished with water level " + water;
            System.out.println(msg);
        }

        System.out.println("end");
        // System.exit(0);
        return;
    }
}
