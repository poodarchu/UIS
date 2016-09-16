package org.social.test;

import it.unimi.dsi.fastutil.ints.IntCollection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Scanner;
import java.util.TreeSet;

import org.mymedialite.IRecommender;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.datatype.Pair;
import org.mymedialite.datatype.SparseMatrix;
import org.mymedialite.datatype.Triple;
import org.mymedialite.eval.CandidateItems;
import org.mymedialite.eval.ItemRecommendationEvaluationResults;
import org.mymedialite.eval.Items;
import org.mymedialite.eval.measures.AUC;
import org.mymedialite.eval.measures.NDCG;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.eval.measures.ReciprocalRank;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.Extensions;
import org.mymedialite.itemrec.WRMF;
import org.mymedialite.util.Utils;
import org.social.util.Parameter;
import org.social.util.WRMFThread;
import org.social.util.QuickTopN.PairComparable;
import org.social.util.SerializedPrecMatrix;

import chosen.social.lda.util.CommunityData;
import chosen.social.lda.util.IDUtil;
import chosen.social.lda.util.TwitterIDUtil;

public class ItemPredictionTest {
  
  public static void main(String args[]) throws Exception {
    
    //!!!!should save models to disk
    
    /*
     * shuffle data for each follower and 10 cross-over test
     * and then split 9 into documents for lda community discovery
     * output community and c.F c.G
     * finally get recommendation score.
    */    
    Scanner scanner = new Scanner(System.in);
    System.out.println("How many Topics?");
    Parameter.L = (short) scanner.nextInt();
    
    System.out.println("Delay your jobs?(in hour)");
    int hour = scanner.nextInt() * 3600 * 1000;
    try 
    { 
      Thread.currentThread().sleep(hour);//毫秒 
    } catch(Exception e){}
    
    String medium = args[0];
    if(medium.equals("0")) {
      medium = "followees";
    } else if(medium.equals("1")) {
      medium = "followers";
    } else {
      medium = "all";
    }
    
    IDUtil idUtil = new IDUtil();
    idUtil = idUtil.read();
    TwitterIDUtil.IDToIndexMap = idUtil.IDToIndexMap;
    TwitterIDUtil.indexToIDMap = idUtil.indexToIDMap;
    
    String trainingDataDir = Parameter.communityDir + medium +"/";
    String testDataPath = Parameter.testDataPath ;
    String orgTestDataPath = "data/keepdata.txt";
    
    IPosOnlyFeedback traningData = ItemData.read(orgTestDataPath, null, null, false);
    List<IPosOnlyFeedback> traningDataList = new ArrayList<IPosOnlyFeedback>();
    IPosOnlyFeedback testData = ItemData.read(testDataPath, null, null, false);
    //read c.F & c.G into data
    CommunityData communityData = new CommunityData(medium);
    communityData = communityData.read();
    communityData.setMedium(medium);

    //get rcommend item ranked list
    //List<List<Triple<Integer, Integer, Double>>> preditionList = new ArrayList<List<Triple<Integer, Integer, Double>>>();
    //float [][][] predictionMatrix = new float [Parameter.L][][];
    
    String precDataPath = Parameter.maxF1Path + medium  +"prec";
    File file = new File(precDataPath);
    file.delete();
    
    ExecutorService exec = Executors.newCachedThreadPool();
    List<Future<WRMF>> mfResults = new ArrayList<>();
    if(args.length > 1 ) {
      for (int i = 0; i < Parameter.L ; i++) {
        WRMF recommender = new WRMF();
        IPosOnlyFeedback training_data = ItemData.read(trainingDataDir + Parameter.cname + i,
            null, null, false);
        traningDataList.add(training_data);
        recommender.setFeedback(training_data);
        WRMFThread mfThread = new WRMFThread(recommender, 
            Parameter.IFMFPath +medium+"."+i , 
            medium ,testData , i);
        mfResults.add(exec.submit(mfThread));
      }
      int counter = 0 ;
      for(Future<WRMF> mf : mfResults) {
        try{
          mf.get();
          System.out.println("tranning done" + counter++);
          //MFList.add(recommender);
          //save models to files for further usage.
        } catch (Exception e) {
          System.err.println(e);
        } finally {
          exec.shutdown();
        }
        
      }
    }
    /*
    IDUtil idUtil = new IDUtil();
    idUtil = idUtil.read();
    TwitterIDUtil.IDToIndexMap = idUtil.IDToIndexMap;
    TwitterIDUtil.indexToIDMap = idUtil.indexToIDMap;
    
    SparseMatrix<Double> max_Matrix = 
        new SparseMatrix<Double>(testData.maxUserID(), testData.maxItemID(), 0.0);
    SparseMatrix<Double> sum_Matrix = 
        new SparseMatrix<Double>(testData.maxUserID(), testData.maxItemID(), 0.0);
    */
    
    List<IRecommender> recommenderList = new ArrayList<IRecommender>();
    List<IPosOnlyFeedback> training_data_list = new ArrayList<IPosOnlyFeedback>();
    
    for(int i = 0 ; i < Parameter.L ; i++) {
      WRMF recommender = new WRMF();
      recommender.loadModel(Parameter.IFMFPath + medium + "." + i);
      recommenderList.add(recommender);
      
      System.out.println("tranning" + i);
      
      IPosOnlyFeedback training_data = ItemData.read(trainingDataDir + Parameter.cname + i,
          null, null, false);
      training_data_list.add(training_data);
    }
    
    evaluate(recommenderList, traningData , testData, training_data_list, 
        testData.allUsers(), testData.allItems(), 
        null,null, communityData, medium);
      /*
      for(Integer user: training_data.allUsers()) {
        //user = TwitterIDUtil.getIndex(user.toString());
        TreeSet<Pair<Integer, Double>> prec_set = 
            new TreeSet<Pair<Integer, Double>>(new PairComparable());
        List<Pair<Integer, Double>> topList = new ArrayList<Pair<Integer,Double>>(1000);
        Collection<Integer> candidate_items_for_pre = Utils.exclusion(testData.allItems(), training_data.userMatrix().get(user)); //exclude all the known followees****
        candidate_items_for_pre = Utils.intersect(candidate_items_for_pre, training_data.allItems());
        Double r;
        for (Integer item : candidate_items_for_pre) {
          //item =  TwitterIDUtil.getIndex(String.valueOf(item));
          r = recommender.predict(user, item);
          if (r > max_Matrix.get(user, item)) {
            max_Matrix.set(user, item, r);
          }
          int orgIdIndex = Integer.parseInt(TwitterIDUtil.getID(user));
          r = r * communityData.getTheta(orgIdIndex, i);  //F1 score need to implement theta * prediction 
          r += sum_Matrix.get(user, item);
          sum_Matrix.set(user, item, r);  //recommender.predict(user_id, item_id);
        }
      }
      //prec_Result.add(prec_Matrix);
      System.out.println("predicted" + i);
    }
    
    recommender = null;
    System.gc();
    */
    /*
    *List<Pair> xx = SparseMatrix.nonEmptyID, if r > set max_Matrix(x)(y) then set it to the matrix
    *use matrix result set, use list to get ranked result, get first 3 results to compare with test data
    *trigger gc if necessary !cal F1 - max score 
    
    
    Map<Integer, List<Pair<Integer, Double>>> topPairMap = rankPair(max_Matrix);
    calSaveFOne(testData, topPairMap,Parameter.maxF1Path + medium);

    topPairMap = rankPair(sum_Matrix);
    calSaveFOne(testData, topPairMap,Parameter.sumF1Path + medium);
    */
  }

  public static void saveprec(String medium, IPosOnlyFeedback testData,
      WRMF recommender, int i, IPosOnlyFeedback training_data)
      throws IOException {
    Collection<Integer> test_users = training_data.allUsers();
    Collection<Integer> candidate_items = training_data.allItems();
    String precDataPath = Parameter.maxF1Path + medium  +"prec";
    BufferedWriter precWriter = new BufferedWriter(new FileWriter(precDataPath,true));
    HashMap<String, Double> results = Items.evaluate(recommender, testData, training_data, test_users, candidate_items);
    precWriter.write(i+"    ");
    precWriter.write("NDCG      " + results.get("NDCG"));
    precWriter.write("prec@3    " + results.get("prec@3"));
    precWriter.write("prec@5    " + results.get("prec@5"));
    precWriter.write("prec@10    " + results.get("prec@10"));
    precWriter.write("num_users " + results.get("num_users"));
    precWriter.write("num_items " + results.get("num_items") + "\n");
    precWriter.close();
  }

  public static Map<Integer, List<Pair<Integer, Double>>> rankPair(
      SparseMatrix<Double> max_Matrix) {
    Map<Integer, HashMap<Integer, Double>> resultsHashMap = max_Matrix.nonEmptyRows();
    Map<Integer,List<Pair<Integer, Double>>> topPairMap = new HashMap<Integer, List<Pair<Integer,Double>>>();
    for(Map.Entry<Integer, HashMap<Integer, Double>> entry : resultsHashMap.entrySet()) {
      Integer user = entry.getKey();
      HashMap<Integer, Double> precMap = entry.getValue();
      List<Pair<Integer, Double>> precRankedList = topN(20, precMap); //record top 20 score
      topPairMap.put(user, precRankedList);
    }
    return topPairMap;
  }

  public static void calSaveFOne(IPosOnlyFeedback testData,
      Map<Integer, List<Pair<Integer, Double>>> topPairMap,
      String pathString) throws IOException {
    int n = 5;
    double precision = 0.0;
    double prec3 =0.0;
    double prec10 =0.0;
    double recall = 0.0;
    double rec3= 0.0;
    double rec10= 0.0;
    int counter = 0;
    int counter3 = 0 ;
    int counter10 = 0;
    int CorrectItemSize = 0;
    for(int user : testData.users()) {
      
      IntCollection correct_items = testData.userMatrix().get(user);
      List<Pair<Integer, Double>> ranked_pairs = topPairMap.get(user);
      List<Integer> ranked_items = new ArrayList<Integer>();
      for(int i = 0 ; i < n ; i++) {
        if ( ranked_pairs == null || !(i < ranked_pairs.size()) ) break;
        ranked_items.add(ranked_pairs.get(i).first);
      }
      if(ranked_items.isEmpty()) continue;
      int t = n ;
      int t3 = 3;
      int t10 = 10;
      if (correct_items.size() < n) t = correct_items.size();
      if(correct_items.size() < 3) t3 = correct_items.size();
      if(correct_items.size() < 10) t10 = correct_items.size();
      
      if(correct_items.size() == 0) continue;
      precision += PrecisionAndRecall.precisionAt(ranked_items, correct_items, t) * t;
      prec3 += PrecisionAndRecall.precisionAt(ranked_items, correct_items, t3) * t3;
      prec10 += PrecisionAndRecall.precisionAt(ranked_items, correct_items, t10) * t10;
      
      recall += PrecisionAndRecall.recallAt(ranked_items, correct_items, t) * correct_items.size();
      rec3 += PrecisionAndRecall.recallAt(ranked_items, correct_items, t3) * correct_items.size();
      rec10 += PrecisionAndRecall.recallAt(ranked_items, correct_items, t10) * correct_items.size();
      counter += t;
      counter3 += t3;
      counter10 += t10;
      CorrectItemSize += correct_items.size();
    }
    precision /= counter;
    prec3 /= counter3;
    prec10 /= counter10;
    
    recall /= CorrectItemSize;
    rec3 /= CorrectItemSize;
    rec10 /= CorrectItemSize;
    double fOne = (2 * prec3 * rec3) / (prec3 + rec3);
    
    BufferedWriter bWriter = new BufferedWriter(new FileWriter(pathString));
    bWriter.write("prec@3: "+prec3+" recall@3: "+rec3+" f1: "+fOne +"\n");
    fOne = (2 * precision * recall) / (precision + recall);
    bWriter.write("prec@5: "+precision+" recall@5: "+recall+" f1: "+fOne+"\n");
    fOne = (2 * prec10 * rec10) / (prec10 + rec10);
    bWriter.write("prec@10: "+prec10+" recall@10: "+rec10+" f1: "+fOne+"\n");
    bWriter.close();
  }
  
  public static ArrayList<Pair<Integer, Double>> topN(int N , HashMap<Integer, Double> precEntry) {
    PairComparable comparable = new PairComparable();
    TreeSet<Pair<Integer, Double>> topN = new TreeSet<Pair<Integer,Double>>(comparable);
    
    Double minScore = 1000.0;
    for(Map.Entry<Integer, Double> indexFreq: precEntry.entrySet()){
      if (indexFreq == null) continue;
      Double score =indexFreq.getValue();
      if(minScore > 990){//第一次运行
        minScore = score;
      }
      if(topN.size() < N){//首先填满topN
        topN.add(new Pair<Integer, Double> (indexFreq.getKey(),indexFreq.getValue() ));
        if(score < minScore){
          minScore = score;//更新最低分
        }
      }else if(score > minScore){
        topN.remove(topN.last());//先删除topN中的最低分
        topN.add(new Pair<Integer, Double> (indexFreq.getKey(),indexFreq.getValue() ));
        minScore = topN.last().second;//更新最低分
      }
    }
    ArrayList<Pair<Integer, Double>> topList = new ArrayList<Pair<Integer,Double>>(topN);
    if (topList == null) {
      System.out.println(precEntry.size());
    }
    return topList;
  }
  
  public static TreeSet<Pair<Integer, Double>> top2HndrdPairs (TreeSet<Pair<Integer, Double>> topN, int item, double score) {
    if(topN.size() < 10000){//首先填满topN
      topN.add(new Pair<Integer, Double> (item,score));
       //min = topN.last().second;
    }else if(score > topN.last().second){
      topN.remove(topN.last());//先删除topN中的最低分
      topN.add(new Pair<Integer, Double> (item,score));
      //min = topN.last().second;
    }
    return topN;
  }
  public static TreeSet<Pair<Integer, Double>> top2HndrdPairs (TreeSet<Pair<Integer, Double>> topN, List<Pair<Integer, Double>> topList) {
    for(Pair<Integer,Double> p : topList) {
      if(topN.size() < 1024){//首先填满topN
        topN.add(p);
         //min = topN.last().second;
      }else if(p.second > topN.last().second){
        topN.remove(topN.last());//先删除topN中的最低分
        topN.add(p);
        //min = topN.last().second;
      }
    }
    return topN;
  }
  
  public static ItemRecommendationEvaluationResults evaluate(
      List<IRecommender> recommenderList,
      IPosOnlyFeedback orgTraining,
      IPosOnlyFeedback test,
      List<IPosOnlyFeedback> training,
      Collection<Integer> test_users,
      Collection<Integer> candidate_items,
      CandidateItems candidate_item_mode,
      Boolean repeated_events,
      CommunityData communityData,
      String medium) throws IOException {

    if(candidate_item_mode == null)  candidate_item_mode = CandidateItems.OVERLAP;
    if(repeated_events == null)  repeated_events = false;
  
    if (test_users == null)
      test_users = test.allUsers();
    
    int num_users = 0;
    int times = 0;
    ItemRecommendationEvaluationResults result = new ItemRecommendationEvaluationResults();
    
    IBooleanMatrix test_user_matrix     = test.userMatrix();
    
    int n = 5;
    double precision = 0.0;
    double prec3 =0.0;
    double prec10 =0.0;
    double prec15 = 0.0;
    double prec20 = 0.0;
    double prec30 = 0.0;
    double prec40 = 0.0;
    double prec50 = 0.0;
    
    double precisionSum = 0.0;
    double prec3Sum =0.0;
    double prec10Sum =0.0;
    double prec15Sum = 0.0;
    double prec20Sum = 0.0;
    double prec30Sum = 0.0;
    double prec40Sum = 0.0;
    double prec50Sum = 0.0;
    
    double rec5 = 0.0;
    double rec3= 0.0;
    double rec10= 0.0;
    double rec15 = 0.0;
    double rec20 = 0.0;
    double rec30 = 0.0;
    double rec40 = 0.0;
    double rec50 = 0.0;
    
    double rec5Sum = 0.0;
    double rec3Sum= 0.0;
    double rec10Sum= 0.0;
    double rec15Sum = 0.0;
    double rec20Sum = 0.0;
    double rec30Sum = 0.0;
    double rec40Sum = 0.0;
    double rec50Sum = 0.0;
    int CorrectItemSize = 0;
    
    for (Integer user_id : test_users) {
      // Items viewed by the user in the test set that were also present in the training set.
      HashSet<Integer> correct_items = new HashSet<Integer>(Utils.intersect(test_user_matrix.get(user_id), candidate_items));
      ArrayList<WeightedItem> precMaxList = new ArrayList<WeightedItem>(20000);
      ArrayList<WeightedItem> precSumList = new ArrayList<WeightedItem>(20000);
      double[] maxMap = new double [orgTraining.maxItemID()+1];
      double[] sumMap = new double [orgTraining.maxItemID()+1];
      
      for(int i = 0 ; i < Parameter.L; i ++) {
        IBooleanMatrix training_user_matrix = training.get(i).userMatrix();
        // The number of items that will be used for this user.
        HashSet<Integer> candidate_items_in_train = new HashSet<Integer> (Utils.intersect(training_user_matrix.get(user_id), candidate_items));; //exclude all the known followees****
        
        int num_eval_items = candidate_items.size() - (repeated_events ? 0 : candidate_items_in_train.size());
  
        // Skip all users that have 0 or #relevant_items test items.
        if (correct_items.size() == 0) continue;
        if (num_eval_items - correct_items.size() == 0) continue;
        
        IRecommender recommender = recommenderList.get(i);
        
        for (int item_id : training.get(i).allItems()) {
          if(!recommender.canPredict(user_id, item_id)) break;
          double predict = recommender.predict(user_id, item_id);
          double score = maxMap[item_id];
          if(predict > score) maxMap[item_id] = predict;
          
          int orgIdIndex = Integer.parseInt(TwitterIDUtil.getID(user_id));
          double weigh = communityData.getTheta(orgIdIndex, i);
          if(weigh > 0) {
            predict = predict * communityData.getTheta(user_id, i);  //F1 score need to implement theta * prediction 
            predict += sumMap[item_id];
            sumMap[item_id] = predict;  //recommender.predict(user_id, item_id);
          }
        }
      }
     
      for(int k = 0 ; k < maxMap.length; k ++) {
        precMaxList.add(new WeightedItem(k, maxMap[k]));
      }
      Collections.sort(precMaxList, Collections.reverseOrder());
      
      for(int k = 0 ; k < maxMap.length; k ++) {
        precSumList.add(new WeightedItem(k, sumMap[k]));
      }
      Collections.sort(precSumList, Collections.reverseOrder());
      
      List<Integer> prediction_max_list = new ArrayList<Integer>(precMaxList.size());
      for (int j = 0; j < precMaxList.size(); j++)  prediction_max_list.add(j, precMaxList.get(j).item_id);
      
      List<Integer> prediction_sum_list = new ArrayList<Integer>(precSumList.size());
      for (int j = 0; j < precSumList.size(); j++)  prediction_sum_list.add(j, precSumList.get(j).item_id);
      
      //if (prediction_max_list.size() != candidate_items.size()) throw new RuntimeException("Not all items have been ranked.");
  
      Collection<Integer> ignore_items = orgTraining.userMatrix().get(user_id);
  
      //double auc     = AUC.compute(prediction_max_list, correct_items, ignore_items);
      double map     = PrecisionAndRecall.AP(prediction_max_list, correct_items, ignore_items);
      double ndcg    = NDCG.compute(prediction_max_list, correct_items, ignore_items);
      double rr      = ReciprocalRank.compute(prediction_max_list, correct_items, ignore_items); 
      
      //double aucSum     = AUC.compute(prediction_max_list, correct_items, ignore_items);
      double mapSum     = PrecisionAndRecall.AP(prediction_sum_list, correct_items, ignore_items);
      double ndcgSum    = NDCG.compute(prediction_sum_list, correct_items, ignore_items);
      double rrSum      = ReciprocalRank.compute(prediction_sum_list, correct_items, ignore_items); 
      
      int t = n ;
      int t3 = 3;
      int t10 = 10;
      int t15 = 15;
      int t20 = 20;
      int t30 = 30;
      int t40 = 40;
      int t50= 50;
      
      if(correct_items.size() == 0) continue;
      int[] positions = new int[] {t3, t, t10, t15, t20, t30, t40, t50};
      Map<Integer, Double> prec = PrecisionAndRecall.precisionAt(prediction_max_list, correct_items, ignore_items, positions);
      Map<Integer, Double> recall = PrecisionAndRecall.recallAt(prediction_max_list, correct_items, ignore_items, positions);
      
      precision += prec.get(t) * t;
      prec3 += prec.get(t3) * t3;
      prec10 += prec.get(t10) * t10;
      prec15 += prec.get(t15) * t15;
      prec20 += prec.get(t20) * t20;
      prec30 += prec.get(t30) * t30;
      prec40 += prec.get(t40) * t40;
      prec50 += prec.get(t50) * t50;
      
      rec5 += recall.get(t) * correct_items.size();
      rec3 += recall.get(t3) * correct_items.size();
      rec10 += recall.get(t10) * correct_items.size();
      rec15 += recall.get(t15) * correct_items.size();
      rec20 += recall.get(t20) * correct_items.size();
      rec30 += recall.get(t30) * correct_items.size();
      rec40 += recall.get(t40) * correct_items.size();
      rec50 += recall.get(t50) * correct_items.size();
      
      prec = PrecisionAndRecall.precisionAt(prediction_sum_list, correct_items, ignore_items, positions);
      recall = PrecisionAndRecall.recallAt(prediction_sum_list, correct_items, ignore_items, positions);
      
      precisionSum += prec.get(t) * t;
      prec3Sum += prec.get(t3) * t3;
      prec10Sum += prec.get(t10) * t10;
      prec15Sum += prec.get(t15) * t15;
      prec20Sum += prec.get(t20) * t20;
      prec30Sum += prec.get(t30) * t30;
      prec40Sum += prec.get(t40) * t40;
      prec50Sum += prec.get(t50) * t50;
      
      rec5Sum += recall.get(t) * correct_items.size();
      rec3Sum += prec.get(t3) * correct_items.size();
      rec10Sum += prec.get(t10) * correct_items.size();
      rec15Sum += recall.get(t15) * correct_items.size();
      rec20Sum += recall.get(t20) * correct_items.size();
      rec30Sum += recall.get(t30) * correct_items.size();
      rec40Sum += recall.get(t40) * correct_items.size();
      rec50Sum += recall.get(t50) * correct_items.size();
      
      CorrectItemSize += correct_items.size();
      
      num_users++;
      //result.put("AUC",       result.get("AUC")       + auc);
      result.put("MAP",       result.get("MAP")       + map);
      result.put("NDCG",      result.get("NDCG")      + ndcg);
      result.put("MRR",       result.get("MRR")       + rr);
     // result.put("AUCSum",       result.get("AUCSum")       + aucSum);
      result.put("MAPSum",       result.get("MAPSum")       + mapSum);
      result.put("NDCGSum",      result.get("NDCGSum")      + ndcgSum);
      result.put("MRRSum",       result.get("MRRSum")       + rrSum);
      if (num_users % 1000 == 0)
        System.out.print(".");
      if (num_users % 60000 == 0)
        System.out.println();
      
     }
    if (num_users > 1000) System.out.println();
    for(Entry<String, Double> entry : result.entrySet()) {
      entry.setValue(entry.getValue() / num_users);
    }
    
    precision /= (5 * num_users);
    prec3 /= (3 * num_users);
    prec10 /= (10 * num_users);
    prec15 /= (15 * num_users);
    prec20 /= (20 * num_users);
    prec30 /= (30 * num_users);
    prec40 /= (40 * num_users);
    prec50 /= (50 * num_users);
    
    rec5 /= CorrectItemSize;
    rec3 /= CorrectItemSize;
    rec10 /= CorrectItemSize;
    rec15 /= CorrectItemSize;
    rec20 /= CorrectItemSize;
    rec30 /= CorrectItemSize;
    rec40 /= CorrectItemSize;
    rec50 /= CorrectItemSize;
    
    double fOne = (2 * prec3 * rec3) / (prec3 + rec3);
    
    String pathString = Parameter.maxF1Path + medium;
    BufferedWriter bWriter = new BufferedWriter(new FileWriter(pathString));
    bWriter.write("prec@3: "+prec3+" recall@3: "+rec3+" f1: "+fOne +"\n");
    fOne = (2 * precision * rec5) / (precision + rec5);
    bWriter.write("prec@5: "+precision+" recall@5: "+rec5+" f1: "+fOne+"\n");
    fOne = (2 * prec10 * rec10) / (prec10 + rec10);
    bWriter.write("prec@10: "+prec10+" recall@10: "+rec10+" f1: "+fOne+"\n");
    fOne = (2 * prec15 * rec15) / (prec15 + rec15);
    bWriter.write("prec@15: "+prec15+" recall@15: "+rec15+" f1: "+fOne+"\n");
    fOne = (2 * prec20 * rec20) / (prec20 + rec20);
    bWriter.write("prec@20: "+prec20+" recall@20: "+rec20+" f1: "+fOne+"\n");
    
    fOne = (2 * prec30 * rec30) / (prec30 + rec30);
    bWriter.write("prec@30: "+prec30+" recall@30: "+rec30+" f1: "+fOne+"\n");
    
    fOne = (2 * prec40 * rec40) / (prec40 + rec40);
    bWriter.write("prec@40: "+prec40+" recall@40: "+rec40+" f1: "+fOne+"\n");
    
    fOne = (2 * prec50 * rec50) / (prec50 + rec50);
    bWriter.write("prec@50: "+prec50+" recall@50: "+rec50+" f1: "+fOne+"\n");
    
    bWriter.write("NDCG      " + result.get("NDCG"));
   // bWriter.write("AUC      " + result.get("AUC"));
    bWriter.write("MAP      " + result.get("MAP"));
    bWriter.write("MRR      " + result.get("MRR"));
    bWriter.close();
    
    precisionSum /= (5 * num_users);
    prec3Sum /= (3 * num_users);
    prec10Sum /= (10 * num_users);
    prec15Sum /= (15 * num_users);
    prec20Sum /= (20 * num_users);
    prec30Sum /= (30 * num_users);
    prec40Sum /= (40 * num_users);
    prec50Sum /= (50 * num_users);
    
    rec5Sum /= CorrectItemSize;
    rec3Sum /= CorrectItemSize;
    rec10Sum /= CorrectItemSize;
    rec15Sum /= CorrectItemSize;
    rec20Sum /= CorrectItemSize;
    rec30Sum /= CorrectItemSize;
    rec40Sum /= CorrectItemSize;
    rec50Sum /= CorrectItemSize;
    fOne = (2 * prec3Sum * rec3Sum) / (prec3Sum + rec3Sum);
    
    pathString = Parameter.sumF1Path + medium;
    bWriter = new BufferedWriter(new FileWriter(pathString));
    bWriter.write("prec@3: "+prec3Sum+" recall@3: "+rec3Sum+" f1: "+fOne +"\n");
    fOne = (2 * precisionSum * rec5Sum) / (precisionSum + rec5Sum);
    bWriter.write("prec@5: "+precisionSum+" recall@5: "+rec5Sum+" f1: "+fOne+"\n");
    fOne = (2 * prec10Sum * rec10Sum) / (prec10Sum + rec10Sum);
    bWriter.write("prec@10: "+prec10Sum+" recall@10: "+rec10Sum+" f1: "+fOne+"\n");
    
    fOne = (2 * prec15Sum * rec15Sum) / (prec15Sum + rec15Sum);
    bWriter.write("prec@15: "+prec15Sum+" recall@15: "+rec15Sum+" f1: "+fOne+"\n");
    fOne = (2 * prec20Sum * rec20Sum) / (prec20Sum + rec20Sum);
    bWriter.write("prec@20: "+prec20Sum+" recall@20: "+rec20Sum+" f1: "+fOne+"\n");
    
    fOne = (2 * prec30Sum * rec30Sum) / (prec30Sum + rec30Sum);
    bWriter.write("prec@30: "+prec30Sum+" recall@30: "+rec30Sum+" f1: "+fOne+"\n");
    
    fOne = (2 * prec40Sum * rec40Sum) / (prec40Sum + rec40Sum);
    bWriter.write("prec@40: "+prec40Sum+" recall@40: "+rec40Sum+" f1: "+fOne+"\n");
    
    fOne = (2 * prec50Sum * rec50Sum) / (prec50Sum + rec50Sum);
    bWriter.write("prec@50: "+prec50Sum+" recall@50: "+rec50Sum+" f1: "+fOne+"\n");
    
    bWriter.write("NDCG      " + result.get("NDCGSum"));
   // bWriter.write("AUC      " + result.get("AUCSum"));
    bWriter.write("MAP      " + result.get("MAPSum"));
    bWriter.write("MRR      " + result.get("MRRSum"));
    bWriter.close();
    //result.put("p3", prec3);
    //result.put("p5", precision);
    result.put("num_users", (double)num_users);
    result.put("num_lists", (double)num_users);
    result.put("num_items", (double)candidate_items.size());
    
    return result;
  }
  
  public static List<Integer> predictItems(IRecommender recommender, int user_id, Collection<Integer> candidate_items) {
    ArrayList<WeightedItem> precResult = new ArrayList<WeightedItem>(candidate_items.size());
    for (int item_id : candidate_items) {
      double predict = recommender.predict(user_id, item_id);
      precResult.add(new WeightedItem(item_id, predict));
    }
    Collections.sort(precResult, Collections.reverseOrder());
    
    List<Integer> return_array = new ArrayList<Integer>(precResult.size());
    for (int i = 0; i < precResult.size(); i++)  return_array.add(i, precResult.get(i).item_id);
    
    return return_array;
  }
}
