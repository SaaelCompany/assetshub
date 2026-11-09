package com.assetstree.jira.service;

import com.assetstree.jira.model.AssetException;
import com.assetstree.jira.model.TniComputer;
import com.assetstree.jira.model.TniDirectory;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads computers from a Configuration Manager site through AdminService.
 * The browser sign-in is not reused: the Jira server authenticates with the stored domain login.
 */
public final class TniSccm {
    public static final String SCCM = "asset-tree.error.tni.sccm";
    public static final String CERT = "asset-tree.error.tni.cert";
    public static final String EMPTY = "asset-tree.error.tni.empty";

    private static final String SELECT = "ResourceId,Name,ResourceDomainORWorkgroup,UserName,"
            + "OperatingSystemNameandVersion,ClientVersion,DistinguishedName";

    private TniSccm() {
    }

    public static final class Page {
        private final List<TniComputer> computers;
        private final String next;

        Page(List<TniComputer> computers, String next) {
            this.computers = computers;
            this.next = next;
        }

        public List<TniComputer> getComputers() {
            return computers;
        }

        public String getNext() {
            return next;
        }
    }

    public static List<TniComputer> read(String host, int port, boolean trust, String account, String password) {
        String start = "https://" + host + ":" + port
                + "/AdminService/wmi/SMS_R_System?$select=" + SELECT + "&$top=500";
        CloseableHttpClient client = null;
        try {
            client = client(host, port, trust, account, password);
            List<TniComputer> all = new ArrayList<TniComputer>();
            String url = start;
            int pages = 0;
            while (url != null && all.size() < TniDirectory.MAX_ROWS && pages < 8) {
                pages++;
                String body = get(client, url);
                Page page = parse(body);
                for (int i = 0; i < page.computers.size() && all.size() < TniDirectory.MAX_ROWS; i++) {
                    all.add(page.computers.get(i));
                }
                url = sameHost(host, page.next);
            }
            return all;
        } catch (AssetException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new AssetException(502, !trust && sslCause(ex) ? CERT : TniDirectory.CONNECT);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                    // The site conversation is already finished.
                }
            }
        }
    }

    public static Page parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new AssetException(502, SCCM);
        }
        String next = stringField(json, "@odata.nextLink");
        int valueAt = key(json, "value", 0);
        if (valueAt < 0) {
            throw new AssetException(502, SCCM);
        }
        int cursor = skip(json, valueAt);
        if (cursor >= json.length() || json.charAt(cursor) != '[') {
            throw new AssetException(502, SCCM);
        }
        cursor++;
        List<TniComputer> computers = new ArrayList<TniComputer>();
        while (cursor < json.length()) {
            cursor = skip(json, cursor);
            if (cursor >= json.length()) {
                break;
            }
            char mark = json.charAt(cursor);
            if (mark == ']') {
                break;
            }
            if (mark == ',') {
                cursor++;
                continue;
            }
            if (mark != '{') {
                throw new AssetException(502, SCCM);
            }
            int end = objectEnd(json, cursor);
            computers.add(computer(json.substring(cursor, end + 1)));
            cursor = end + 1;
        }
        List<TniComputer> clean = new ArrayList<TniComputer>();
        for (int i = 0; i < computers.size(); i++) {
            if (computers.get(i) != null) {
                clean.add(computers.get(i));
            }
        }
        return new Page(clean, next);
    }

    static String sameHost(String host, String link) {
        if (link == null || link.isEmpty() || host == null || host.isEmpty()) {
            return null;
        }
        String lower = link.toLowerCase(Locale.ROOT);
        String prefix = "https://" + host.toLowerCase(Locale.ROOT);
        if (lower.startsWith(prefix + "/") || lower.startsWith(prefix + ":")) {
            return link;
        }
        return null;
    }

    private static TniComputer computer(String object) {
        Map<String, String> fields = fields(object);
        String id = fields.get("resourceid");
        String name = fields.get("name");
        if (id == null || id.isEmpty() || name == null || name.isEmpty()) {
            return null;
        }
        String domain = fields.get("resourcedomainorworkgroup");
        String host = domain != null && domain.indexOf('.') > 0 ? name + "." + domain : "";
        return TniDirectory.computer("sccm", id, name,
                fields.get("operatingsystemnameandversion"), "",
                fields.get("username"), "", "", "", "",
                domain,
                fields.get("distinguishedname"),
                fields.get("clientversion"), false, host);
    }

    private static Map<String, String> fields(String object) {
        Map<String, String> fields = new LinkedHashMap<String, String>();
        int cursor = 1;
        while (cursor < object.length()) {
            cursor = skip(object, cursor);
            if (cursor >= object.length() || object.charAt(cursor) == '}') {
                break;
            }
            if (object.charAt(cursor) == ',') {
                cursor++;
                continue;
            }
            if (object.charAt(cursor) != '"') {
                break;
            }
            String key = readString(object, cursor);
            cursor = skip(object, cursor + key.length() + 2);
            if (cursor < object.length() && object.charAt(cursor) == ':') {
                cursor++;
            }
            cursor = skip(object, cursor);
            if (cursor >= object.length()) {
                break;
            }
            char mark = object.charAt(cursor);
            String value = "";
            if (mark == '"') {
                value = readString(object, cursor);
                cursor += span(object, cursor);
            } else if (mark == '-' || (mark >= '0' && mark <= '9')) {
                int start = cursor;
                if (object.charAt(cursor) == '-') {
                    cursor++;
                }
                while (cursor < object.length() && object.charAt(cursor) >= '0' && object.charAt(cursor) <= '9') {
                    cursor++;
                }
                value = object.substring(start, cursor);
            } else if (mark == '[') {
                value = joinArray(object, cursor);
                cursor = arrayEnd(object, cursor) + 1;
            } else {
                while (cursor < object.length() && object.charAt(cursor) != ',' && object.charAt(cursor) != '}') {
                    cursor++;
                }
            }
            if (!value.isEmpty()) {
                fields.put(key.toLowerCase(Locale.ROOT), value);
            }
        }
        return fields;
    }

    private static String stringField(String json, String name) {
        int at = key(json, name, 0);
        if (at < 0) {
            return "";
        }
        int cursor = skip(json, at);
        if (cursor >= json.length() || json.charAt(cursor) != '"') {
            return "";
        }
        return readString(json, cursor);
    }

    private static int key(String json, String name, int from) {
        String needle = "\"" + name + "\"";
        int at = json.indexOf(needle, from);
        if (at < 0) {
            return -1;
        }
        int cursor = skip(json, at + needle.length());
        if (cursor < json.length() && json.charAt(cursor) == ':') {
            return cursor + 1;
        }
        return -1;
    }

    private static int objectEnd(String json, int start) {
        int depth = 0;
        boolean quote = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quote) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quote = false;
                }
                continue;
            }
            if (c == '"') {
                quote = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new AssetException(502, SCCM);
    }

    private static int arrayEnd(String json, int start) {
        int depth = 0;
        boolean quote = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quote) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    quote = false;
                }
                continue;
            }
            if (c == '"') {
                quote = true;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new AssetException(502, SCCM);
    }

    private static String joinArray(String json, int start) {
        int end = arrayEnd(json, start);
        String body = json.substring(start + 1, end);
        StringBuilder joined = new StringBuilder();
        int cursor = 0;
        while (cursor < body.length()) {
            cursor = skip(body, cursor);
            if (cursor >= body.length()) {
                break;
            }
            if (body.charAt(cursor) == ',') {
                cursor++;
                continue;
            }
            if (body.charAt(cursor) != '"') {
                break;
            }
            String item = readString(body, cursor);
            if (!item.isEmpty()) {
                if (joined.length() > 0) {
                    joined.append(", ");
                }
                joined.append(item);
            }
            cursor += span(body, cursor);
        }
        return joined.toString();
    }

    private static String readString(String json, int quoteAt) {
        StringBuilder out = new StringBuilder();
        for (int i = quoteAt + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                if (next == 'u' && i + 4 < json.length()) {
                    int code = Integer.parseInt(json.substring(i + 1, i + 5), 16);
                    out.append((char) code);
                    i += 4;
                } else if (next == 'n') {
                    out.append('\n');
                } else {
                    out.append(next);
                }
                continue;
            }
            if (c == '"') {
                break;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static int span(String json, int quoteAt) {
        for (int i = quoteAt + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '"') {
                return i - quoteAt + 1;
            }
        }
        return json.length() - quoteAt;
    }

    private static int skip(String json, int cursor) {
        while (cursor < json.length() && json.charAt(cursor) <= ' ') {
            cursor++;
        }
        return cursor;
    }

    private static boolean sslCause(Throwable failure) {
        Throwable cursor = failure;
        while (cursor != null) {
            if (cursor instanceof SSLException || cursor instanceof CertificateException) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static CloseableHttpClient client(String host, int port, boolean trust, String account, String password)
            throws IOException {
        String[] login = login(account);
        BasicCredentialsProvider credentials = new BasicCredentialsProvider();
        credentials.setCredentials(new AuthScope(host, port, AuthScope.ANY_REALM, "NTLM"),
                new NTCredentials(login[0], password, "jira", login[1]));
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(8000)
                .setSocketTimeout(30000)
                .setRedirectsEnabled(false)
                .build();
        if (!trust) {
            return HttpClients.custom()
                    .setDefaultCredentialsProvider(credentials)
                    .setDefaultRequestConfig(config)
                    .build();
        }
        try {
            return HttpClients.custom()
                    .setDefaultCredentialsProvider(credentials)
                    .setDefaultRequestConfig(config)
                    .setSSLContext(SSLContextBuilder.create().loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] chain, String authType) {
                            return true;
                        }
                    }).build())
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    private static String[] login(String account) {
        int slash = account.indexOf('\\');
        if (slash > 0) {
            return new String[]{account.substring(slash + 1), account.substring(0, slash)};
        }
        int at = account.indexOf('@');
        if (at > 0 && at < account.length() - 1) {
            return new String[]{account.substring(0, at), account.substring(at + 1)};
        }
        return new String[]{account, ""};
    }

    private static String get(CloseableHttpClient client, String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");
        CloseableHttpResponse response = client.execute(request);
        try {
            int status = response.getStatusLine() == null ? 0 : response.getStatusLine().getStatusCode();
            String body = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity(), "UTF-8");
            if (status == 401) {
                throw new AssetException(401, TniDirectory.AUTH);
            }
            if (status == 404) {
                throw new AssetException(404, SCCM);
            }
            if (status < 200 || status >= 300) {
                throw new AssetException(502, TniDirectory.CONNECT);
            }
            if (body.length() > 4_000_000) {
                throw new AssetException(400, TniDirectory.ROWS);
            }
            return body;
        } finally {
            response.close();
        }
    }
}
