package com.endpoint;

import com.daoImpl.UserDaoImpl;
import com.entities.User;
import com.enums.AccountType;
import com.filters.ApiKeyCheck;
import com.response.ApiResponse;
import com.services.TemplateService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;   
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

@Path("/")
@Produces(MediaType.TEXT_HTML)
public class AppEndpoint extends AbstractBaseApiEndpoint {
    static {
        {
            User user =new User();
            user.setId("test");
            user.setFirstName("ajith");
            user.setLastName("ajith");
            user.setPassword("123");
            user.setEmail("ajith@gmail.com");
            user.setCompany("FULL");
            user.setAccountType(AccountType.ADMIN);
            try {
                new UserDaoImpl().createClientAccount(user);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }
    @GET
    public Response app() throws IOException {
        String response;

        HttpSession session = servletRequest.getSession(false);
//        try {
            if (session != null) {
                if(session.getAttribute("accountType")==null){
                    session.invalidate();
                    Cookie[] cookies = servletRequest.getCookies();
                    if (cookies.length > 0) {
                        for (Cookie cookie : cookies) {
                            cookie.setMaxAge(0);
                            servletResponse.addCookie(cookie);
                        }
                    }
                    String result = TemplateService.modify(servletContext,new ArrayList<>(),"/QuizHubApplication/html/index.html");
                    return Response.status(200).entity(result).build();
                }
                if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
                    servletResponse.sendRedirect(servletRequest.getRequestURL()+"/QuizHubApplication/html/Dashboard.html");;

                    String result = TemplateService.modify(servletContext,new ArrayList<>(),"/QuizHubApplication/html/Dashboard.html");
                    return Response.status(302).entity(result).build();
                } else {
                    response="<h1>User account is not permitted</h1>";
                    return Response.status(403).entity(response).build();
                }
            } else {

                response="<h1>no session exist</h1>";
               // servletResponse.sendRedirect(servletRequest.getRequestURL()+"/QuizHubApplication/html/index.html");
                String result = TemplateService.modify(servletContext,new ArrayList<>(),"/QuizHubApplication/html/index.html");
                return Response.status(200).entity(result).build();

//                return Response.status(401).entity(response).build();
            }
//        } catch (Exception e) {
//            response=e.toString();
//            return Response.status(500).entity(response).build();
//        }
    }
}