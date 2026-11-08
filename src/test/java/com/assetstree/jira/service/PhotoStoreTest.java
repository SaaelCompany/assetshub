package com.assetstree.jira.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PhotoStoreTest {
    @Test
    public void acceptsPngAndJpegAndKeepsItsOwnFile() {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
        byte[] gif = new byte[] {'G', 'I', 'F', '8'};
        assertEquals("png", PhotoStore.kindOf(png));
        assertEquals("jpeg", PhotoStore.kindOf(jpeg));
        assertEquals("", PhotoStore.kindOf(gif));
        assertEquals("image/jpeg", PhotoStore.contentType("jpeg"));
        assertEquals("image/png", PhotoStore.contentType("png"));
        assertEquals(2 * 1024 * 1024, PhotoStore.MAX_BYTES);
    }
}
