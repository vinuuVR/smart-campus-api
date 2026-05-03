package com.smartcampus.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        logger.log(Level.SEVERE, "Unexpected error occurred", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)   // 500
                .entity(Map.of(
                    "status", 500,
                    "error", "Internal Server Error",
                    "message", "An unexpected error occurred. Please contact the administrator."
                ))
                .build();
    }
}