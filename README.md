# FreeDV 2400B Java modem

Pure-Java raw-payload FreeDV 2400B modem compatible with Codec2 revision
`96e8a19c2487fd83bd981ce570f257aef42618f9`. It consumes and produces the
seven-byte, MSB-first packed 52-bit modem payload; Codec2 speech coding is not
included.

`FreeDv2400bEncoder` and `FreeDv2400bDecoder` are strict low-level APIs. The
decoder consumes exactly `inputSamplesRequired()` samples (1915, 1920, or
1925). Use its overload accepting `MutableDecodeResult` for allocation-free
steady-state operation. `StreamingEncoder` and `StreamingDecoder` adapt these
APIs to arbitrary callback chunk sizes. Instances are stateful and must not be
shared between streams.

```java
var encoder = new FreeDv2400bEncoder();
short[] pcm = new short[encoder.outputSamples()];
encoder.encode(payload, 0, pcm, 0);

var decoder = new FreeDv2400bDecoder();
var result = new MutableDecodeResult();
decoder.decode(outputPayload, 0, input, 0, result);
```

Run the interoperability and unit tests with `mvn test`.

## Codec2 attribution

This modem is a Java port closely based on the Codec2 project at revision
`96e8a19c2487fd83bd981ce570f257aef42618f9`, especially
[`fmfsk.c`](https://github.com/drowe67/codec2/blob/96e8a19c2487fd83bd981ce570f257aef42618f9/src/fmfsk.c)
and
[`freedv_vhf_framing.c`](https://github.com/drowe67/codec2/blob/96e8a19c2487fd83bd981ce570f257aef42618f9/src/freedv_vhf_framing.c).
Those sources identify Brady O'Brien as the original author and are copyright
David Rowe. This Java project remains licensed under GPLv3; see `LICENSE`.

## Test vector note

The checked-in test resource `modem_2400b_short.wav` is a Codec2-generated
waveform containing 25 distinct payloads, beginning with `a3156e07b505c0`.
Tests validate its published WAV/PCM data, retain the separate
`11223344556670` Type-A framing assertion, and verify the payload sequence
actually present in the waveform.

The longer `ve9qrp_2400b.wav` receiver fixture comes from the
[Debian FreeDV 1.4.3 source package](https://sources.debian.org/src/freedv/1.4.3~1gdc71a1c-1/wav/ve9qrp_2400b.wav/)
(SHA-256 `b14cac59215cef0f8d7ed290145b6fd48198a0ed1288fa7a7e547f32f2a78110`).
It verifies that the decoder acquires sync on a real recording, remains
synchronized, and extracts all 2,810 voice frames.
