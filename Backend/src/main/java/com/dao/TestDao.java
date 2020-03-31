package com.dao;

import com.entities.Test;

import java.util.List;
import java.util.Map;

public interface TestDao {
   public boolean createTest(Test test);
   public boolean saveTest(Test test);
   public String validateTest(Test test, Map testValues);
   public boolean checkTestValid(List<String> questionIds);
   public List<Test> getAllTestByUser(String userId);
}
