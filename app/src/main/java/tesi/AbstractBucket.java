package tesi;

import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractBucket {
    // current water in the bucket. Thread safe.
    protected final AtomicLong water = new AtomicLong(0);
    // maximum capacity of the bucket, upon reaching this value the bucket calls `overflow`.
    protected final long capacity;
    // how much water is removed every `timeWindow` events.
    protected final long leakAmount;
    // time between the leaks in seconds.
    protected final long leakWindow;
    

    /**
     * Construct a Bucket with the specified capacity, leakAmount and timeWindow
     *
     * @param capacity maximum capacity of the bucket, upon reaching this value the bucket calls `overflow`.
     * @param leakAmount how much water is removed every `timeWindow` seconds.
     * @param timeWindow time between the leaks in seconds.
     */
    public AbstractBucket(long capacity, long leakAmount, long leakWindow){
        this.capacity = capacity;
        this.leakAmount = leakAmount;
        this.leakWindow = leakWindow;
    }

    /**
     * Adds a specified amount of water to the bucket
     *
     * @param addition the water to add.
     * @return true if bucket is 'OK', false if `not OK`.
     */
    public boolean addWater(long addition, int... sizes){
        beforeAddWater(addition, sizes);

        long w = water.addAndGet(addition);

        // if there bucket is not overflowing, report true without calling `overflow`
        if (w < capacity){
            return true;
        }

        // the bucket is overflowing: call `overflow` and return false.
        overflow();
        System.out.println("add water after overflow in abstract bucket");

        return false;
    }

    public long getWater(){
        return water.get();
    }

    /**
     * Override with callback to execute before adding water, like halving the amount in specific buckets or leaking in time buckets
     * @param addition
     */
    public void beforeAddWater(long addition, int... sizes){}

    /**
     * Override with the correct way the water is leaked having `unit` steps passed
     * @param unit how many steps passed, can be time or file or whatever.
      */
    protected abstract void leak(long unit);

    /** 
     * Override with the function to call when the bucket is overflowing.
     */
    protected abstract void overflow();
}
