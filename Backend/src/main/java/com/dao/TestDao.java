package com.dao;

import com.entities.Test;

import java.util.Map;

public interface TestDao {
   public boolean createTest(Test test);
   public boolean saveTest(Test test);
   public String validateTest(Test test, Map testValues);

}
