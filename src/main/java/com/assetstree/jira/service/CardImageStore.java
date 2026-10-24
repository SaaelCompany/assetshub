package com.assetstree.jira.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * One PNG or JPEG per equipment card, stored beside the other asset files.
 */
public final class CardImageStore {
    public static final int MAX_BYTES = 512 * 1024;

    private CardImageStore() {
    }

    public static File file(int assetId) {
        return new File(AssetFileStore.directory(), "card-" + assetId);
    }

    public static void write(int assetId, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(file(assetId));
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    public static void delete(int assetId) {
        File stored = file(assetId);
        if (stored.isFile()) {
            stored.delete();
        }
    }

    /** png, jpeg, or an empty string when the bytes are neither. */
    public static String kindOf(byte[] data) {
        if (data == null) {
            return "";
        }
        if (data.length >= 8
                && (data[0] & 0xff) == 0x89
                && (data[1] & 0xff) == 0x50
                && (data[2] & 0xff) == 0x4e
                && (data[3] & 0xff) == 0x47) {
            return "png";
        }
        if (data.length >= 3
                && (data[0] & 0xff) == 0xff
                && (data[1] & 0xff) == 0xd8
                && (data[2] & 0xff) == 0xff) {
            return "jpeg";
        }
        return "";
    }

    public static String contentType(String kind) {
        return "jpeg".equals(kind) ? "image/jpeg" : "image/png";
    }
}
