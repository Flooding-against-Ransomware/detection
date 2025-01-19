package tesi;

public abstract class TimeBucket extends AbstractBucket {
    // timestamp of the last leak calculation, used to determine how much water has leaked over time
    protected volatile long lastLeakTimestamp;

    public TimeBucket(long capacity, long leakAmount, long leakWindow){
        super(capacity, leakAmount, leakWindow);
        lastLeakTimestamp = System.currentTimeMillis();
    }

    @Override
    protected void leak(long unit) {
        long now = unit;
        long missedLeaks = (now - lastLeakTimestamp) / leakWindow;
        long leak = missedLeaks * leakAmount;

        // remove the leaked water
        if (leak > 0){
            water.addAndGet(-leak);
            lastLeakTimestamp = now;
        }
        
        // ensure the water level does not go below 0
        if (water.get() < 0){
            water.set(0);
        }
    }

    @Override
    public void beforeAddWater(long addition, int... sizes){
        // update the bucket removing the amount of water that should have leaked between `lastLeakTimestamp` and now
        leak(System.currentTimeMillis());
    }
}
