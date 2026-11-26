package com.assetstree.jira.service;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void framesEveryPhotoAsTheSameJpeg() throws IOException {
        byte[] tall = PhotoStore.frame(png(20, 80, Color.RED));
        byte[] wide = PhotoStore.frame(png(80, 20, Color.BLUE));
        assertEquals("jpeg", PhotoStore.kindOf(tall));
        assertEquals("jpeg", PhotoStore.kindOf(wide));
        BufferedImage tallImage = ImageIO.read(new ByteArrayInputStream(tall));
        BufferedImage wideImage = ImageIO.read(new ByteArrayInputStream(wide));
        assertEquals(PhotoStore.FRAME_WIDTH, tallImage.getWidth());
        assertEquals(PhotoStore.FRAME_HEIGHT, tallImage.getHeight());
        assertEquals(PhotoStore.FRAME_WIDTH, wideImage.getWidth());
        assertEquals(PhotoStore.FRAME_HEIGHT, wideImage.getHeight());
        assertSame(tall, PhotoStore.frame(tall));
        int[] size = PhotoStore.jpegSize(tall);
        assertEquals(PhotoStore.FRAME_WIDTH, size[0]);
        assertEquals(PhotoStore.FRAME_HEIGHT, size[1]);
        assertTrue(reddish(tallImage.getRGB(4, 4)));
        assertTrue(reddish(tallImage.getRGB(PhotoStore.FRAME_WIDTH - 5, PhotoStore.FRAME_HEIGHT - 5)));
        assertTrue(bluish(wideImage.getRGB(4, 4)));
        assertTrue(bluish(wideImage.getRGB(PhotoStore.FRAME_WIDTH - 5, PhotoStore.FRAME_HEIGHT - 5)));
    }

    @Test
    public void cropsToTheCenterOfTheFrame() throws IOException {
        BufferedImage split = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D pen = split.createGraphics();
        pen.setColor(Color.RED);
        pen.fillRect(0, 0, 100, 100);
        pen.setColor(Color.BLUE);
        pen.fillRect(100, 0, 100, 100);
        pen.dispose();
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        ImageIO.write(split, "png", raw);
        BufferedImage framed = ImageIO.read(new ByteArrayInputStream(PhotoStore.frame(raw.toByteArray())));
        assertTrue(reddish(framed.getRGB(8, PhotoStore.FRAME_HEIGHT / 2)));
        assertTrue(bluish(framed.getRGB(PhotoStore.FRAME_WIDTH - 8, PhotoStore.FRAME_HEIGHT / 2)));
    }

    @Test
    public void turnsAPhonePhotoUprightBeforeFitting() throws IOException {
        byte[] sideways = jpegWithOrientation(png(40, 10, Color.RED), 6);
        assertEquals(6, PhotoStore.orientation(sideways));
        BufferedImage framed = ImageIO.read(new ByteArrayInputStream(PhotoStore.frame(sideways)));
        assertTrue(reddish(framed.getRGB(PhotoStore.FRAME_WIDTH / 2, 8)));
        assertTrue(reddish(framed.getRGB(8, PhotoStore.FRAME_HEIGHT / 2)));
    }

    private static byte[] png(int width, int height, Color color) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D pen = image.createGraphics();
        pen.setColor(color);
        pen.fillRect(0, 0, width, height);
        pen.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] jpegWithOrientation(byte[] png, int orientation) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", jpeg);
        byte[] raw = jpeg.toByteArray();
        byte[] app1 = exif(orientation);
        byte[] out = new byte[raw.length + app1.length];
        out[0] = raw[0];
        out[1] = raw[1];
        System.arraycopy(app1, 0, out, 2, app1.length);
        System.arraycopy(raw, 2, out, 2 + app1.length, raw.length - 2);
        return out;
    }

    private static byte[] exif(int orientation) {
        byte[] body = new byte[] {
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0, 42, 0, 0, 0, 8,
                0, 1,
                0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
                0, 0, 0, 0
        };
        byte[] out = new byte[body.length + 4];
        out[0] = (byte) 0xff;
        out[1] = (byte) 0xe1;
        out[2] = (byte) ((body.length + 2) >> 8);
        out[3] = (byte) ((body.length + 2) & 0xff);
        System.arraycopy(body, 0, out, 4, body.length);
        return out;
    }

    private static boolean reddish(int rgb) {
        return ((rgb >> 16) & 0xff) > 180 && ((rgb >> 8) & 0xff) < 80 && (rgb & 0xff) < 80;
    }

    private static boolean bluish(int rgb) {
        return ((rgb >> 16) & 0xff) < 80 && ((rgb >> 8) & 0xff) < 80 && (rgb & 0xff) > 180;
    }

}
