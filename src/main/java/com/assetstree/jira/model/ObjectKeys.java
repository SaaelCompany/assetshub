package com.assetstree.jira.model;

/**
 * Object keys already issued stay as they are.
 * New keys use the asset prefix stored for the project, not the Jira project key.
 */
public final class ObjectKeys {
    private ObjectKeys() {
    }

    public static String prefix(String projectKey) {
        String raw = projectKey == null ? "" : projectKey.trim();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char current = raw.charAt(i);
            if (current >= 'a' && current <= 'z') {
                builder.append((char) (current - 32));
            } else if ((current >= 'A' && current <= 'Z') || (current >= '0' && current <= '9')) {
                builder.append(current);
            }
        }
        if (builder.length() == 0) {
            return "AST";
        }
        if (builder.length() > 20) {
            builder.setLength(20);
        }
        return builder.toString();
    }

    public static String format(String projectKey, int number) {
        return prefix(projectKey) + "-" + number;
    }

    /** The number after {@code prefix-}, or 0 when the key is not that sequence. */
    public static int suffix(String prefix, String objectKey) {
        if (prefix == null || prefix.isEmpty() || objectKey == null) {
            return 0;
        }
        String head = prefix + "-";
        if (objectKey.length() <= head.length() || !objectKey.regionMatches(true, 0, head, 0, head.length())) {
            return 0;
        }
        String tail = objectKey.substring(head.length());
        if (tail.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < tail.length(); i++) {
            char current = tail.charAt(i);
            if (current < '0' || current > '9') {
                return 0;
            }
        }
        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /** A key parked while its object is in the trash. It does not occupy {@code prefix-number}. */
    public static boolean parked(String objectKey) {
        return objectKey != null && objectKey.startsWith("~");
    }

    /** Hides a live key so a new object can take the same number. The original key is kept after the dot. */
    public static String park(int id, String objectKey) {
        String key = objectKey == null ? "" : objectKey.trim();
        String head = "~" + id;
        if (key.isEmpty() || parked(key)) {
            return head.length() <= 32 ? head : head.substring(0, 32);
        }
        String parked = head + "." + key;
        if (parked.length() <= 32) {
            return parked;
        }
        return head.length() <= 32 ? head : head.substring(0, 32);
    }

    /** The key to give back on restore, or empty when this value is a live key. */
    public static String released(String objectKey) {
        if (!parked(objectKey)) {
            return "";
        }
        int dot = objectKey.indexOf('.');
        if (dot < 2 || dot >= objectKey.length() - 1) {
            return "";
        }
        for (int i = 1; i < dot; i++) {
            char current = objectKey.charAt(i);
            if (current < '0' || current > '9') {
                return "";
            }
        }
        return objectKey.substring(dot + 1);
    }

    /**
     * The next number to issue. It is one past the highest key that still belongs
     * to an object. A counter that ran ahead of deleted objects comes back.
     * A hole between two keys that still exist stays unused.
     */
    public static int nextNumber(String prefix, Iterable<String> keys) {
        int highest = 0;
        if (keys != null) {
            for (String key : keys) {
                int number = suffix(prefix, key);
                if (number > highest) {
                    highest = number;
                }
            }
        }
        if (highest == Integer.MAX_VALUE) {
            return highest;
        }
        return highest + 1;
    }

    /**
     * New numbers from 1 with no gaps. The current order of the numbers is kept.
     * A value that is not a real number sorts after the numbered keys.
     */
    public static int[] compact(int[] suffixes) {
        int count = suffixes == null ? 0 : suffixes.length;
        int[] numbers = new int[count];
        if (count == 0) {
            return numbers;
        }
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = Integer.valueOf(i);
        }
        java.util.Arrays.sort(order, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                int leftNumber = rank(suffixes[left.intValue()]);
                int rightNumber = rank(suffixes[right.intValue()]);
                if (leftNumber != rightNumber) {
                    return leftNumber < rightNumber ? -1 : 1;
                }
                return left.intValue() - right.intValue();
            }
        });
        for (int rank = 0; rank < order.length; rank++) {
            numbers[order[rank].intValue()] = rank + 1;
        }
        return numbers;
    }

    private static int rank(int suffix) {
        return suffix > 0 ? suffix : Integer.MAX_VALUE;
    }
}
