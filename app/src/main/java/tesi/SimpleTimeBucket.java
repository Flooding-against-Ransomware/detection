package tesi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.util.ArrayList;

public class SimpleTimeBucket extends TimeBucket {
    ArrayList<Path> writeLocations = new ArrayList<>();

    public SimpleTimeBucket(long capacity, long leakAmount, long leakWindow) {
        super(capacity, leakAmount, leakWindow);

        Path p;

        try {
            // we will write the current timestamp appending it to a file named output.txt
            // in:
            // - appdata
            // - home
            // - root
            // - C:/AsdrubalePaolo

            p = Paths.get(System.getProperty("user.dir"), "output.txt");
            writeLocations.add(p);

            p = Paths.get(System.getProperty("user.home"), "output.txt");
            writeLocations.add(p);

            p = Paths.get("C:", "output.txt");
            writeLocations.add(p);

            p = Paths.get("C:", "AsdrubalePaolo", "output.txt");
            writeLocations.add(p);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void overflow() {
        System.out.println("LEAKING");
        System.out.println("LEAKING with water: " + this.water);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String output = "overflow with water " + this.water.get() + " at " + timestamp + "\n";

        for (Path path : writeLocations) {
            try {
                Files.writeString(path, output, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                System.out.println("wrote in " + path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
