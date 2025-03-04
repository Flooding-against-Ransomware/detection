package tesi;

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// class adapted from https://stackoverflow.com/a/65251819
public class Watcher implements Runnable {

    private final Set<Path> created = new LinkedHashSet<>();
    private final Set<Path> updated = new LinkedHashSet<>();
    private final Set<Path> deleted = new LinkedHashSet<>();

    private volatile boolean appIsRunning = true;
    // Decide how sensitive the polling is:
    private final int pollmillis = 500;
    private WatchService ws;

    private Listener listener = Watcher::fireEvents;
    private final PathHandler handler;

    @FunctionalInterface
    interface Listener
    {
        public long fileChange(Set<Path> deleted, Set<Path> created, Set<Path> modified);
    }
    
    Watcher(PathHandler handler){
        this.handler = handler;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void shutdown() {
        System.out.println("shutdown()");
        this.appIsRunning = false;
    }

    public void run() {
        System.out.println();
        System.out.println("run() START watch");
        System.out.println();

        try(WatchService autoclose = ws) {

            while(appIsRunning) {

                boolean hasPending = created.size() + updated.size() + deleted.size() > 0;
                //System.out.println((hasPending ? "ws.poll("+pollmillis+")" : "ws.take()")+" as hasPending="+hasPending);

                // Use poll if last cycle has some events, as take() may block
                WatchKey wk = hasPending ? ws.poll(pollmillis,TimeUnit.MILLISECONDS) : ws.take();
                if (wk != null)  {
                    for (WatchEvent<?> event : wk.pollEvents()) {
                         Path parent = (Path) wk.watchable();
                         Path eventPath = (Path) event.context();
                         storeEvent(event.kind(), parent.resolve(eventPath));
                     }
                     boolean valid = wk.reset();
                     if (!valid) {
                         System.out.println("Check the path, dir may be deleted "+wk);
                     }
                }

                //System.out.println("PENDING: cre="+created.size()+" mod="+updated.size()+" del="+deleted.size());

                // This only sends new notifications when there was NO event this cycle:
                if (wk == null && hasPending) {
                    //System.out.println("sending events");

                    // mostly for phobos & ryuk, can be useful regardless in testing
                    try {
                        App.firstFileMod();
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                    // end phobos & ryuk

                    try {
                        long addition = listener.fileChange(deleted, created, updated);
                        boolean result = handler.addWater(addition, deleted.size(), created.size(), updated.size());
                        if(!result){
                            Set<Path> combined = new LinkedHashSet<Path>();
                            combined.addAll(deleted);
                            combined.addAll(created);
                            combined.addAll(updated);

                            var p = combined.stream().collect(Collectors.toList());
                            System.out.println("p: " + Arrays.toString(p.toArray()));
                        }

                    } catch (NullPointerException e) {
                        System.out.println("problem: discarding events");
                        System.out.println(e.getMessage());
                    }
                    deleted.clear();
                    created.clear();
                    updated.clear();
                }
            }
        }
        catch (InterruptedException e) {
            System.out.println("Watch was interrupted, sending final updates");
            fireEvents(deleted, created, updated);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        System.out.println("run() END watch");
    }

    public void register(Kind<?> [] kinds, Path dir) throws IOException {
        System.out.println("register watch for "+dir);

        // If dirs are from different filesystems WatchService will give errors later
        if (this.ws == null) {
            ws = dir.getFileSystem().newWatchService();
        }
        dir.register(ws, kinds, FILE_TREE);
    }

    /**
     * Save event for later processing by event kind EXCEPT for:
     * <li>DELETE followed by CREATE           => store as MODIFY
     * <li>CREATE followed by MODIFY           => store as CREATE
     * <li>CREATE or MODIFY followed by DELETE => store as DELETE
     */
    private void storeEvent(Kind<?> kind, Path path) {
        //System.out.println("STORE "+kind+" path:"+path);

        boolean cre = false;
        boolean mod = false;
        boolean del = kind == StandardWatchEventKinds.ENTRY_DELETE;

        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            mod = deleted.contains(path);
            cre = !mod;
        }
        else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            cre = created.contains(path);
            mod = !cre;
        }
        addOrRemove(created, cre, path);
        addOrRemove(updated, mod, path);
        addOrRemove(deleted, del, path);
    }
    
    // Add or remove from the set:
    private static void addOrRemove(Set<Path> set, boolean add, Path path) {
        if (add) set.add(path);
        else     set.remove(path);
    }

    public static long fireEvents(Set<Path> deleted, Set<Path> created, Set<Path> modified) {
        System.out.println();
        System.out.println("fireEvents START");
        for (Path path : deleted)
            System.out.println("  DELETED: "+path);
        for (Path path : created)
            System.out.println("  CREATED: "+path);
        for (Path path : modified)
            System.out.println("  UPDATED: "+path);
        System.out.println("fireEvents END");
        System.out.println();

        return 0;
    }
}