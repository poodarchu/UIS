package me.poodar.uis.social.lda.util;

import org.javatuples.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Community implements Serializable {

  private static final long serialVersionUID = -3200069514002950050L;

  public List<List<Integer> > CF = new ArrayList<List<Integer>>(); // followers for community,存储着每个follower所属的多个community
  public List<List<Integer> > CG = new ArrayList<List<Integer>>(); // followees for community 

  //community的边界,定义为一个存储Pair的List的链表,该Pair存储follower-followee对象
  public List< List< Pair<Integer, Integer> > > CEdgeList = 
      new ArrayList< List <Pair<Integer,Integer> > >(); //edge for community
}
