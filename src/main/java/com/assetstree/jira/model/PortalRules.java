package com.assetstree.jira.model;

import com.assetstree.jira.dto.PortalRuleDto;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Picks the portal rule that should open the asset field.
 * A rule matches when every one of its fields has the expected option.
 * The rule with more matching fields wins, so "place A and department A"
 * opens the department, not the whole place. A deeper target breaks a tie.
 */
public final class PortalRules {
    private PortalRules() {
    }

    public static final class Answer {
        private final String label;
        private final String id;
        private final String name;
        private final String text;
        private final String value;

        public Answer(String label, String id, String name, String text, String value) {
            this.label = label;
            this.id = id;
            this.name = name;
            this.text = text;
            this.value = value;
        }
    }

    public static PortalRuleDto choose(List<PortalRuleDto> rules, List<Answer> answers, Map<Integer, Integer> parents) {
        PortalRuleDto best = null;
        int bestScore = -1;
        int bestDepth = -1;
        if (rules == null) {
            return null;
        }
        for (PortalRuleDto rule : rules) {
            if (rule == null || !matches(rule, answers)) {
                continue;
            }
            int score = rule.getConditions() == null ? 0 : rule.getConditions().size();
            int depth = depth(rule.getAssetId(), parents);
            if (score > bestScore || (score == bestScore && depth > bestDepth)) {
                best = rule;
                bestScore = score;
                bestDepth = depth;
            }
        }
        return best;
    }

    static boolean matches(PortalRuleDto rule, List<Answer> answers) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty() || answers == null) {
            return false;
        }
        for (PortalConditionDraft condition : rule.getConditions()) {
            if (!holds(condition, answers)) {
                return false;
            }
        }
        return true;
    }

    private static boolean holds(PortalConditionDraft condition, List<Answer> answers) {
        if (condition == null) {
            return false;
        }
        String field = tidy(condition.getField());
        String option = tidy(condition.getOption());
        if (field.isEmpty() || option.isEmpty()) {
            return false;
        }
        for (Answer answer : answers) {
            if (answer == null || norm(answer.value).isEmpty()) {
                continue;
            }
            if (!sameField(field, answer)) {
                continue;
            }
            if (option.equals(tidy(answer.text)) || option.equals(tidy(answer.value))) {
                return true;
            }
        }
        return false;
    }

    static int depth(int assetId, Map<Integer, Integer> parents) {
        if (parents == null || assetId <= 0 || !parents.containsKey(Integer.valueOf(assetId))) {
            return 0;
        }
        int depth = 0;
        Integer cursor = Integer.valueOf(assetId);
        int guard = 0;
        while (cursor != null && parents.containsKey(cursor) && guard < 40) {
            depth++;
            cursor = parents.get(cursor);
            guard++;
        }
        return depth;
    }

    static String norm(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static String tidy(String value) {
        String plain = value == null ? "" : value;
        plain = plain.replace('\u2013', '-').replace('\u2014', '-').replace('\u2212', '-');
        plain = plain.replaceAll("\\([^)]{0,40}\\)", " ");
        plain = plain.replaceAll("(?i)необязательно", " ");
        plain = plain.replaceAll("(?i)\\boptional\\b", " ");
        plain = plain.replace('*', ' ').replace(':', ' ');
        return norm(plain);
    }

    private static boolean sameField(String field, Answer answer) {
        if (field.isEmpty() || answer == null) {
            return false;
        }
        String label = tidy(answer.label);
        if (field.equals(label) || field.equals(norm(answer.id)) || field.equals(norm(answer.name))) {
            return true;
        }
        return label.startsWith(field + " ");
    }
}
