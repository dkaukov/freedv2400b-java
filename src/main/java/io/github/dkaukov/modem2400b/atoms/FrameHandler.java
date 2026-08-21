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

@FunctionalInterface
public interface FrameHandler {
    /**
     * Handles an extracted frame. Voice frames carry the decoder's payload;
     * data frames have a payload length of zero. The payload array is borrowed
     * and is valid only during this callback.
     */
    void onFrame(byte[] payload, int offset, int length, DecodeResult result);
}
