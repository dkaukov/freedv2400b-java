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

/** Estimates Manchester symbol timing from the squared 4.8 kHz clock line. */
final class ManchesterTimingEstimator {
    private static final int PERIOD = 10;
    private static final float[] CLOCK_COS = new float[PERIOD], CLOCK_SIN = new float[PERIOD];

    static {
        for (int i = 0; i < PERIOD; i++) {
            double phase = 2 * Math.PI * i / PERIOD;
            CLOCK_COS[i] = (float) Math.cos(phase);
            CLOCK_SIN[i] = (float) Math.sin(phase);
        }
    }

    private ManchesterTimingEstimator() {
    }

    /**
     * Correlates squared filtered samples against one exact-period clock table.
     *
     * @param filtered integrate-and-dump samples
     * @param length number of samples to correlate
     * @return normalized symbol timing with Codec2's phase calibration applied
     */
    static float estimate(int[] filtered, int length) {
        float real = 0;
        float imaginary = 0;
        int clockPhase = 0;
        for (int i = 0; i < length; i++) {
            float value = filtered[i];
            float squared = value * value;
            real += squared * CLOCK_COS[clockPhase];
            imaginary += squared * CLOCK_SIN[clockPhase];
            if (++clockPhase == PERIOD) {
                clockPhase = 0;
            }
        }
        return (float) (Math.atan2(imaginary, real) / (2 * Math.PI) - .42);
    }
}
