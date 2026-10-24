package com.assetstree.jira.model;

import com.assetstree.jira.dto.FlowRuleDto;

import java.util.List;
import java.util.Set;

/**
 * Picks the equipment status for a request type. Rules already belong to one project.
 * A place is not equipment, so it keeps its status. An unknown status is ignored.
 */
public final class FlowRules {
    private FlowRules() {
    }

    public static String statusToApply(List<FlowRuleDto> rules, String issueTypeId, boolean equipment,
                                        String currentStatus, Set<String> knownStatuses) {
        if (!equipment || rules == null || knownStatuses == null) {
            return null;
        }
        String next = statusFor(rules, issueTypeId);
        if (next == null || !knownStatuses.contains(next)) {
            return null;
        }
        String current = currentStatus == null ? "" : currentStatus;
        if (current.equals(next)) {
            return null;
        }
        return next;
    }

    static String statusFor(List<FlowRuleDto> rules, String issueTypeId) {
        if (rules == null || issueTypeId == null || issueTypeId.trim().isEmpty()) {
            return null;
        }
        String wanted = issueTypeId.trim();
        String found = null;
        for (FlowRuleDto rule : rules) {
            if (rule == null || rule.getIssueTypeId() == null || rule.getStatusKey() == null) {
                continue;
            }
            if (!wanted.equals(rule.getIssueTypeId().trim())) {
                continue;
            }
            String status = rule.getStatusKey().trim();
            if (!status.isEmpty()) {
                found = status;
            }
        }
        return found;
    }
}
