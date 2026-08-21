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

/**
 * Builds a 96-bit FreeDV VHF Type-A voice frame from a packed 52-bit payload.
 *
 * <p>The payload is read MSB first from seven bytes and inserted at frame
 * positions 16 through 39 and 56 through 83. The unused low nibble of the
 * seventh payload byte is ignored. Protocol, varicode, padding, and the voice
 * unique word remain at their raw-wrapper defaults.</p>
 *
 * <p>Ported from Codec2 {@code freedv_vhf_framing.c} at revision
 * {@code 96e8a19c2487fd83bd981ce570f257aef42618f9}. The original framing code
 * identifies Brady O'Brien as author and is copyright David Rowe.</p>
 */
final class VhfTypeAFramer {
    /** Unpacked fixed raw-payload Type-A frame template. */
    private static final byte[] BLANK_BITS = unpack(hex("a7a700000067ad0000000272"));

    private VhfTypeAFramer() {
    }

    /**
     * Populates an unpacked Type-A frame.
     *
     * @param payload packed MSB-first payload containing at least seven bytes
     *                from {@code payloadOffset}
     * @param payloadOffset starting offset in {@code payload}
     * @param bits destination for 96 unpacked bits, each represented by zero
     *             or one
     */
    static void frame(byte[] payload, int payloadOffset, byte[] bits) {
        System.arraycopy(BLANK_BITS, 0, bits, 0, BLANK_BITS.length);
        for (int i = 0; i < 24; i++) {
            bits[16 + i] = (byte) bit(payload, payloadOffset, i);
        }
        for (int i = 24; i < 52; i++) {
            bits[56 + i - 24] = (byte) bit(payload, payloadOffset, i);
        }
    }

    /**
     * Reads one bit from an MSB-first packed byte array.
     *
     * @param b packed bytes
     * @param byteOffset byte offset at which bit numbering starts
     * @param i zero-based bit index
     * @return zero or one
     */
    static int bit(byte[] b, int byteOffset, int i) {
        return ((b[byteOffset + (i >>> 3)] & 0xff) >>> (7 - (i & 7))) & 1;
    }

    /** Converts packed MSB-first bytes to individual zero-or-one bytes. */
    private static byte[] unpack(byte[] packed) {
        byte[] out = new byte[packed.length * 8];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) bit(packed, 0, i);
        }
        return out;
    }

    /** Decodes an even-length hexadecimal string. */
    private static byte[] hex(String s) {
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return b;
    }
}
