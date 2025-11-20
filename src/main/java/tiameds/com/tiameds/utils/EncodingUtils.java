package tiameds.com.tiameds.utils;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * Utility helpers to normalize text input that may have been saved in a legacy encoding.
 * We prefer UTF-8, but fall back to Windows-1252 (common for Excel-exported CSVs on Windows)
 * whenever UTF-8 decoding reports malformed or unmappable sequences.
 */
public final class EncodingUtils {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private EncodingUtils() {
    }

    /**
     * Decode the provided bytes using UTF-8. If decoding fails due to malformed input,
     * retry using Windows-1252 before returning the final String.
     */
    public static String decodeWithUtf8Fallback(byte[] bytes) {
        try {
            return decode(bytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException utf8Error) {
            try {
                return decode(bytes, WINDOWS_1252);
            } catch (CharacterCodingException legacyError) {
                throw new IllegalArgumentException(
                        "Unable to decode content as UTF-8 or Windows-1252", legacyError);
            }
        }
    }

    private static String decode(byte[] bytes, Charset charset) throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(bytes));
        return charBuffer.toString();
    }
}

