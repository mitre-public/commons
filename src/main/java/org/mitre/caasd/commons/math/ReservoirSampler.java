package org.mitre.caasd.commons.math;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Consumer;

/**
 * A ReservoirSampler harvests a random sample of items from a stream of items it is shown via calls
 * to "accept(T item)".
 * <p>
 * The sample is extracted using "reservoir sampling". The sample is kept in-memory
 *
 * @param <T>
 */
public class ReservoirSampler<T> implements Consumer<T> {

    private final ArrayList<T> samples;
    private final Random rng;
    private final int sampleSize;
    private int countSeen = 0;

    /**
     * Create a ReservoirSampler that uses a new Random seed every time.
     *
     * @param sampleSize The number of items to keep in memory throughout the sampling process
     */
    public ReservoirSampler(int sampleSize) {
        this(sampleSize, new Random());
    }

    /**
     * Create a ReservoirSampler that uses a new Random seed every time.
     *
     * @param sampleSize The number of items to keep in memory throughout the sampling process
     * @param random     The random number generator that controls the sampling process.  If you
     *                   want reproducible samples provide an instance of Random with a known seed.
     */
    public ReservoirSampler(int sampleSize, Random random) {
        requireNonNull(random);
        checkArgument(sampleSize >= 0);
        this.sampleSize = sampleSize;
        this.samples = new ArrayList<>(sampleSize);
        this.rng = random;
        this.countSeen = 0;
    }

    /**
     * Retain an item with a gradually reducing probability.  After the sample size has been reached
     * each new retention randomly evicts an item that was retained previously.
     */
    @Override
    public void accept(T item) {
        // memorize the first n examples ...
        if (countSeen < sampleSize) {
            samples.add(item);
        } else {
            // randomly overwrite prior memory with smaller and smaller probability
            int randomIndex = rng.nextInt(countSeen + 1);
            if (randomIndex < sampleSize) {
                samples.set(randomIndex, item);
            }
        }
        countSeen++;
    }

    /**
     * @return The samples extracted.  The order of the samples in this list is produced by random
     *     chance, NOT the order of exposure.  Note: this list will be smaller than "sampleSize"
     *     when "accept(T item)" was called fewer times than "sampleSize"
     */
    public ArrayList<T> currentSample() {
        return new ArrayList<>(samples);
    }

    /**
     * @return The total number of items this sampler has seen in its lifetime (i.e. num calls to
     *     "accept") .
     */
    public int countSeen() {
        return countSeen;
    }

    /** The number of items this Sampler wants to extract. */
    public int sampleSize() {
        return sampleSize;
    }

    /** Resets "countSeen" to zero and purges the "currentSample". */
    public void reset() {
        samples.clear();
        countSeen = 0;
    }
}
