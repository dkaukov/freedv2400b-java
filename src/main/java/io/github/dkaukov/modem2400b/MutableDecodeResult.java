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

import io.github.dkaukov.modem2400b.atoms.DecodeResult;
import io.github.dkaukov.modem2400b.atoms.FrameType;

/**
 * Caller-owned result for allocation-free decoding.
 */
public final class MutableDecodeResult {
    private boolean framePresent, synchronizedNow;
    private FrameType frameType = FrameType.NONE;
    private int uniqueWordErrors;
    private float discriminatorSnrDb, clockOffsetPpm;

    public boolean framePresent() {
        return framePresent;
    }

    public boolean synchronizedNow() {
        return synchronizedNow;
    }

    public FrameType frameType() {
        return frameType;
    }

    public int uniqueWordErrors() {
        return uniqueWordErrors;
    }

    public float discriminatorSnrDb() {
        return discriminatorSnrDb;
    }

    public float clockOffsetPpm() {
        return clockOffsetPpm;
    }

    void set(boolean fp, boolean sync, FrameType ft, int uwe, float snr, float ppm) {
        framePresent = fp;
        synchronizedNow = sync;
        frameType = ft;
        uniqueWordErrors = uwe;
        discriminatorSnrDb = snr;
        clockOffsetPpm = ppm;
    }

    DecodeResult snapshot() {
        return new DecodeResult(framePresent, synchronizedNow, frameType, uniqueWordErrors, discriminatorSnrDb, clockOffsetPpm);
    }
}
