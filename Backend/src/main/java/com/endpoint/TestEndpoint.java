package com.endpoint;

import com.daoImpl.QuestionDaoImpl;
import com.daoImpl.TestDaoImpl;
import com.entities.Question;
import com.entities.Test;
import com.entities.User;
import com.enums.AccountType;
import com.enums.TestStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filters.ApiKeyCheck;
import com.filters.SessionCheck;
import com.google.appengine.repackaged.com.google.protobuf.Api;
import com.googlecode.objectify.ObjectifyService;
import com.response.ApiResponse;
import com.services.TemplateService;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.*;
@NoCache
@Path("/api/test")
public class TestEndpoint extends AbstractBaseApiEndpoint {
    static TestDaoImpl testOption;

    @GET
    @Path("/getTests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllTest(){
        HttpSession session = servletRequest.getSession(false);
        ApiResponse response = new ApiResponse();
        if (session != null) {
            if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
                try{
                    List allTest =testOption.getAllTestByUser(session.getAttribute("userId").toString());
                    if(!allTest.isEmpty()){
                        response.setOk(true);
                        response.addData("testList",allTest);
                        return Response.status(200).entity(response).build();
                    }
                    else{
                        response.setError("No test found");
                        return Response.status(404).entity(response).build();
                    }
                }catch (NullPointerException exp){
                    response.setError("No test found");
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


    @POST
    @Path("/generateTestLink")
    @ApiKeyCheck
    @SessionCheck
    public Response generateTestLink(Test test) {
        //System.out.println(test.getQuesionIds());
        testOption = new TestDaoImpl();
        ApiResponse response = new ApiResponse();
        try{
        String testUrl = UUID.randomUUID().toString();
        HttpSession session = servletRequest.getSession(false);
            if(session !=null) {
                if (session.getAttribute("accountType") == null) {
                    session.invalidate();
                }
            }
        if (session != null) {
            if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
                if(!test.getUserEmail().matches(".+\\@.+\\..+")   || session.getAttribute("email").toString().equals(test.getUserEmail())){
                    response.setError("invalid email");
                    return Response.status(400).entity(response).build();
                }
                if(!testOption.checkTestValid(test.getQuestionIds())){
                    response.setError("given one of the question id is invalid");
                    return Response.status(400).entity(response).build();
                }
                test.setTestURL(testUrl);
                test.setCreatedBy(session.getAttribute("userId").toString());
                if (testOption.createTest(test)) {
                    response.setOk(true);
                    response.addData("testURL", "http://localhost:8080/api/test/" + testUrl);
                    return Response.status(200).entity(response).build();
                } else {
                    response.setError("Error while creating test");
                    return Response.status(400).entity(response).build();
                }
            } else {
                response.setError("User account is not permitted");
                return Response.status(401).entity(response).build();
            }
        } else {
            response.setError("no session exist");
            return Response.status(401).entity(response).build();
        }
        }catch(Exception e){
            response.setError(e.getMessage());
            return Response.status(500).entity(response).build();
        }
    }




    @GET
    @Path("/{testURL}")
    @Produces(MediaType.TEXT_HTML)
    public Response getTest(@PathParam("testURL") String testURL) throws IOException, ServletException {
        String response = "";
        testOption = new TestDaoImpl();
        List<String> data = new ArrayList<>();
        HttpSession session = servletRequest.getSession(false);
        Test test = ObjectifyService.ofy().load().type(Test.class).filter("testURL", testURL).first().now();
        if (test == null) {
            response = "no test exist";
            return Response.status(400).entity(response).build();
        }
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }
        if (session != null) {
            if (session.getAttribute("userId") != null && session.getAttribute("userId").toString().equals(test.getUserId())) {
                if(test.getStatus().equals(TestStatus.CANCELED)) {
                    response="test is "+test.getStatus();
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }
                if(test.getStatus().equals(TestStatus.ONGOING)){
                    test.setStatus(TestStatus.CANCELED);
                    testOption.saveTest(test);
                    response = "test canceled due to reload";
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }
                if(test.getStatus().equals(TestStatus.COMPLETED)){
                    servletResponse.sendRedirect(servletRequest.getRequestURL() + "/result");
                    return Response.status(302).entity("<h1>test completed</h1>").build();
                }

                servletResponse.sendRedirect(servletRequest.getRequestURL() + "/testStart");
                return Response.status(302).entity("<h1>session exist</h1>").build();
            } else {
                return Response.status(400).entity("<h1>This test is not for you</h1>").build();
            }
        }
        else {
            User user = ObjectifyService.ofy().load().type(User.class).id(test.getUserId()).now();
            test.setUserEmail(user.getEmail());

            Map<String, String> testDetails = new HashMap<>();
            testDetails.put("testId", test.getId());
            testDetails.put("userId", test.getUserId());
            data.add(Base64.getEncoder().encodeToString(new ObjectMapper().writeValueAsString(testDetails).getBytes()));
            data.add(test.getUserEmail());
            String content = TemplateService.modify(servletContext, data, "/resources/testLoginTemplate.html");
            return Response.status(200).entity(content).build();
        }

    }


    @GET
    @Path("/{testURL}/testStart")
    @Produces(MediaType.TEXT_HTML)
    public Response testStart (@PathParam("testURL") String testURL) throws IOException, ServletException {
        String response;
        testOption = new TestDaoImpl();
        List<String> data = new ArrayList<>();
        HttpSession session = servletRequest.getSession(false);
        Test test = ObjectifyService.ofy().load().type(Test.class).filter("testURL", testURL).first().now();
        if (test == null) {
            response = "no test exist";
            data.add(response);
            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
            return Response.status(400).entity(content).build();

        }
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }
        if (session != null) {
            if (session.getAttribute("userId") != null && session.getAttribute("userId").toString().equals(test.getUserId())) {
                if(test.getStatus().equals(TestStatus.COMPLETED)){
                    servletResponse.sendRedirect(servletRequest.getRequestURL().toString().replace("testStart","result"));
                    return Response.status(302).entity("<h1>test completed</h1>").build();
                }
                if(test.getStatus().equals(TestStatus.CANCELED) ){
                    response="test is "+test.getStatus();
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }

                if(test.getStatus().equals(TestStatus.ONGOING)){
                    test.setStatus(TestStatus.CANCELED);
                    testOption.saveTest(test);
                    response = "test canceled due to reload";
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }
                data.add(session.getAttribute("firstName").toString());
                data.add(Long.toString(test.getExpireTime()/60000) + "min");
                data.add(test.getTestURL());
                String content = TemplateService.modify(servletContext, data, "/resources/startTestTemplate.html");
                return Response.status(200).entity(content).build();

            }
            else{
                response ="This test not for you";
                data.add(response);
                String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                return Response.status(403).entity(content).build();

            }
        }else{
            response ="Please login for the test";
            data.add(response);
            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
            return Response.status(403).entity(content).build();
        }
    }




    @GET
    @Path("/{testURL}/doTest")
    @Produces(MediaType.TEXT_HTML)
    public Response doTest(@PathParam("testURL") String testURL) throws IOException, ServletException {
        String response;
        testOption = new TestDaoImpl();
        List<String> data = new ArrayList<>();
        HttpSession session = servletRequest.getSession(false);
        Test test = ObjectifyService.ofy().load().type(Test.class).filter("testURL", testURL).first().now();
        if (test == null) {
            response = "no test exist";
            data.add(response);
            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
            return Response.status(400).entity(content).build();

        }
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }

        if (session != null) {
            if (session.getAttribute("userId") != null && session.getAttribute("userId").toString().equals(test.getUserId())) {
                if(test.getStatus().equals(TestStatus.COMPLETED)){
                    servletResponse.sendRedirect(servletRequest.getRequestURL().toString().replace("doTest","result"));
                    return Response.status(302).entity("<h1>test completed</h1>").build();
                }
                if(test.getStatus().equals(TestStatus.CANCELED) ){
                    response="test is "+test.getStatus();
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }
                if(test.getStatus().equals(TestStatus.NOTSTARTED)){
                    test.setStatus(TestStatus.ONGOING);
                    testOption.saveTest(test);
                }
                else if(test.getStatus().equals(TestStatus.ONGOING)){
                    test.setStatus(TestStatus.CANCELED);
                    testOption.saveTest(test);
                    response = "test canceled due to reload";
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }
                List<Question> questionsList = new QuestionDaoImpl().getQuestionByIds(test.getQuestionIds());
                test.setQueList(questionsList);
                test.setQuestionIds(null);
                data.add(Base64.getEncoder().encodeToString(new ObjectMapper().writeValueAsString(test).getBytes()));
                String content = TemplateService.modify(servletContext, data, "/resources/testTemplate.html");
                return Response.status(200).entity(content).build();
            }
            else{
                response ="This test not for you";
                data.add(response);
                String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                return Response.status(403).entity(content).build();

            }
        }else{
            response ="Please login for the test";
            data.add(response);
            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
            return Response.status(403).entity(content).build();
        }
    }


    @POST
    @Path("/{testURL}/submitTest")
    @Produces(MediaType.TEXT_HTML)
    public Response validateTest(@PathParam("testURL") String testURL,Map testValues) throws IOException {
        String response;
        //System.out.println(testValues);
        testOption = new TestDaoImpl();
        List<String> data = new ArrayList<>();
        HttpSession session = servletRequest.getSession(false);
        Test test = ObjectifyService.ofy().load().type(Test.class).filter("testURL", testURL).first().now();
        if (test == null) {
            response = "no test exist";
            data.add(response);
            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
            return Response.status(400).entity(content).build();

        }
        if(session !=null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }
        if (session != null) {
            if (session.getAttribute("userId") != null && session.getAttribute("userId").toString().equals(test.getUserId())) {
                if(test.getStatus().equals(TestStatus.CANCELED) || test.getStatus().equals(TestStatus.NOTSTARTED) ){
                    response="test is "+test.getStatus();
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }
                if(test.getStatus().equals(TestStatus.ONGOING)){
                    test.setStatus(TestStatus.COMPLETED);

                    String result = testOption.validateTest(test,testValues);
                    test.setResult(result);
                    testOption.saveTest(test);
                    response = "Total score is "+result;
                    data.add(session.getAttribute("firstName").toString());
                    data.add(response);
                    session.invalidate();
                    String content = TemplateService.modify(servletContext, data, "/resources/resultPageTemplate.html");
                    return Response.status(200).entity(content).build();
                }
                else  if(test.getStatus().equals(TestStatus.COMPLETED)){

                    return Response.status(200).entity("<h1>test completed</h1>").build();
                }
                else{
                    response="test is "+test.getStatus();
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }
            }
            else{
                response ="This test not for you";
                data.add(response);
                String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                return Response.status(403).entity(content).build();

            }
        }else{
            response ="Please login for the test";
            data.add(response);
            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
            return Response.status(403).entity(content).build();
        }
    }

    @GET
    @Path("/{testURL}/result")
    @Produces(MediaType.TEXT_HTML)
    public Response result (@PathParam("testURL") String testURL) throws IOException {
        String response;
        testOption = new TestDaoImpl();
        List<String> data = new ArrayList<>();
        HttpSession session = servletRequest.getSession(false);
        Test test = ObjectifyService.ofy().load().type(Test.class).filter("testURL", testURL).first().now();
        if (test == null) {
            response = "no test exist";
            data.add(response);
            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
            return Response.status(400).entity(content).build();

        }

        if (session != null) {
            session.invalidate();
        }
//
//        if (session != null) {
//            if (session.getAttribute("userId") != null && session.getAttribute("userId").toString().equals(test.getUserId())) {

                if(test.getStatus().equals(TestStatus.CANCELED) || test.getStatus().equals(TestStatus.NOTSTARTED) ){
                    response="test is "+test.getStatus();
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }

                if(test.getStatus().equals(TestStatus.ONGOING)){
                    test.setStatus(TestStatus.CANCELED);
                    testOption.saveTest(test);
                    response = "test canceled due to reload";
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
                    return Response.status(403).entity(content).build();
                }

                    String result=test.getResult();
                    response = result;
                    User user = ObjectifyService.ofy().load().type(User.class).id(test.getUserId()).now();
                    data.add(user.getFirstName());
                    data.add(response);
                    String content = TemplateService.modify(servletContext, data, "/resources/resultPageTemplate.html");
                    return Response.status(200).entity(content).build();


//            }
//            else{
//                response ="This test not for you";
//                data.add(response);
//                String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
//                return Response.status(403).entity(content).build();
//
//            }
//        }else{
//            response ="Please login for the result";
//            data.add(response);
//            String content = TemplateService.modify(servletContext, data, "/resources/errorPageTemplate.html");
//            return Response.status(403).entity(content).build();
//        }
    }

}

