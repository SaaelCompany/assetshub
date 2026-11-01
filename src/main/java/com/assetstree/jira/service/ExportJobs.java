package com.assetstree.jira.service;

import com.assetstree.jira.dto.ExportJobDto;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Builds a workbook off the request thread and keeps the bytes for a short download. */
public final class ExportJobs {
    private static final long TTL = 20L * 60L * 1000L;
    private static final int MAX = 6;
    private static final Map<String, Job> JOBS = new ConcurrentHashMap<String, Job>();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "asset-tree-export");
            thread.setDaemon(true);
            return thread;
        }
    });

    private ExportJobs() {
    }

    public interface Work {
        byte[] build() throws Exception;
    }

    public static ExportJobDto start(final String fileName, final Work work) {
        sweep();
        final String id = UUID.randomUUID().toString();
        final Job job = new Job(fileName);
        JOBS.put(id, job);
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                job.state = "running";
                try {
                    job.body = work.build();
                    job.state = "done";
                } catch (Exception ex) {
                    job.state = "failed";
                    job.message = ex.getMessage() == null ? "failed" : ex.getMessage();
                }
            }
        });
        return status(id);
    }

    public static ExportJobDto status(String id) {
        Job job = JOBS.get(id);
        ExportJobDto dto = new ExportJobDto();
        dto.setId(id);
        if (job == null) {
            dto.setState("missing");
            return dto;
        }
        dto.setState(job.state);
        dto.setMessage(job.message == null ? "" : job.message);
        return dto;
    }

    public static byte[] body(String id) {
        Job job = JOBS.get(id);
        if (job == null || !"done".equals(job.state)) {
            return null;
        }
        return job.body;
    }

    public static String fileName(String id) {
        Job job = JOBS.get(id);
        return job == null || job.fileName == null ? "export.xlsx" : job.fileName;
    }

    private static void sweep() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Job>> entries = JOBS.entrySet().iterator();
        while (entries.hasNext()) {
            if (now - entries.next().getValue().created > TTL) {
                entries.remove();
            }
        }
        while (JOBS.size() > MAX) {
            String oldest = null;
            long stamp = Long.MAX_VALUE;
            for (Map.Entry<String, Job> entry : JOBS.entrySet()) {
                if (entry.getValue().created < stamp) {
                    stamp = entry.getValue().created;
                    oldest = entry.getKey();
                }
            }
            if (oldest == null) {
                break;
            }
            JOBS.remove(oldest);
        }
    }

    private static final class Job {
        private final String fileName;
        private final long created = System.currentTimeMillis();
        private volatile String state = "queued";
        private volatile String message = "";
        private volatile byte[] body;

        private Job(String fileName) {
            this.fileName = fileName;
        }
    }
}
