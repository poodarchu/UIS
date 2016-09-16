package me.poodar.uis.lda.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.utils.Link;
import me.poodar.uis.lda.utils.TwitterIDUtil;

public class ReadDocTest {
	
	public static void main(String args []) {
		
    // path for save follower & ee doc
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
          new File(PathConfig.twitterUserLinksFile)));
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
          readData = readData.replace(',', ' ');
          fileData.append(readData);
          if(++transferTimes % 100 == 0 )
            System.out.println( (++transferTimes) + " M block has been loaded in");
          if (++count >= 1000) {
            // 每读128次写一次文档  
            lastLine = "";
            writeToDocuments(fileData, lastLine);

            // append not read last line to file
            fileData = new StringBuffer();
            fileData.append(lastLine);
            count = 0;
            System.gc();
          }
        }
        //写入剩余部分 
        writeToDocuments(fileData, lastLine);
        
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

  public static void writeToDocuments(StringBuffer fileData, String lastLine) throws IOException {
    Map<Integer, ArrayList<Integer>> follower_Map; //每个人的index对应一个他的关注者index的List
    Map<Integer, ArrayList<Integer>> followee_Map; //每个人的index 对应着 关注他的人的index的 List
    ArrayList<Link> links = new ArrayList<Link>();
    //
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
    
    //输出文档 , 注意文档名为id ,实际内容也为id
    //文档分为两个文件夹 follower 和  followee
    shuffleListToDocuments(follower_Map, PathConfig.followerPath);
    follower_Map.clear();
    follower_Map = null;
    System.gc();
    
    listToDocuments(followee_Map, PathConfig.followeePath);
    followee_Map.clear();
    followee_Map = null;
    System.gc();
    //listToDocuments(follower_Map, followee_Map, PathConfig.allFollowPathString);
  }

  public static void shuffleListToDocuments(
      Map<Integer, ArrayList<Integer>> follower_Map, String path) throws IOException {
    
    BufferedWriter bufferedWriter = null;
    String testDataPath = PathConfig.testDataPath;
    BufferedWriter testDataBufferedWriter = new BufferedWriter(new FileWriter(testDataPath));
    BufferedWriter keepDataBufferedWriter = new BufferedWriter(new FileWriter(PathConfig.keepDataPath));
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
        testDataBufferedWriter.append(Link.getLinkeeVaule(followEntry.getKey()) + " " + Link.getLinkeeVaule(index) + "\n");
      }
      for (int i = crossOverRemark; i < shuffleList.size(); i++) {
        Integer index = shuffleList.get(i);
        bufferedWriter.append(Link.getLinkeeVaule(index) + " ");
        keepDataBufferedWriter.append(Link.getLinkeeVaule(followEntry.getKey()) + " " + Link.getLinkeeVaule(index) + "\n");
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
