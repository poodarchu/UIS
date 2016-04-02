package me.poodar.uis.lda.util;

public class MSIPair {
  public int doc;
  public int sent;
  public int index;
  
  public MSIPair(int doc,int sentence , int wordPosit) {
    this.doc = doc;
    this.sent = sentence ; 
    this.index = wordPosit;
  }
  
  public String toString () {
    return String.valueOf(doc) + String.valueOf(sent) +"+" + String.valueOf(index);
  }
  
  public boolean equals(MSIPair msiPair) {
    if(
        msiPair.doc == this.doc && 
        msiPair.sent == this.sent && 
        msiPair.index == this.index) {
      
      return true ;
    } else {
      return false;
    }
  }
}
