package com.assetstree.jira.service;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Recovers a UTF-8 file name that was read as ISO-8859-1. */
public final class FileNames {
    private static final Charset LATIN = Charset.forName("ISO-8859-1");

    private FileNames() {
    }

    public static String repair(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 255) {
                return value;
            }
        }
        byte[] raw = value.getBytes(LATIN);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            String utf8 = decoder.decode(ByteBuffer.wrap(raw)).toString();
            return utf8.isEmpty() ? value : utf8;
        } catch (CharacterCodingException ex) {
            return value;
        }
    }
}
