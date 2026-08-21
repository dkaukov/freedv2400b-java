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

import java.util.List;
import org.junit.jupiter.api.Test;

class DegradedChannelTest {
    @Test
    void deterministicAudioPathDegradationsStillDecode() throws Exception {
        short[] clean = Codec2TestSupport.readWav();
        List<ChannelCase> cases = List.of(
                new ChannelCase("half amplitude", gainAndOffset(clean, 1, 2, 0)),
                new ChannelCase("increased amplitude", gainAndOffset(clean, 3, 2, 0)),
                new ChannelCase("DC offset", gainAndOffset(clean, 1, 1, 2_000)),
                new ChannelCase("mild deterministic noise", addNoise(clean, 600)),
                new ChannelCase("three-tap low-pass", lowPass(clean)));

        for (ChannelCase channelCase : cases) {
            Codec2TestSupport.DecodedStream decoded = Codec2TestSupport.decode(
                    new FreeDv2400bDecoder(), channelCase.samples());
            assertEquals(Codec2TestSupport.EXPECTED_PAYLOADS, decoded.payloads(), channelCase.name());
        }
    }

    private static short[] gainAndOffset(short[] input, int numerator, int denominator, int offset) {
        short[] output = new short[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = clamp((input[i] * numerator) / denominator + offset);
        }
        return output;
    }

    private static short[] addNoise(short[] input, int peakAmplitude) {
        short[] output = new short[input.length];
        int state = 0x13579bdf;
        for (int i = 0; i < input.length; i++) {
            state = state * 1_664_525 + 1_013_904_223;
            int noise = (int) (((state & 0xffffffffL) * (2L * peakAmplitude + 1)) >>> 32)
                    - peakAmplitude;
            output[i] = clamp(input[i] + noise);
        }
        return output;
    }

    private static short[] lowPass(short[] input) {
        short[] output = new short[input.length];
        output[0] = input[0];
        for (int i = 1; i < input.length - 1; i++) {
            output[i] = (short) ((input[i - 1] + 2 * input[i] + input[i + 1]) / 4);
        }
        output[output.length - 1] = input[input.length - 1];
        return output;
    }

    private static short clamp(int value) {
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value));
    }

    private record ChannelCase(String name, short[] samples) { }
}
