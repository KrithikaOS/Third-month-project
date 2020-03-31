package com.filters;


import com.Constants.Constant;
import com.enums.ApiErrorCode;
import com.response.ApiResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;


@Provider
@ApiKeyCheck
public class ApiFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ApiResponse response = new ApiResponse();
        String header = requestContext.getHeaderString("API-KEY");
        if (header==null || !(header.equals(Constant.API_KEY))) {
            response.setCode(ApiErrorCode.UNAUTHORIZED);
            response.setError("No Authorization APIKey");
            requestContext.abortWith(Response.status(401).entity(response).build());
        }

    }

}
