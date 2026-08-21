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

/**
 * Synchronizes and extracts FreeDV VHF Type-A frames from hard-decision bits.
 *
 * <p>The deframer keeps circular normal and inverted 96-bit buffers so that
 * either discriminator polarity can be acquired. While unlocked it checks
 * every received bit and permits one unique-word error. Once locked it checks
 * every 96 bits, permits three unique-word errors, and returns to acquisition
 * after five consecutive misses. Voice payloads are packed MSB first into
 * seven bytes; data frames are identified but not decoded by this raw voice
 * payload implementation.</p>
 *
 * <p>Ported from Codec2 {@code freedv_vhf_framing.c} at revision
 * {@code 96e8a19c2487fd83bd981ce570f257aef42618f9}. The original framing code
 * identifies Brady O'Brien as author and is copyright David Rowe.</p>
 *
 * <p>Instances are stateful, are not thread-safe, and belong to one receive
 * stream.</p>
 */
final class VhfTypeADeframer {
    /** Voice and data 16-bit unique words. */
    private static final int VOICE = 0x67ad, DATA = 0xf1fc;
    /** Circular candidate frames for both possible signal polarities. */
    private final byte[] normal = new byte[96], inverted = new byte[96];
    private int ptr, lastUw, misses;
    private boolean sync, onInverted;
    private FrameType type = FrameType.NONE, matchType = FrameType.NONE;
    private int errors, matchErrors;

    /**
     * Accepts exactly 96 hard-decision bits and advances synchronization.
     *
     * <p>A frame can be returned on the same call that a fifth tracking miss
     * drops synchronization, matching the Codec2 deframer behavior.</p>
     *
     * @param in 96 unpacked input bits, each represented by zero or one
     * @param payload output buffer for a seven-byte voice payload
     * @param payloadOffset starting offset in {@code payload}
     * @return {@code true} if a voice or data frame was extracted
     */
    boolean accept(byte[] in, byte[] payload, int payloadOffset) {
        boolean extracted = false;
        for (int i = 0; i < 96; i++) {
            normal[ptr] = in[i];
            inverted[ptr] = (byte) (in[i] ^ 1);
            ptr = (ptr + 1) % 96;
            if (sync) {
                if (++lastUw == 96) {
                    lastUw = 0;
                    boolean matched = match(onInverted ? inverted : normal, 3);
                    if (matched) {
                        misses = 0;
                    } else {
                        misses++;
                    }
                    if (misses > 4) {
                        sync = false;
                    }
                    type = matchType;
                    errors = matchErrors;
                    extracted = true;
                    if (type == FrameType.VOICE) {
                        extract(onInverted ? inverted : normal, payload, payloadOffset);
                    }
                }
            } else {
                if (match(inverted, 1)) {
                    sync = true;
                    lastUw = misses = 0;
                    onInverted = true;
                    type = matchType;
                    errors = matchErrors;
                    extracted = true;
                    if (type == FrameType.VOICE) {
                        extract(inverted, payload, payloadOffset);
                    }
                }
                if (match(normal, 1)) {
                    sync = true;
                    lastUw = misses = 0;
                    onInverted = false;
                    type = matchType;
                    errors = matchErrors;
                    extracted = true;
                    if (type == FrameType.VOICE) {
                        extract(normal, payload, payloadOffset);
                    }
                }
            }
        }
        if (!extracted) {
            type = FrameType.NONE;
            errors = 0;
        }
        return extracted;
    }

    /**
     * Compares the candidate frame against both Type-A unique words.
     *
     * <p>Equal voice and data distances select data, as in Codec2.</p>
     *
     * @param bits circular candidate frame
     * @param tol maximum accepted Hamming distance
     * @return whether the closer unique word is within tolerance
     */
    private boolean match(byte[] bits, int tol) {
        int v = 0, d = 0;
        for (int i = 0; i < 16; i++) {
            int b = bits[(ptr + 40 + i) % 96];
            v += b ^ ((VOICE >>> (15 - i)) & 1);
            d += b ^ ((DATA >>> (15 - i)) & 1);
        }
        boolean voice = v < d;
        matchErrors = voice ? v : d;
        matchType = voice ? FrameType.VOICE : FrameType.DATA;
        return matchErrors <= tol;
    }

    /** Extracts and MSB-first packs the 52 voice bits from a candidate frame. */
    private void extract(byte[] bits, byte[] out, int outputOffset) {
        for (int i = 0; i < 7; i++) {
            out[outputOffset + i] = 0;
        }
        for (int i = 0; i < 52; i++) {
            int pos = i < 24 ? ptr + 16 + i : ptr + 56 + i - 24;
            out[outputOffset + (i >>> 3)] |= bits[pos % 96] << (7 - (i & 7));
        }
        out[outputOffset + 6] &= (byte) 0xf0;
    }

    /**
     * Returns whether the deframer remains tracking-locked.
     *
     * @return current synchronization state
     */
    boolean synchronizedNow() {
        return sync;
    }

    /**
     * Returns the type of the frame most recently extracted by {@link #accept}.
     *
     * @return voice, data, or none when the last call extracted no frame
     */
    FrameType frameType() {
        return type;
    }

    /**
     * Returns the unique-word Hamming distance for the last extracted frame.
     *
     * @return unique-word error count, or zero if no frame was extracted
     */
    int errors() {
        return errors;
    }

    /** Restores acquisition state and clears all circular bit history. */
    void reset() {
        java.util.Arrays.fill(normal, (byte) 0);
        java.util.Arrays.fill(inverted, (byte) 0);
        ptr = lastUw = misses = errors = 0;
        sync = onInverted = false;
        type = FrameType.NONE;
    }
}
