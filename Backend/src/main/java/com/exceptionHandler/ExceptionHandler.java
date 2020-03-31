package com.exceptionHandler;

import com.response.ApiResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

public class ExceptionHandler implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        ApiResponse response = new ApiResponse();
        response.setError(exception.toString());

        return Response.status(500).entity(response).build();
    }
}
