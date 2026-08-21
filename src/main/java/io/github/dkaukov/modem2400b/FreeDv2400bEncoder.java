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

import io.github.dkaukov.modem2400b.atoms.ModemEncoder;

/**
 * Bit-exact Codec2-compatible discriminator-level encoder.
 *
 * <p>The Manchester level mapping is ported from Codec2 {@code fmfsk.c} at
 * revision {@code 96e8a19c2487fd83bd981ce570f257aef42618f9}. The original
 * FMFSK modem code identifies Brady O'Brien as author and is copyright David
 * Rowe.</p>
 */
public final class FreeDv2400bEncoder implements ModemEncoder {
    private final byte[] frame = new byte[FreeDv2400b.FRAME_BITS];

    @Override
    public int outputSamples() {
        return FreeDv2400b.TX_SAMPLES;
    }

    @Override
    public void encode(byte[] payload, int payloadOffset, short[] output, int outputOffset) {
        check(payload, payloadOffset, 7, "payload");
        check(output, outputOffset, 1920, "output");
        VhfTypeAFramer.frame(payload, payloadOffset, frame);
        int p = outputOffset;
        for (byte bit : frame) {
            short a = bit == 0 ? (short) -16383 : (short) 16383, b = (short) -a;
            for (int j = 0; j < 10; j++) {
                output[p++] = a;
            }
            for (int j = 0; j < 10; j++) {
                output[p++] = b;
            }
        }
    }

    @Override
    public void reset() {
    }

    private static void check(Object a, int offset, int n, String name) {
        if (a == null) {
            throw new NullPointerException(name);
        }
        int len = a instanceof byte[] b ? b.length : ((short[]) a).length;
        if (offset < 0 || n < 0 || offset > len - n) {
            throw new IndexOutOfBoundsException(name + " offset/length");
        }
    }
}
