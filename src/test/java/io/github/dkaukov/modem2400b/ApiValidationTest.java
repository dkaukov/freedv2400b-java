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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ApiValidationTest {
    @Test void boundariesAreValidated(){
        FreeDv2400bEncoder e=new FreeDv2400bEncoder();assertThrows(IndexOutOfBoundsException.class,()->e.encode(new byte[6],0,new short[1920],0));assertThrows(IndexOutOfBoundsException.class,()->e.encode(new byte[7],0,new short[1919],0));
        FreeDv2400bDecoder d=new FreeDv2400bDecoder();assertThrows(IndexOutOfBoundsException.class,()->d.decode(new byte[7],0,new short[1919],0,new MutableDecodeResult()));
        StreamingDecoder s=new StreamingDecoder(d,(p,o,n,r)->{});assertThrows(IndexOutOfBoundsException.class,()->s.accept(new short[2],1,2));
    }
    @Test void streamingEncoderHonorsChunkSize(){
        int[] calls={0},total={0};StreamingEncoder s=new StreamingEncoder(new FreeDv2400bEncoder(),159,(a,o,n)->{calls[0]++;total[0]+=n;assertTrue(n<=159);});s.encode(new byte[7],0);assertEquals(13,calls[0]);assertEquals(1920,total[0]);
    }
}
