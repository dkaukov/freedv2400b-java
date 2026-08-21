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
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DeframerTest {
    private static final byte[] PAYLOAD={0x11,0x22,0x33,0x44,0x55,0x66,0x70};
    @Test void acquisitionAcceptsOneUwErrorAndBothPolarities(){
        for(boolean inverted:new boolean[]{false,true}){
            VhfTypeADeframer d=new VhfTypeADeframer();byte[] frame=frame();frame[40]^=1;if(inverted)invert(frame);byte[] out=new byte[7];
            assertTrue(d.accept(frame,out,0));assertTrue(d.synchronizedNow());assertEquals(FrameType.VOICE,d.frameType());assertEquals(1,d.errors());assertArrayEquals(PAYLOAD,out);
        }
    }
    @Test void trackingAcceptsThreeErrorsAndFifthMissDropsLock(){
        VhfTypeADeframer d=new VhfTypeADeframer();byte[] out=new byte[7];assertTrue(d.accept(frame(),out,0));
        byte[] three=frame();for(int i=0;i<3;i++)three[40+i]^=1;assertTrue(d.accept(three,out,0));assertTrue(d.synchronizedNow());assertEquals(3,d.errors());
        byte[] miss=frame();for(int i=0;i<8;i++)miss[40+i]^=1;
        for(int i=0;i<4;i++){assertTrue(d.accept(miss,out,0));assertTrue(d.synchronizedNow());}
        assertTrue(d.accept(miss,out,0));assertFalse(d.synchronizedNow());
    }
    private static byte[] frame(){byte[] b=new byte[96];VhfTypeAFramer.frame(PAYLOAD,0,b);return b;}
    private static void invert(byte[] b){for(int i=0;i<b.length;i++)b[i]^=1;}
}
