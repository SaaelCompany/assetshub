package com.assetstree.jira.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Posts a small JSON event. A failure is logged and does not roll back the asset. */
public final class WebhookBus {
    private static final Logger log = LoggerFactory.getLogger(WebhookBus.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "asset-tree-hook");
            thread.setDaemon(true);
            return thread;
        }
    });

    private WebhookBus() {
    }

    public static void post(final String url, final String secret, final String event, final String json) {
        if (url == null || url.isEmpty() || json == null) {
            return;
        }
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(url).openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(4000);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    connection.setRequestProperty("X-Asset-Tree-Event", event == null ? "" : event);
                    if (secret != null && !secret.isEmpty()) {
                        connection.setRequestProperty("X-Asset-Tree-Token", secret);
                    }
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(bytes.length);
                    OutputStream out = connection.getOutputStream();
                    try {
                        out.write(bytes);
                    } finally {
                        out.close();
                    }
                    connection.getResponseCode();
                } catch (Exception ex) {
                    log.warn("Webhook {} was not delivered", url, ex);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }
}
