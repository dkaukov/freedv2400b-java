/*
 * This file is licensed under the GNU General Public License v3.0.
 *
 * You may obtain a copy of the License at
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package io.github.dkaukov.modem2400b;

import java.util.Arrays;

/** Maintains the sum of a fixed-size moving window of integer samples. */
final class MovingSum {
    private final int[] samples;
    private int pointer;
    private int sum;

    /**
     * Creates an empty moving window initially filled with zeros.
     *
     * @param size number of samples in the moving window
     * @throws IllegalArgumentException if {@code size} is not positive
     */
    MovingSum(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        samples = new int[size];
    }

    /**
     * Replaces the oldest sample with a new sample and returns the new sum.
     *
     * @param sample sample to append to the moving window
     * @return sum of all samples currently in the window
     */
    int push(int sample) {
        sum = sum - samples[pointer] + sample;
        samples[pointer] = sample;
        if (++pointer == samples.length) {
            pointer = 0;
        }
        return sum;
    }

    /** Returns the current window sum without modifying the window. */
    int sum() {
        return sum;
    }

    /** Clears the moving window, pointer, and accumulated sum. */
    void reset() {
        Arrays.fill(samples, 0);
        pointer = 0;
        sum = 0;
    }
}
