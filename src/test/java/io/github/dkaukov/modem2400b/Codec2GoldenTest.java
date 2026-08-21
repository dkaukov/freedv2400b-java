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
import java.io.InputStream;
import java.nio.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class Codec2GoldenTest {
    private static final byte[] PAYLOAD=hex("11223344556670");
    @Test void framerAndTransmitterMatchCodec2Exactly() throws Exception {
        byte[] bits=new byte[96];VhfTypeAFramer.frame(PAYLOAD,0,bits);
        assertEquals("a7a711223367ad4455667272",packHex(bits));
        // The WAV metadata names PAYLOAD, but its actual first frame carries a3156e07b505c0.
        short[] encoded=new short[1920];new FreeDv2400bEncoder().encode(hex("a3156e07b505c0"),0,encoded,0);
        byte[] wav=readWavBytes();
        ByteBuffer b=ByteBuffer.wrap(wav,44,3840).slice().order(ByteOrder.LITTLE_ENDIAN);
        for(int i=0;i<1920;i++)assertEquals(b.getShort(),encoded[i],"sample "+i);
        ByteBuffer pcm=ByteBuffer.allocate(3840).order(ByteOrder.LITTLE_ENDIAN);for(short s:encoded)pcm.putShort(s);
        assertEquals("4a816f8167e952f4754300ae06957fbde4928322c3b2731298db6b1b497af4c8",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pcm.array())));
    }
    @Test void codec2WavDecodes24FramesAndResetRepeats() throws Exception {
        short[] pcm=readWav();
        String[] expected={"a3156e07b505c0","00a387b19f4270","81ac5b5f9e4550","1b50739da51200","2b4a2a47c211a0","bf3c46d9c6b150","ee3fed3ff52b70","633de8ed69fe70","d539abb7c54bf0","43c1cac19590f0","9df9d55fc246f0","b58b9dfd080a10","569276779c43d0","d9d52cf925cea0","c46d4dbfe79e70","8aa9ee4de8abd0","cf0d8e67d63520","f38fa8712c1170","0689305f634f50","ab4b5edd46abe0","f5aa2fa7ea69e0","ead11299b5bc00","f2ab1e3fe05370","0004020d2e8a50"};
        for(boolean invert:new boolean[]{false,true}) {
            FreeDv2400bDecoder d=new FreeDv2400bDecoder();byte[] out=new byte[7];MutableDecodeResult r=new MutableDecodeResult();int p=0,calls=0,frames=0,first=0;
            while(p+d.inputSamplesRequired()<=pcm.length){int n=d.inputSamplesRequired();if(invert)for(int i=0;i<n;i++)pcm[p+i]=(short)-pcm[p+i];d.decode(out,0,pcm,p,r);p+=n;calls++;if(r.framePresent()){if(frames==0)first=calls;assertEquals(expected[frames++],HexFormat.of().formatHex(out));}}
            assertEquals(24,frames);assertEquals(2,first);
            if(invert)for(int i=0;i<pcm.length;i++)pcm[i]=(short)-pcm[i];
        }
    }
    @Test void arbitraryChunkingMatchesExactDecode() throws Exception {
        short[] pcm=readWav();List<String> got=new ArrayList<>();StreamingDecoder s=new StreamingDecoder(new FreeDv2400bDecoder(),(p,o,n,r)->got.add(HexFormat.of().formatHex(p,o,o+n)));
        int[] sizes={1,5,10,159,160,1915,1920,1925};int p=0,i=0;while(p<pcm.length){int n=Math.min(sizes[i++%sizes.length],pcm.length-p);s.accept(pcm,p,n);p+=n;}
        assertEquals(24,got.size());assertEquals("a3156e07b505c0",got.get(0));assertEquals("0004020d2e8a50",got.get(23));
        s.reset();assertEquals(0,s.bufferedSamples());assertFalse(s.synchronizedNow());
    }
    @Test void unusedPayloadNibbleIsIgnored() {short[] a=new short[1920],b=new short[1920];byte[] p=PAYLOAD.clone(),q=PAYLOAD.clone();q[6]|=15;FreeDv2400bEncoder e=new FreeDv2400bEncoder();e.encode(p,0,a,0);e.encode(q,0,b,0);assertArrayEquals(a,b);}
    private static short[] readWav() throws Exception {byte[] wav=readWavBytes();ByteBuffer b=ByteBuffer.wrap(wav,44,wav.length-44).slice().order(ByteOrder.LITTLE_ENDIAN);short[] s=new short[b.remaining()/2];for(int i=0;i<s.length;i++)s[i]=b.getShort();return s;}
    private static byte[] readWavBytes() throws Exception {try(InputStream input=Codec2GoldenTest.class.getResourceAsStream("/modem_2400b_short.wav")){return Objects.requireNonNull(input).readAllBytes();}}
    private static String packHex(byte[] bits){byte[] p=new byte[bits.length/8];for(int i=0;i<bits.length;i++)p[i>>>3]|=bits[i]<<(7-(i&7));return HexFormat.of().formatHex(p);}
    private static byte[] hex(String s){return HexFormat.of().parseHex(s);}
}
