package com.assetstree.jira.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** The words a document template can ask the equipment card for. */
public final class DocFields {
    public static final class Source {
        public String name = "";
        public String objectKey = "";
        public String typeLabel = "";
        public String statusLabel = "";
        public String place = "";
        public String branch = "";
        public String address = "";
        public String city = "";
        public String mol = "";
        public String issued = "";
        public String role = "";
        public String phone = "";
        public String email = "";
        public String department = "";
        public String custodian = "";
        public String description = "";
        public String project = "";
        public String today = "";
        public String life = "";
        public String year = "";
        public String maker = "";
        public String model = "";
        public String group = "";
        public String request = "";
        public String quantity = "1";
        /** Field key to the text shown on the card. */
        public Map<String, String> fields = new LinkedHashMap<String, String>();
        /** Field key to the field title. */
        public Map<String, String> labels = new LinkedHashMap<String, String>();
    }

    private DocFields() {
    }

    public static Map<String, String> values(Source source) {
        Source card = source == null ? new Source() : source;
        Map<String, String> out = new LinkedHashMap<String, String>();
        put(out, card.name, "name", "наименование", "название");
        put(out, card.objectKey, "key", "ключ");
        put(out, card.typeLabel, "type", "тип");
        put(out, card.statusLabel, "status", "статус");
        put(out, card.place, "place", "площадка", "место", "кабинет", "отделение");
        put(out, first(card.branch, card.place), "филиал");
        put(out, card.address, "address", "адрес");
        put(out, card.city, "city", "город");
        put(out, card.mol, "mol", "мол");
        put(out, card.issued, "issued", "issuedto", "кому выдано", "выдано");
        put(out, card.role, "должность", "роль", "title");
        put(out, card.phone, "phone", "телефон", "тел");
        put(out, card.email, "email", "почта");
        put(out, card.department, "department", "отдел", "подразделение");
        put(out, card.custodian, "custodian", "ответственный");
        put(out, card.description, "description", "описание");
        put(out, card.project, "project", "организация", "клиника");
        put(out, card.today, "today", "date", "дата");
        put(out, card.life, "life", "срок");
        put(out, card.year, "year", "год");
        put(out, card.maker, "maker", "производитель");
        put(out, card.model, "model", "модель");
        put(out, card.group, "group", "группа");
        put(out, card.request, "request", "заявка");
        put(out, card.quantity, "qty", "количество");
        if (card.fields != null) {
            for (Map.Entry<String, String> entry : card.fields.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue();
                String label = card.labels == null ? "" : card.labels.get(key);
                remember(out, key, value);
                remember(out, label, value);
                if (matches(key, label, "serial", "серийн")) {
                    put(out, value, "serial", "серийный", "sn", "s/n");
                }
                if (matches(key, label, "inventory", "инвентар")) {
                    put(out, value, "inventory", "инвентарный", "инв");
                }
                if (matches(key, label, "maker", "manufacturer", "произвол")) {
                    put(out, value, "maker", "производитель");
                }
                if (matches(key, label, "model", "модел")) {
                    put(out, value, "model", "модель");
                }
                if (matches(key, label, "year", "год")) {
                    put(out, value, "year", "год");
                }
                if (matches(key, label, "life", "срок")) {
                    put(out, value, "life", "срок");
                }
                if (matches(key, label, "group", "групп")) {
                    put(out, value, "group", "группа");
                }
            }
        }
        return out;
    }

    private static void put(Map<String, String> out, String value, String... keys) {
        String text = value == null ? "" : value;
        for (int i = 0; i < keys.length; i++) {
            out.put(DocxFill.normalize(keys[i]), text);
        }
    }

    private static void remember(Map<String, String> out, String key, String value) {
        String token = DocxFill.normalize(key);
        if (token.isEmpty()) {
            return;
        }
        String text = value == null ? "" : value;
        String current = out.get(token);
        if (current == null || (current.isEmpty() && !text.isEmpty())) {
            out.put(token, text);
        }
    }

    private static boolean matches(String key, String label, String... needles) {
        String hay = DocxFill.normalize(key) + " " + DocxFill.normalize(label);
        for (int i = 0; i < needles.length; i++) {
            if (hay.contains(DocxFill.normalize(needles[i]))) {
                return true;
            }
        }
        return false;
    }

    private static String first(String preferred, String fallback) {
        if (preferred != null && !preferred.trim().isEmpty()) {
            return preferred;
        }
        return fallback == null ? "" : fallback;
    }
}
