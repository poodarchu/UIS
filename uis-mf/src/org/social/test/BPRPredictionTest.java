package org.social.test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collection;
import java.util.HashMap;

import org.mymedialite.IRecommender;
import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.eval.Items;
import org.mymedialite.io.ItemData;
import org.mymedialite.itemrec.BPRMF;
import org.mymedialite.itemrec.MostPopular;
import org.social.util.Parameter;

public class BPRPredictionTest {

  /**
   * @param args
   * @throws Exception 
   */
  public static void main(String[] args) throws Exception {
    // TODO Auto-generated method stub
    String testDataPath = Parameter.testDataPath;
    IPosOnlyFeedback testData = ItemData.read(testDataPath , null, null, false);
    String orgTestDataPath = Parameter.trainingDataPath+".100";
    IPosOnlyFeedback training_data = ItemData.read(orgTestDataPath, null, null, false);
    String name = "ORG";
    BPRMF recommender = new BPRMF();
    //MostPopular recommender = new MostPopular();
    if(args[0].equals("0")) {
      recommender.setFeedback(training_data);
      recommender.train();
      recommender.saveModel(Parameter.BPRPath + name);
    } else {
      recommender.loadModel(Parameter.BPRPath + name);
    }
    Collection<Integer> candidate_items = training_data.allItems(); 
    Collection<Integer> test_users      = training_data.allUsers();
    try {
      HashMap<String, Double> results = Items.evaluate(recommender, testData, training_data, test_users, candidate_items);
      System.out.println("AUC       " + results.get("AUC"));
      System.out.println("MAP       " + results.get("MAP"));
      System.out.println("NDCG      " + results.get("NDCG"));
      System.out.println("prec@5    " + results.get("prec@5"));
      System.out.println("prec@10   " + results.get("prec@10"));
      System.out.println("prec@15   " + results.get("prec@15"));
      System.out.println("num_users " + results.get("num_users"));
      System.out.println("num_items " + results.get("num_items"));
      System.out.println();
      BufferedWriter bWriter = new BufferedWriter(new FileWriter(Parameter.maxF1Path + "BPR.ORG"));
      int[] positions = new int[] {1, 2, 3 , 4, 5, 10 ,15 , 20 , 25 ,30 };
      for(int i = 0 ; i < positions.length ; i ++) {
        double prec = results.get("prec@" + positions[i]);
        double recall = results.get("recall@" + positions[i]);
        double fOne = 2 * prec * recall / (recall + prec);
        double conversion = results.get("conversion@" + positions[i]);
        double ndcg = results.get("NDCG@"+positions[i]);
        bWriter.write(positions[i]+" precision: "+prec+" recall: "+recall+" f1: "+fOne + 
            " conversion :" + conversion +" NDCG:" + ndcg +"\n");
      }
      bWriter.write("NDCG      " + results.get("NDCG"));
      bWriter.close();  
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
}
