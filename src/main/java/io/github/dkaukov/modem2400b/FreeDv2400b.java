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

/** Constants for the raw-payload FreeDV 2400B modem. */
public final class FreeDv2400b {
    public static final int SAMPLE_RATE = 48_000;
    public static final int PAYLOAD_BITS = 52;
    public static final int PAYLOAD_BYTES = 7;
    public static final int FRAME_BITS = 96;
    public static final int TX_SAMPLES = 1_920;
    public static final int MAX_RX_INPUT = 1_925;
    private FreeDv2400b() { }
}
