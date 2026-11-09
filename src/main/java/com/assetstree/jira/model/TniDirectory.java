package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Checks a directory connection and reads an AD or SCCM CSV export. */
public final class TniDirectory {
    public static final int MAX_ROWS = 2000;
    public static final String ACCOUNT = "asset-tree.error.tni.account";
    public static final String CONNECT = "asset-tree.error.tni.connect";
    public static final String AUTH = "asset-tree.error.tni.auth";
    public static final String BASE = "asset-tree.error.tni.base";
    public static final String SHEET = "asset-tree.error.tni.sheet";
    public static final String ROWS = "asset-tree.error.tni.rows";

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private TniDirectory() {
    }

    public static String host(String raw) {
        String host = raw == null ? "" : raw.trim();
        if (host.length() < 1 || host.length() > 253 || host.charAt(0) == '.' || host.charAt(host.length() - 1) == '.') {
            throw new AssetException(400, CONNECT);
        }
        if (host.indexOf("..") >= 0) {
            throw new AssetException(400, CONNECT);
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-';
            if (!ok) {
                throw new AssetException(400, CONNECT);
            }
        }
        return host;
    }

    public static int port(Integer raw, boolean ssl) {
        if (raw == null || raw.intValue() == 0) {
            return ssl ? 636 : 389;
        }
        int port = raw.intValue();
        if (port < 1 || port > 65535) {
            throw new AssetException(400, CONNECT);
        }
        return port;
    }

    /**
     * A search base is an LDAP distinguished name, not a SQL database and not the short domain
     * name. {@code budzdorov.ru} becomes {@code DC=budzdorov,DC=ru}. {@code BUDZDOROV} is left
     * empty so the controller can supply {@code defaultNamingContext}.
     */
    public static String base(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            return "";
        }
        if (text.indexOf('=') >= 0) {
            return distinguished(text);
        }
        String dns = fromDns(text);
        if (!dns.isEmpty()) {
            return dns;
        }
        if (netbios(text)) {
            return "";
        }
        throw new AssetException(400, BASE);
    }

    private static String distinguished(String base) {
        if (base.length() < 3 || base.length() > 400) {
            throw new AssetException(400, BASE);
        }
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (c < 32 || c == '/' || c == '\\' || c == ';' || c == '?' || c == '#' || c == '"' || c == '<' || c == '>') {
                throw new AssetException(400, BASE);
            }
        }
        return base;
    }

    private static String fromDns(String raw) {
        String text = raw.toLowerCase(Locale.ROOT);
        if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
        }
        if (text.indexOf('.') < 1 || text.indexOf("..") >= 0 || text.length() > 253) {
            return "";
        }
        boolean letter = false;
        String[] parts = text.split("\\.");
        if (parts.length < 2) {
            return "";
        }
        StringBuilder dn = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!dnsLabel(part)) {
                return "";
            }
            for (int c = 0; c < part.length(); c++) {
                char ch = part.charAt(c);
                if (ch >= 'a' && ch <= 'z') {
                    letter = true;
                }
            }
            if (dn.length() > 0) {
                dn.append(',');
            }
            dn.append("DC=").append(part);
        }
        if (!letter || dn.length() > 400) {
            return "";
        }
        return dn.toString();
    }

    private static boolean dnsLabel(String part) {
        if (part.isEmpty() || part.length() > 63 || part.charAt(0) == '-' || part.charAt(part.length() - 1) == '-') {
            return false;
        }
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static boolean netbios(String text) {
        if (text.length() < 1 || text.length() > 15) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static String account(String raw) {
        String account = raw == null ? "" : raw.trim();
        if (account.isEmpty() || account.length() > 200) {
            throw new AssetException(400, ACCOUNT);
        }
        for (int i = 0; i < account.length(); i++) {
            if (account.charAt(i) < 32) {
                throw new AssetException(400, ACCOUNT);
            }
        }
        return account;
    }

    public static String password(String raw) {
        String password = raw == null ? "" : raw;
        if (password.length() > 200) {
            throw new AssetException(400, ACCOUNT);
        }
        for (int i = 0; i < password.length(); i++) {
            if (password.charAt(i) < 32) {
                throw new AssetException(400, ACCOUNT);
            }
        }
        return password;
    }

    /** Active Directory stores objectGUID with the first three groups in little-endian order. */
    public static String guid(byte[] raw) {
        if (raw == null || raw.length != 16) {
            return "";
        }
        int[] order = {3, 2, 1, 0, 5, 4, 7, 6, 8, 9, 10, 11, 12, 13, 14, 15};
        char[] text = new char[36];
        int pos = 0;
        for (int i = 0; i < order.length; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                text[pos++] = '-';
            }
            int value = raw[order[i]] & 0xff;
            text[pos++] = HEX[value >>> 4];
            text[pos++] = HEX[value & 0x0f];
        }
        return new String(text);
    }

    public static TniComputer computer(String source, String identity, String name, String os, String version,
                                       String user, String ip, String mac, String maker, String model,
                                       String domain, String dn, String description, boolean disabled) {
        return computer(source, identity, name, os, version, user, ip, mac, maker, model,
                domain, dn, description, disabled, "");
    }

    public static TniComputer computer(String source, String identity, String name, String os, String version,
                                       String user, String ip, String mac, String maker, String model,
                                       String domain, String dn, String description, boolean disabled, String host) {
        String id = clip(plain(identity), 100);
        String label = clip(plain(name), 255);
        if (label.isEmpty()) {
            label = clip(id, 255);
        }
        if (id.isEmpty() || label.isEmpty()) {
            return null;
        }
        String kind = "sccm".equals(source) ? "sccm" : "ad";
        StringBuilder detail = new StringBuilder();
        add(detail, os);
        if (!plain(version).isEmpty() && plain(os).indexOf(plain(version)) < 0) {
            add(detail, version);
        }
        add(detail, user);
        add(detail, ip);
        add(detail, mac);
        add(detail, join(maker, model));
        add(detail, domain);
        add(detail, dn);
        add(detail, description);
        String text = detail.toString();
        if (text.length() > 4000) {
            text = text.substring(0, 4000);
        }
        return new TniComputer(kind, id, label, text, disabled, TniName.clean(host));
    }

    public static List<TniComputer> parseSheet(String csv) {
        if (csv == null) {
            throw new AssetException(400, SHEET);
        }
        String text = csv;
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        if (text.length() > 2_000_000) {
            throw new AssetException(400, ROWS);
        }
        char delim = delimiter(text);
        List<List<String>> table = split(text, delim);
        int headerAt = -1;
        for (int i = 0; i < table.size(); i++) {
            if (header(table.get(i)) != null) {
                headerAt = i;
                break;
            }
        }
        if (headerAt < 0) {
            throw new AssetException(400, SHEET);
        }
        Map<String, Integer> cols = header(table.get(headerAt));
        int data = 0;
        Map<String, TniComputer> found = new LinkedHashMap<String, TniComputer>();
        for (int i = headerAt + 1; i < table.size(); i++) {
            List<String> row = table.get(i);
            if (blank(row)) {
                continue;
            }
            data++;
            if (data > MAX_ROWS) {
                throw new AssetException(400, ROWS);
            }
            TniComputer computer = row(row, cols);
            if (computer != null) {
                found.put(computer.externalKey(), computer);
            }
        }
        if (found.isEmpty()) {
            throw new AssetException(400, SHEET);
        }
        return new ArrayList<TniComputer>(found.values());
    }

    private static TniComputer row(List<String> row, Map<String, Integer> cols) {
        String name = cell(row, cols, "name");
        String dns = cell(row, cols, "dns");
        String guid = guidText(cell(row, cols, "guid"));
        String resource = resource(cell(row, cols, "resource"));
        if (name.isEmpty() && !dns.isEmpty()) {
            int dot = dns.indexOf('.');
            name = dot > 0 ? dns.substring(0, dot) : dns;
        }
        if (name.isEmpty()) {
            name = resource;
        }
        String source;
        String identity;
        if (!resource.isEmpty()) {
            source = "sccm";
            identity = resource;
        } else if (!guid.isEmpty()) {
            source = "ad";
            identity = guid;
        } else if (!dns.isEmpty()) {
            source = "ad";
            identity = dns.toLowerCase(Locale.ROOT);
        } else if (!name.isEmpty()) {
            source = cols.containsKey("resource") ? "sccm" : "ad";
            identity = name.toLowerCase(Locale.ROOT);
        } else {
            return null;
        }
        return computer(source, identity, name,
                cell(row, cols, "os"), cell(row, cols, "version"), cell(row, cols, "user"),
                cell(row, cols, "ip"), cell(row, cols, "mac"), cell(row, cols, "maker"),
                cell(row, cols, "model"), cell(row, cols, "domain"), cell(row, cols, "dn"),
                cell(row, cols, "description"), disabled(cell(row, cols, "enabled")),
                cell(row, cols, "dns"));
    }

    private static Map<String, Integer> header(List<String> row) {
        Map<String, Integer> cols = new LinkedHashMap<String, Integer>();
        if (row == null) {
            return null;
        }
        for (int i = 0; i < row.size(); i++) {
            String key = column(row.get(i));
            if (!key.isEmpty() && !cols.containsKey(key)) {
                cols.put(key, Integer.valueOf(i));
            }
        }
        if (!cols.containsKey("name") && !cols.containsKey("dns") && !cols.containsKey("guid") && !cols.containsKey("resource")) {
            return null;
        }
        return cols;
    }

    static String column(String header) {
        String key = norm(header);
        if (same(key, "name", "computername", "cn", "netbiosname", "name0", "имя", "имякомпьютера", "название", "компьютер")) {
            return "name";
        }
        if (same(key, "dnshostname", "dns", "fqdn", "dnsимя", "имяdns")) {
            return "dns";
        }
        if (same(key, "operatingsystem", "operatingsystemname", "os", "caption0", "операционнаясистема", "ос")) {
            return "os";
        }
        if (same(key, "operatingsystemversion", "osversion", "version", "version0", "версия")) {
            return "version";
        }
        if (same(key, "distinguishedname", "dn", "различаемоеимя")) {
            return "dn";
        }
        if (same(key, "objectguid", "guid")) {
            return "guid";
        }
        if (same(key, "resourceid", "resource", "идресурса", "кодресурса")) {
            return "resource";
        }
        if (same(key, "username", "user", "lastlogonuser", "primaryuser", "пользователь", "имяпользователя")) {
            return "user";
        }
        if (same(key, "ipaddress", "ipv4address", "ip", "ipадрес", "айпи")) {
            return "ip";
        }
        if (same(key, "macaddress", "mac", "macадрес")) {
            return "mac";
        }
        if (same(key, "manufacturer", "производитель")) {
            return "maker";
        }
        if (same(key, "model", "модель")) {
            return "model";
        }
        if (same(key, "domain", "домен")) {
            return "domain";
        }
        if (same(key, "enabled", "включен", "включена")) {
            return "enabled";
        }
        if (same(key, "description", "описание")) {
            return "description";
        }
        return "";
    }

    private static boolean same(String key, String... names) {
        for (int i = 0; i < names.length; i++) {
            if (key.equals(names[i])) {
                return true;
            }
        }
        return false;
    }

    private static String cell(List<String> row, Map<String, Integer> cols, String key) {
        Integer at = cols.get(key);
        if (at == null || at.intValue() < 0 || at.intValue() >= row.size()) {
            return "";
        }
        String value = row.get(at.intValue());
        return value == null ? "" : value.trim();
    }

    private static boolean blank(List<String> row) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < row.size(); i++) {
            if (row.get(i) != null && !row.get(i).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean disabled(String value) {
        String key = norm(value);
        return key.equals("false") || key.equals("0") || key.equals("no") || key.equals("нет")
                || key.equals("disabled") || key.equals("отключен") || key.equals("отключена");
    }

    private static String resource(String value) {
        String text = value == null ? "" : value.trim();
        if (text.endsWith(".0") && text.length() > 2) {
            String head = text.substring(0, text.length() - 2);
            boolean digits = true;
            for (int i = 0; i < head.length(); i++) {
                if (!Character.isDigit(head.charAt(i))) {
                    digits = false;
                    break;
                }
            }
            if (digits && !head.isEmpty()) {
                return head;
            }
        }
        return text;
    }

    private static String guidText(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("{") && text.endsWith("}") && text.length() > 2) {
            text = text.substring(1, text.length() - 1);
        }
        if (text.length() != 36) {
            return "";
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean dash = c == '-' && (i == 8 || i == 13 || i == 18 || i == 23);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!dash && !hex) {
                return "";
            }
        }
        return text.toLowerCase(Locale.ROOT);
    }

    static String norm(String value) {
        String lower = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == ' ' || c == '_' || c == '-' || c == '.' || c == '/' || c == '\u00a0') {
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static char delimiter(String text) {
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(start, end);
            if (!line.startsWith("#") && (line.indexOf(',') >= 0 || line.indexOf(';') >= 0 || line.indexOf('\t') >= 0)) {
                return delimiterLine(line);
            }
            if (end == text.length()) {
                break;
            }
            start = end + 1;
        }
        return ',';
    }

    private static char delimiterLine(String line) {
        int commas = 0;
        int semis = 0;
        int tabs = 0;
        boolean quote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                quote = !quote;
            } else if (!quote && c == ',') {
                commas++;
            } else if (!quote && c == ';') {
                semis++;
            } else if (!quote && c == '\t') {
                tabs++;
            }
        }
        if (tabs > 0 && tabs >= commas && tabs >= semis) {
            return '\t';
        }
        if (semis > commas) {
            return ';';
        }
        return ',';
    }

    private static List<List<String>> split(String text, char delim) {
        List<List<String>> table = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean quote = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quote = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quote = true;
            } else if (c == delim) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                table.add(row);
                row = new ArrayList<String>();
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            table.add(row);
        }
        return table;
    }

    private static void add(StringBuilder detail, String value) {
        String text = plain(value);
        if (text.isEmpty()) {
            return;
        }
        if (detail.length() > 0) {
            detail.append(" · ");
        }
        detail.append(text);
    }

    private static String join(String left, String right) {
        String a = plain(left);
        String b = plain(right);
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return a + " " + b;
    }

    private static String plain(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 32) {
                out.append(c);
            }
        }
        return out.toString().trim();
    }

    private static String clip(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
