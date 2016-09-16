package org.social.test;

import it.unimi.dsi.fastutil.ints.IntList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mymedialite.IRecommender;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.eval.CandidateItems;
import org.mymedialite.eval.ItemRecommendationEvaluationResults;
import org.mymedialite.eval.measures.NDCG;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.eval.measures.ReciprocalRank;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.WRMF;
import org.mymedialite.util.Utils;
import org.social.util.KLPredictor;
import org.social.util.KLRecommender;
import org.social.util.Parameter;
import org.social.util.Predictor;
import org.social.util.PredictorParameter;
import org.social.util.WRMFThread;

import chosen.social.lda.util.CommunityData;
import chosen.social.lda.util.IDUtil;
import chosen.social.lda.util.TwitterIDUtil;

public class ItemKLPredictionTest {

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

    System.out.println("size" + communityData.docIndexMap.size());
    
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
    /*
     KLtest predictor = new KLtest( 

        orgTraining,
        test,
        communityData,
        userAllocationList.get(0));
    predictor.call();
    */
    List<KLPredictor> predictors = new ArrayList<>();
    List<Future<PredictorParameter>> ppResults = new ArrayList<>();
    ExecutorService exec = Executors.newCachedThreadPool();

    for (int i = 0; i < userAllocationList.size(); i++) {
      KLPredictor predictor = new KLPredictor( 
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

    String pathString = Parameter.kLPath + medium + "." + Parameter.L;
    BufferedWriter bWriter = new BufferedWriter(new FileWriter(pathString));

    for (int index : PredictorParameter.topNum) {
      double precV = pp.prec.get(index) / (index * num_users);
      double recV = pp.recall.get(index) / pp.CorrectItemSize;
      double conv = ((double) pp.conversion.get(index)) / num_users;
      double fOne = (2 * precV * recV) / (precV + recV);
      bWriter.write("prec@" + index + ": " + precV + " recall@3: " + recV
          + " f1: " + fOne + " conversion: " + conv + "\n");
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
  
  
  public static class KLtest {
    
    PredictorParameter pp;

    private CommunityData cData;
    private IPosOnlyFeedback orgTraining;
    IBooleanMatrix test_user_matrix;
    
    private List<Integer> user_id_queue;
    //iL is size of interest topics 
    
    
    public List<Integer> getUser_id_queue() {
      return user_id_queue;
    }

    public void setUser_id_queue(List<Integer> user_id_queue) {
      this.user_id_queue = user_id_queue;
    }
    
    public PredictorParameter getPp() {
      return pp;
    }

    public void setPp(PredictorParameter pp) {
      this.pp = pp;
    }
    
    public KLtest(
        IPosOnlyFeedback orgTraining,
        IPosOnlyFeedback test,
        CommunityData cData,
        List<Integer> user_id_queue ) {
      
      this.orgTraining = orgTraining;
      test_user_matrix = test.userMatrix();
      this.cData = cData;
      pp = new PredictorParameter();
      this.user_id_queue = user_id_queue;
    }
    
    public PredictorParameter call() {
      // TODO Auto-generated method stub
      for(Integer user_id : user_id_queue) {
        predict(user_id);
      }
      return pp;
    }
    
    public void predict(int user_id) {
      HashSet<Integer> correct_items = new HashSet<Integer>(Utils.intersect(
          test_user_matrix.get(user_id), orgTraining.allUsers()));
      ArrayList<WeightedItem> precMaxList = new ArrayList<WeightedItem>(200000);
      double[] maxIMap = new double[200000];
      if (correct_items.size() == 0)
        return;
      IntList itemList = orgTraining.allUsers();
      
      int orgU = Integer.parseInt(TwitterIDUtil.getID(user_id));
      if(cData.docIndexMap.get(orgU) == null) 
        return;
      
      for (int item_id : itemList) {
        int orgI = Integer.parseInt(TwitterIDUtil.getID(item_id));
        double predict = KLRecommender.predict(getThetaArray(orgU),
            getThetaArray(orgI));
        maxIMap[item_id] = predict;
      }

      for (int k = 0; k < maxIMap.length; k++) {
        precMaxList.add(new WeightedItem(k, maxIMap[k]));
      }
      Collections.sort(precMaxList);

      List<Integer> prediction_max_list = new ArrayList<Integer>(
          precMaxList.size());
      for (int j = 0; j < precMaxList.size(); j++)
        prediction_max_list.add(j, precMaxList.get(j).item_id);

      // if (prediction_max_list.size() != candidate_items.size()) throw new
      // RuntimeException("Not all items have been ranked.");

      Collection<Integer> ignore_items = orgTraining.userMatrix().get(user_id);

      // double auc = AUC.compute(prediction_max_list, correct_items,
      // ignore_items);
      double map = PrecisionAndRecall.AP(prediction_max_list, correct_items,
          ignore_items);
      double ndcg = NDCG.compute(prediction_max_list, correct_items,
          ignore_items);
      double rr = ReciprocalRank.compute(prediction_max_list, correct_items,
          ignore_items);

      if (correct_items.size() == 0)
        return;
      int[] positions = PredictorParameter.topNum;
      Map<Integer, Double> prec = PrecisionAndRecall.precisionAt(
          prediction_max_list, correct_items, ignore_items, positions);
      Map<Integer, Double> recall = PrecisionAndRecall.recallAt(
          prediction_max_list, correct_items, ignore_items, positions);
      
      for(int num : PredictorParameter.topNum) {
        double pv = pp.prec.get(num) + prec.get(num) * num;
        double rv = pp.recall.get(num) + recall.get(num) * correct_items.size();
        int c = pp.conversion.get(num);
        if( prec.get(num) > 0.000001 ) pp.conversion.put(num, c + 1); 
        pp.prec.put(num, pv);
        pp.recall.put(num, rv);
      }
    
        pp.CorrectItemSize += correct_items.size();
        
        pp.num_users++;
        // result.put("AUC", result.get("AUC") + auc);
        pp.MAP += map;
        pp.MRR += rr;
        if (pp.num_users % 1000 == 0)
          System.out.print(".");
        if (pp.num_users % 60000 == 0)
          System.out.println();
        }

    public double[] getThetaArray(int follow) {
      if(cData.docIndexMap.get(follow) != null)
        follow = cData.docIndexMap.get(follow);
      else 
        return new double[cData.theta[0].length];
      return cData.theta[follow];
    }
  }
}


