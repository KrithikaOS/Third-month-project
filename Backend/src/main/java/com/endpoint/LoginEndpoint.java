package com.endpoint;

import com.daoImpl.UserDaoImpl;
import com.entities.User;
import com.enums.AccountType;
import com.filters.ApiKeyCheck;
import com.filters.SessionCheck;
import com.googlecode.objectify.ObjectifyService;
import com.response.ApiResponse;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;

@NoCache
@Path("/api/")
public class LoginEndpoint extends AbstractBaseApiEndpoint {
    static UserDaoImpl userOption;

    static {
        userOption = new UserDaoImpl();
    }

    @POST
    @Path("client/clientLogin")
    @ApiKeyCheck
    public Response adminLogin(User user) throws NoSuchAlgorithmException {
        ApiResponse response = new ApiResponse();

        HttpSession session = servletRequest.getSession(false);

            if(session !=null) {
                if (session.getAttribute("accountType") == null) {
                    session.invalidate();
                }
            }
            if (session != null) {

                    response.setError("Session already exists");
                    return Response.status(302).entity(response).build();

            } else {
                if (userOption.clientAuthenticate(user)) {

                    user = ObjectifyService.ofy().load().type(User.class).filter("email", user.getEmail()).first().now();
                    session = servletRequest.getSession(true);
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("firstName", user.getFirstName());
                    session.setAttribute("accountType", user.getAccountType());
                    session.setAttribute("email",user.getEmail());
                    response.setOk(true);
                    response.addData("data", "Account login successfull");
                    return Response.status(200).entity(response).build();
                } else {
                    response.setError("Invalid credentials");
                    return Response.status(400).entity(response).build();
                }
            }

    }

    @POST
    @Path("user/userLogin")
    public Response userLogin(User user) throws NoSuchAlgorithmException {
        ApiResponse response = new ApiResponse();

        HttpSession session = servletRequest.getSession(false);

            if(session !=null) {
                if (session.getAttribute("accountType") == null) {
                    session.invalidate();
                }
            }
            if (session != null) {
                response.setError("Session already exists");
                return Response.status(302).entity(response).build();
            } else {
                if (userOption.userAuthenticate(user)) {
                    session = servletRequest.getSession(true);
                    user = ObjectifyService.ofy().load().type(User.class).filter("email", user.getEmail()).first().now();
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("firstName", user.getFirstName());
                    session.setAttribute("accountType", user.getAccountType());
                    session.setAttribute("email",user.getEmail());
                   // session.setMaxInactiveInterval(11 * 60);
                    response.setOk(true);
                    response.addData("data", "Account login successfull");
                    return Response.status(200).entity(response).build();
                } else {
                    response.setError("Invalid credentials");
                    return Response.status(400).entity(response).build();

                }
            }

    }

    @POST
    @Path("client/clientSignup")
    @ApiKeyCheck
    public Response adminSignup(User user) throws NoSuchAlgorithmException {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }

            if (session != null) {
                response.setError("Session already exist");
                return Response.status(302).entity(response).build();
            }
            if (!userOption.checkValidDetails(user)) {
                response.setError("invalid input fields");
                return Response.status(400).entity(response).build();
            }
            if (userOption.createClientAccount(user)) {
                response.setOk(true);
                response.addData("data", "Account Created");
                return Response.status(200).entity(response).build();
            } else {
                response.setError("EmailId is already exist!");
                return Response.status(409).entity(response).build();
            }


    }

    @POST
    @Path("client/userSignup")
    @ApiKeyCheck
    @SessionCheck
    public Response userSignup(User user) throws NoSuchAlgorithmException {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }

            if (session == null) {
                response.setError(" no session exist");
                return Response.status(302).entity(response).build();
            }
            if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {

                if (!userOption.checkValidDetails(user)) {
                    response.setError("invalid input fields");
                    return Response.status(400).entity(response).build();
                }
                if (userOption.createUserAccount(user)) {
                    response.setOk(true);
                    response.addData("data", "Account Created");
                    response.addData("email",user.getEmail());
                    return Response.status(200).entity(response).build();
                } else {
                    response.setError("EmailId is already exist!");
                    return Response.status(409).entity(response).build();
                }
            } else {
                response.setError("User account is not permitted");
                return Response.status(401).entity(response).build();
            }


    }

    @GET
    @Path("/user/checkEmail")
    @ApiKeyCheck
    @SessionCheck
    public Response getUserEmail(@QueryParam("email") String email) {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }

            if (servletRequest.getSession(false) != null) {
                if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
                    if (email.matches("\\w+@\\w+\\.\\w+")) {
                        response.setError("invalid email");

                    }
                    if(session.getAttribute("email").toString().equals(email)){
                        response.setError("it is your email");
                        return Response.status(403).entity(response).build();
                    }
                    if (userOption.checkUserEmail(email)) {
                        response.setOk(true);
                        response.addData("message", "email found");
                        return Response.status(200).entity(response).build();
                    } else {
                        response.setError("email not found");
                        return Response.status(404).entity(response).build();
                    }


                } else {
                    response.setError("User account is not permitted");
                    return Response.status(401).entity(response).build();
                }
            } else {
                response.setError("no session exist");
                return Response.status(401).entity(response).build();
            }

    }


    @GET
    @Path("/logout")
    @ApiKeyCheck
    @SessionCheck
    public Response userLogut() {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }


            if (session != null) {
                session.invalidate();
                Cookie[] cookies = servletRequest.getCookies();
                if (cookies.length > 0) {
                    for (Cookie cookie : cookies) {
                        cookie.setMaxAge(0);
                        servletResponse.addCookie(cookie);
                    }
                }
                response.setOk(true);
                response.addData("data", "Account logged out");
                return Response.status(200).entity(response).build();
            } else {

                response.setError("Please login first");
                return Response.status(401).entity(response).build();
            }


    }
}
