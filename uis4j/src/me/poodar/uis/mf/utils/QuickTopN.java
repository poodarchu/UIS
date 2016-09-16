package me.poodar.uis.mf.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.mymedialite.datatype.Pair;



public class QuickTopN {
  public static ArrayList<Pair<Integer, Double>> topN(int N , List<Pair<Integer, Double>> indexFreqs) {
    PairComparable comparable = new PairComparable();
    TreeSet<Pair<Integer, Double>> topN = new TreeSet<Pair<Integer,Double>>(comparable);
    
    Double minScore = 1000.0;
    for(Pair indexFreq :indexFreqs){
      if (indexFreq == null) continue;
      Double score =(Double) indexFreq.second;
      if(minScore > 990){//第一次运行
        minScore = score;
      }
      if(topN.size() < N){//首先填满topN
        topN.add(indexFreq);
        if(score < minScore){
          minScore = score;//更新最低分
        }
      }else if(score > minScore){
        topN.remove(topN.last());//先删除topN中的最低分
        Pair<Integer, Double> IF = topN.last();
        topN.add(indexFreq);
        minScore = topN.last().second;//更新最低分
      }
    }
    
    return new ArrayList<Pair<Integer,Double>>(topN);
  }
  
public static class PairComparable implements Comparator<Pair<Integer, Double>> {
   public PairComparable() {}
    @Override
    public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
      // TODO Auto-generated method stub
      if(o1.second > o2.second) return -1;
      else if(o1.second < o2.second) return 1;
      else return 0;
    }
  }
}
