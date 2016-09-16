package me.poodar.uis.lda.utils;

import java.io.Serializable;
import java.util.Random;

import me.poodar.uis.lda.utils.Doc;
import me.poodar.uis.lda.utils.DocSentence;

public class IndexFreq implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -5100819550103587959L;

  public int index;
  
  public int times;
  
  public IndexFreq(int index , int times) {
    // TODO Auto-generated constructor stub
    this.index = index;
    this.times = times;
  }
  
  public IndexFreq (String token , Doc doc) {
    String[] tokens = token.split(":");
    if(doc.contains(tokens[0])) {
      this.index = doc.getIndex(tokens[0]);
      times =  (int) (Double.parseDouble(tokens[1]) * 100);
    } 
  }
  
  public boolean equals(IndexFreq indexFreq) {
    if (indexFreq.index == this.index && indexFreq.times == this.times) {
      return true;
    } else  {
      return false;
    }
  }
}
