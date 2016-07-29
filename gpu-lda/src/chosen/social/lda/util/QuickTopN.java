package chosen.social.lda.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import org.javatuples.Pair;

public class QuickTopN {
  public static ArrayList<Pair<Integer, Double>> topN(int N , List<Pair<Integer, Double>> indexFreqs) {
    PairComparable comparable = new PairComparable();
    TreeSet<Pair<Integer, Double>> topN = new TreeSet<Pair<Integer,Double>>(comparable);
    
    Double minScore = 1000.0;
    for(Pair indexFreq :indexFreqs){
      if (indexFreq == null) continue;
      if(minScore > 990){//第一次运行
        minScore = (Double) indexFreq.getValue1();
      }
      if(topN.size() < N){//首先填满topN
        topN.add(indexFreq);
        if((Double)indexFreq.getValue1()  < minScore){
          minScore = (Double)indexFreq.getValue1();//更新最低分
        }
      }else if((Double)indexFreq.getValue1() > minScore){
        topN.remove(topN.first());//先删除topN中的最低分
        topN.add(indexFreq);
        minScore = topN.first().getValue1();//更新最低分
      }
    }
    
    return new ArrayList<Pair<Integer,Double>>(topN);
  }
  
public static class PairComparable implements Comparator<Pair<Integer, Double>> {
   public PairComparable() {}
    @Override
    public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
      // TODO Auto-generated method stub
      if(o1.getValue1() > o2.getValue1()) return -1;
      else if(o1.getValue1() < o2.getValue1()) return 1;
      else return 0;
    }
  }
}
