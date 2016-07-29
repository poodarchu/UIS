package org.social.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Callable;

import org.mymedialite.data.IPosOnlyFeedback;
import org.mymedialite.eval.Items;
import org.mymedialite.itemrec.WRMF;

public class WRMFThread implements Callable<WRMF>{
  
  private WRMF mf;
  private String savePath;
  String medium ;
  IPosOnlyFeedback testData;
  int tnum;
  public WRMFThread(WRMF wrmf ,
      String path,String medium ,
      IPosOnlyFeedback testData,int tnum) {
    mf = wrmf;
    savePath = path;
    this.medium = medium;
    this.testData = testData;
    this.tnum = tnum;
  }
  
  @Override
  public WRMF call() {
    // TODO Auto-generated method stub
    mf.train();
    try {
      mf.saveModel(savePath);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    /*
    try {
      saveprec(medium, testData, mf, tnum, mf.getFeedback());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }*/
    return mf;
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
}
