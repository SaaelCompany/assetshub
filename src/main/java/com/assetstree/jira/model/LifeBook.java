package com.assetstree.jira.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lifecycle workbook. The first row names the columns. A blank cell leaves the
 * value already stored on the card. The object key picks the equipment.
 */
public final class LifeBook {
    public static final int MAX_ROWS = 5000;

    private static final String[] HEADERS = new String[] {
            "\u041a\u043b\u044e\u0447",
            "\u0414\u0430\u0442\u0430 \u043f\u043e\u043a\u0443\u043f\u043a\u0438",
            "\u041f\u043e\u0441\u0442\u0430\u0432\u0449\u0438\u043a",
            "\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442",
            "\u0426\u0435\u043d\u0430",
            "\u0412\u0430\u043b\u044e\u0442\u0430",
            "\u0414\u0430\u0442\u0430 \u0432\u0432\u043e\u0434\u0430",
            "\u0413\u0430\u0440\u0430\u043d\u0442\u0438\u044f \u0434\u043e",
            "\u0413\u0430\u0440\u0430\u043d\u0442\u0438\u0439\u043d\u044b\u0439 \u0441\u0435\u0440\u0432\u0438\u0441",
            "\u041f\u043e\u0434\u0440\u044f\u0434\u0447\u0438\u043a",
            "\u041d\u043e\u043c\u0435\u0440 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
            "\u041d\u0430\u0447\u0430\u043b\u043e \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
            "\u041e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435 \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430",
            "\u0421\u0440\u043e\u043a \u0441\u043b\u0443\u0436\u0431\u044b, \u043c\u0435\u0441.",
            "\u041e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u0430\u044f \u0441\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c"
    };

    private static final String[] FIELDS = new String[] {
            "key", "purchased", "supplier", "document", "price", "currency",
            "commissioned", "warrantyUntil", "warrantyService", "contractor",
            "contractNo", "contractFrom", "contractUntil", "lifeMonths", "residual"
    };

    private LifeBook() {
    }

    public static byte[] template() {
        List<String> headers = new ArrayList<String>();
        Collections.addAll(headers, HEADERS);
        return WorkbookSheet.write(Collections.singletonList(
                new WorkbookSheet.Page("Lifecycle", headers, new ArrayList<List<String>>())));
    }

    public static List<Change> read(List<EquipmentSheet.Sheet> sheets) {
        if (sheets == null || sheets.isEmpty() || sheets.get(0) == null) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        EquipmentSheet.Sheet sheet = sheets.get(0);
        List<String> headers = sheet.getHeaders();
        int[] columns = columns(headers);
        if (columns[0] < 0) {
            throw new AssetException(400, "asset-tree.error.import.workbook");
        }
        List<Change> changes = new ArrayList<Change>();
        List<EquipmentSheet.Record> records = sheet.getRecords();
        if (records == null) {
            return changes;
        }
        for (int i = 0; i < records.size(); i++) {
            if (changes.size() >= MAX_ROWS) {
                break;
            }
            EquipmentSheet.Record record = records.get(i);
            if (record == null) {
                continue;
            }
            String key = record.cell(columns[0]).trim();
            if (key.isEmpty()) {
                continue;
            }
            Map<String, String> fields = new LinkedHashMap<String, String>();
            for (int c = 1; c < FIELDS.length; c++) {
                if (columns[c] < 0) {
                    continue;
                }
                String value = record.cell(columns[c]).trim();
                if (!value.isEmpty()) {
                    fields.put(FIELDS[c], value);
                }
            }
            changes.add(new Change(record.getRow(), key, fields));
        }
        return changes;
    }

    /** Fills only the columns present in the change. A bad value returns an error key. */
    public static String apply(Stored stored, Change change) {
        if (stored == null || change == null) {
            return "asset-tree.error.life";
        }
        if (change.has("purchased")) {
            String date = FieldDates.canonical(change.get("purchased"));
            if (date == null) {
                return "asset-tree.error.life";
            }
            stored.purchased = date;
        }
        if (change.has("supplier")) {
            stored.supplier = trim(change.get("supplier"), 120);
            if (stored.supplier == null) {
                return "asset-tree.error.life";
            }
        }
        if (change.has("document")) {
            stored.document = trim(change.get("document"), 160);
            if (stored.document == null) {
                return "asset-tree.error.life";
            }
        }
        if (change.has("price")) {
            Long price = MoneyText.parse(change.get("price"));
            if (price == null) {
                return "asset-tree.error.life";
            }
            stored.priceMinor = price.longValue();
        }
        if (change.has("currency")) {
            String currency = change.get("currency").trim().toUpperCase(Locale.ROOT);
            if (currency.length() != 3) {
                return "asset-tree.error.life";
            }
            stored.currency = currency;
        }
        if (change.has("commissioned")) {
            String date = FieldDates.canonical(change.get("commissioned"));
            if (date == null) {
                return "asset-tree.error.life";
            }
            stored.commissioned = date;
        }
        if (change.has("warrantyUntil")) {
            String date = FieldDates.canonical(change.get("warrantyUntil"));
            if (date == null) {
                return "asset-tree.error.life";
            }
            stored.warrantyUntil = date;
        }
        if (change.has("warrantyService")) {
            stored.warrantyService = trim(change.get("warrantyService"), 160);
            if (stored.warrantyService == null) {
                return "asset-tree.error.life";
            }
        }
        if (change.has("contractor")) {
            stored.contractor = trim(change.get("contractor"), ContractorContract.NAME_LIMIT);
            if (stored.contractor == null) {
                return "asset-tree.error.life";
            }
        }
        if (change.has("contractNo")) {
            stored.contractNo = trim(change.get("contractNo"), ContractorContract.NUMBER_LIMIT);
            if (stored.contractNo == null) {
                return "asset-tree.error.life";
            }
        }
        if (change.has("contractFrom")) {
            String date = FieldDates.canonical(change.get("contractFrom"));
            if (date == null) {
                return "asset-tree.error.life";
            }
            stored.contractFrom = date;
        }
        if (change.has("contractUntil")) {
            String date = FieldDates.canonical(change.get("contractUntil"));
            if (date == null) {
                return "asset-tree.error.life";
            }
            stored.contractUntil = date;
        }
        if (change.has("lifeMonths")) {
            Integer months = months(change.get("lifeMonths"));
            if (months == null) {
                return "asset-tree.error.life";
            }
            stored.lifeMonths = months.intValue();
        }
        if (change.has("residual")) {
            Long residual = MoneyText.parse(change.get("residual"));
            if (residual == null) {
                return "asset-tree.error.life";
            }
            stored.residualMinor = residual.longValue();
        }
        if (stored.currency == null || stored.currency.trim().isEmpty()) {
            stored.currency = "RUB";
        }
        if (stored.residualMinor > stored.priceMinor) {
            return "asset-tree.error.life";
        }
        return ContractorContract.reject(stored.contractor, stored.contractNo, stored.contractFrom, stored.contractUntil);
    }

    private static Integer months(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        int dot = text.indexOf('.');
        if (dot >= 0) {
            text = text.substring(0, dot);
        }
        if (!text.matches("\\d{1,3}")) {
            return null;
        }
        int value = Integer.parseInt(text);
        if (value > 600) {
            return null;
        }
        return Integer.valueOf(value);
    }

    /** Null when the text is longer than the column. */
    private static String trim(String raw, int limit) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > limit) {
            return null;
        }
        return value;
    }

    private static int[] columns(List<String> headers) {
        int[] columns = new int[FIELDS.length];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = -1;
        }
        if (headers == null) {
            return columns;
        }
        for (int i = 0; i < headers.size(); i++) {
            String field = fieldOf(headers.get(i));
            if (field == null) {
                continue;
            }
            for (int c = 0; c < FIELDS.length; c++) {
                if (FIELDS[c].equals(field) && columns[c] < 0) {
                    columns[c] = i;
                }
            }
        }
        return columns;
    }

    private static String fieldOf(String header) {
        String name = norm(header);
        if (name.isEmpty()) {
            return null;
        }
        if ("key".equals(name) || "objectkey".equals(name)
                || "\u043a\u043b\u044e\u0447".equals(name) || "\u043a\u043b\u044e\u0447\u043e\u0431\u044a\u0435\u043a\u0442\u0430".equals(name)) {
            return "key";
        }
        if ("purchased".equals(name) || "purchasedate".equals(name)
                || "\u0434\u0430\u0442\u0430\u043f\u043e\u043a\u0443\u043f\u043a\u0438".equals(name) || "\u043f\u043e\u043a\u0443\u043f\u043a\u0430".equals(name)) {
            return "purchased";
        }
        if ("supplier".equals(name) || "\u043f\u043e\u0441\u0442\u0430\u0432\u0449\u0438\u043a".equals(name)) {
            return "supplier";
        }
        if ("document".equals(name) || "\u0434\u043e\u043a\u0443\u043c\u0435\u043d\u0442".equals(name)) {
            return "document";
        }
        if ("price".equals(name) || "\u0446\u0435\u043d\u0430".equals(name)) {
            return "price";
        }
        if ("currency".equals(name) || "\u0432\u0430\u043b\u044e\u0442\u0430".equals(name)) {
            return "currency";
        }
        if ("commissioned".equals(name) || "\u0434\u0430\u0442\u0430\u0432\u0432\u043e\u0434\u0430".equals(name)
                || "\u0432\u0432\u043e\u0434".equals(name) || "\u0432\u0432\u043e\u0434\u0432\u044d\u043a\u0441\u043f\u043b\u0443\u0430\u0442\u0430\u0446\u0438\u044e".equals(name)) {
            return "commissioned";
        }
        if ("warrantyuntil".equals(name) || "warranty".equals(name)
                || "\u0433\u0430\u0440\u0430\u043d\u0442\u0438\u044f\u0434\u043e".equals(name) || "\u0433\u0430\u0440\u0430\u043d\u0442\u0438\u044f".equals(name)) {
            return "warrantyUntil";
        }
        if ("warrantyservice".equals(name) || "\u0433\u0430\u0440\u0430\u043d\u0442\u0438\u0439\u043d\u044b\u0439\u0441\u0435\u0440\u0432\u0438\u0441".equals(name)) {
            return "warrantyService";
        }
        if ("contractor".equals(name) || "\u043f\u043e\u0434\u0440\u044f\u0434\u0447\u0438\u043a".equals(name)) {
            return "contractor";
        }
        if ("contractno".equals(name) || "contract".equals(name)
                || "\u043d\u043e\u043c\u0435\u0440\u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430".equals(name) || "\u0434\u043e\u0433\u043e\u0432\u043e\u0440".equals(name)) {
            return "contractNo";
        }
        if ("contractfrom".equals(name) || "\u043d\u0430\u0447\u0430\u043b\u043e\u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430".equals(name)) {
            return "contractFrom";
        }
        if ("contractuntil".equals(name) || "\u043e\u043a\u043e\u043d\u0447\u0430\u043d\u0438\u0435\u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0430".equals(name)) {
            return "contractUntil";
        }
        if ("lifemonths".equals(name) || "months".equals(name)
                || "\u0441\u0440\u043e\u043a\u0441\u043b\u0443\u0436\u0431\u044b\u043c\u0435\u0441".equals(name) || "\u0441\u0440\u043e\u043a\u0441\u043b\u0443\u0436\u0431\u044b".equals(name)) {
            return "lifeMonths";
        }
        if ("residual".equals(name) || "\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u0430\u044f\u0441\u0442\u043e\u0438\u043c\u043e\u0441\u0442\u044c".equals(name)
                || "\u043e\u0441\u0442\u0430\u0442\u043e\u0447\u043d\u0430\u044f".equals(name)) {
            return "residual";
        }
        return null;
    }

    private static String norm(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replace('\u0451', '\u0435');
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isLetterOrDigit(current)) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    /** Values already stored on the card. Blank text stays an empty string. */
    public static final class Stored {
        public String purchased = "";
        public String supplier = "";
        public String document = "";
        public String currency = "RUB";
        public String commissioned = "";
        public String warrantyUntil = "";
        public String warrantyService = "";
        public String contractor = "";
        public String contractNo = "";
        public String contractFrom = "";
        public String contractUntil = "";
        public long priceMinor;
        public long residualMinor;
        public int lifeMonths;
    }

    /** One data row. Fields hold only the filled cells. */
    public static final class Change {
        private final int row;
        private final String key;
        private final Map<String, String> fields;

        Change(int row, String key, Map<String, String> fields) {
            this.row = row;
            this.key = key;
            this.fields = fields == null ? new LinkedHashMap<String, String>() : fields;
        }

        public int getRow() {
            return row;
        }

        public String getKey() {
            return key;
        }

        public boolean has(String field) {
            return fields.containsKey(field);
        }

        public String get(String field) {
            String value = fields.get(field);
            return value == null ? "" : value;
        }
    }
}
