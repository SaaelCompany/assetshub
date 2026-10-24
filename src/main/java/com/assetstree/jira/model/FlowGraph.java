package com.assetstree.jira.model;

import com.assetstree.jira.dto.FlowGraphDto;
import com.assetstree.jira.dto.FlowLinkDto;
import com.assetstree.jira.dto.FlowNodeDto;
import com.assetstree.jira.dto.FlowRuleDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A project process is a few nodes and the lines between them.
 * A request node joined straight to an equipment status fires when equipment
 * is added on that request, at creation or any time later. A request status
 * between them fires when that request moves to the chosen status, and also
 * when equipment is added to a request that is already in that status.
 * A node with no line does nothing.
 */
public final class FlowGraph {
    public static final String REQUEST = "request";
    public static final String ISSUE = "issue";
    public static final String STATUS = "status";
    private static final Pattern ID = Pattern.compile("[a-z0-9]{4,16}");
    private static final int MAX_NODES = 40;
    private static final int MAX_LINKS = 80;

    private FlowGraph() {
    }

    public static FlowGraphDto normalize(FlowGraphDto raw) {
        FlowGraphDto graph = new FlowGraphDto();
        Map<String, FlowNodeDto> nodes = new LinkedHashMap<String, FlowNodeDto>();
        if (raw != null && raw.getNodes() != null) {
            for (FlowNodeDto node : raw.getNodes()) {
                FlowNodeDto clean = cleanNode(node);
                if (clean == null || nodes.containsKey(clean.getId())) {
                    continue;
                }
                nodes.put(clean.getId(), clean);
                if (nodes.size() >= MAX_NODES) {
                    break;
                }
            }
        }
        List<FlowLinkDto> links = new ArrayList<FlowLinkDto>();
        Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>();
        if (raw != null && raw.getLinks() != null) {
            for (FlowLinkDto link : raw.getLinks()) {
                if (link == null || link.getFrom() == null || link.getTo() == null) {
                    continue;
                }
                FlowNodeDto source = nodes.get(link.getFrom().trim().toLowerCase(Locale.ROOT));
                FlowNodeDto target = nodes.get(link.getTo().trim().toLowerCase(Locale.ROOT));
                if (source == null || target == null || source.getId().equals(target.getId()) || !allowed(source, target)) {
                    continue;
                }
                String key = source.getId() + "\n" + target.getId();
                if (seen.containsKey(key)) {
                    continue;
                }
                seen.put(key, Boolean.TRUE);
                FlowLinkDto clean = new FlowLinkDto();
                clean.setFrom(source.getId());
                clean.setTo(target.getId());
                links.add(clean);
                if (links.size() >= MAX_LINKS) {
                    break;
                }
            }
        }
        graph.setNodes(new ArrayList<FlowNodeDto>(nodes.values()));
        graph.setLinks(links);
        return graph;
    }

    /**
     * Equipment status for one request event. Adding equipment follows a direct
     * line from the request type to an equipment status. A transition, or equipment
     * added while the request is already in a status, follows a request-status
     * node with that Jira status id, then its line to equipment.
     * When that node has incoming request lines, one of them must be this type.
     * A request-status node with no incoming request line applies to every type.
     * The last matching line wins.
     */
    public static String equipmentStatus(FlowGraphDto graph, String issueTypeId, String issueStatusId, boolean onTransition) {
        FlowGraphDto clean = normalize(graph);
        Map<String, FlowNodeDto> nodes = index(clean);
        String found = null;
        for (FlowLinkDto link : clean.getLinks()) {
            FlowNodeDto source = nodes.get(link.getFrom());
            FlowNodeDto target = nodes.get(link.getTo());
            if (source == null || target == null || !STATUS.equals(target.getKind()) || blank(target.getStatusKey())) {
                continue;
            }
            if (!onTransition) {
                if (REQUEST.equals(source.getKind()) && same(source.getIssueTypeId(), issueTypeId)) {
                    found = target.getStatusKey().trim();
                }
                continue;
            }
            if (!ISSUE.equals(source.getKind()) || !same(source.getIssueStatusId(), issueStatusId)) {
                continue;
            }
            if (applies(clean, nodes, source, issueTypeId)) {
                found = target.getStatusKey().trim();
            }
        }
        return blank(found) ? null : found;
    }

    public static FlowGraphDto fromRules(List<FlowRuleDto> rules) {
        FlowGraphDto graph = new FlowGraphDto();
        if (rules == null) {
            return graph;
        }
        int row = 0;
        for (FlowRuleDto rule : rules) {
            if (rule == null || blank(rule.getIssueTypeId()) || blank(rule.getStatusKey())) {
                continue;
            }
            int seed = rule.getId() > 0 ? rule.getId() : row + 1;
            FlowNodeDto request = new FlowNodeDto();
            request.setId(nodeId("req", seed));
            request.setKind(REQUEST);
            request.setX(48);
            request.setY(36 + row * 140);
            request.setIssueTypeId(rule.getIssueTypeId().trim());
            request.setIssueTypeName(rule.getIssueTypeName());
            FlowNodeDto status = new FlowNodeDto();
            status.setId(nodeId("sta", seed));
            status.setKind(STATUS);
            status.setX(420);
            status.setY(36 + row * 140);
            status.setStatusKey(rule.getStatusKey().trim());
            graph.getNodes().add(request);
            graph.getNodes().add(status);
            FlowLinkDto link = new FlowLinkDto();
            link.setFrom(request.getId());
            link.setTo(status.getId());
            graph.getLinks().add(link);
            row++;
            if (graph.getNodes().size() >= MAX_NODES) {
                break;
            }
        }
        return normalize(graph);
    }

    /** One status per request type. A later line for the same type replaces the earlier one. */
    public static List<FlowRuleDto> pairs(FlowGraphDto graph) {
        FlowGraphDto clean = normalize(graph);
        Map<String, FlowNodeDto> nodes = new LinkedHashMap<String, FlowNodeDto>();
        for (FlowNodeDto node : clean.getNodes()) {
            nodes.put(node.getId(), node);
        }
        Map<String, String> statusByType = new LinkedHashMap<String, String>();
        for (FlowLinkDto link : clean.getLinks()) {
            FlowNodeDto source = nodes.get(link.getFrom());
            FlowNodeDto target = nodes.get(link.getTo());
            if (source == null || target == null || !REQUEST.equals(source.getKind()) || !STATUS.equals(target.getKind())) {
                continue;
            }
            if (blank(source.getIssueTypeId()) || blank(target.getStatusKey())) {
                continue;
            }
            statusByType.put(source.getIssueTypeId().trim(), target.getStatusKey().trim());
        }
        List<FlowRuleDto> pairs = new ArrayList<FlowRuleDto>();
        for (Map.Entry<String, String> entry : statusByType.entrySet()) {
            FlowRuleDto pair = new FlowRuleDto();
            pair.setIssueTypeId(entry.getKey());
            pair.setStatusKey(entry.getValue());
            pairs.add(pair);
        }
        return pairs;
    }

    public static String write(FlowGraphDto graph) {
        FlowGraphDto clean = normalize(graph);
        StringBuilder out = new StringBuilder();
        out.append("{\"nodes\":[");
        for (int i = 0; i < clean.getNodes().size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            writeNode(out, clean.getNodes().get(i));
        }
        out.append("],\"links\":[");
        for (int i = 0; i < clean.getLinks().size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            FlowLinkDto link = clean.getLinks().get(i);
            out.append("{\"from\":");
            quote(out, link.getFrom());
            out.append(",\"to\":");
            quote(out, link.getTo());
            out.append('}');
        }
        out.append("]}");
        return out.toString();
    }

    public static FlowGraphDto read(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new FlowGraphDto();
        }
        Object parsed = CardCanvas.parse(raw.trim());
        if (!(parsed instanceof Map)) {
            return new FlowGraphDto();
        }
        Map<?, ?> root = (Map<?, ?>) parsed;
        FlowGraphDto graph = new FlowGraphDto();
        Object nodes = root.get("nodes");
        if (nodes instanceof List) {
            for (Object item : (List<?>) nodes) {
                if (item instanceof Map) {
                    graph.getNodes().add(nodeOf((Map<?, ?>) item));
                }
            }
        }
        Object links = root.get("links");
        if (links instanceof List) {
            for (Object item : (List<?>) links) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> map = (Map<?, ?>) item;
                FlowLinkDto link = new FlowLinkDto();
                link.setFrom(text(map.get("from")));
                link.setTo(text(map.get("to")));
                graph.getLinks().add(link);
            }
        }
        return normalize(graph);
    }

    static void writeNode(StringBuilder out, FlowNodeDto node) {
        out.append("{\"id\":");
        quote(out, node.getId());
        out.append(",\"kind\":");
        quote(out, node.getKind());
        out.append(",\"x\":").append(node.getX());
        out.append(",\"y\":").append(node.getY());
        out.append(",\"issueTypeId\":");
        quote(out, node.getIssueTypeId() == null ? "" : node.getIssueTypeId());
        out.append(",\"issueStatusId\":");
        quote(out, node.getIssueStatusId() == null ? "" : node.getIssueStatusId());
        out.append(",\"statusKey\":");
        quote(out, node.getStatusKey() == null ? "" : node.getStatusKey());
        out.append('}');
    }

    private static FlowNodeDto nodeOf(Map<?, ?> map) {
        FlowNodeDto node = new FlowNodeDto();
        node.setId(text(map.get("id")));
        node.setKind(text(map.get("kind")));
        node.setX(coord(map.get("x")));
        node.setY(coord(map.get("y")));
        node.setIssueTypeId(text(map.get("issueTypeId")));
        node.setIssueStatusId(text(map.get("issueStatusId")));
        node.setStatusKey(text(map.get("statusKey")));
        return node;
    }

    private static FlowNodeDto cleanNode(FlowNodeDto node) {
        if (node == null || node.getId() == null) {
            return null;
        }
        String id = node.getId().trim().toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) {
            return null;
        }
        String kind = node.getKind() == null ? "" : node.getKind().trim();
        if (!REQUEST.equals(kind) && !ISSUE.equals(kind) && !STATUS.equals(kind)) {
            return null;
        }
        FlowNodeDto clean = new FlowNodeDto();
        clean.setId(id);
        clean.setKind(kind);
        clean.setX(clamp(node.getX()));
        clean.setY(clamp(node.getY()));
        if (REQUEST.equals(kind)) {
            clean.setIssueTypeId(clip(node.getIssueTypeId(), 60));
            clean.setIssueTypeName(clip(node.getIssueTypeName(), 255));
        } else if (ISSUE.equals(kind)) {
            clean.setIssueStatusId(clip(node.getIssueStatusId(), 60));
            clean.setIssueStatusName(clip(node.getIssueStatusName(), 255));
        } else {
            clean.setStatusKey(clip(node.getStatusKey(), 40));
        }
        return clean;
    }

    private static boolean allowed(FlowNodeDto source, FlowNodeDto target) {
        if (REQUEST.equals(source.getKind())) {
            return ISSUE.equals(target.getKind()) || STATUS.equals(target.getKind());
        }
        return ISSUE.equals(source.getKind()) && STATUS.equals(target.getKind());
    }

    private static boolean applies(FlowGraphDto graph, Map<String, FlowNodeDto> nodes, FlowNodeDto issue, String issueTypeId) {
        boolean gated = false;
        boolean match = false;
        for (FlowLinkDto link : graph.getLinks()) {
            if (!issue.getId().equals(link.getTo())) {
                continue;
            }
            FlowNodeDto from = nodes.get(link.getFrom());
            if (from == null || !REQUEST.equals(from.getKind())) {
                continue;
            }
            gated = true;
            if (same(from.getIssueTypeId(), issueTypeId)) {
                match = true;
            }
        }
        return !gated || match;
    }

    private static Map<String, FlowNodeDto> index(FlowGraphDto graph) {
        Map<String, FlowNodeDto> nodes = new LinkedHashMap<String, FlowNodeDto>();
        for (FlowNodeDto node : graph.getNodes()) {
            nodes.put(node.getId(), node);
        }
        return nodes;
    }

    private static boolean same(String stored, String wanted) {
        if (blank(stored) || blank(wanted)) {
            return false;
        }
        return stored.trim().equals(wanted.trim());
    }

    private static String nodeId(String prefix, int seed) {
        String body = Integer.toString(Math.abs(seed));
        String id = prefix + body;
        if (id.length() < 4) {
            id = prefix + "0000".substring(id.length()) + body;
        }
        if (id.length() > 16) {
            id = id.substring(0, 16);
        }
        return id;
    }

    private static int coord(Object value) {
        if (!(value instanceof Number)) {
            return 0;
        }
        return clamp((int) Math.round(((Number) value).doubleValue()));
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 2400) {
            return 2400;
        }
        return value;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            return trimmed.substring(0, max);
        }
        return trimmed;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }

    static void quote(StringBuilder out, String value) {
        out.append('"');
        String text = value == null ? "" : value;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c < 0x20 || c > 0x7e) {
                out.append(String.format(Locale.US, "\\u%04x", Integer.valueOf(c)));
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }
}
