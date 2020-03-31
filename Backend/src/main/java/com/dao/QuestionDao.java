package com.dao;

import com.entities.Question;
import com.entities.Rate;
import com.response.ApiResponse;

import java.util.List;

public interface QuestionDao {
    public ApiResponse addAQuestion(Question question);
    public void createRating(Rate queRating, Question question);
    public void updateRating(Rate queRating, Question question);
    public boolean checkQuestionValid(Question question);
    public List<Question> getAllQuestions();
    public boolean rateQuestion(Rate queRating);

    public List<Question> getQuestionByTag(String tag);
    public Question getQuestionById(String id);
    public List<Question> getQuestionByIds(List<String> questionIds);
}
