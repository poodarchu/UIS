package org.social.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mymedialite.IRecommender;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.eval.CandidateItems;
import org.mymedialite.eval.ItemRecommendationEvaluationResults;
import org.mymedialite.io.ItemData;
import org.social.util.Parameter;
import org.social.util.PredictorParameter;
import org.social.util.TMPredictor;

import chosen.social.lda.util.CommunityData;
import chosen.social.lda.util.IDUtil;
import chosen.social.lda.util.TwitterIDUtil;

public class TMPredictionTest {

  public static void main(String args[]) throws Exception {

    // !!!!should save models to disk

    /*
     * shuffle data for each follower and 10 cross-over test and then split 9
     * into documents for lda community discovery output community and c.F c.G
     * finally get recommendation score.
     */
    Scanner scanner = new Scanner(System.in);
    System.out.println("How many Topics?");
    Parameter.L = (short) scanner.nextInt();

    System.out.println("Delay your jobs?(in hour)");
    int hour = scanner.nextInt() * 3600 * 1000;
    try {
      Thread.currentThread().sleep(hour);
    } catch (Exception e) {
    }
    
    IDUtil idUtil = new IDUtil();
    idUtil = idUtil.read();
    TwitterIDUtil.IDToIndexMap = idUtil.IDToIndexMap;
    TwitterIDUtil.indexToIDMap = idUtil.indexToIDMap;
    
    String medium = args[0];
    if (medium.equals("0")) {
      medium = "followees";
    } else if (medium.equals("1")) {
      medium = "followers";
    } else {
      medium = "all";
    }
    String testDataPath = Parameter.testDataPath;
    String orgTestDataPath = "data/keepdata.txt.100";

    IPosOnlyFeedback traningData = ItemData.read(orgTestDataPath, null, null,
        false);
    IPosOnlyFeedback testData = ItemData.read(testDataPath, null, null, false);
    // read c.F & c.G into data
    CommunityData communityData = new CommunityData(medium);
    communityData = communityData.read();
    communityData.setMedium(medium);

    evaluate(traningData, testData,
        testData.allUsers(), testData.allItems(), null, null, communityData,
        medium);

  }

  public static ItemRecommendationEvaluationResults evaluate(
      IPosOnlyFeedback orgTraining,
      IPosOnlyFeedback test,
      Collection<Integer> test_users, Collection<Integer> candidate_items,
      CandidateItems candidate_item_mode, Boolean repeated_events,
      CommunityData communityData, String medium) throws IOException {

    if (candidate_item_mode == null)
      candidate_item_mode = CandidateItems.OVERLAP;
    if (repeated_events == null)
      repeated_events = false;

    if (test_users == null)
      test_users = test.allUsers();

    int num_users = 0;
    ItemRecommendationEvaluationResults result = new ItemRecommendationEvaluationResults();

    List<List<Integer>> userAllocationList = new ArrayList<List<Integer>>();
    int num = 0;
    for (Integer user_id : test_users) {
      int threadNum = (num++ / 1000) % 7;
      List<Integer> usersAssigned;
      if (userAllocationList.size() <= threadNum) {
        usersAssigned = new ArrayList<>();
        userAllocationList.add(usersAssigned);
      } else {
        usersAssigned = userAllocationList.get(threadNum);
      }
      usersAssigned.add(user_id);
    }
    List<TMPredictor> predictors = new ArrayList<>();
    List<Future<PredictorParameter>> ppResults = new ArrayList<>();
    ExecutorService exec = Executors.newCachedThreadPool();

    for (int i = 0; i < userAllocationList.size(); i++) {
      TMPredictor predictor = new TMPredictor( 
          orgTraining,
          test,
          communityData,
          userAllocationList.get(i));
      predictors.add(predictor);
      ppResults.add(exec.submit(predictor));
    }

    List<PredictorParameter> ppList = new ArrayList<>();

    for (Future<PredictorParameter> fs : ppResults)
      try {
        ppList.add(fs.get());
      } catch (Exception e) {
        System.out.println(e);
        return result;
      } finally {
        exec.shutdown();
      }

    PredictorParameter pp = new PredictorParameter();
    PredictorParameter.sum(pp, ppList);
    num_users = pp.num_users;
    for (Entry<String, Double> entry : result.entrySet()) {
      entry.setValue(entry.getValue() / num_users);
    }

    String pathString = Parameter.kLPath + medium + ".based." + Parameter.L;
    BufferedWriter bWriter = new BufferedWriter(new FileWriter(pathString));

    for (int index : PredictorParameter.topNum) {
      double precV = pp.prec.get(index) / (index * num_users);
      double recV = pp.recall.get(index) / pp.CorrectItemSize;
      double conv = ((double) pp.conversion.get(index)) / num_users;
      double fOne = (2 * precV * recV) / (precV + recV);
      double ndcg = pp.ndcg.get(index) / num_users ;
      bWriter.write("prec@" + index +  ": " + precV + " recall@3: " + recV + " f1: " + fOne + " conversion: " + conv
          + " NDCG: "+ ndcg + "\n");
    }
    // bWriter.write("AUC      " + result.get("AUC"));
    bWriter.write("MAP      " + pp.MAP);
    bWriter.write("MRR      " + pp.MRR);
    bWriter.close();

    // result.put("p3", prec3);
    // result.put("p5", precision);
    result.put("num_users", (double) num_users);
    result.put("num_lists", (double) num_users);
    result.put("num_items", (double) candidate_items.size());

    return result;
  }

  public static List<Integer> predictItems(IRecommender recommender,
      int user_id, Collection<Integer> candidate_items) {
    ArrayList<WeightedItem> precResult = new ArrayList<WeightedItem>(
        candidate_items.size());
    for (int item_id : candidate_items) {
      double predict = recommender.predict(user_id, item_id);
      precResult.add(new WeightedItem(item_id, predict));
    }
    Collections.sort(precResult, Collections.reverseOrder());

    List<Integer> return_array = new ArrayList<Integer>(precResult.size());
    for (int i = 0; i < precResult.size(); i++)
      return_array.add(i, precResult.get(i).item_id);

    return return_array;
  }
}
