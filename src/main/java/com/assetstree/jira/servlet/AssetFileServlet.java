package com.assetstree.jira.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.assetstree.jira.dto.FileDto;
import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.service.AssetBridge;
import com.assetstree.jira.service.AssetFileStore;
import com.assetstree.jira.service.AssetService;
import com.assetstree.jira.service.StoredFile;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

public class AssetFileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApplicationUser user = user();
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        int id = parseId(request.getParameter("id"));
        if (id <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            StoredFile stored = service().openFile(user, id);
            String name = stored.getFileName() == null ? "file" : stored.getFileName().replace("\"", "").replace("\r", "").replace("\n", "");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(stored.getContentType() == null ? "application/octet-stream" : stored.getContentType());
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + name + "\"");
            response.setContentLength(stored.getData().length);
            response.getOutputStream().write(stored.getData());
        } catch (AssetException ex) {
            response.sendError(ex.getStatus());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ApplicationUser user = user();
        if (user == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        int assetId = parseId(queryValue(request.getQueryString(), "assetId"));
        if (assetId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            String type = request.getContentType() == null ? "" : request.getContentType();
            if (type.toLowerCase(Locale.ROOT).indexOf("multipart/") != 0) {
                throw new AssetException(400, "asset-tree.error.file.required");
            }
            byte[] body = readAll(request.getInputStream(), request.getContentLength());
            MultipartBody.FilePart upload = MultipartBody.firstFile(body, type);
            if (upload == null || upload.getData().length == 0) {
                throw new AssetException(400, "asset-tree.error.file.required");
            }
            if (upload.getData().length > AssetFileStore.MAX_BYTES) {
                throw new AssetException(400, "asset-tree.error.file.size");
            }
            FileDto saved = service().storeFile(user, assetId, upload.getFileName(), upload.getContentType(), upload.getData());
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("application/json; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"id\":" + saved.getId() + "}");
        } catch (AssetException ex) {
            writeError(response, ex);
        }
    }

    private static String queryValue(String query, String name) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        String[] pairs = query.split("&");
        for (int i = 0; i < pairs.length; i++) {
            int eq = pairs[i].indexOf('=');
            String key = eq >= 0 ? pairs[i].substring(0, eq) : pairs[i];
            if (!name.equals(decode(key))) {
                continue;
            }
            return eq >= 0 ? decode(pairs[i].substring(eq + 1)) : "";
        }
        return "";
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return value;
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private static byte[] readAll(InputStream input, int expected) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(expected, 0));
        byte[] buffer = new byte[8192];
        int read;
        int total = 0;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > AssetFileStore.MAX_BYTES + 65536) {
                throw new AssetException(400, "asset-tree.error.file.size");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static int parseId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void writeError(HttpServletResponse response, AssetException ex) throws IOException {
        response.setStatus(ex.getStatus());
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + jsonString(text(ex.getMessageKey())) + "\"}");
    }

    private String text(String key) {
        JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        I18nHelper i18n = authenticationContext == null ? null : authenticationContext.getI18nHelper();
        if (i18n == null) {
            return key;
        }
        String value = i18n.getText(key);
        return value == null || value.isEmpty() ? key : value;
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }

    private ApplicationUser user() {
        JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        return authenticationContext == null ? null : authenticationContext.getLoggedInUser();
    }

    private AssetService service() {
        return AssetBridge.service();
    }

}
