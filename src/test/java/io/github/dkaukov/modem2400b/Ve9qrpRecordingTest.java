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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dkaukov.modem2400b.atoms.FrameType;
import org.junit.jupiter.api.Test;

class Ve9qrpRecordingTest {

    /**
     * Verifies synchronization against the FreeDV 1.4.3 {@code ve9qrp_2400b.wav}
     * recording distributed by Debian.
     */
    @Test
    void decoderSynchronizesToVe9qrpRecording() throws Exception {
        short[] samples = Codec2TestSupport.readWav("/ve9qrp_2400b.wav");
        FreeDv2400bDecoder decoder = new FreeDv2400bDecoder();
        byte[] payload = new byte[FreeDv2400b.PAYLOAD_BYTES];
        MutableDecodeResult result = new MutableDecodeResult();
        int offset = 0;
        int decodeCalls = 0;
        int firstSynchronizedCall = 0;
        int syncAcquisitions = 0;
        int synchronizedCalls = 0;
        int voiceFrames = 0;
        boolean previouslySynchronized = false;

        while (offset + decoder.inputSamplesRequired() <= samples.length) {
            int inputLength = decoder.inputSamplesRequired();
            decoder.decode(payload, 0, samples, offset, result);
            offset += inputLength;
            decodeCalls++;
            if (result.synchronizedNow()) {
                synchronizedCalls++;
                if (!previouslySynchronized) {
                    syncAcquisitions++;
                    if (firstSynchronizedCall == 0) {
                        firstSynchronizedCall = decodeCalls;
                    }
                }
            }
            if (result.framePresent() && result.frameType() == FrameType.VOICE) {
                voiceFrames++;
            }
            previouslySynchronized = result.synchronizedNow();
        }

        assertEquals(2, firstSynchronizedCall);
        assertEquals(1, syncAcquisitions);
        assertEquals(decodeCalls - 1, synchronizedCalls);
        assertEquals(2810, voiceFrames);
        assertTrue(result.synchronizedNow(), "decoder lost synchronization");
    }
}
