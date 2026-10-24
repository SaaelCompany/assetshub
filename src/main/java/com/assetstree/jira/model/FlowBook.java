package com.assetstree.jira.model;

import com.assetstree.jira.dto.FlowBookDto;
import com.assetstree.jira.dto.FlowGraphDto;
import com.assetstree.jira.dto.FlowLinkDto;
import com.assetstree.jira.dto.FlowNodeDto;
import com.assetstree.jira.dto.FlowProcessDto;
import com.assetstree.jira.dto.FlowRuleDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Several named processes for one project. A stored body that is still a single
 * graph is read as one process. The first process that matches a request wins.
 */
public final class FlowBook {
    /** Removed-status value that brings back the status the equipment had before it joined the request. */
    public static final String RESTORE = "_restore";
    private static final Pattern ID = Pattern.compile("[a-z0-9]{4,16}");
    private static final int MAX_PROCESSES = 20;
    private static final int NAME = 80;
    private static final Pattern STATUS_KEY = Pattern.compile("[a-z0-9_-]{1,40}");

    private FlowBook() {
    }

    public static List<FlowProcessDto> normalize(List<FlowProcessDto> raw) {
        List<FlowProcessDto> processes = new ArrayList<FlowProcessDto>();
        Map<String, Boolean> seen = new LinkedHashMap<String, Boolean>();
        if (raw == null) {
            return processes;
        }
        for (FlowProcessDto process : raw) {
            FlowProcessDto clean = clean(process);
            if (clean == null || seen.containsKey(clean.getId())) {
                continue;
            }
            seen.put(clean.getId(), Boolean.TRUE);
            processes.add(clean);
            if (processes.size() >= MAX_PROCESSES) {
                break;
            }
        }
        return processes;
    }

    public static FlowBookDto book(List<FlowProcessDto> processes) {
        FlowBookDto book = new FlowBookDto();
        book.setProcesses(normalize(processes));
        return book;
    }

    public static String equipmentStatus(List<FlowProcessDto> processes, String issueTypeId, String issueStatusId, boolean onTransition) {
        for (FlowProcessDto process : normalize(processes)) {
            String found = FlowGraph.equipmentStatus(graphOf(process), issueTypeId, issueStatusId, onTransition);
            if (found != null && !found.trim().isEmpty()) {
                return found;
            }
        }
        return null;
    }

    /**
     * Equipment status when it leaves a request of this type. A process that names request types
     * applies only to them; a process without request nodes applies to every type.
     */
    public static String removedStatus(List<FlowProcessDto> processes, String issueTypeId) {
        String wanted = issueTypeId == null ? "" : issueTypeId.trim();
        for (FlowProcessDto process : normalize(processes)) {
            String status = process.getRemovedStatus();
            if (status == null || status.isEmpty()) {
                continue;
            }
            boolean typed = false;
            boolean matches = false;
            for (FlowNodeDto node : process.getNodes()) {
                if (!FlowGraph.REQUEST.equals(node.getKind())) {
                    continue;
                }
                typed = true;
                if (node.getIssueTypeId() != null && node.getIssueTypeId().trim().equals(wanted)) {
                    matches = true;
                }
            }
            if (!typed || matches) {
                return status;
            }
        }
        return null;
    }

    /** Direct request-to-status pairs. The first process that names a request type keeps it. */
    public static List<FlowRuleDto> pairs(List<FlowProcessDto> processes) {
        Map<String, String> statusByType = new LinkedHashMap<String, String>();
        for (FlowProcessDto process : normalize(processes)) {
            for (FlowRuleDto pair : FlowGraph.pairs(graphOf(process))) {
                if (pair.getIssueTypeId() == null || statusByType.containsKey(pair.getIssueTypeId())) {
                    continue;
                }
                statusByType.put(pair.getIssueTypeId(), pair.getStatusKey());
            }
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

    public static String write(List<FlowProcessDto> processes) {
        List<FlowProcessDto> clean = normalize(processes);
        StringBuilder out = new StringBuilder();
        out.append("{\"processes\":[");
        for (int i = 0; i < clean.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            writeProcess(out, clean.get(i));
        }
        out.append("]}");
        return out.toString();
    }

    public static List<FlowProcessDto> read(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<FlowProcessDto>();
        }
        Object parsed = CardCanvas.parse(raw.trim());
        if (!(parsed instanceof Map)) {
            return new ArrayList<FlowProcessDto>();
        }
        Map<?, ?> root = (Map<?, ?>) parsed;
        Object processes = root.get("processes");
        if (processes instanceof List) {
            List<FlowProcessDto> read = new ArrayList<FlowProcessDto>();
            for (Object item : (List<?>) processes) {
                if (item instanceof Map) {
                    read.add(processOf((Map<?, ?>) item));
                }
            }
            return normalize(read);
        }
        FlowGraphDto graph = FlowGraph.read(raw);
        FlowProcessDto one = new FlowProcessDto();
        one.setId("legacy");
        one.setName("");
        one.setNodes(graph.getNodes());
        one.setLinks(graph.getLinks());
        List<FlowProcessDto> wrapped = new ArrayList<FlowProcessDto>();
        wrapped.add(one);
        return wrapped;
    }

    private static void writeProcess(StringBuilder out, FlowProcessDto process) {
        out.append("{\"id\":");
        FlowGraph.quote(out, process.getId());
        out.append(",\"name\":");
        FlowGraph.quote(out, process.getName() == null ? "" : process.getName());
        if (process.getRemovedStatus() != null && !process.getRemovedStatus().isEmpty()) {
            out.append(",\"removedStatus\":");
            FlowGraph.quote(out, process.getRemovedStatus());
        }
        out.append(",\"nodes\":[");
        List<FlowNodeDto> nodes = process.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            FlowGraph.writeNode(out, nodes.get(i));
        }
        out.append("],\"links\":[");
        List<FlowLinkDto> links = process.getLinks();
        for (int i = 0; i < links.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            FlowLinkDto link = links.get(i);
            out.append("{\"from\":");
            FlowGraph.quote(out, link.getFrom());
            out.append(",\"to\":");
            FlowGraph.quote(out, link.getTo());
            out.append('}');
        }
        out.append("]}");
    }

    private static FlowProcessDto processOf(Map<?, ?> map) {
        FlowProcessDto process = new FlowProcessDto();
        process.setId(text(map.get("id")));
        process.setName(text(map.get("name")));
        process.setRemovedStatus(text(map.get("removedStatus")));
        Object nodes = map.get("nodes");
        if (nodes instanceof List) {
            for (Object item : (List<?>) nodes) {
                if (item instanceof Map) {
                    process.getNodes().add(nodeOf((Map<?, ?>) item));
                }
            }
        }
        Object links = map.get("links");
        if (links instanceof List) {
            for (Object item : (List<?>) links) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<?, ?> linkMap = (Map<?, ?>) item;
                FlowLinkDto link = new FlowLinkDto();
                link.setFrom(text(linkMap.get("from")));
                link.setTo(text(linkMap.get("to")));
                process.getLinks().add(link);
            }
        }
        return process;
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

    private static FlowProcessDto clean(FlowProcessDto process) {
        if (process == null || process.getId() == null) {
            return null;
        }
        String id = process.getId().trim().toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) {
            return null;
        }
        FlowGraphDto graph = FlowGraph.normalize(graphOf(process));
        FlowProcessDto clean = new FlowProcessDto();
        clean.setId(id);
        clean.setName(clip(process.getName()));
        clean.setRemovedStatus(statusKey(process.getRemovedStatus()));
        clean.setNodes(graph.getNodes());
        clean.setLinks(graph.getLinks());
        return clean;
    }

    private static FlowGraphDto graphOf(FlowProcessDto process) {
        FlowGraphDto graph = new FlowGraphDto();
        graph.setNodes(process.getNodes());
        graph.setLinks(process.getLinks());
        return graph;
    }

    private static String clip(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > NAME) {
            return trimmed.substring(0, NAME);
        }
        return trimmed;
    }

    private static String statusKey(String value) {
        String key = value == null ? "" : value.trim();
        return STATUS_KEY.matcher(key).matches() ? key : "";
    }

    private static int coord(Object value) {
        if (!(value instanceof Number)) {
            return 0;
        }
        return (int) Math.round(((Number) value).doubleValue());
    }

    private static String text(Object value) {
        return value instanceof String ? (String) value : "";
    }
}
