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
import io.github.dkaukov.modem2400b.atoms.SampleHandler;
import java.util.Objects;

public final class StreamingEncoder {
    private final ModemEncoder encoder;
    private final int chunkSize;
    private final SampleHandler handler;
    private final short[] samples;

    public StreamingEncoder(ModemEncoder encoder, int chunkSize, SampleHandler handler) {
        this.encoder = Objects.requireNonNull(encoder);
        this.handler = Objects.requireNonNull(handler);
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize");
        }
        this.chunkSize = chunkSize;
        samples = new short[encoder.outputSamples()];
    }

    public void encode(byte[] payload, int offset) {
        encoder.encode(payload, offset, samples, 0);
        for (int p = 0; p < samples.length; p += chunkSize) {
            handler.onSamples(samples, p, Math.min(chunkSize, samples.length - p));
        }
    }

    public void reset() {
        encoder.reset();
    }
}
