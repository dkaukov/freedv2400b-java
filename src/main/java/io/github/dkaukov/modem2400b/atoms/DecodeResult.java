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

import io.github.dkaukov.modem2400b.MutableDecodeResult;

/**
 * Immutable convenience result. Use {@link MutableDecodeResult} in real-time paths.
 */
public record DecodeResult(
        boolean framePresent,
        boolean synchronizedNow,
        FrameType frameType,
        int uniqueWordErrors,
        float discriminatorSnrDb,
        float clockOffsetPpm
) { }
