package com.smartcampus.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        return Response.status(422)  // 422 Unprocessable Entity
                .entity(Map.of(
                    "status", 422,
                    "error", "Unprocessable Entity",
                    "message", exception.getMessage()
                ))
                .build();
    }
}