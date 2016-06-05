package me.poodar.uis.lda.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighFreWords {
  
  public static List<String> wordsList = new ArrayList<String> ();
  
  public static Map<String, Boolean> wordsMap = new HashMap<String, Boolean>();
  
  public HighFreWords () {
    /*
    wordsList.add("canon");
    wordsList.add("camera");
    
      
    wordsList.add("sx");
    wordsList.add("powershot");
    wordsList.add("cameras");
    
    
    wordsList.add("quality");
    wordsList.add("feature");
    wordsList.add("work");
    wordsList.add("product");
    wordsList.add("time");
   */
    
    /*
    wordsList.add("great");
    wordsList.add("good");
     
    wordsList.add("perfect");
    wordsList.add("love");
    wordsList.add("nice");
    wordsList.add("best");
    wordsList.add("amazing");
    wordsList.add("wonderful");
    wordsList.add("better");
    */
    
    /*
    wordsMap.put("canon", true);
    wordsMap.put("camera" , true);
    
    
    wordsMap.put("sx", true);
    wordsMap.put("powershot", true);
    wordsMap.put("cameras",true);
    
  
    
    wordsMap.put("quality", true);
    wordsMap.put("feature", true);
    wordsMap.put("work", true);
    wordsMap.put("product", true);
    wordsMap.put("time", true);
    */
    
    /*
    wordsMap.put("great", true);
    wordsMap.put("good", true);
    
    wordsMap.put("perfect", true);
    wordsMap.put("love", true);
    wordsMap.put("nice", true);
    wordsMap.put("best", true);
    wordsMap.put("amazing", true);
    wordsMap.put("wonderful", true);
    wordsMap.put("better", true);
    */
  }
  
  public static boolean isHighFre(String word) {
    boolean isHigh = false;
    if(wordsMap.get(word) != null && 
        wordsMap.get(word) == true ) {
      isHigh = true;
    }
    return isHigh;
  }
}
