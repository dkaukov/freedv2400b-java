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

## Vector note

The checked-in `codec2_2400b_golden.json` payload metadata does not describe
the checked-in WAV: the WAV contains 25 different payloads, beginning with
`a3156e07b505c0`, rather than 25 copies of `11223344556670`. Its published WAV,
PCM, and first-frame hashes do match the file. Tests therefore retain the
`11223344556670` framing assertion and validate the WAV against the payloads
actually present in its independently generated Codec2 waveform.
