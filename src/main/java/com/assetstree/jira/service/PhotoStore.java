package com.assetstree.jira.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * One PNG or JPEG shown on an equipment card. Separate from the picture printed on a label.
 */
public final class PhotoStore {
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    private PhotoStore() {
    }

    public static File file(int assetId) {
        return new File(AssetFileStore.directory(), "photo-" + assetId);
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

    public static String kindOf(byte[] data) {
        return CardImageStore.kindOf(data);
    }

    public static String contentType(String kind) {
        return CardImageStore.contentType(kind);
    }
}
