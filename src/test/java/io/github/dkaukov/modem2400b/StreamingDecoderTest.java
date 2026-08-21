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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dkaukov.modem2400b.atoms.DecodeResult;
import io.github.dkaukov.modem2400b.atoms.FrameType;
import io.github.dkaukov.modem2400b.atoms.ModemDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreamingDecoderTest {
    @Test
    void dataFrameUsesZeroLengthPayloadInsteadOfStaleVoiceBytes() {
        List<byte[]> payloads = new ArrayList<>();
        List<DecodeResult> results = new ArrayList<>();
        StreamingDecoder streaming = new StreamingDecoder(new VoiceThenDataDecoder(), (payload, offset, length, result) -> {
            payloads.add(Arrays.copyOfRange(payload, offset, offset + length));
            results.add(result);
        });

        streaming.accept(new short[] {1, 2}, 0, 2);

        assertEquals(2, results.size());
        assertEquals(FrameType.VOICE, results.get(0).frameType());
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5, 6, 7}, payloads.get(0));
        assertEquals(FrameType.DATA, results.get(1).frameType());
        assertEquals(0, payloads.get(1).length);
        assertTrue(streaming.synchronizedNow());
    }

    private static final class VoiceThenDataDecoder implements ModemDecoder {
        private int calls;

        @Override
        public int maximumInputSamples() {
            return 1;
        }

        @Override
        public int payloadBytes() {
            return 7;
        }

        @Override
        public int inputSamplesRequired() {
            return 1;
        }

        @Override
        public void decode(byte[] payloadOutput, int payloadOffset, short[] input, int inputOffset,
                MutableDecodeResult result) {
            if (calls++ == 0) {
                for (int i = 0; i < payloadBytes(); i++) {
                    payloadOutput[payloadOffset + i] = (byte) (i + 1);
                }
                result.set(true, true, FrameType.VOICE, 0, 0, 0);
            } else {
                result.set(true, true, FrameType.DATA, 0, 0, 0);
            }
        }

        @Override
        public void reset() {
            calls = 0;
        }
    }
}
