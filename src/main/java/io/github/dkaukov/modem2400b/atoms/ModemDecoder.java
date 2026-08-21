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
package io.github.dkaukov.modem2400b.atoms;

import io.github.dkaukov.modem2400b.MutableDecodeResult;

public interface ModemDecoder {
    /** Maximum number of input samples requested by this decoder. */
    int maximumInputSamples();

    /** Number of bytes in the decoder's voice payload output. */
    int payloadBytes();

    /**
     * Number of samples the next decode call must provide.
     */
    int inputSamplesRequired();

    /**
     * Allocation-free decode; payload is written only when a voice frame is present.
     */
    void decode(byte[] payloadOutput, int payloadOffset, short[] input, int inputOffset, MutableDecodeResult result);

    default DecodeResult decode(short[] input, int inputOffset, byte[] payloadOutput, int payloadOffset) {
        MutableDecodeResult result = new MutableDecodeResult();
        decode(payloadOutput, payloadOffset, input, inputOffset, result);
        return new DecodeResult(result.framePresent(), result.synchronizedNow(), result.frameType(),
                result.uniqueWordErrors(), result.discriminatorSnrDb(), result.clockOffsetPpm());
    }

    void reset();
}
