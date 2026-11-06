package com.assetstree.jira.servlet;

import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AssetPageServletTest {
    @Test
    public void planScriptUsesThePlanExchangeResource() throws Exception {
        InputStream input = AssetPageServlet.class.getResourceAsStream("/templates/page.html");
        assertNotNull(input);
        InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[2048];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        reader.close();
        String plan = "/jira/download/resources/com.assetstree.jira.asset-tree:asset-tree-web/plan-exchange.js?v=1.3.124";
        String html = AssetPageServlet.fill(builder.toString(), "ru", "Assets", "asset-tree.css", "qr-code.js",
                plan, "asset-tree.js", "/jira/rest/asset-tree/1.0", "STP", "mine");
        assertTrue(html.contains(plan));
        assertFalse(html.contains("@@"));
    }
}
