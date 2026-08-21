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

import io.github.dkaukov.modem2400b.atoms.FrameHandler;
import io.github.dkaukov.modem2400b.atoms.FrameType;
import io.github.dkaukov.modem2400b.atoms.ModemDecoder;
import java.util.Objects;

/** Adapts arbitrary audio callback chunks to the receiver's variable block size. */
public final class StreamingDecoder {
    private final ModemDecoder decoder;
    private final FrameHandler handler;
    private final short[] fifo;
    private final byte[] payload;
    private final MutableDecodeResult result = new MutableDecodeResult();
    private int buffered;
    private boolean sync;

    public StreamingDecoder(ModemDecoder decoder, FrameHandler handler) {
        this.decoder = Objects.requireNonNull(decoder);
        this.handler = Objects.requireNonNull(handler);
        if (decoder.maximumInputSamples() <= 0 || decoder.payloadBytes() <= 0) {
            throw new IllegalArgumentException("decoder sizes must be positive");
        }
        fifo = new short[decoder.maximumInputSamples()];
        payload = new byte[decoder.payloadBytes()];
    }

    public int accept(short[] samples, int offset, int length) {
        if (samples == null) {
            throw new NullPointerException("samples");
        }
        if (offset < 0 || length < 0 || offset > samples.length - length) {
            throw new IndexOutOfBoundsException();
        }
        int end = offset + length;
        while (offset < end) {
            int required = inputSamplesRequired();
            int need = required - buffered;
            int n = Math.min(need, end - offset);
            System.arraycopy(samples, offset, fifo, buffered, n);
            offset += n;
            buffered += n;
            if (buffered == required) {
                decoder.decode(payload, 0, fifo, 0, result);
                buffered = 0;
                sync = result.synchronizedNow();
                if (result.framePresent()) {
                    int payloadLength = result.frameType() == FrameType.VOICE ? payload.length : 0;
                    handler.onFrame(payload, 0, payloadLength, result.snapshot());
                }
            }
        }
        return length;
    }

    public boolean synchronizedNow() {
        return sync;
    }

    public int bufferedSamples() {
        return buffered;
    }

    public void reset() {
        decoder.reset();
        buffered = 0;
        sync = false;
    }

    private int inputSamplesRequired() {
        int required = decoder.inputSamplesRequired();
        if (required <= 0 || required > fifo.length) {
            throw new IllegalStateException("decoder requested unsupported input size: " + required);
        }
        return required;
    }
}
