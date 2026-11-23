package com.assetstree.jira.model;

import java.text.Collator;
import java.util.Locale;

/** Name order used by the tree: case-insensitive, and 2 comes before 10. */
public final class NameOrder {
    private static final Collator RUSSIAN = Collator.getInstance(new Locale("ru"));

    static {
        RUSSIAN.setStrength(Collator.PRIMARY);
    }

    private NameOrder() {
    }

    public static int compare(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            if (isDigit(a.charAt(i)) && isDigit(b.charAt(j))) {
                int aStart = i;
                int bStart = j;
                while (i < a.length() && isDigit(a.charAt(i))) {
                    i++;
                }
                while (j < b.length() && isDigit(b.charAt(j))) {
                    j++;
                }
                int byNumber = compareDigits(a.substring(aStart, i), b.substring(bStart, j));
                if (byNumber != 0) {
                    return byNumber;
                }
                continue;
            }
            int aEnd = i;
            int bEnd = j;
            while (aEnd < a.length() && !isDigit(a.charAt(aEnd))) {
                aEnd++;
            }
            while (bEnd < b.length() && !isDigit(b.charAt(bEnd))) {
                bEnd++;
            }
            int byText = RUSSIAN.compare(a.substring(i, aEnd), b.substring(j, bEnd));
            if (byText != 0) {
                return byText;
            }
            i = aEnd;
            j = bEnd;
        }
        return Integer.compare(a.length(), b.length());
    }

    private static int compareDigits(String left, String right) {
        String a = stripZeros(left);
        String b = stripZeros(right);
        int byLength = Integer.compare(a.length(), b.length());
        if (byLength != 0) {
            return byLength;
        }
        int byValue = a.compareTo(b);
        if (byValue != 0) {
            return byValue;
        }
        return Integer.compare(left.length(), right.length());
    }

    private static String stripZeros(String digits) {
        int start = 0;
        while (start < digits.length() - 1 && digits.charAt(start) == '0') {
            start++;
        }
        return digits.substring(start);
    }

    private static boolean isDigit(char value) {
        return value >= '0' && value <= '9';
    }
}
