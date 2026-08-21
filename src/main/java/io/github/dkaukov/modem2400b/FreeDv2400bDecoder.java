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

import io.github.dkaukov.modem2400b.atoms.FrameType;
import io.github.dkaukov.modem2400b.atoms.ModemDecoder;
import java.util.Arrays;

/**
 * Stateful Codec2-compatible reference receiver. Instances are not thread-safe.
 *
 * <p>The demodulator is a Java port of Codec2 {@code fmfsk.c} at revision
 * {@code 96e8a19c2487fd83bd981ce570f257aef42618f9}. The original FMFSK modem
 * code identifies Brady O'Brien as author and is copyright David Rowe.</p>
 */
public final class FreeDv2400bDecoder implements ModemDecoder {
    private static final int SAMPLES_PER_MANCHESTER_SYMBOL = 10;
    private static final int TIMING_ADJUSTMENT_SAMPLE_COUNT = 5;
    private static final int MANCHESTER_SYMBOL_COUNT = FreeDv2400b.FRAME_BITS * 2;
    private static final int PROCESSING_WINDOW_SAMPLE_COUNT = FreeDv2400b.TX_SAMPLES + 4 * SAMPLES_PER_MANCHESTER_SYMBOL;
    private static final int FILTERED_SAMPLE_COUNT = (MANCHESTER_SYMBOL_COUNT + 1) * SAMPLES_PER_MANCHESTER_SYMBOL;
    private static final int TAIL_SAMPLE_COUNT = 4 * SAMPLES_PER_MANCHESTER_SYMBOL + TIMING_ADJUSTMENT_SAMPLE_COUNT;
    private final short[] sampleTail = new short[TAIL_SAMPLE_COUNT];
    private final int[] filtered = new int[FILTERED_SAMPLE_COUNT];
    private final MovingSum boxcar = new MovingSum(SAMPLES_PER_MANCHESTER_SYMBOL);
    private final byte[] bits = new byte[96];
    private final VhfTypeADeframer deFramer = new VhfTypeADeframer();
    private int requiredInputSamples = FreeDv2400b.TX_SAMPLES;
    private float lastOdd, previousTiming, ppm, snr;

    @Override
    public int maximumInputSamples() {
        return FreeDv2400b.MAX_RX_INPUT;
    }

    @Override
    public int payloadBytes() {
        return FreeDv2400b.PAYLOAD_BYTES;
    }

    @Override
    public int inputSamplesRequired() {
        return requiredInputSamples;
    }

    @Override
    public void decode(byte[] payload, int po, short[] input, int io, MutableDecodeResult result) {
        check(input, io, requiredInputSamples, "input");
        check(payload, po, 7, "payload");
        if (result == null) {
            throw new NullPointerException("result");
        }
        int retainedSampleCount = PROCESSING_WINDOW_SAMPLE_COUNT - requiredInputSamples;
        int tailOffset = TAIL_SAMPLE_COUNT - retainedSampleCount;
        for (int i = 0; i < FILTERED_SAMPLE_COUNT + SAMPLES_PER_MANCHESTER_SYMBOL - 1; i++) {
            int sample = i < retainedSampleCount ? sampleTail[tailOffset + i] : input[io + i - retainedSampleCount];
            int sum = boxcar.push(sample);
            if (i >= SAMPLES_PER_MANCHESTER_SYMBOL - 1) {
                filtered[i - (SAMPLES_PER_MANCHESTER_SYMBOL - 1)] = sum;
            }
        }
        System.arraycopy(input, io + requiredInputSamples - TAIL_SAMPLE_COUNT, sampleTail, 0, TAIL_SAMPLE_COUNT);

        float timing = ManchesterTimingEstimator.estimate(filtered, FILTERED_SAMPLE_COUNT);
        int rxTiming = Math.round(timing * SAMPLES_PER_MANCHESTER_SYMBOL);
        float delta = timing - previousTiming;
        previousTiming = timing;
        if (Math.abs(delta) < .2f) {
            ppm = .9f * ppm + .1f * (1_000_000f * delta / MANCHESTER_SYMBOL_COUNT);
        }
        requiredInputSamples = FreeDv2400b.TX_SAMPLES;
        if (timing > -.2f) {
            requiredInputSamples += TIMING_ADJUSTMENT_SAMPLE_COUNT;
        }
        if (timing < -.65f) {
            requiredInputSamples -= TIMING_ADJUSTMENT_SAMPLE_COUNT;
        }
        int sampleOffset = SAMPLES_PER_MANCHESTER_SYMBOL
                + TIMING_ADJUSTMENT_SAMPLE_COUNT - 1 + rxTiming;

        float last = lastOdd, lastAbs = Math.abs(last), even = 0, odd = 0, signal = 0, noise = 0;
        for (int i = 0; i < MANCHESTER_SYMBOL_COUNT; i++) {
            float current = filtered[sampleOffset + SAMPLES_PER_MANCHESTER_SYMBOL * i];
            float diff = last - current;
            int bit = diff > 0 ? 1 : 0;
            last = current;
            signal += current * current;
            float a = Math.abs(current), nd = a - lastAbs;
            noise += nd * nd;
            lastAbs = a;
            float confidence = Math.abs(diff);
            if ((i & 1) == 1) {
                even += confidence;
                bits[i >>> 1] |= (byte) bit;
            } else {
                odd += confidence;
                bits[i >>> 1] = (byte) (bit << 1);
            }
        }
        if (even > odd) {
            for (int i = 0; i < 96; i++) {
                bits[i] &= 1;
            }
        } else {
            for (int i = 0; i < 96; i++) {
                bits[i] = (byte) ((bits[i] & 2) >>> 1);
            }
        }
        lastOdd = last;
        noise *= .5f;
        float currentSnr = (float) (10 * Math.log10((signal + 1e-6 / 3.1) / (noise + 1e-6)));
        snr = snr < .1f ? currentSnr : .9f * snr + .1f * currentSnr;
        boolean present = deFramer.accept(bits, payload, po);
        result.set(present,
                deFramer.synchronizedNow(),
                present ? deFramer.frameType() : FrameType.NONE,
                present ? deFramer.errors() : 0,
                snr,
                ppm);
    }

    @Override
    public void reset() {
        Arrays.fill(sampleTail, (short) 0);
        Arrays.fill(filtered, 0);
        Arrays.fill(bits, (byte) 0);
        boxcar.reset();
        requiredInputSamples = FreeDv2400b.TX_SAMPLES;
        lastOdd = previousTiming = ppm = snr = 0;
        deFramer.reset();
    }

    private static void check(Object a, int off, int n, String name) {
        if (a == null) {
            throw new NullPointerException(name);
        }
        int len = a instanceof byte[] b ? b.length : ((short[]) a).length;
        if (off < 0 || off > len - n) {
            throw new IndexOutOfBoundsException(name + " offset/length");
        }
    }
}
