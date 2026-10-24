package com.assetstree.jira.service;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.util.JiraHome;
import com.assetstree.jira.model.AssetException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class AssetFileStore {
    public static final int MAX_BYTES = 10 * 1024 * 1024;

    private AssetFileStore() {
    }

    public static File directory() {
        JiraHome home = ComponentAccessor.getComponent(JiraHome.class);
        File dir = new File(home.getHomePath(), "data/asset-tree");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new AssetException(500, "asset-tree.error.unexpected");
        }
        return dir;
    }

    public static File file(int id) {
        return new File(directory(), Integer.toString(id));
    }

    public static void write(int id, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(file(id));
        try {
            out.write(data);
        } finally {
            out.close();
        }
    }

    public static void delete(int id) {
        File stored = new File(directory(), Integer.toString(id));
        if (stored.isFile()) {
            stored.delete();
        }
    }
}
