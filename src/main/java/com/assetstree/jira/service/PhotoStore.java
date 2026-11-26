package com.assetstree.jira.service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * One photo on an equipment card. Every upload is stored as the same JPEG frame
 * so the plugin and the customer portal show one size.
 */
public final class PhotoStore {
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final int FRAME_WIDTH = 960;
    public static final int FRAME_HEIGHT = 640;
    private static final Color FRAME = new Color(244, 245, 247);

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

    /**
     * Crop a PNG or JPEG so it fills the shared frame. A picture that is already
     * that JPEG is returned unchanged.
     */
    public static byte[] frame(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("photo");
        }
        int orientation = orientation(data);
        if ("jpeg".equals(kindOf(data)) && orientation <= 1 && alreadyFrame(data)) {
            return data;
        }
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(data));
        if (source == null) {
            throw new IOException("photo");
        }
        BufferedImage turned = turn(source, orientation);
        BufferedImage canvas = new BufferedImage(FRAME_WIDTH, FRAME_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D pen = canvas.createGraphics();
        try {
            pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pen.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int sw = turned.getWidth();
            int sh = turned.getHeight();
            double scale = Math.max(FRAME_WIDTH / (double) sw, FRAME_HEIGHT / (double) sh);
            int dw = Math.max(FRAME_WIDTH, (int) Math.ceil(sw * scale));
            int dh = Math.max(FRAME_HEIGHT, (int) Math.ceil(sh * scale));
            int x = (FRAME_WIDTH - dw) / 2;
            int y = (FRAME_HEIGHT - dh) / 2;
            pen.drawImage(turned, x, y, dw, dh, null);
        } finally {
            pen.dispose();
        }
        return jpeg(canvas);
    }

    static int orientation(byte[] data) {
        if (data == null || data.length < 16) {
            return 1;
        }
        if ((data[0] & 0xff) != 0xff || (data[1] & 0xff) != 0xd8) {
            return 1;
        }
        int i = 2;
        while (i + 4 < data.length) {
            if ((data[i] & 0xff) != 0xff) {
                return 1;
            }
            int marker = data[i + 1] & 0xff;
            if (marker == 0xd8 || marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
                i += 2;
                continue;
            }
            int size = ((data[i + 2] & 0xff) << 8) | (data[i + 3] & 0xff);
            if (size < 2 || i + 2 + size > data.length) {
                return 1;
            }
            if (marker == 0xe1) {
                return app1Orientation(data, i + 4, size - 2);
            }
            if (marker == 0xda) {
                return 1;
            }
            i += 2 + size;
        }
        return 1;
    }

    private static int app1Orientation(byte[] data, int offset, int length) {
        if (length < 14 || offset < 0 || offset + length > data.length) {
            return 1;
        }
        if (data[offset] != 'E' || data[offset + 1] != 'x' || data[offset + 2] != 'i' || data[offset + 3] != 'f'
                || data[offset + 4] != 0 || data[offset + 5] != 0) {
            return 1;
        }
        int tiff = offset + 6;
        int end = offset + length;
        if (tiff + 8 > end) {
            return 1;
        }
        boolean little = data[tiff] == 'I' && data[tiff + 1] == 'I';
        boolean big = data[tiff] == 'M' && data[tiff + 1] == 'M';
        if (!little && !big) {
            return 1;
        }
        if (u16(data, tiff + 2, little) != 42) {
            return 1;
        }
        int ifd = tiff + u32(data, tiff + 4, little);
        if (ifd < tiff || ifd + 2 > end) {
            return 1;
        }
        int count = u16(data, ifd, little);
        int entry = ifd + 2;
        for (int n = 0; n < count; n++) {
            if (entry + 12 > end) {
                return 1;
            }
            int tag = u16(data, entry, little);
            if (tag == 0x0112) {
                int value = u16(data, entry + 8, little);
                return value >= 1 && value <= 8 ? value : 1;
            }
            entry += 12;
        }
        return 1;
    }

    private static int u16(byte[] data, int offset, boolean little) {
        int a = data[offset] & 0xff;
        int b = data[offset + 1] & 0xff;
        return little ? (b << 8) | a : (a << 8) | b;
    }

    private static int u32(byte[] data, int offset, boolean little) {
        int a = data[offset] & 0xff;
        int b = data[offset + 1] & 0xff;
        int c = data[offset + 2] & 0xff;
        int d = data[offset + 3] & 0xff;
        if (little) {
            return (d << 24) | (c << 16) | (b << 8) | a;
        }
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    private static boolean alreadyFrame(byte[] data) {
        int[] jpeg = jpegSize(data);
        if (jpeg != null) {
            return jpeg[0] == FRAME_WIDTH && jpeg[1] == FRAME_HEIGHT;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            return image != null && image.getWidth() == FRAME_WIDTH && image.getHeight() == FRAME_HEIGHT;
        } catch (IOException ex) {
            return false;
        }
    }

    /** Width and height from a JPEG SOF marker, without decoding the pixels. */
    static int[] jpegSize(byte[] data) {
        if (data == null || data.length < 10) {
            return null;
        }
        if ((data[0] & 0xff) != 0xff || (data[1] & 0xff) != 0xd8) {
            return null;
        }
        int i = 2;
        while (i + 9 < data.length) {
            if ((data[i] & 0xff) != 0xff) {
                return null;
            }
            int marker = data[i + 1] & 0xff;
            if (marker == 0xd8 || marker == 0x01 || (marker >= 0xd0 && marker <= 0xd7)) {
                i += 2;
                continue;
            }
            if (marker == 0xd9 || marker == 0xda) {
                return null;
            }
            int size = ((data[i + 2] & 0xff) << 8) | (data[i + 3] & 0xff);
            if (size < 2 || i + 2 + size > data.length) {
                return null;
            }
            if (marker >= 0xc0 && marker <= 0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc) {
                int height = ((data[i + 5] & 0xff) << 8) | (data[i + 6] & 0xff);
                int width = ((data[i + 7] & 0xff) << 8) | (data[i + 8] & 0xff);
                if (width > 0 && height > 0) {
                    return new int[] {width, height};
                }
                return null;
            }
            i += 2 + size;
        }
        return null;
    }

    static BufferedImage turn(BufferedImage source, int orientation) {
        if (source == null || orientation < 2 || orientation > 8) {
            return source;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        boolean swap = orientation >= 5;
        BufferedImage out = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_RGB);
        Graphics2D pen = out.createGraphics();
        try {
            pen.setColor(FRAME);
            pen.fillRect(0, 0, out.getWidth(), out.getHeight());
            pen.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            pen.drawImage(source, turnTransform(orientation, w, h), null);
        } finally {
            pen.dispose();
        }
        return out;
    }

    private static AffineTransform turnTransform(int orientation, int w, int h) {
        AffineTransform transform = new AffineTransform();
        if (orientation == 2) {
            transform.scale(-1.0, 1.0);
            transform.translate(-w, 0);
        } else if (orientation == 3) {
            transform.translate(w, h);
            transform.rotate(Math.PI);
        } else if (orientation == 4) {
            transform.scale(1.0, -1.0);
            transform.translate(0, -h);
        } else if (orientation == 5) {
            transform.rotate(-Math.PI / 2);
            transform.scale(-1.0, 1.0);
        } else if (orientation == 6) {
            transform.translate(h, 0);
            transform.rotate(Math.PI / 2);
        } else if (orientation == 7) {
            transform.scale(-1.0, 1.0);
            transform.translate(-h, 0);
            transform.translate(0, w);
            transform.rotate(3 * Math.PI / 2);
        } else if (orientation == 8) {
            transform.translate(0, w);
            transform.rotate(3 * Math.PI / 2);
        }
        return transform;
    }

    private static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.9f);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageOutputStream out = ImageIO.createImageOutputStream(bytes);
        try {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
            out.close();
        }
        return bytes.toByteArray();
    }
}
