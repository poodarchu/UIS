package me.poodar.uis.mf.utils;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.mymedialite.IRecommender;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.data.WeightedItem;
import org.mymedialite.datatype.IBooleanMatrix;
import org.mymedialite.eval.CandidateItems;
import org.mymedialite.eval.measures.NDCG;
import org.mymedialite.eval.measures.PrecisionAndRecall;
import org.mymedialite.eval.measures.ReciprocalRank;
import org.mymedialite.util.Utils;


public class Predictor implements Callable<PredictorParameter> {
  PredictorParameter pp;

  private IPosOnlyFeedback orgTraining;
  private List<IPosOnlyFeedback> training;
  private List<IRecommender> recommenderList;
  private Collection<Integer> candidate_items;
  private Boolean repeated_events;
  IBooleanMatrix test_user_matrix;
  
  private List<Integer> user_id_queue;
  private List<IBooleanMatrix> userMatrixs = new ArrayList<>(50);
  private List<IntList> allItems = new ArrayList<>(50);
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
  public Predictor(
      List<IRecommender> recommenderList, IPosOnlyFeedback orgTraining,
      IPosOnlyFeedback test, List<IPosOnlyFeedback> training,
      Collection<Integer> candidate_items,
      CandidateItems candidate_item_mode, 
      Boolean repeated_events,
      List<Integer> user_id_queue ) {
    
    this.recommenderList = recommenderList;
    this.orgTraining = orgTraining;
    this.training = training;
    this.candidate_items = candidate_items;
    this.repeated_events = repeated_events;
    test_user_matrix = test.userMatrix();
    pp = new PredictorParameter();
    this.user_id_queue = user_id_queue;
    
    for (int i = 0; i < Parameter.L; i++) {
      IBooleanMatrix training_user_matrix = training.get(i).userMatrix();
      userMatrixs.add(training_user_matrix);
      IntList intList = training.get(i).allItems();
      allItems.add(intList);
    }
  }
  
  @Override
  public PredictorParameter call() {
    // TODO Auto-generated method stub
    for(Integer user_id : user_id_queue) {
      predict(user_id);
    }
    return pp;
  }
  
  public void predict(int user_id) {
    HashSet<Integer> correct_items = new HashSet<Integer>(Utils.intersect(
        test_user_matrix.get(user_id), candidate_items));
    ArrayList<WeightedItem> precMaxList = new ArrayList<WeightedItem>(20000);
    double[] maxIMap = new double[orgTraining.maxItemID() + 1];
    //double[] maxSMap = new double [orgTraining.maxItemID() + 1];
    for (int i = 0; i < Parameter.L; i++) {
      IBooleanMatrix training_user_matrix = userMatrixs.get(i);
      
      // The number of items that will be used for this user.
      HashSet<Integer> candidate_items_in_train = new HashSet<Integer>(
          Utils.intersect(training_user_matrix.get(user_id), candidate_items));
      // exclude all the known followees****

      int num_eval_items = candidate_items.size()
          - (repeated_events ? 0 : candidate_items_in_train.size());

      // Skip all users that have 0 or #relevant_items test items.
      if (correct_items.size() == 0)
        continue;
      if (num_eval_items - correct_items.size() == 0)
        continue;

      IRecommender recommender = recommenderList.get(i);

      for (int item_id : allItems.get(i)) {
        if (!recommender.canPredict(user_id, item_id))
          break;
        double predict = recommender.predict(user_id, item_id);
        /*
        if(i < Parameter.iL) {
          double score = maxIMap[item_id];
          if (predict > score)
            maxIMap[item_id] = predict;
        } else {
          double score = maxSMap[item_id];
          if (predict > score)
            maxSMap[item_id] = predict;
        }
        */
        double score = maxIMap[item_id];
        if (predict > score)
          maxIMap[item_id] = predict;
      }
    }

    for (int k = 0; k < maxIMap.length; k++) {
//      double iRank = maxIMap[k] > 0 ? maxIMap[k] : 0;
//      double sRank = maxSMap[k] > 0 ? maxSMap[k] : 0 ;
//      double rankV = iRank + sRank;
//      precMaxList.add(new WeightedItem(k, rankV));
      precMaxList.add(new WeightedItem(k,maxIMap[k]));
    }
    Collections.sort(precMaxList, Collections.reverseOrder());

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
      List<Integer> prediction_sub_list = prediction_max_list.subList(0, num);
      double ndcg = NDCG.compute(prediction_sub_list , correct_items,
        ignore_items);
      double pv = pp.prec.get(num) + prec.get(num) * num;
      double rv = pp.recall.get(num) + recall.get(num) * correct_items.size();
      double ndcgV = pp.ndcg.get(num) + ndcg;
      int c = pp.conversion.get(num);
      if( prec.get(num) > 0.000001 ) pp.conversion.put(num, c + 1); 
      pp.prec.put(num, pv);
      pp.recall.put(num, rv);
      pp.ndcg.put(num, ndcgV);
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
  
}
