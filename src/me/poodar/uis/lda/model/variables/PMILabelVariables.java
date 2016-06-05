package me.poodar.uis.lda.model.variables;

import me.poodar.uis.lda.util.DocSentence;
import me.poodar.uis.lda.util.MSIPair;

import java.util.HashMap;
import java.util.Map;

public class PMILabelVariables {
  public final static int O_up = 0;
  public final static int O_down = 1;
  
  public final static int H_up = 0;
  public final static int H_down = 1;
  
  public Map<String, Integer> lambda = new HashMap<String,Integer> ();
  public int lambdaInSentence[][]; //indicate whether sentence d,s has words in l set. 
  
  //store String in I/L set & occurrence times
  public Map<String, Integer> ISetMap = new HashMap<String, Integer>();
  public Map<String, Integer> LSetMap = new HashMap<String, Integer>();
  
  public int S;  //Num of seed word set
  public int L;  //Num of label set
  public int P;  //Num of word with low PMI with topic 
  
  public int switcher[][]; //switcher like z[][] in LDA
  public int currentSwitcher = 0; 
  
  //a hash table
  
  public double gmma[][];
  public double gmmaSum[];
  
  public double PMISeed[][];  //PMI for seed word set & label set
  public double PMILabel[];  //PMI for label set itself
  public double PMI[][]; //
  public double PMI_v_Sum[]; //
  
  //given switcher s for word v (actually have v * Switcher dimension) 
  //count times of term v
  public int nlv[][];
  public int nlvSum[];
  
  public PMILabelVariables() {
    
  }
  
  public PMILabelVariables(int topicNum ,int wordNum ,int allocatedSpaceForWord) {
    this.PMI = new double [topicNum][allocatedSpaceForWord];
    this.PMI_v_Sum = new double [allocatedSpaceForWord];
    int Switcher = 2;
    this.nlv = new int [wordNum][Switcher];
    this.nlvSum = new int [wordNum];
    
  }
  
  public MSIPair getNearMsiPair(int m , int s , int index , int sentenceLength) {
    for(int i = 1;index - i >= 0 && index + i < sentenceLength;i++) {
      if(index - i >= 0) {
        if (getLambdaValue(m, s, index - i) == PMILabelVariables.H_up) {
          return new MSIPair(m,s,index - 1);
        }
      }
      if(index + i < sentenceLength) {
        if (getLambdaValue(m, s, index + i) == PMILabelVariables.H_up) {
          return new MSIPair(m,s,index + 1);
        }
      }
    }
    return null;
  }
  
  public void initializeGmma(int LV , double gmmaValue) {
    int Switcher = 2;
    gmma = new double [LV][Switcher];
    gmmaSum = new double [LV];
    for(int v = 0 ; v < LV ; v++) {
      gmma[v][0] = gmmaValue;
      gmma[v][1] = 1 - gmmaValue;
      gmmaSum[v] = 1.0 ;
    }
  }
  
  public void initializeLambda (int M , DocSentence dc) {
    lambdaInSentence = new int [M][];
    for(int m = 0 ; m < M ; m++) {
      lambdaInSentence[m] = new int [dc.docs.get(m).lines];
    }
  }
  
  public int getLambdaValue(int m , int s , int index) {
    Integer lambdaValue = lambda.get(new MSIPair(m, s, index).toString());
    if(lambdaValue != null) {
      return PMILabelVariables.H_up;
    } else {
      return PMILabelVariables.H_down;
    }
  }
  
  public void setLambdaValueUP (int m , int s , int index ,int value) {
    Integer lambdaValue = lambda.get(new MSIPair(m, s, index).toString());
    if(lambdaValue != null ) {
      lambda.put(new MSIPair(m, s, index).toString(), PMILabelVariables.H_up);
    } else {
      return ;
    }
  }
  
  public void setLambdaValueDOWN (int m ,int s ,int index ,int value) {
    Integer lambdaValue = lambda.get(new MSIPair(m, s, index).toString());
    if(lambdaValue != null) {
      lambda.remove(new MSIPair(m, s, index).toString());
    } else {
      return ;
    }
  }

  public void putIntoISetMap() {
    
  }
  
  public void putIntoLSetMap() {
    
  }
}
