package me.poodar.uis.lda.test;

import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.util.Documents;
import me.poodar.uis.social.lda.util.Link;
import me.poodar.uis.social.lda.util.TwitterIDUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.*;

public class ExtractLatentLink {
  
 /**
  * As long as the referent of a soft reference is strongly reachable, that is, is actually in use, the soft reference will not be cleared. 
  * Thus a sophisticated cache can, for example, prevent its most recently used entries from being discarded by keeping strong referents to those entries, 
  * leaving the remaining entries to be discarded at the discretion of the garbage collector.
  */
  // the followeeDocuments will soft referent to Class Documents
  public static SoftReference<Documents> followerDocuments = new SoftReference<Documents>(new Documents());
  public static SoftReference<Documents> followeeDocuments = new SoftReference<Documents>(new Documents());
  
  public static List< List<IndexFreq> > latentFolloweeMap = new LinkedList<List<IndexFreq>>();  //隐含followee的Map
  public static List< List<IndexFreq> > latentFollowerMap = new LinkedList<List<IndexFreq>>();
  
  public static int switcher = 0;
  public static String arg;

  /**
   * getDocs需要完成的工作有：
   * 1，将ReadDocTest输出的followers和followees文件夹下的文件保存到Documents中，每个*.txt对应一个Document对象
   * 2，将生成的map复制到TwitterIDUtil中
   */
  public void getDocs() {
    //read documents and 
    followerDocuments.get().readDocs(PathConfig.followerPath); //从ReadDocTest的输出文件/wb/followers/中读取*.txt, 生成每篇txt对应的Document对象
    
    //对于soft reference的对象，需要手动调用get方法来获取对象引用，而不能只是用标识符
//    followeeDocuments的各种Map和follower相同
    followeeDocuments.get().indexToTermMap = followerDocuments.get().indexToTermMap;
    followeeDocuments.get().termCountMap = followerDocuments.get().termCountMap;
    followeeDocuments.get().termToIndexMap = followerDocuments.get().termToIndexMap;
    //补充followerDocuments中没有的部分
    followeeDocuments.get().readDocs(PathConfig.followeePath);
    
    //copy the map out for gc
    TwitterIDUtil.IDToIndexMap = new HashMap<String, Integer>(followerDocuments.get().termToIndexMap);
    TwitterIDUtil.indexToIDMap = new ArrayList<String>(followerDocuments.get().indexToTermMap);
    
  }
  
  /**
   * extract步骤：
   * 1， 将上一步保存到followerDocuments中的words存入二维数组（转化为二维数组的形式）
   * 2， 将二维数组中的每一项保存到followe*List中
   * 3， 从以上得到的两个二维数组中提取双向关系
   * @param args
   */
  public static void extract (String args) {
    //
//    latentFolloweeMap = new IndexFreq [TwitterIDUtil.getSize()][];
//    latentFollowerMap = new IndexFreq [TwitterIDUtil.getSize()][];
    arg = args;
    
    //二维数组形式的follower_map,每个follower对应一个一维的followee数组
    Integer[][] follower_Map  = new Integer [TwitterIDUtil.getSize()][];  //size是indexToIDMap的size， 每个follower对应一个index
    Integer[][] followee_Map  = new Integer [TwitterIDUtil.getSize()][];
    
    //遍历followerDocuments
    for(Documents.Document fDocument: followerDocuments.get().docs) {
      Integer [] dest = new Integer[fDocument.docWords.length];
      //将每一个Document中的docwords存储到新建好的dest数组中，即以数组形式表示docwords
      System.arraycopy( fDocument.docWords, 0, dest, 0, fDocument.docWords.length );
      //获取docName对应的index
      Integer index = followerDocuments.get().getIndex(fDocument.docName);
      //将新的二维数组的第index个设置为dest
      follower_Map[index] = dest;
    }
    for(Documents.Document fDocument: followeeDocuments.get().docs) {
      Integer [] dest = new Integer[fDocument.docWords.length];
      System.arraycopy( fDocument.docWords, 0, dest, 0, fDocument.docWords.length );
      followee_Map[followeeDocuments.get().getIndex(fDocument.docName)] = dest;
    }
    
    
    List<List<Integer>> followerList = new ArrayList<List<Integer>>();
    List<List<Integer>> followeeList = new ArrayList<List<Integer>>();
    List<List<Integer>> mutualLink_List = new ArrayList<List<Integer>>();
    
    for (int i = 0; i < follower_Map.length; i++) {//follower_map.length即为follower的总数目
      if (follower_Map[i] == null) {
        followerList.add( new ArrayList<Integer>() );
      } else {
        followerList.add( Arrays.asList(follower_Map[i]) ); //将follower_map的第i项转换为List存到followerList链表中
      }
    }
    
    //对followeeList采用同样的处理
    for(int i = 0 ; i < followee_Map.length ; i++ ) {
      if (followee_Map[i] == null) {
        followeeList.add( new ArrayList<Integer>() );
      } else {
        followeeList.add( Arrays.asList(followee_Map[i]) );
      }
    }
    
    follower_Map = null;
    followee_Map = null;
    System.gc();
    
    //从followerList和followeeList中寻找双向的关系，符合条件的加入到mutualLink_list
    findMutualLink(followerList, followeeList, mutualLink_List); 
    /*
    if (!arg.equals("0")) {
      findLatentFollower(followerList, followeeList, mutualLink_List);
    } else {
      findLatentFollowee(followerList, followeeList, mutualLink_List);
    }
    */
    //将找到的mutualLink存到相应的文件中
    LinkToDocuments(mutualLink_List);
    
  }
  
  private static void findMutualLink(
      List<List<Integer>> followerList, 
      List<List<Integer>> followeeList,
      List<List<Integer>> mutualLink_List ) {
	  
    Set<Integer> retainSet = new HashSet<Integer>();
    
    for(int i = 0 ; i < followerList.size() ;i++) {
      retainSet.clear();
      retainSet.addAll(followerList.get(i));
      
      List<Integer> followees = followeeList.get(i);
      if(followees != null && followees.size() > 0){
    	  //retainAll():
    	  //Retains only the elements in this set 
    	  //that are contained in the specified collection (optional operation).
    	  //In other words, removes from this set all of its elements that are not contained in the specified collection. 
    	  //If the specified collection is also a set, this operation 
    	  //effectively modifies this set so that its value is the intersection of the two sets.
    	  //意思就是求这两个集合的交集
        retainSet.retainAll(followees);
      } else {
        retainSet.clear();
      }
      
      mutualLink_List.add(new ArrayList<Integer>(retainSet));
//      int k = 0;
    }
    System.out.println("findMutualLink finishes");
  }

  //发现潜在的folowee
  public static void findLatentFollowee(
      List<List<Integer>> followerList, 
      List<List<Integer>> followeeList,
      List<List<Integer>> mutualLink_List ) {


	int times = 0 ;
    for(int i = 0 ; i < TwitterIDUtil.getSize() ; i++) {
      //先找出follower的相互关注者 与 相互关注者的 关注对象求 不相同的集合
      if(++times % 10000 == 0)
        System.out.println(times + "latentFollowee links ");
      
      List<Integer> latentFollowee = new ArrayList<Integer>();
      
      //先找出follower关注的人
      List<Integer> followees = followerList.get(i);
      //从关注的人中提取出相互关注的人
      if(mutualLink_List.get(i) == null) continue;//循环重新开始
      
      for(Integer mutualIndex : mutualLink_List.get(i)) {
    	  //获取相互关注的人的folowee列表，将其加到latentFolowee的List中
        List<Integer> followeeOfMutuals = followerList.get(mutualIndex);
        latentFollowee.addAll( followeeOfMutuals );
      }
      
      //从latentFollowee中截取出最有可能的一部分，加入到LatentFoloweeMap中
      List<IndexFreq> topList = cutList(latentFollowee, followees, i);
//      latentFolloweeMap[i] = (IndexFreq[]) topList.toArray(new IndexFreq[topList.size()]);
      latentFolloweeMap.add(topList);
    }
    
    for(int j = 0 ; j < followerList.size() ; j++) {
      
      if(++times % 10000 == 0)
        System.out.println(times + "latentFollowee links ");
      
      List<Integer> mutualOfFollowee = new LinkedList<Integer>();
      List<Integer> mutualList= mutualLink_List.get(j);
      //folowerList is a List of List<Integer>
      for(int i : followerList.get(j)) {
        if (mutualList != null && mutualList.contains(i)) {
          continue;
        }
        if(mutualLink_List.get(i) == null) continue;
        mutualOfFollowee.addAll( mutualLink_List.get(i) );
      }
      List<IndexFreq> existLatent = latentFolloweeMap.get(j);
      List<IndexFreq> topList = cutList(existLatent,mutualOfFollowee, followerList.get(j), j);
      latentFolloweeMap.remove(j);
      latentFolloweeMap.add(j, topList);
    }
    
  }

  public static void findLatentFollower(
      List<List<Integer>> followerList, 
      List<List<Integer>> followeeList,
      List<List<Integer>> mutualLink_List ) {
    // TODO Auto-generated method stub
    int times = 0 ;
    for(int i = 0; i < TwitterIDUtil.getSize() ; i++) {
      //找到followee 的 mutuallink 将mutuallink的follower 放进followee中
      if(++times % 10000 == 0)
        System.out.println(times + "latentFollower links ");
      
      List<Integer> latentFollower = new ArrayList<Integer>();
      List<Integer> followers = followeeList.get(i);
      
      if(mutualLink_List.get(i) == null) continue;
      
      for (Integer mutualIndex : mutualLink_List.get(i)) {
        List<Integer> followerOfMutuals = followeeList.get(mutualIndex);
        latentFollower.addAll(followerOfMutuals);
      }
      List<IndexFreq> topList = cutList(latentFollower, followers, i);
//      latentFollowerMap[i] = (IndexFreq[]) topList.toArray(new IndexFreq[topList.size()]);
      latentFollowerMap.add(topList);
    }
    
    for(int j = 0 ; j <followeeList.size() ; j++) {
      
      if(++times % 10000 == 0)
        System.out.println(times + "latentFollower links ");
      
      List<Integer> mutualOfFollowee = new LinkedList<Integer>();
      List<Integer> mutualList = mutualLink_List.get(j);
      for(int i : followeeList.get(j)) {
        if (mutualList != null && mutualList.contains(i)) {
          continue;
        }
        if(mutualLink_List.get(i) == null) continue;
        mutualOfFollowee.addAll(mutualLink_List.get(i));
      }
      List<IndexFreq> existLatent = latentFollowerMap.get(j);
      List<IndexFreq> topList = cutList(existLatent, mutualOfFollowee, followeeList.get(j), j);
      latentFollowerMap.remove(j);
      latentFollowerMap.add(j, topList);
    }
  }

  private static void LinkToDocuments(
      List<List<Integer>> mutualLink_List) {
    
    BufferedWriter bufferedWriter ;
    try {
      bufferedWriter = new BufferedWriter(new FileWriter(PathConfig.mutualLinkPath));
      int times = 0 ;
      for(int i = 0; i < TwitterIDUtil.getSize() ; i++) {
        if(++times % 10000 == 0) {
          System.out.println("++++++++"+times + "mutual-links ok. ++++++++");
        }
        
        bufferedWriter.write("#" + Link.getLinkeeVaule(i ) + " ");
        if(mutualLink_List.get(i) == null) continue;
        
        //randomize mutuallinks
        List<Integer> mutualList = mutualLink_List.get(i);
        Collections.shuffle(mutualList);
        
        for(Integer index : mutualList) {
          bufferedWriter.write(Link.getLinkeeVaule(index) + " ");
        }
        bufferedWriter.write("\n");
      }
      bufferedWriter.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
    
    String pathString  = PathConfig.latentFolloweePath;
    if (!arg.equals("0")) {
      //writeLatentFollow(PathConfig.latentFollowerPath, latentFollowerMap);
    } else {
      writeLatentFollow(pathString, latentFolloweeMap);
    }
  }

  /**
   * oList: latentFoloweeList
   * rList：currentFoloweeList
   */
  public static List<IndexFreq> cutList (List<Integer> oList , List<Integer> rList ,Integer self) {
    
    if (rList == null) {
      rList = new ArrayList<Integer>();
      rList.add( -1 );
    }
    //将rList转为HashSet
    HashSet<Integer> rSet = new HashSet<Integer>(rList);
    
    //countMap中存储的是每个latentFolowee在整个oList中出现的次数（count）
    Map<Integer, Integer> countMap = new HashMap<Integer,Integer>();
    //过滤掉LatentList中和已关注者之间的重叠部分
    for(Integer index : oList) {
      if (index.equals(self) || rSet.contains(index)) continue;
      Integer count = countMap.get(index);
      //将剩余的latentFolowee的index加入到countMap中，如果之前已经存在，就将count + 1
      countMap.put(index, (count == null) ? 1 : count + 1);
    }
    
    List<IndexFreq> topList = new LinkedList<IndexFreq>();

    for (Map.Entry<Integer, Integer> countEntry : countMap.entrySet()) {
    	//IndexFreq对象中存储的是每个latentFolowee的index和他出现的次数times
      topList.add(new IndexFreq( countEntry.getKey(), countEntry.getValue() ) );
    }
    Collections.sort(topList);
    Collections.reverse(topList);
    
    List<IndexFreq> newtopList = new LinkedList<IndexFreq>();
//    保留至多100个latentFolowee
    int size = topList.size() > 100 ? 100 : topList.size();
    for(int i = 0 ; i < size ; i ++) {
      newtopList.add(topList.get(i));
    }
    return newtopList;
  }
  
  public static List<IndexFreq> cutList (
      List<IndexFreq> existLatent ,
      List<Integer> oList , 
      List<Integer> rList ,
      Integer self) {
    
    if (rList == null || rList.size() == 0) {
      rList = new ArrayList<Integer>();
      rList.add( -1 );
    }
    
    HashSet<Integer> rSet = new HashSet<Integer>(rList);
    
    Map<Integer, Integer> countExistMap = new HashMap<Integer,Integer>();
    
    if(existLatent != null) {
      for(IndexFreq index : existLatent) {
        if (index == null) continue;
        if (self == null ) break;
        if (index.equals(self) || rSet.contains(index)) continue;
        countExistMap.put(index.index,index.times);
      }
    }
    Map<Integer, Integer> countMap = new HashMap<Integer,Integer>();
    for(Integer index : oList) {
      if (index.equals(self) || rSet.contains(index)) continue;
      Integer count = countMap.get(index);
      countMap.put(index, (count == null) ? 1 : count + 1);
    }
    
    //this is a wrong doing
    //countMap.putAll(countExistMap);
    for(Map.Entry<Integer, Integer> entry : countExistMap.entrySet()) {
      if(!countMap.containsKey(entry.getKey())) {
        countMap.put(entry.getKey(), entry.getValue());
      } else {
    	  //如果exsitLatent中已经有某一个对象，则将该entrySet.getKey()的count和原count相加
        Integer sum = countMap.get(entry.getKey()) + entry.getValue();
        countMap.put(entry.getKey(), sum);
      }
    }

    List<IndexFreq> topList = new LinkedList<IndexFreq>();
    
    for (Map.Entry<Integer, Integer> countEntry : countMap.entrySet()) {
      topList.add(new IndexFreq( countEntry.getKey(), countEntry.getValue() ) );
    }
    
    Collections.sort(topList);
    Collections.reverse(topList);
    
    List<IndexFreq> newtopList = new LinkedList<IndexFreq>();
    int size = topList.size() > 100 ? 100 : topList.size();
    for(int i = 0 ; i < size ; i ++) {
      newtopList.add(topList.get(i));
    }
    return newtopList;
  }
  
  public static void writeLatentFollow(String pathString ,
      List <List<IndexFreq>> latentFollowMap) {
    
    BufferedWriter bufferedWriter;
    try {
      bufferedWriter = new BufferedWriter(new FileWriter(pathString));
      for(int i = 0 ; i < TwitterIDUtil.getSize() ; i++) {
        bufferedWriter.write("#" + Link.getLinkeeVaule( i ) + " " );
        if(latentFollowMap.get(i) == null) continue;
        
        //找出前N个最大的latent follow
        List<IndexFreq> latentFollows = latentFollowMap.get(i);
        List <IndexFreq> element = topN(25, latentFollows);
        //先移除latentFolows中原有的List，找出前25个后，重新加入LatentFollowMap
        latentFollowMap.remove(i);
        latentFollowMap.add(i, element);
        
        
        for (IndexFreq indexFreq : latentFollowMap.get(i)) {
          if (indexFreq == null) continue;
          bufferedWriter.write(getV(indexFreq.index) + ":" + 
              ((indexFreq.times <= 20) ? indexFreq.times * 0.01 : 0.2)+ " ");
        }
        bufferedWriter.write("\n");
      }
      bufferedWriter.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void listToDocuments(
      Map<Integer, List<Integer>> follower_Map, String path) {
    
    BufferedWriter bufferedWriter = null;
    int times = 0 ;
    for(Map.Entry<Integer, List<Integer> > followEntry : follower_Map.entrySet()) {
      if(++times % 10000 == 0) {
        System.out.println(times + "links ok.");
      }
      
      try {
        String filePath = path + Link.getLinkeeVaule(followEntry.getKey()) + ".txt";
        bufferedWriter =new BufferedWriter(new FileWriter(
            filePath, true) );
        
        for(Integer index : followEntry.getValue()) {
          bufferedWriter.append(Link.getLinkeeVaule(index) + " ");
        }
        bufferedWriter.close();
        
      } catch (IOException e) {
        // exception handling left as an exercise for the reader
      }
    }
  }

  public static void listToDocuments(
      Map<Integer, List<Integer>> follower_Map, 
      Map<Integer, List<Integer>> followee_Map,String path) {
    
    BufferedWriter bufferedWriter = null;     
    int times = 0 ;
    for(Map.Entry<Integer, List<Integer> > followEntry : follower_Map.entrySet()) {
      if(++times % 10000 == 0) {
        System.out.println(times + "links ok.");
      }
      
      try {
        String filePath = path + Link.getLinkeeVaule(followEntry.getKey()) + ".txt";
        bufferedWriter =new BufferedWriter(new FileWriter(
            filePath, true) );
        
        HashSet<Integer> unionSet = new HashSet<Integer>();
        unionSet.addAll(followEntry.getValue());
        if(followee_Map.containsKey(followEntry.getKey())){
          List<Integer> followeeList= followee_Map.get(followEntry.getKey());
          unionSet.addAll(followeeList);
        }
        
        for(Integer index : unionSet) {
          bufferedWriter.append(Link.getLinkeeVaule(index) + " ");
        }
        bufferedWriter.close();
        
      } catch (IOException e) {
        // exception handling left as an exercise for the reader
      }
    }
  }
  public static void addToLinkListMap(
      Map<Integer, List<Integer>> follow_Map, int executor , int target) {
    List<Integer> followList;
    if (!follow_Map.containsKey(executor) ) {
      followList = new ArrayList<Integer>();
      followList.add(target);
      follow_Map.put(executor, followList);
    } else {
      followList = follow_Map.get(executor);
      followList.add(target);
      follow_Map.put(executor, followList);
    }
  }
  
  public static void tokenizedToLinkAndgetLastLine(String dataString,
      List<Link> links,char spliter, String lastLine) {
    // TODO Auto-generated method stub
    
    StringTokenizer strTok = new StringTokenizer(dataString);
    while (strTok.hasMoreTokens()) {
      String token_follower = strTok.nextToken();
      if( strTok.hasMoreTokens() ) {
        String token_followee = strTok.nextToken();
        links.add( new Link(token_follower,token_followee) );
      } else {
        //get the last line 
        lastLine = token_follower;
      }
    }
    if (lastLine.length() > 0) {
      //get the last line
      Link link = links.get(links.size()-1);
      lastLine = Link.getLinkeeVaule(link.follower) +" "+Link.getLinkeeVaule(link.follower);
      links.remove(links.size()-1);
    }
  }
  
  public static List<IndexFreq> topN(int N , List<IndexFreq> indexFreqs) {
    TreeSet<IndexFreq> topN = new TreeSet<IndexFreq>();
    
    int minScore = 1000;
    for(IndexFreq indexFreq :indexFreqs){
      if (indexFreq == null) continue;
      if(minScore > 990){//第一次运行
        minScore = indexFreq.times;
      }
      if(topN.size() < N){//首先填满topN
        topN.add(indexFreq);
        if(indexFreq.times < minScore){
          minScore = indexFreq.times;//更新最低分
        }
      }else if(indexFreq.times > minScore){
        topN.remove(topN.first());//先删除topN中的最低分
        topN.add(indexFreq);
        minScore = topN.first().times;//更新最低分
      }
    }
    
    return new ArrayList<IndexFreq>(topN);
  }
  
  private static class IndexFreq implements Comparable<IndexFreq>{
    public int index;
    
    public int times;
    
    public IndexFreq(int index , int times) {
      // TODO Auto-generated constructor stub
      this.index = index;
      this.times = times;
    }
    
    public boolean equals(IndexFreq indexFreq) {
      if (indexFreq.index == this.index && indexFreq.times == this.times) {
        return true;
      } else  {
        return false;
      }
    }

    @Override
    public int compareTo(IndexFreq o) {
      if( this.times < o.times ) return -1;
      if(this.times > o.times ) return 1;
      
      if (o.times == this.times && o.index == this.index) {
        return 0;
      }
      return 0;
      
    }
  }
  
  private static String getV(int index){
    return Link.getLinkeeVaule(index);
  }
}
