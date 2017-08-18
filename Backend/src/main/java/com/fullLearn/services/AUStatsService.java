package com.fullLearn.services;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.memcache.ErrorHandlers;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullLearn.beans.Contacts;
import com.fullLearn.beans.Frequency;
import com.fullLearn.beans.LearningStats;
import com.fullLearn.beans.LearningStatsAverage;
import com.fullLearn.beans.TrendingChallenges;
import com.fullLearn.helpers.Constants;
import com.fullLearn.model.AUStatsChallangeInfo;
import com.fullLearn.model.AUStatsChallanges;
import com.fullLearn.model.AUStatsResponse;
import com.fullLearn.model.ChallengesInfo;
import com.fullauth.api.http.HttpMethod;
import com.fullauth.api.http.HttpRequest;
import com.fullauth.api.http.HttpResponse;
import com.fullauth.api.http.UrlFetcher;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import lombok.extern.slf4j.Slf4j;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Slf4j
public class AUStatsService {
    final static MemcacheService cache = MemcacheServiceFactory.getMemcacheService();
    public Map<String, ChallengesInfo> challengesCountMap = new HashMap<>();

    public AUStatsService() {
        cache.setErrorHandler(ErrorHandlers.getConsistentLogAndContinue(Level.INFO));
    }

    /**
     * Fetching user contacts from the Contact Kind, with limit 50
     * */
    public void fetchAllUserDailyStats() throws Exception {

        final String key = "dailyStatsCursor";
        String cursorStr = (String) cache.get(key);

        final long startTime = getSpecifiedDate(-1,0,0,0,0);
        final long endTime = getSpecifiedDate(-1,23,59,59,59);

        log.info("fetching stats for range: {} - {}", startTime, endTime);

        BatchContactFetcher.fetchAllContacts(cursorStr, 100, new BatchContactWork() {

            List<LearningStats> saveEntities = new ArrayList<>();

            @Override
            public void run(Contacts contact) throws Exception{

                AUStatsResponse auStatsResponse = fetchUserAUStats(contact.getLogin(), startTime, endTime);

                LearningStats dailyEntity = mapUserLearningStats(auStatsResponse, contact, startTime, endTime);
                if( dailyEntity == null )
                    return;

                saveEntities.add(dailyEntity);

                calculateLearningTrends(dailyEntity.getChallengesDetails());
            }

            @Override
            public void nextCursor(String cursor) {

                ofy().save().entities(saveEntities);

                saveEntities = new ArrayList<>();
                cache.put(key, cursor, Expiration.byDeltaSeconds(300));
            }
        });

        cache.delete(key);

        TrendingChallenges yesterdayTrends = getYesterdayTrends();
        ofy().save().entity(yesterdayTrends).now();

        log.info("trending challenges saved for : {}", yesterdayTrends.getTime());
    }

    /**
     * Sorting trends and store the top 15 trend challenges in TrendingChallenges
     */
    private TrendingChallenges getYesterdayTrends() {

        List<Map.Entry<String, ChallengesInfo>> challenges = new ArrayList(challengesCountMap.entrySet());

        Collections.sort(challenges, new Comparator<Map.Entry<String, ChallengesInfo>>() {
            public int compare(Map.Entry<String, ChallengesInfo> o1, Map.Entry<String, ChallengesInfo> o2) {
                if (o1.getValue().getViews() > o2.getValue().getViews())
                    return -1;
                else if (o1.getValue().getViews() < o2.getValue().getViews())
                    return 1;
                else
                    return 0;
            }
        });

        LinkedHashMap<String, ChallengesInfo> topTrends = new LinkedHashMap<>();

        int rowCount = 1;
        for (Map.Entry<String, ChallengesInfo> challenge : challenges) {

            if (rowCount > 15 || challenge.getValue().getViews() < 2)
                break;

            topTrends.put(challenge.getKey(), challenge.getValue());
            rowCount++;
        }

        long startTime = getSpecifiedDate(-1,0,0,0,0);

        TrendingChallenges yesterdayTrends = new TrendingChallenges();
        yesterdayTrends.setTrends(topTrends);
        yesterdayTrends.setId(startTime);
        yesterdayTrends.setTime(startTime);

        return yesterdayTrends;
    }

    /**
     * Getting the challenges information and store it into the Global Hashmap challengesCountMap
     */
    private void calculateLearningTrends(Map<String, AUStatsChallangeInfo> challenges) throws Exception {

        try {
            if (challenges == null || challenges.isEmpty())
                return;

            for (Map.Entry<String, AUStatsChallangeInfo> challengesDetailsSet : challenges.entrySet()) {

                String challengeTitle = challengesDetailsSet.getKey();

                ChallengesInfo challengeInfo = challengesCountMap.get(challengeTitle);
                if (challengeInfo == null) {

                    AUStatsChallangeInfo challengeDetails = challengesDetailsSet.getValue();

                    challengeInfo = new ChallengesInfo();
                    challengeInfo.setDuration(challengeDetails.getMinutes());
                    challengeInfo.setImage(challengeDetails.getImage());
                    challengeInfo.setUrl(challengeDetails.getLink());
                }

                challengeInfo.setViews(challengeInfo.getViews() + 1);
                challengesCountMap.put(challengeTitle, challengeInfo);
            }

        } catch (Exception e) {
            log.warn(e.getMessage());
            throw new Exception("Exception occured " + e.getMessage());
        }
    }

    /**
     * Fetching user Learning stats from the Adaptive using API, based on the start and end time
     */
    private AUStatsResponse fetchUserAUStats(String email, long startTime, long endTime) throws Exception {

        String url = Constants.AU_API_URL + "/v1/completedMinutes?apiKey=" + Constants.AU_APIKEY + "&email=" + email + "&startTime=" + startTime + "&endTime=" + endTime;
        HttpRequest httpRequest = new HttpRequest(url, HttpMethod.POST);
        httpRequest.setContentType("application/json");
        httpRequest.setConnectionTimeOut(30000);

        HttpResponse httpResponse = UrlFetcher.makeRequest(httpRequest);
        if (httpResponse.getStatusCode() == 200) {
            String message = httpResponse.getResponseContent();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(message, AUStatsResponse.class);
        }
        System.out.println("Error occured, while fetching information from the API" + httpResponse.getResponseContent());
        throw new Exception(httpResponse.getResponseContent());
    }

    /**
     * Mapping the user Learning stat into LearningStats and return that entity.
     */
    private LearningStats mapUserLearningStats(AUStatsResponse response, Contacts contact, long startTime, long endTime) {

        if (!response.isResponse())
            return null;

        String email = contact.getLogin();
        String userId = contact.getId();

        LearningStats learningStats = new LearningStats();
        learningStats.setId(userId + ":" + startTime + ":" + endTime);
        learningStats.setUserId(userId);
        learningStats.setStartTime(startTime);
        learningStats.setEndTime(endTime);
        learningStats.setFrequency(Frequency.DAY);
        learningStats.setEmail(email);
        Map<String, AUStatsChallanges> userLearningStats = response.getData();

        if (userLearningStats.get(email) == null) {

            learningStats.setMinutes(0);
            learningStats.setChallengesCompleted(0);
            learningStats.setChallengesDetails(null);
            return learningStats;
        }

        AUStatsChallanges auStatsChallanges = userLearningStats.get(email);
        learningStats.setMinutes(auStatsChallanges.getMinutes());
        learningStats.setChallengesDetails(auStatsChallanges.getChallengesDetails());
        learningStats.setChallengesCompleted(auStatsChallanges.getChallengesCompleted());

        return learningStats;
    }

    /**
     * Calculating all user learning stats average for previous four and twelfth week
     */
    public void calculateAllUserOverallAverage() throws Exception{

        String cursorStr = null;
        BatchContactFetcher.fetchAllContacts(cursorStr, 100, new BatchContactWork() {

            List<LearningStatsAverage> saveEntities = new ArrayList<>();

            @Override
            public void run(Contacts contact) throws Exception {
                saveEntities.add(calculateUserOverAllAverage(contact));
            }

            @Override
            public void nextCursor(String cursor) {
                ofy().save().entities(saveEntities);
                saveEntities = new ArrayList<>();
            }
        });
    }

    /**
     * Compute individual user average learning stats for the previous four and twelfth week,
     * then save it into the LearningStatsAverage
     */
    private LearningStatsAverage calculateUserOverAllAverage(Contacts contact){

        List<LearningStats> weeklyLearningStat = ofy().load().type(LearningStats.class)
                .filter("userId", contact.getId())
                .filter("frequency", Frequency.WEEK)
                .order("-startTime").limit(12).list();

        if(weeklyLearningStat == null)
            return null;

        int fourWeekAverage = 0;
        int twelfthWeekAverage = 0;

        int weekCount = 1;
        for(LearningStats learningStat : weeklyLearningStat){

            twelfthWeekAverage = twelfthWeekAverage + learningStat.getMinutes();

            if (weekCount <= 4)
                fourWeekAverage = fourWeekAverage + learningStat.getMinutes();

            weekCount++;
        }

        fourWeekAverage = Math.round(fourWeekAverage / 4);
        twelfthWeekAverage = Math.round(twelfthWeekAverage / 12);

        LearningStatsAverage learningStatsAverage = new LearningStatsAverage();
        learningStatsAverage.setUserId(contact.getId());
        learningStatsAverage.setFourWeekAvg(fourWeekAverage);
        learningStatsAverage.setTwelveWeekAvg(twelfthWeekAverage);
        learningStatsAverage.setEmail(contact.getLogin());

        return learningStatsAverage;
    }

    /**
     * Calculate Weekly Learning stats of all user
     */
    public void calculateAllUserWeeklyStats() throws Exception{

        String cursorStr = null;
        BatchContactFetcher.fetchAllContacts(cursorStr, 100, new BatchContactWork() {

            List<LearningStats> saveEntities = new ArrayList<>();

            @Override
            public void run(Contacts contact) throws Exception{

                saveEntities.add(calculateUserWeekStats(contact));
            }

            @Override
            public void nextCursor(String cursor) {

                ofy().save().entities(saveEntities);
                saveEntities = new ArrayList<>();
            }
        });
    }

    /**
     * Computing Weekly Learning stats of a individual User
     */
    private LearningStats calculateUserWeekStats(Contacts contact){

        long startDate = getSpecifiedDate(-7,0,0,0,0);
        long endDate = getSpecifiedDate(-1,23,59,59,59);

        List<LearningStats> userDailyLearning = ofy().load().type(LearningStats.class)
                .filter("userId", contact.getId())
                .filter("frequency", Frequency.DAY)
                .filter("startTime >=", startDate)
                .filter("startTime <", endDate)
                .list();

        if( userDailyLearning == null )
            return null;

        int totalMinutes = 0;
        int totalChallenges = 0;
        for(LearningStats dailyLearning : userDailyLearning){

            totalMinutes = totalMinutes + dailyLearning.getMinutes();
            totalChallenges = totalChallenges + dailyLearning.getChallengesCompleted();
        }

        LearningStats weeklyLearningStats = new LearningStats();
        weeklyLearningStats.setId(contact.getId() + ":" + startDate + ":" + endDate);
        weeklyLearningStats.setUserId(contact.getId());
        weeklyLearningStats.setMinutes(totalMinutes);
        weeklyLearningStats.setChallengesCompleted(totalChallenges);
        weeklyLearningStats.setEmail(contact.getLogin());
        weeklyLearningStats.setFrequency(Frequency.WEEK);
        weeklyLearningStats.setStartTime(startDate);
        weeklyLearningStats.setEndTime(endDate);

        return weeklyLearningStats;
    }

    private long getSpecifiedDate(int day, int hour, int minute, int second, int milliSecond){

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, milliSecond);

        return cal.getTime().getTime();
    }
}
