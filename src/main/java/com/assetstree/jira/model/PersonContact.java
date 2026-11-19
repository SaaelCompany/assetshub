package com.assetstree.jira.model;

import java.util.Map;

/** Picks a person's phone, job title and department out of directory attributes and user properties. */
public final class PersonContact {
    private PersonContact() {
    }

    public static void apply(com.assetstree.jira.dto.UserProfileDto dto, Map<String, String> attributes) {
        if (dto == null || attributes == null) {
            return;
        }
        if (blank(dto.getPhone())) {
            dto.setPhone(phone(attributes));
        }
        if (blank(dto.getTitle())) {
            dto.setTitle(title(attributes));
        }
        if (blank(dto.getDepartment())) {
            dto.setDepartment(department(attributes));
        }
        if (blank(dto.getEmail())) {
            String mail = email(attributes);
            if (mail.indexOf('@') >= 0) {
                dto.setEmail(mail);
            }
        }
    }

    public static String phone(Map<String, String> attributes) {
        return best(attributes, true);
    }

    public static String title(Map<String, String> attributes) {
        return best(attributes, false);
    }

    public static String department(Map<String, String> attributes) {
        String winner = "";
        int score = 0;
        if (attributes == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String value = clean(entry.getValue());
            int next = departmentScore(entry.getKey());
            if (value.isEmpty() || next <= score) {
                continue;
            }
            score = next;
            winner = value;
        }
        return winner;
    }

    public static String email(Map<String, String> attributes) {
        if (attributes == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String value = clean(entry.getValue());
            String norm = DocxFill.normalize(entry.getKey());
            if (value.indexOf('@') >= 0 && (norm.contains("mail") || norm.contains("почт") || norm.contains("principal"))) {
                return value;
            }
        }
        return "";
    }

    private static String best(Map<String, String> attributes, boolean phone) {
        String winner = "";
        int score = 0;
        if (attributes == null) {
            return "";
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String value = clean(entry.getValue());
            int next = phone ? phoneScore(entry.getKey(), value) : titleScore(entry.getKey(), value);
            if (next <= score) {
                continue;
            }
            score = next;
            winner = value;
        }
        return winner;
    }

    static int phoneScore(String key, String value) {
        if (value == null || !hasDigit(value)) {
            return 0;
        }
        String norm = DocxFill.normalize(key);
        if (norm.isEmpty() || blocked(norm) || norm.contains("fax") || norm.contains("факс")
                || norm.contains("facsimile") || norm.contains("pager")) {
            return 0;
        }
        if (norm.contains("microphone") || norm.contains("headphone") || norm.contains("микрофон")) {
            return 0;
        }
        if (contains(norm, "telephonenumber", "telephone", "phone", "телефон")) {
            return 3;
        }
        if (contains(norm, "mobile", "cellphone", "cell", "мобил", "сотов")) {
            return 2;
        }
        if (contains(norm, "ipphone", "homephone", "othertelephone")) {
            return 1;
        }
        return 0;
    }

    static int titleScore(String key, String value) {
        if (!looksLikeTitle(value)) {
            return 0;
        }
        String norm = DocxFill.normalize(key);
        if (norm.isEmpty() || blocked(norm) || norm.contains("entitlement") || norm.contains("subtitle")
                || norm.contains("composition") || norm.contains("disposition")) {
            return 0;
        }
        if (contains(norm, "jobtitle", "должность")) {
            return 3;
        }
        if (norm.contains("title") && !norm.contains("untitled")) {
            return 3;
        }
        if (contains(norm, "position", "profession", "профес", "специальн", "звание")) {
            return 2;
        }
        return 0;
    }

    private static int departmentScore(String key) {
        String norm = DocxFill.normalize(key);
        if (norm.isEmpty() || blocked(norm) || norm.contains("distinguished") || norm.contains("memberof")) {
            return 0;
        }
        if (contains(norm, "department", "division", "отдел", "подраздел", "отделени")) {
            return 2;
        }
        if ("ou".equals(norm)) {
            return 1;
        }
        return 0;
    }

    private static boolean looksLikeTitle(String value) {
        String text = clean(value);
        if (text.isEmpty() || text.length() > 120 || !hasLetter(text) || digitsOnly(text)) {
            return false;
        }
        String norm = DocxFill.normalize(text);
        return !"user".equals(norm) && !"person".equals(norm) && !"employee".equals(norm)
                && !"mr".equals(norm) && !"mrs".equals(norm) && !"ms".equals(norm) && !"dr".equals(norm)
                && !"гн".equals(norm) && !"гжа".equals(norm) && !"господин".equals(norm) && !"госпожа".equals(norm);
    }

    private static boolean blocked(String norm) {
        return norm.contains("password") || norm.contains("token") || norm.contains("secret")
                || norm.contains("avatar") || norm.contains("photo");
    }

    private static boolean contains(String norm, String... needles) {
        for (int i = 0; i < needles.length; i++) {
            if (norm.contains(needles[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLetter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean digitsOnly(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetter(c)) {
                return false;
            }
        }
        return hasDigit(value);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
