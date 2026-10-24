package com.assetstree.jira.servlet;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.assetstree.jira.ao.ProjectViewEntity;
import com.assetstree.jira.model.ProjectModules;
import com.assetstree.jira.web.PortalLookup;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * The customer portal never renders the asset field template: the field stays a
 * text input. This filter adds the picker script to the portal HTML itself.
 */
public class PortalPageFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(PortalPageFilter.class);
    private static final int MAX_HTML = 3_000_000;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest http = (HttpServletRequest) request;
        String uri = http.getRequestURI() == null ? "" : http.getRequestURI();
        if (!"GET".equalsIgnoreCase(http.getMethod()) || uri.indexOf("/servicedesk/customer/") < 0) {
            chain.doFilter(request, response);
            return;
        }
        CapturingResponse capture = new CapturingResponse((HttpServletResponse) response);
        chain.doFilter(request, capture);
        if (capture.passedThrough()) {
            return;
        }
        byte[] body = capture.body();
        byte[] output = body;
        String type = capture.getContentType();
        int status = capture.status();
        if ((status == 200 || status == 0)
                && type != null
                && type.toLowerCase(java.util.Locale.US).contains("text/html")
                && body.length > 0
                && body.length < MAX_HTML) {
            try {
                output = PortalPageMarkup.insertBoot(body, boot(http));
            } catch (RuntimeException ex) {
                log.warn("Could not add the asset field to the customer portal", ex);
                output = body;
            }
        }
        if (output.length == 0) {
            return;
        }
        HttpServletResponse raw = (HttpServletResponse) response;
        raw.setContentLength(output.length);
        raw.getOutputStream().write(output);
    }

    private static byte[] boot(HttpServletRequest request) {
        String base = request.getContextPath() == null ? "" : request.getContextPath();
        int portalId = PortalLookup.portalId(request);
        String projectKey = "";
        String fieldIds = "";
        try {
            if (portalId > 0) {
                ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
                String key = PortalLookup.projectKeyForPortal(user, portalId);
                if (key != null) {
                    projectKey = key;
                }
            }
            fieldIds = PortalLookup.fieldIdList();
        } catch (RuntimeException ex) {
            log.debug("Portal project was not resolved for {}", portalId, ex);
        }
        if (!portalModuleOn(projectKey)) {
            return new byte[0];
        }
        return PortalPageMarkup.snippet(base, portalId, projectKey, fieldIds);
    }

    private static boolean portalModuleOn(String projectKey) {
        if (projectKey == null || projectKey.isEmpty()) {
            return false;
        }
        try {
            ActiveObjects ao = ComponentAccessor.getOSGiComponentInstanceOfType(ActiveObjects.class);
            if (ao == null) {
                return false;
            }
            ProjectViewEntity[] rows = ao.find(ProjectViewEntity.class, Query.select().where("PROJECT_KEY = ?", projectKey));
            if (rows == null || rows.length == 0 || rows[0] == null) {
                return false;
            }
            return ProjectModules.has(rows[0].getModuleList(), ProjectModules.PORTAL);
        } catch (RuntimeException ex) {
            log.debug("Portal module was not read for {}", projectKey, ex);
            return false;
        }
    }

    private static final class CapturingResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private ServletOutputStream stream;
        private PrintWriter writer;
        private boolean streamUsed;
        private boolean writerUsed;
        private boolean passed;
        private int status = 200;

        private CapturingResponse(HttpServletResponse response) {
            super(response);
        }

        private boolean passedThrough() {
            return passed;
        }

        private int status() {
            return status;
        }

        private byte[] body() {
            if (writer != null) {
                writer.flush();
            }
            return buffer.toByteArray();
        }

        @Override
        public void setStatus(int code) {
            status = code;
            super.setStatus(code);
        }

        @Override
        public void setStatus(int code, String message) {
            status = code;
            super.setStatus(code, message);
        }

        @Override
        public void sendError(int code) throws IOException {
            status = code;
            passed = true;
            super.sendError(code);
        }

        @Override
        public void sendError(int code, String message) throws IOException {
            status = code;
            passed = true;
            super.sendError(code, message);
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            passed = true;
            super.sendRedirect(location);
        }

        @Override
        public void setContentLength(int length) {
        }

        @Override
        public void setContentLengthLong(long length) {
        }

        @Override
        public void flushBuffer() {
        }

        @Override
        public void resetBuffer() {
            buffer.reset();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (writerUsed) {
                throw new IllegalStateException("getWriter() already called");
            }
            streamUsed = true;
            if (stream == null) {
                stream = new ServletOutputStream() {
                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener listener) {
                    }

                    @Override
                    public void write(int bite) {
                        buffer.write(bite);
                    }
                };
            }
            return stream;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (streamUsed) {
                throw new IllegalStateException("getOutputStream() already called");
            }
            writerUsed = true;
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(buffer, charset()), false);
            }
            return writer;
        }

        private Charset charset() {
            String name = getCharacterEncoding();
            if (name == null || name.isEmpty()) {
                return StandardCharsets.UTF_8;
            }
            try {
                return Charset.forName(name);
            } catch (RuntimeException ex) {
                return StandardCharsets.UTF_8;
            }
        }
    }
}
