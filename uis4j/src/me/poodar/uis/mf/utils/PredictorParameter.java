package me.poodar.uis.mf.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PredictorParameter {
  public int n = 5;
  public static int topNum[] = {1,2,3,4,5, 10, 15, 20, 25,30, 40, 50};
  
  public Map<Integer, Double> prec = new HashMap<>();
  public Map<Integer, Double> recall = new HashMap<>();
  public Map<Integer, Double> ndcg = new HashMap<>();
  public Map<Integer, Integer> conversion = new HashMap<Integer, Integer>();
  
  public int CorrectItemSize = 0;
  public int num_users = 0;
  
  public double MAP = 0 ;
  public double MRR = 0;
  
  public PredictorParameter() {
    for(int i : topNum) {
      prec.put(i, 0.0);
      recall.put(i, 0.0);
      conversion.put(i, 0);
      ndcg.put(i, 0.0);
    }
  }
  
  public static void sum(PredictorParameter pp , List<PredictorParameter> ppList) {
    for(int i = 0; i < ppList.size(); i ++) {
      pp.CorrectItemSize += ppList.get(i).CorrectItemSize;
      pp.num_users += ppList.get(i).num_users;
      pp.MAP += ppList.get(i).MAP;
      pp.MRR += ppList.get(i).MRR;
      for(int num : topNum) {
        Double precV = pp.prec.get(num);
        Double precVo = ppList.get(i).prec.get(num);
        pp.prec.put(num, precV + precVo);
        
        Double recallV = pp.recall.get(num);
        Double recallVo = ppList.get(i).recall.get(num);
        pp.recall.put(num, recallV + recallVo);
        
        Double ndcgV = pp.ndcg.get(num);
        Double ndcgVo = ppList.get(i).ndcg.get(num);
        pp.ndcg.put(num, ndcgV + ndcgVo);
        
        //
        int v = pp.conversion.containsKey(num) ? pp.conversion.get(num) : 0 ;
        int vo = ppList.get(i).conversion.get(num);
        pp.conversion.put(num, v + vo);
      }
    }
    
    pp.MAP = pp.MAP / pp.num_users;
    pp.MRR = pp.MAP / pp.num_users;
  }
}
