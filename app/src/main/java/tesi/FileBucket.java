package tesi;

public abstract class FileBucket extends AbstractBucket {
    // count of files has been modified, used to determine how much water has leaked over time
    protected volatile long fileCount;

    public FileBucket(long capacity, long leakAmount, long leakWindow){
        super(capacity, leakAmount, leakWindow);
    }

    @Override
    protected void leak(long unit) {
        //fileCount += unit;
        //long missedLeaks = fileCount / leakWindow;
        //fileCount = fileCount % leakWindow;
        //long leak = fileCount % leakWindow;
        
        long elapsedFiles = fileCount + unit;
        long missedLeaks = elapsedFiles / leakWindow;
        fileCount = elapsedFiles % leakWindow;

        long leak = missedLeaks * leakAmount;

        // remove the leaked water
        if (leak > 0){
            water.addAndGet(-leak);
        }
        
        // ensure the water level does not go below 0
        if (water.get() < 0){
            water.set(0);
        }
    }

    @Override
    public void beforeAddWater(long addition, int... sizes){
        // sum the sizes of the sets, i.e. the number of files modified
        int count = 0;
        for (int size : sizes) {
            count += size;
        }

        // leak based on the number of files modified
        leak(count);
    }
}
