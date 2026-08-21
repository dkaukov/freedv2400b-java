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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class Codec2GoldenTest {
    private static final byte[] PAYLOAD = hex("11223344556670");

    @Test
    void framerAndTransmitterMatchCodec2Exactly() throws Exception {
        byte[] bits = new byte[FreeDv2400b.FRAME_BITS];
        VhfTypeAFramer.frame(PAYLOAD, 0, bits);
        assertEquals("a7a711223367ad4455667272", packHex(bits));

        short[] encoded = new short[FreeDv2400b.TX_SAMPLES];
        new FreeDv2400bEncoder().encode(hex("a3156e07b505c0"), 0, encoded, 0);
        byte[] wav = Codec2TestSupport.readWavBytes();
        ByteBuffer expected = ByteBuffer.wrap(wav, 44, 3840).slice().order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < encoded.length; i++) {
            assertEquals(expected.getShort(), encoded[i], "sample " + i);
        }

        ByteBuffer pcm = ByteBuffer.allocate(3840).order(ByteOrder.LITTLE_ENDIAN);
        for (short sample : encoded) {
            pcm.putShort(sample);
        }
        assertEquals("4a816f8167e952f4754300ae06957fbde4928322c3b2731298db6b1b497af4c8",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pcm.array())));
    }

    @Test
    void codec2WavDecodesAgainAfterReset() throws Exception {
        short[] pcm = Codec2TestSupport.readWav();
        FreeDv2400bDecoder decoder = new FreeDv2400bDecoder();

        assertGoldenDecode(Codec2TestSupport.decode(decoder, pcm));
        decoder.reset();
        assertEquals(FreeDv2400b.TX_SAMPLES, decoder.inputSamplesRequired());
        assertGoldenDecode(Codec2TestSupport.decode(decoder, pcm));
    }

    @Test
    void invertedCodec2WavStillDecodes() throws Exception {
        short[] pcm = Codec2TestSupport.readWav();
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (short) -pcm[i];
        }
        assertGoldenDecode(Codec2TestSupport.decode(new FreeDv2400bDecoder(), pcm));
    }

    @Test
    void arbitraryChunkingMatchesExactDecode() throws Exception {
        short[] pcm = Codec2TestSupport.readWav();
        List<String> payloads = new ArrayList<>();
        StreamingDecoder streaming = new StreamingDecoder(new FreeDv2400bDecoder(),
                (payload, offset, length, result) -> payloads.add(
                        HexFormat.of().formatHex(payload, offset, offset + length)));
        int[] sizes = {1, 5, 10, 159, 160, 1915, 1920, 1925};
        int offset = 0;
        int sizeIndex = 0;
        while (offset < pcm.length) {
            int length = Math.min(sizes[sizeIndex++ % sizes.length], pcm.length - offset);
            streaming.accept(pcm, offset, length);
            offset += length;
        }

        assertEquals(Codec2TestSupport.EXPECTED_PAYLOADS, payloads);
        streaming.reset();
        assertEquals(0, streaming.bufferedSamples());
        assertFalse(streaming.synchronizedNow());
    }

    @Test
    void unusedPayloadNibbleIsIgnored() {
        short[] canonical = new short[FreeDv2400b.TX_SAMPLES];
        short[] padded = new short[FreeDv2400b.TX_SAMPLES];
        byte[] alternate = PAYLOAD.clone();
        alternate[6] |= 15;
        FreeDv2400bEncoder encoder = new FreeDv2400bEncoder();
        encoder.encode(PAYLOAD, 0, canonical, 0);
        encoder.encode(alternate, 0, padded, 0);
        assertArrayEquals(canonical, padded);
    }

    private static void assertGoldenDecode(Codec2TestSupport.DecodedStream decoded) {
        assertEquals(Codec2TestSupport.EXPECTED_PAYLOADS, decoded.payloads());
        assertEquals(2, decoded.firstFrameCall());
    }

    private static String packHex(byte[] bits) {
        byte[] packed = new byte[bits.length / 8];
        for (int i = 0; i < bits.length; i++) {
            packed[i >>> 3] |= bits[i] << (7 - (i & 7));
        }
        return HexFormat.of().formatHex(packed);
    }

    private static byte[] hex(String value) {
        return HexFormat.of().parseHex(value);
    }
}
