package tesi;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.Set;

import tesi.Watcher.Listener;

public class PathHandler {
    public String name;

    // Bucket used for this set of path
    public final AbstractBucket bucket;

    public final List<Double> randomEntropies;
    private final int bytes = 40;

    // set of paths to watch
    private final Watcher watcher;

    // helper default kinds of events to register
    public final static Kind<?> [] defaultKinds = { ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE};

    public PathHandler(Set<Path> paths, AbstractBucket bucket) throws IOException{
        this.bucket = bucket;
        this.randomEntropies = EntropyUtils.generateRandomEntropies(bytes);
        this.watcher = new Watcher(this);

        for(Path dir : paths){
            this.watcher.register(defaultKinds, dir);
        }
    }

    public void setListener(Listener listener){
        this.watcher.setListener(listener);
    }

    public boolean addWater(long addition, int i, int j, int k){
        return this.bucket.addWater(addition);
    }

    public void leak(long unit){
        this.bucket.leak(unit);
    }

    public void run(){
        watcher.run();
    }

    public void shutdown() {
        watcher.shutdown();
    }
}