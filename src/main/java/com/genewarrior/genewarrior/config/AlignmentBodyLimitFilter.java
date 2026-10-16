package com.genewarrior.genewarrior.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genewarrior.genewarrior.api.dto.ApiErrorDto;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.time.Instant;
import java.util.Map;

@Component
public class AlignmentBodyLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 262144;
    private final ObjectMapper json;

    public AlignmentBodyLimitFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getServletPath().equals("/api/alignments") || request.getServletPath().equals("/api/primers") || request.getServletPath().equals("/api/trees"))
                || !request.getMethod().equals("POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        byte[] body = request.getInputStream().readNBytes(LIMIT + 1);
        if (body.length > LIMIT) {
            response.setStatus(413);
            response.setContentType("application/json");
            json.writeValue(response.getOutputStream(), new ApiErrorDto(Instant.now(), 413, "Payload Too Large",
                    "Tool request exceeds 256 KiB", Map.of()));
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public ServletInputStream getInputStream() {
                var input = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override
                    public int read() {
                        return input.read();
                    }

                    @Override
                    public int read(byte[] buffer, int off, int len) {
                        return input.read(buffer, off, len);
                    }

                    @Override
                    public boolean isFinished() {
                        return input.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        }, response);
    }
}
