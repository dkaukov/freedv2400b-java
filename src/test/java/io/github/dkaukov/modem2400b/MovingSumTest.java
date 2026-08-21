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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MovingSumTest {
    @Test
    void replacesOldestSampleAfterWindowFills() {
        MovingSum movingSum = new MovingSum(3);

        assertEquals(1, movingSum.push(1));
        assertEquals(3, movingSum.push(2));
        assertEquals(6, movingSum.push(3));
        assertEquals(9, movingSum.push(4));
        assertEquals(12, movingSum.push(5));
    }

    @Test
    void resetClearsWindowAndPointer() {
        MovingSum movingSum = new MovingSum(2);
        movingSum.push(7);
        movingSum.push(11);

        movingSum.reset();

        assertEquals(0, movingSum.sum());
        assertEquals(3, movingSum.push(3));
        assertEquals(8, movingSum.push(5));
    }

    @Test
    void completeNewWindowReplacesPreviousStateWithoutReset() {
        MovingSum movingSum = new MovingSum(3);
        movingSum.push(100);
        movingSum.push(200);
        movingSum.push(300);

        movingSum.push(1);
        movingSum.push(2);

        assertEquals(6, movingSum.push(3));
        assertEquals(9, movingSum.push(4));
    }

    @Test
    void rejectsNonPositiveWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> new MovingSum(0));
        assertThrows(IllegalArgumentException.class, () -> new MovingSum(-1));
    }
}
