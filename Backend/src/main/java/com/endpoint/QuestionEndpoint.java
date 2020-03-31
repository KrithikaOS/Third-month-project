package com.endpoint;

import com.daoImpl.QuestionDaoImpl;
import com.entities.Question;
import com.entities.Rate;

import com.entities.User;
import com.enums.AccountType;
import com.filters.ApiKeyCheck;
import com.filters.SessionCheck;
import com.googlecode.objectify.ObjectifyService;
import com.response.ApiResponse;
import org.jboss.resteasy.annotations.cache.NoCache;

import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoCache
@Path("/api/question")
@ApiKeyCheck
@SessionCheck
public class QuestionEndpoint extends AbstractBaseApiEndpoint {
    static QuestionDaoImpl questionOption;

    static {
        questionOption = new QuestionDaoImpl();

    }

    //    @GET
//    public Response getAllQuestion(){
//        ApiResponse response=new ApiResponse();
//        Question question = new Question();
//        HttpSession session = servletRequest.getSession(false);
//        if(session==null){
//            response.setError("No session exist");
//            return Response.status(401).entity(response).build();
//        }else{
//
//            response = questionOption.getAllQuestion();
//            return  Response.status(200).entity(response).build();
//        }
//
//    }
    @POST
    @Path("/getQuestionByIds/")
    public Response getQuestionsByIds(List<String> questionIds) {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }
        try {
            if (session != null) {
                if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
                    if (questionIds.isEmpty()) {
                        response.setError("question ids is empty");
                        return Response.status(400).entity(response).build();
                    }
                    for (String questionId : questionIds) {
                        Question question = questionOption.getQuestionById(questionId);
                        if (question == null) {
                            response.setError("Given one of the question id is not found");
                            return Response.status(404).entity(response).build();
                        }
                        question.setCorrectAns(null);
                        response.addData(questionId, question);
                    }

                    response.setOk(true);

                    return Response.status(200).entity(response).build();
                } else {
                    response.setError("User account is not permitted");
                    return Response.status(401).entity(response).build();
                }
            } else {
                response.setError("no session exist");
                return Response.status(401).entity(response).build();
            }
        } catch (Exception e) {
            response.setError(e.getMessage());
            return Response.status(500).entity(response).build();
        }
    }

    @GET
    @Path("/getQuestion/{tag}")
    public Response getQuestionByTag(@PathParam("tag") String tag, @QueryParam("cursor") String cursor) {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);

        if (session != null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }


        try {
            if (servletRequest.getSession(false) != null) {
                if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
//                    Query<Question> query= ObjectifyService.ofy().load().type(Question.class).limit(Constant.QUESTION_LIMIT);
//                    if (cursor != null) {
//                        query = query.startAt(Cursor.fromWebSafeString(cursor));
//                    }
//                    boolean continu = false;
                    List<Question> questionList = new ArrayList<>();
//                    QueryResultIterator<Question> iterator = query.iterator();
//                    while (iterator.hasNext()) {
//                        questionList.add(iterator.next());
//                        continu = true;
//                    }
//                    if (continu) {
//                        Cursor cursorS = iterator.getCursor();
//
//                        // System.out.println("\n DEBUG: cursor: "+cursorS.toString());
//                        response.addData("cursor",cursorS.toWebSafeString());
//                    }

                    questionList = questionOption.getQuestionByTag(tag);
                    if (questionList.isEmpty()) {
                        response.setError("no questions in the tag");
                        return Response.status(404).entity(response).build();
                    }
                    response.setOk(true);
                    response.addData("questions", questionList);
                    return Response.status(200).entity(response).build();
                } else {
                    response.setError("User account is not permitted");
                    return Response.status(401).entity(response).build();
                }
            } else {
                response.setError("no session exist");
                return Response.status(401).entity(response).build();
            }
        } catch (Exception e) {
            response.setError(e.getMessage());
            return Response.status(500).entity(response).build();
        }
    }

    @GET
    @Path("/getAllQuestions")
    public Response fetchAllQuestion(@QueryParam("cursor") String cursor) {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);

        if (session != null) {
            if (session.getAttribute("accountType") == null) {
                session.invalidate();
            }
        }


        try {
            if (servletRequest.getSession(false) != null) {
                if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
//                    Query<Question> query= ObjectifyService.ofy().load().type(Question.class).limit(Constant.QUESTION_LIMIT);
//                    if (cursor != null) {
//                        query = query.startAt(Cursor.fromWebSafeString(cursor));
//                    }
//                    boolean continu = false;
                    List<Question> questionList = new ArrayList<>();
//                    QueryResultIterator<Question> iterator = query.iterator();
//                    while (iterator.hasNext()) {
//                        questionList.add(iterator.next());
//                        continu = true;
//                    }
//                    if (continu) {
//                        Cursor cursorS = iterator.getCursor();
//
//                        // System.out.println("\n DEBUG: cursor: "+cursorS.toString());
//                        response.addData("cursor",cursorS.toWebSafeString());
//                    }
                    String userId=(session.getAttribute("userId").toString());
                    com.googlecode.objectify.Key userKey= com.googlecode.objectify.Key.create(User.class,userId);
                    User user= (User) ObjectifyService.ofy().load().key(userKey).now();
                    questionList = questionOption.getAllQuestions();
                    if (questionList.isEmpty()) {
                        response.setError("no questions found");
                        return Response.status(404).entity(response).build();
                    }

                    response.setOk(true);
                    response.addData("questions", questionList);
                    response.addData("currentUserRating", user.getUserRatedQuestion());
                    return Response.status(200).entity(response).build();
                } else {
                    response.setError("User account is not permitted");
                    return Response.status(401).entity(response).build();
                }
            } else {
                response.setError("no session exist");
                return Response.status(401).entity(response).build();
            }
        } catch (Exception e) {
            response.setError(e.getMessage());
            return Response.status(500).entity(response).build();
        }
    }

    @POST
    @Path("/addQuestion")
    public Response addQuestion(Map<String, Question> questionDetail) {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);
//        if (session != null) {
//            if (session.getAttribute("accountType") == null) {
//                session.invalidate();
//            }
//        }

//            if (servletRequest.getSession(false) != null) {
                if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
                    if (questionOption.checkQuestionValid(questionDetail.get("question"))) {
                        response = questionOption.addAQuestion(questionDetail.get("question"));
                        return Response.status(200).entity(response).build();
                    } else {
                        response.setError("invalid question data");
                        return Response.status(400).entity(response).build();
                    }
                } else {
                    response.setError("User account is not permitted");
                    return Response.status(401).entity(response).build();
                }
//            } else {
//                response.setError("no session exist");
//                return Response.status(401).entity(response).build();
//            }

    }

    @POST
    @Path("/giveRating")
    public Response giveRating(Rate rating) {
        ApiResponse response = new ApiResponse();
        HttpSession session = servletRequest.getSession(false);
        try {
            if (servletRequest.getSession(false) != null) {
                if (session.getAttribute("accountType") != null && session.getAttribute("accountType").equals(AccountType.ADMIN)) {
                    String userId=(session.getAttribute("userId").toString());

                    com.googlecode.objectify.Key userKey= com.googlecode.objectify.Key.create(User.class,userId);
                    User user= (User) ObjectifyService.ofy().load().key(userKey).now();
                    Map<String,Integer> currentUserRatedQuestion = user.getUserRatedQuestion();
                    if(currentUserRatedQuestion == null){
                        currentUserRatedQuestion = new HashMap<>();
                    }
                    currentUserRatedQuestion.put(rating.getQuestionId(),rating.getRating());
                    user.setUserRatedQuestion(currentUserRatedQuestion);
                    ObjectifyService.ofy().save().entity(user).now();
                    rating.setUserId(userId);
                    if (questionOption.rateQuestion(rating)) {
                        ObjectifyService.ofy().save().entity(user).now();
                        response.setOk(true);
                        response.addData("msg", "rated");
                        return Response.status(200).entity(response).build();
                    } else {
                        response.setError("invalid rating details");
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
        } catch (Exception e) {
            response.setError(e.toString());
            return Response.status(500).entity(response).build();
        }
    }
}
