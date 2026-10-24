package com.assetstree.jira.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.assetstree.jira.PluginInfo;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class AssetPageServlet extends HttpServlet {
    private volatile String template;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser user = authenticationContext.getLoggedInUser();
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        if (user == null) {
            String query = request.getQueryString();
            String destination = "/plugins/servlet/asset-tree" + (query == null || query.isEmpty() ? "" : "?" + query);
            response.sendRedirect(contextPath + "/login.jsp?os_destination=" + URLEncoder.encode(destination, "UTF-8"));
            return;
        }
        I18nHelper i18n = authenticationContext.getI18nHelper();
        String language = i18n.getLocale() == null ? "en" : i18n.getLocale().toLanguageTag();
        String title = i18n.getText("asset-tree.ui.title");
        String css = contextPath + "/download/resources/" + PluginInfo.KEY + ":asset-tree-web/asset-tree.css?v=" + PluginInfo.VERSION;
        String qr = contextPath + "/download/resources/" + PluginInfo.KEY + ":asset-tree-web/qr-code.js?v=" + PluginInfo.VERSION;
        String js = contextPath + "/download/resources/" + PluginInfo.KEY + ":asset-tree-web/asset-tree.js?v=" + PluginInfo.VERSION;
        String rest = contextPath + "/rest/asset-tree/1.0";
        String project = request.getParameter("project");
        String view = viewOf(request.getParameter("view"));
        String html = template()
                .replace("@@LANG@@", escape(language))
                .replace("@@TITLE@@", escape(title))
                .replace("@@CSS@@", escape(css))
                .replace("@@QR@@", escape(qr))
                .replace("@@JS@@", escape(js))
                .replace("@@REST@@", escape(rest))
                .replace("@@PROJECT@@", escape(project == null ? "" : project))
                .replace("@@VIEW@@", escape(view));
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write(html);
    }

    private String template() throws IOException {
        String cached = template;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (template == null) {
                template = readTemplate();
            }
            return template;
        }
    }

    private String readTemplate() throws IOException {
        InputStream input = getClass().getResourceAsStream("/templates/page.html");
        if (input == null) {
            throw new IOException("Missing asset tree page template");
        }
        try {
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            input.close();
        }
    }

    private static String viewOf(String raw) {
        if (raw == null) {
            return "all";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if ("dashboard".equals(value)) {
            return "all";
        }
        if ("mine".equals(value) || "search".equals(value) || "settings".equals(value)
                || "schedules".equals(value) || "reports".equals(value) || "map".equals(value)) {
            return value;
        }
        return "all";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
