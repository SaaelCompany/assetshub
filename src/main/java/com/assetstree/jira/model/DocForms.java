package com.assetstree.jira.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** The act forms shipped with the plugin. A project can add its own beside these. */
public final class DocForms {
    public static final class Builtin {
        private final String id;
        private final String nameKey;
        private final String file;

        Builtin(String id, String nameKey, String file) {
            this.id = id;
            this.nameKey = nameKey;
            this.file = file;
        }

        public String getId() {
            return id;
        }

        public String getNameKey() {
            return nameKey;
        }

        public String getFile() {
            return file;
        }
    }

    public static final Builtin[] ALL = new Builtin[] {
            new Builtin("act-commission", "asset-tree.ui.docActCommission", "act-commission.docx"),
            new Builtin("act-atol", "asset-tree.ui.docActAtol", "act-atol.docx"),
            new Builtin("act-defect", "asset-tree.ui.docActDefect", "act-defect.docx"),
            new Builtin("act-repair", "asset-tree.ui.docActRepair", "act-repair.docx"),
            new Builtin("act-done", "asset-tree.ui.docActDone", "act-done.docx"),
            new Builtin("act-handover", "asset-tree.ui.docActHandover", "act-handover.docx")
    };

    private DocForms() {
    }

    public static Builtin find(String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim();
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].id.equals(wanted)) {
                return ALL[i];
            }
        }
        return null;
    }

    public static byte[] bytes(Builtin form) throws IOException {
        if (form == null) {
            throw new IOException("docx");
        }
        InputStream input = DocForms.class.getResourceAsStream("/docs/" + form.file);
        if (input == null) {
            throw new IOException("docx");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            input.close();
        }
    }
}
