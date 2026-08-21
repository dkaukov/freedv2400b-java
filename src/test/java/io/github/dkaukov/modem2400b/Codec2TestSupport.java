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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

final class Codec2TestSupport {
    static final List<String> EXPECTED_PAYLOADS = List.of(
            "a3156e07b505c0", "00a387b19f4270", "81ac5b5f9e4550", "1b50739da51200",
            "2b4a2a47c211a0", "bf3c46d9c6b150", "ee3fed3ff52b70", "633de8ed69fe70",
            "d539abb7c54bf0", "43c1cac19590f0", "9df9d55fc246f0", "b58b9dfd080a10",
            "569276779c43d0", "d9d52cf925cea0", "c46d4dbfe79e70", "8aa9ee4de8abd0",
            "cf0d8e67d63520", "f38fa8712c1170", "0689305f634f50", "ab4b5edd46abe0",
            "f5aa2fa7ea69e0", "ead11299b5bc00", "f2ab1e3fe05370", "0004020d2e8a50");

    private Codec2TestSupport() {
    }

    static short[] readWav() throws Exception {
        return readWav("/modem_2400b_short.wav");
    }

    static short[] readWav(String resourceName) throws Exception {
        byte[] wav = readWavBytes(resourceName);
        ByteBuffer pcm = ByteBuffer.wrap(wav, 44, wav.length - 44).slice().order(ByteOrder.LITTLE_ENDIAN);
        short[] samples = new short[pcm.remaining() / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = pcm.getShort();
        }
        return samples;
    }

    static byte[] readWavBytes() throws Exception {
        return readWavBytes("/modem_2400b_short.wav");
    }

    private static byte[] readWavBytes(String resourceName) throws Exception {
        try (InputStream input = Codec2TestSupport.class.getResourceAsStream(resourceName)) {
            return Objects.requireNonNull(input).readAllBytes();
        }
    }

    static DecodedStream decode(FreeDv2400bDecoder decoder, short[] samples) {
        List<String> payloads = new ArrayList<>();
        byte[] output = new byte[FreeDv2400b.PAYLOAD_BYTES];
        MutableDecodeResult result = new MutableDecodeResult();
        int offset = 0;
        int calls = 0;
        int firstFrameCall = 0;
        while (offset + decoder.inputSamplesRequired() <= samples.length) {
            int length = decoder.inputSamplesRequired();
            decoder.decode(output, 0, samples, offset, result);
            offset += length;
            calls++;
            if (result.framePresent() && result.frameType() == FrameType.VOICE) {
                if (payloads.isEmpty()) {
                    firstFrameCall = calls;
                }
                payloads.add(HexFormat.of().formatHex(output));
            }
        }
        return new DecodedStream(payloads, firstFrameCall);
    }

    record DecodedStream(List<String> payloads, int firstFrameCall) { }
}
