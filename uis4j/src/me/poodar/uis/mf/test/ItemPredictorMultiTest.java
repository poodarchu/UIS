package me.poodar.uis.mf.test;

import it.unimi.dsi.fastutil.ints.IntCollection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.mymedialite.IRecommender;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.Pair;
import org.mymedialite.datatype.SparseMatrix;
import org.mymedialite.eval.CandidateItems;
import org.mymedialite.eval.ItemRecommendationEvaluationResults;
import org.mymedialite.eval.Items;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.WRMF;
import me.poodar.uis.mf.utils.Parameter;
import me.poodar.uis.mf.utils.Predictor;
import me.poodar.uis.mf.utils.PredictorParameter;
import me.poodar.uis.mf.utils.WRMFThread;
import me.poodar.uis.mf.utils.QuickTopN.PairComparable;

import me.poodar.uis.mf.utils.CommunityData;
import me.poodar.uis.mf.utils.IDUtil;
import me.poodar.uis.mf.utils.TwitterIDUtil;

public class ItemPredictorMultiTest {

  public static void main(String args[]) throws Exception {

    // !!!!should save models to disk

    /*
     * shuffle data for each follower and 10 cross-over test and then split 9
     * into documents for lda community discovery output community and c.F c.G
     * finally get recommendation score.
     */
    @SuppressWarnings("resource") 
    Scanner scanner = new Scanner(System.in);
    System.out.println("How many Topics? " + args[2]);
    Parameter.L = (short) Integer.parseInt(args[2]);
    Parameter.LAssign = Integer.parseInt(args[2]);
    System.out.println("How many interest Topics?" + args[3]);
    Parameter.iL = Integer.parseInt(args[3]);
    
    System.out.println("Delay your jobs?(in hour)");
    int hour = Integer.parseInt(args[4]) * 3600 * 1000;
    try {
      Thread.sleep(hour);// 毫秒
    } catch (Exception e) {
    }

    String medium = args[0];
    if (medium.equals("0")) {
      medium = "followees";
    } else if (medium.equals("1")) {
      medium = "followers";
    } else {
      medium = "all";
    }
    String trainingDataDir = Parameter.communityDir + medium + "/";
    String testDataPath = Parameter.testDataPath;
    String orgTestDataPath = "data/keepdata.txt.100";

    IPosOnlyFeedback traningData = ItemData.read(orgTestDataPath, null, null,
        false);
    IPosOnlyFeedback testData = ItemData.read(testDataPath, null, null, false);
    List<IPosOnlyFeedback> training_data_list = new ArrayList<IPosOnlyFeedback>();
    // read c.F & c.G into data
    CommunityData communityData = new CommunityData(medium);
    communityData = communityData.read();
    communityData.setMedium(medium);

    String precDataPath = Parameter.maxF1Path + medium + "prec";
    File file = new File(precDataPath);
    file.delete();

    ExecutorService exec = Executors.newCachedThreadPool();
    List<Future<WRMF>> mfResults = new ArrayList<>();
    int counter = 0 ;
    if(args[1].equals("2") ) {
      int sysTheadSize = 8 ;
      for(int j = 0 ; j < Parameter.L / sysTheadSize + 1 ; j++) {
        for (int i = 0; i < sysTheadSize ; i++) {
          int t_no = j * 8 + i;
          if(t_no >= Parameter.L) break;
          WRMF recommender = new WRMF();
          IPosOnlyFeedback training_data = ItemData.read(trainingDataDir + Parameter.cname + t_no,
              null, null, false);
          training_data_list.add(training_data);
          recommender.setFeedback(training_data);
          WRMFThread mfThread = new WRMFThread(recommender, 
              Parameter.IFMFPath +medium+"."+t_no , 
              medium ,testData , t_no);
          mfResults.add(exec.submit(mfThread));
        }
        for (int i = 0; i < sysTheadSize ; i++) {
          int t_no = j * 8 + i;
          if(t_no >= Parameter.L) break;
          Future<WRMF> mf = mfResults.get(t_no);
          try {
            mf.get();
            System.out.println("tranning done" + counter++);
            // MFList.add(recommender);
            // save models to files for further usage.
          } catch (Exception e) {
            System.err.println(e);
          } finally {
          }
        }
      }
      exec.shutdown();
      
      System.gc();
      IDUtil idUtil = new IDUtil();
      idUtil.IDToIndexMap = TwitterIDUtil.IDToIndexMap;
      idUtil.indexToIDMap = TwitterIDUtil.indexToIDMap;
      idUtil.write();
      return ;
    }

    IDUtil idUtil = new IDUtil();
    idUtil = idUtil.read();
    TwitterIDUtil.IDToIndexMap = idUtil.IDToIndexMap;
    TwitterIDUtil.indexToIDMap = idUtil.indexToIDMap;
    
    List<IRecommender> recommenderList = new ArrayList<IRecommender>();
    
    training_data_list = new ArrayList<IPosOnlyFeedback>();
    for (int i = 0; i < Parameter.L; i++) {
      IPosOnlyFeedback training_data = ItemData.read(trainingDataDir
          + Parameter.cname + i, null, null, false);
      //数据集为空时,执行下一个
      if(training_data.userMatrix().numberOfEntries() == 0) continue;
      training_data_list.add(training_data);
      WRMF recommender = new WRMF();
      recommender.loadModel(Parameter.IFMFPath + medium + "." + i);
      recommenderList.add(recommender);
      System.out.println("tranning" + i);
    }
    Parameter.L = (short) (training_data_list.size() );
    evaluate(recommenderList, traningData, testData, training_data_list,
        testData.allUsers(), testData.allItems(), null, null, communityData,
        medium);
  }

  public static void saveprec(String medium, IPosOnlyFeedback testData,
      WRMF recommender, int i, IPosOnlyFeedback training_data)
      throws IOException {
    Collection<Integer> test_users = training_data.allUsers();
    Collection<Integer> candidate_items = training_data.allItems();
    String precDataPath = Parameter.maxF1Path + medium + "prec";
    BufferedWriter precWriter = new BufferedWriter(new FileWriter(precDataPath,
        true));
    HashMap<String, Double> results = Items.evaluate(recommender, testData,
        training_data, test_users, candidate_items);
    precWriter.write(i + "    ");
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
    Map<Integer, HashMap<Integer, Double>> resultsHashMap = max_Matrix
        .nonEmptyRows();
    Map<Integer, List<Pair<Integer, Double>>> topPairMap = new HashMap<Integer, List<Pair<Integer, Double>>>();
    for (Map.Entry<Integer, HashMap<Integer, Double>> entry : resultsHashMap
        .entrySet()) {
      Integer user = entry.getKey();
      HashMap<Integer, Double> precMap = entry.getValue();
      List<Pair<Integer, Double>> precRankedList = topN(20, precMap); // record
                                                                      // top 20
                                                                      // score
      topPairMap.put(user, precRankedList);
    }
    return topPairMap;
  }

  

  public static ArrayList<Pair<Integer, Double>> topN(int N,
      HashMap<Integer, Double> precEntry) {
    PairComparable comparable = new PairComparable();
    TreeSet<Pair<Integer, Double>> topN = new TreeSet<Pair<Integer, Double>>(
        comparable);

    Double minScore = 1000.0;
    for (Map.Entry<Integer, Double> indexFreq : precEntry.entrySet()) {
      if (indexFreq == null)
        continue;
      Double score = indexFreq.getValue();
      if (minScore > 990) {// 第一次运行
        minScore = score;
      }
      if (topN.size() < N) {// 首先填满topN
        topN.add(new Pair<Integer, Double>(indexFreq.getKey(), indexFreq
            .getValue()));
        if (score < minScore) {
          minScore = score;// 更新最低分
        }
      } else if (score > minScore) {
        topN.remove(topN.last());// 先删除topN中的最低分
        topN.add(new Pair<Integer, Double>(indexFreq.getKey(), indexFreq
            .getValue()));
        minScore = topN.last().second;// 更新最低分
      }
    }
    ArrayList<Pair<Integer, Double>> topList = new ArrayList<Pair<Integer, Double>>(
        topN);
    return topList;
  }

  public static TreeSet<Pair<Integer, Double>> top2HndrdPairs(
      TreeSet<Pair<Integer, Double>> topN, int item, double score) {
    if (topN.size() < 10000) {// 首先填满topN
      topN.add(new Pair<Integer, Double>(item, score));
      // min = topN.last().second;
    } else if (score > topN.last().second) {
      topN.remove(topN.last());// 先删除topN中的最低分
      topN.add(new Pair<Integer, Double>(item, score));
      // min = topN.last().second;
    }
    return topN;
  }

  public static TreeSet<Pair<Integer, Double>> top2HndrdPairs(
      TreeSet<Pair<Integer, Double>> topN, List<Pair<Integer, Double>> topList) {
    for (Pair<Integer, Double> p : topList) {
      if (topN.size() < 1024) {// 首先填满topN
        topN.add(p);
        // min = topN.last().second;
      } else if (p.second > topN.last().second) {
        topN.remove(topN.last());// 先删除topN中的最低分
        topN.add(p);
        // min = topN.last().second;
      }
    }
    return topN;
  }

  public static ItemRecommendationEvaluationResults evaluate(
      List<IRecommender> recommenderList, IPosOnlyFeedback orgTraining,
      IPosOnlyFeedback test, List<IPosOnlyFeedback> training,
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

    List< List<Integer> > userAllocationList = new ArrayList<List<Integer>>();
    int num = 0;
    for (Integer user_id : test_users) {
      int threadNum = (num++ / 100) % 8 ;
      List <Integer> usersAssigned;
      if(userAllocationList .size() <= threadNum) {
        usersAssigned = new ArrayList<>();
        userAllocationList.add(usersAssigned);
      } else {
        usersAssigned = userAllocationList.get(threadNum);
      }
      usersAssigned.add(user_id);
    }
    List<Predictor> predictors = new ArrayList<>();
    List<Future<PredictorParameter>> ppResults = new ArrayList<>();
    ExecutorService exec = Executors.newCachedThreadPool();
    
    for(int i = 0 ; i < userAllocationList.size() ; i ++) { 
      Predictor predictor = new Predictor(recommenderList, 
          orgTraining, 
          test, 
          training, 
          candidate_items, 
          candidate_item_mode, 
          repeated_events,
          userAllocationList.get(i));
      predictors.add(predictor);
      ppResults.add(exec.submit(predictor));
    }
    
    List<PredictorParameter> ppList = new ArrayList<>();
    
    for(Future<PredictorParameter> fs : ppResults)
      try {
        ppList.add(fs.get());
      } catch (Exception e) {
        System.out.println(e);
        return result;
      } finally {
        exec.shutdown();
      }
    
    PredictorParameter pp  = new PredictorParameter();
    PredictorParameter.sum(pp, ppList);
    num_users = pp.num_users;
    for (Entry<String, Double> entry : result.entrySet()) {
      entry.setValue(entry.getValue() / num_users);
    }

    String pathString = Parameter.maxF1Path + medium+".IF."+ Parameter.L + ".I"+Parameter.iL;
    BufferedWriter bWriter = new BufferedWriter(new FileWriter(pathString));
    
    for(int index : PredictorParameter.topNum) {
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
