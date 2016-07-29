package chosen.nlp.lda.test;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import chosen.nlp.lda.conf.PathConfig;
import chosen.social.lda.util.IDUtil;
import chosen.social.lda.util.Link;
import chosen.social.lda.util.TwitterIDUtil;

public class BigFileFilter {
	public static double ration = 0;
	public static void main(String args []) throws ClassNotFoundException {
		
    // path for save follower & ee doc
    try {
      Scanner scanner = new Scanner(System.in);
      System.out.println("split Files into ");
      ration = scanner.nextDouble();
      String path;
      if(args[0].equals("0"))
        path = PathConfig.keepDataPath + ".100";
      else {
        path = PathConfig.twitterUserLinksFile;
      }
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
          new File(path)));
      StringBuffer fileData = new StringBuffer();
      int bufferSize = 1024 * 1024;
      char[] buffer = new char[bufferSize];
      int numRead = 0;
      int count = 0;
      int transferTimes = 0;
      String lastLine = "";
      try {
        while ((numRead = bufferedReader.read(buffer)) != -1) {
          String readData = String.valueOf(buffer, 0, numRead);
          //readData = readData.replace('\t', ' ');
          fileData.append(readData);
          if(++transferTimes % 100 == 0 )
            System.out.println( (++transferTimes) + " M block has been loaded in");
          if (++count >= 1000) {
            // 每读128次写一次文档  
            lastLine = "";
            writeToDocuments(fileData, lastLine , args[0]);

            // append not read last line to file
            fileData = new StringBuffer();
            fileData.append(lastLine);
            count = 0;
            System.gc();
          }
        }
        //写入剩余部分 
        writeToDocuments(fileData, lastLine, args[0]);
        
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      bufferedReader.close();

    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public static void writeToDocuments(StringBuffer fileData, String lastLine , String arg) throws IOException, ClassNotFoundException {
    Map<Integer, ArrayList<Integer>> follower_Map; //每个人的index对应一个他的关注者index的List
    Map<Integer, ArrayList<Integer>> followee_Map; //每个人的index 对应着 关注他的人的index的 List
    ArrayList<Link> links = new ArrayList<Link>();
    //
    if (arg.equals("0")) {
      IDUtil idUtil = new IDUtil();
      idUtil = idUtil.read();
      TwitterIDUtil.IDToIndexMap = idUtil.IDToIndexMap;
      TwitterIDUtil.indexToIDMap = idUtil.indexToIDMap;
    }
    
    tokenizedToLinkAndgetLastLine(fileData.toString(), links, ',' ,lastLine);
    
    follower_Map = new HashMap <Integer , ArrayList<Integer> > ();
    followee_Map = new HashMap <Integer , ArrayList<Integer> > ();
    int times = 0 ;
    for(Link twitterLink : links) {
      if(++times % 100000 == 0)
        System.out.println(times + "links ");
      //follower粉丝            followee 关注对象 
      addToLinkListMap(follower_Map, twitterLink.follower, twitterLink.followee);
      addToLinkListMap(followee_Map, twitterLink.followee, twitterLink.follower);
    }
    
    links.clear();
    links = null;
    System.gc();
    if (! arg.equals("0")) {
      //int limit = 200;
      Iterator<Entry<Integer, ArrayList<Integer>>> it = follower_Map.entrySet()
          .iterator();
      while (it.hasNext()) {
        Entry<Integer, ArrayList<Integer>> followerEntry = it.next();
        int key = followerEntry.getKey();
        double cursor = Math.random() * ration ;
        if (cursor < ration - 1) { //followeeSize < limit || followerSize < limit
          it.remove();
          followee_Map.remove(key);
        }
      }
      Iterator<Entry<Integer,ArrayList<Integer>>> eeIterator = followee_Map.entrySet().
          iterator();
      //remove those followees that are not followers
      while(eeIterator.hasNext()) {
        Entry<Integer, ArrayList<Integer>> followeeEntry = eeIterator.next();
        int key = followeeEntry.getKey();
        if(!follower_Map.containsKey(key)) {
          eeIterator.remove();
        }
      }
      
      recursiveRemove(10, follower_Map, followee_Map);
      
      clearExistDocs(PathConfig.followerPath,PathConfig.followeePath);
      
      // 输出文档 , 注意文档名为id ,实际内容也为id
      // 文档分为两个文件夹 follower 和 followee
      ShuffleListToDocuments(follower_Map, followee_Map,
          PathConfig.followerPath);
      follower_Map.clear();
      follower_Map = null;
      System.gc();
      IDUtil idUtil = new IDUtil();
      idUtil.IDToIndexMap = TwitterIDUtil.IDToIndexMap;
      idUtil.indexToIDMap = TwitterIDUtil.indexToIDMap;
      idUtil.write();
      
    } else {
      listToDocuments(followee_Map, PathConfig.followeePath);
      followee_Map.clear();
      followee_Map = null;
      System.gc();
    }
    //listToDocuments(follower_Map, followee_Map, PathConfig.allFollowPathString);
  }

  private static void recursiveRemove (int limit , Map<Integer, ArrayList<Integer>> follower_Map , Map<Integer, ArrayList<Integer>> followee_Map) {
    boolean flag = true; 
    int followerSizeMap[] = new int[TwitterIDUtil.getSize()];
    int followeeSizeMap[] = new int [TwitterIDUtil.getSize()];
    
    Iterator<Entry<Integer, ArrayList<Integer>>> it = follower_Map.entrySet()
        .iterator();
    while (it.hasNext()) {
      Entry<Integer, ArrayList<Integer>> followerEntry = it.next();
      int key = followerEntry.getKey();
      int followeeSize = 0;
      if (followee_Map.containsKey(key)) {
        followeeSize = followee_Map.get(key).size();
        followeeSizeMap[key] = followeeSize;
      }
      int followerSize = followerEntry.getValue().size();
      followerSizeMap[key] = followerSize;
    }
    while(flag == true) {
      flag = false;
      Iterator<Entry<Integer, ArrayList<Integer>>> it_rm = follower_Map.entrySet()
          .iterator();
      while (it_rm.hasNext()) {
        Entry<Integer, ArrayList<Integer>> followerEntry = it_rm.next();
        int key = followerEntry.getKey();
        int followeeSize = followeeSizeMap[key];
        int followerSize = followerSizeMap[key];
        if ( followerSize < limit || followeeSize < limit ) { //followeeSize < limit || followerSize < limit
          it_rm.remove();
          followeeSizeMap[key] = 0;
          followerSizeMap[key] = 0;
          if(followee_Map.containsKey(key)) 
            for(int erId : followee_Map.get(key)) {
              followerSizeMap[erId] --;
            }
          followee_Map.remove(key);
          flag = true;
        }
      }
    }
  }
  
  private static void clearExistDocs(String followerPath, String followeePath) {
    // TODO Auto-generated method stub
    File followerdir = new File(followerPath);
    File followeedir = new File (followeePath);
    for(File file: followerdir.listFiles()) file.delete();
    for(File file: followeedir.listFiles()) file.delete();
  }

  public static void ShuffleListToDocuments(
      Map<Integer, ArrayList<Integer>> follower_Map, 
      Map<Integer, ArrayList<Integer>> followee_Map,
      String path) throws IOException {
    
    BufferedWriter bufferedWriter = null;
    String testDataPath = PathConfig.testDataPath + ".100";
    BufferedWriter testDataBufferedWriter = new BufferedWriter(new FileWriter(testDataPath));
    BufferedWriter keepDataBufferedWriter = new BufferedWriter(new FileWriter(PathConfig.keepDataPath + ".100"));
    int times = 0 ;
    for(Map.Entry<Integer, ArrayList<Integer> > followEntry : follower_Map.entrySet()) {
      if(++times % 10000 == 0) {
        System.out.println(times + "links ok.");
      }
      
      String filePath = path + Link.getLinkeeVaule(followEntry.getKey()) + ".txt";
      bufferedWriter =new BufferedWriter(new FileWriter(
          filePath, true) );
      //shuffle , make down split point ,save to a different doc.
      ArrayList<Integer> shuffleList = followEntry.getValue();
      Collections.shuffle(shuffleList);
      int crossOverRemark = shuffleList.size() / 10 ;
      for(int i = 0 ; i < crossOverRemark ; i++) {
        Integer index = shuffleList.get(i);
        if(followee_Map.containsKey(index))
          testDataBufferedWriter.append(Link.getLinkeeVaule(followEntry.getKey()) + 
            " " + Link.getLinkeeVaule(index) + "\n");
      }
      for (int i = crossOverRemark; i < shuffleList.size(); i++) {
        Integer index = shuffleList.get(i);
        if(followee_Map.containsKey(index)) {
        bufferedWriter.append(Link.getLinkeeVaule(index) + " ");
        keepDataBufferedWriter.append(Link.getLinkeeVaule(followEntry.getKey()) + 
            " " + Link.getLinkeeVaule(index) + "\n");
        }
      }
      bufferedWriter.close();
    }
    testDataBufferedWriter.close();
    keepDataBufferedWriter.close();
  }

  public static void listToDocuments(
      Map<Integer, ArrayList<Integer>> followee_Map, String path) throws IOException {
    
    BufferedWriter bufferedWriter = null;
    int times = 0 ;
    for(Map.Entry<Integer, ArrayList<Integer> > followEntry : followee_Map.entrySet()) {
      if(++times % 10000 == 0) {
        System.out.println(times + "links ok.");
      }
      
      String filePath = path + Link.getLinkeeVaule(followEntry.getKey()) + ".txt";
      bufferedWriter =new BufferedWriter(new FileWriter(
          filePath, true) );
      //shuffle , make down split point ,save to a different doc.
      ArrayList<Integer> followValueList = followEntry.getValue();
     
      for (int i = 0 ; i < followValueList.size(); i++) {
        Integer index = followValueList.get(i);
        bufferedWriter.append(Link.getLinkeeVaule(index) + " ");
      }
      bufferedWriter.close();
    }
  }
  
  public static void listToDocuments(
      Map<Integer, ArrayList<Integer>> follower_Map, 
      Map<Integer, ArrayList<Integer>> followee_Map,String path) {
    
    BufferedWriter bufferedWriter = null;
    int times = 0 ;
    for(Map.Entry<Integer, ArrayList<Integer> > followEntry : follower_Map.entrySet()) {
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
          ArrayList<Integer> followeeList= followee_Map.get(followEntry.getKey());
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
      Map<Integer, ArrayList<Integer>> follow_Map, int executor , int target) {
    ArrayList<Integer> followList;
    if (!follow_Map.containsKey(executor) ) {
      followList = new ArrayList<Integer>();
      followList.add(target);
      follow_Map.put(executor, followList);
    } else {
      followList = follow_Map.get(executor);
      followList.add(target);
    }
  }
	
/*
	private static void splitFile() {
		try {
			BufferedReader bufferedReader = new BufferedReader( 
					new FileReader( new File( PathConfig.twitterUserLinksFile ) ) );
			StringBuffer fileData = new StringBuffer();
			int bufferSize = 1024 * 1024;
			char[] buffer = new char[bufferSize];
			int numRead = 0;
			int counter = 0;
			
			try {
		        while( (numRead = bufferedReader.read(buffer)) != -1) {
		          String readData = String.valueOf(buffer,0,numRead);
		          fileData.append(readData);
		          if(++ counter > 256) break;
		        }
		      } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		      }
			
			bufferedReader.close();
			
			BufferedWriter bufferedWriter = new BufferedWriter(
					new FileWriter( PathConfig.userLinksReveiwFile ) );
			bufferedWriter.write(fileData.toString());
			bufferedWriter.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/
  public static void tokenizedToLinkAndgetLastLine(String dataString,
      ArrayList<Link> links,char spliter, String lastLine) {
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
}
