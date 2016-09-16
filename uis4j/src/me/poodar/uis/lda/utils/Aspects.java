package me.poodar.uis.lda.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Aspects {
  public static Map<String, List<String>> aspToSeedList 
    = new HashMap<String, List<String>> (); 
  public static Map <String,String> seedToAspect 
    = new HashMap<String, String> (); 
  
  public Aspects() {
    String asp;
    List <String> seedList = new ArrayList<String>();
    
    asp = "picture";
    seedList.add("picture");
    seedList.add("photos");
    seedList.add("image");
    addToMap(asp, seedList);
    
    asp = "len";
    seedList = new ArrayList<String>();
    seedList.add("zoom");
    seedList.add("slr");
    seedList.add("dslr");
    seedList.add("lens");
    addToMap(asp, seedList);
    
    asp = "screen";
    seedList = new ArrayList<String>();
    seedList.add("screen");
    seedList.add("lcd");
    addToMap(asp, seedList);
    
    asp = "price";
    seedList = new ArrayList<String>();
    seedList.add("money");
    seedList.add("price");
    seedList.add("worth");
    addToMap(asp, seedList);
    
    asp = "shooting";
    seedList = new ArrayList<String>();
    seedList.add("shooting");
    seedList.add("shoot");
    seedList.add("shots");
    addToMap(asp, seedList);
    
    asp = "mode";
    seedList = new ArrayList<String>();
    seedList.add("auto");
    seedList.add("mode");
    seedList.add("manual");
    addToMap(asp, seedList);
    
    asp = "shutter";
    seedList = new ArrayList<String>();
    seedList.add("shutter");
    seedList.add("speed");
    addToMap(asp, seedList);
    
    asp = "battery";
    seedList = new ArrayList<String>();
    seedList.add("battery");
    seedList.add("adapter");
    //seedList.add("voltage");
    addToMap(asp, seedList);
    
    /*
    asp = "beginner";
    seedList = new ArrayList<String>();
    //seedList.add("user");
    seedList.add("beginner");
    seedList.add("easy");
    addToMap(asp, seedList);
    */
    /*
    asp = "purchase";
    seedList = new ArrayList<String>();
    seedList.add("purchased");
    seedList.add("purchase");
    addToMap(asp, seedList);
    */
  }

  private void addToMap(String asp, List<String> seedList) {
    aspToSeedList.put(asp, seedList);
    for(String seedWord : seedList) {
      seedToAspect.put(seedWord, asp);
    }
  }
  
  public static boolean isSeed( String word ) {
    return (seedToAspect.get(word) != null);
  }
  
  public static String getAspect(String word) {
    return seedToAspect.get(word);
  }
  
}
