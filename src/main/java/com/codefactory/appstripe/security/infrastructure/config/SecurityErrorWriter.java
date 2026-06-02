package com.codefactory.appstripe.security.infrastructure.config;

import com.codefactory.appstripe.common.api.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletResponse response,
            int status,
            String errorCode,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .details(List.of(message))
                .traceId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();

        objectMapper.writeValue(response.getWriter(), body);
    }
}
