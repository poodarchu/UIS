package me.poodar.uis.lda.utils;

import java.util.List;

public class SIPair {
  public int index ; 
  public int sent;

  public SIPair(int sentence , int wordPosit) {
    this.sent = sentence ; 
    this.index = wordPosit;
  }
  
  public static SIPair getNearest (SIPair si ,List<SIPair> list) {
    if(list == null) 
      return null;
    if(list.isEmpty())
      return null;
    SIPair siPair = new SIPair(si.sent,si.index);
    int dis = 10000;
    for(SIPair pairCompared : list) {
      int disCompared = Math.abs(si.index - pairCompared.index);
      if ( disCompared  < dis) {
        dis = disCompared;
        siPair = pairCompared;
      }
    }
    return siPair;
  }
  
  public String toString () {
    return String.valueOf(sent) +"+" + String.valueOf(index);
  }
  
  public int getIndex() {
    return index;
  }
  public void setIndex(int index) {
    this.index = index;
  }
  public int getSent() {
    return sent;
  }
  public void setSent(int sent) {
    this.sent = sent;
  } 
  public boolean equals(SIPair siPair) {
    if(siPair.sent == this.sent && siPair.index == this.index) {
      return true;
    }
    else {
      return false;
    }
  }
}
