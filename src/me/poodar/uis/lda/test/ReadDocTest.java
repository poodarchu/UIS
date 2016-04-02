package me.poodar.uis.lda.test;

import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.social.lda.util.Link;

import java.io.*;
import java.util.*;

public class ReadDocTest {
	
	public static void main(String args []) {
		
    // path for save follower & ee doc
    try {
      BufferedReader bufferedReader = new BufferedReader (
    		  new FileReader(
    				  new File(PathConfig.twitterUserLinksFile)
    						)
    		  );
      StringBuffer fileData = new StringBuffer(); //要输出到文件中的整个file
      int bufferSize = 1024 * 1024;      		//每次的bufferSize大小为1MB
      char[] buffer = new char[bufferSize];		//长度为1M的buffer
      int numRead = 0;                          //一次读取的字节数目（1MB的字节流中）
      int count = 0;                            //读取的次数
      int transferTimes = 0;        
      String lastLine = "";                     //在文件的末尾添加空行
      try {
        while ((numRead = bufferedReader.read(buffer)) != -1) {  //read（） returns the num of characters read once
          String readData = String.valueOf(buffer, 0, numRead);
          readData = readData.replace(',', ' ');                 //raplace , with blankspace
          fileData.append(readData);							 //将该次read的data追加到fileData
          if(++transferTimes % 100 == 0 )
            System.out.println( (++transferTimes) + " M block has been loaded in");  //每100M 
          if (++count >= 1000) {
            // 每读128次写一次文档  
            lastLine = "";
            writeToDocuments(fileData, lastLine);     //写入文件

            // append not read last line to file
            fileData = new StringBuffer();           //清空fileData
            fileData.append(lastLine);
            count = 0;
            System.gc();             //garbage collector
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
    Map<Integer, ArrayList<Integer>> follower_Map; //每个follower 对应一个ArrayList类型的followee_list,数字表示每个人的index
    Map<Integer, ArrayList<Integer>> followee_Map; //每个followee 对应一个ArrayList类型的follower_list
    ArrayList<Link> links = new ArrayList<Link>(); //每个Link对象表示一个follower-followee关系
    //
    
    //先将string分割成一个又一个字符，在将其映射到Link的集合links中
    tokenizedToLinkAndgetLastLine(fileData.toString(), links, ',' ,lastLine);  //将fileData中的内容（String形式）转换为Link类型的对象那个，存到links中，并添加lastline
    
    follower_Map = new HashMap <Integer , ArrayList<Integer> > ();  //follower_map是非线程安全和可null的
    followee_Map = new HashMap <Integer , ArrayList<Integer> > ();
    int times = 0 ;
    for(Link twitterLink : links) {   //遍历links中的每个Link对象
      if(++times % 100000 == 0)
        System.out.println(times + "links ");
      //follower粉丝            followee 关注对象 
      addToLinkListMap(follower_Map, twitterLink.follower, twitterLink.followee);  //对follower_map进行遍历，找到links中每个link.follower, 将其对应的followee添加到map中对每个follower对应的ArrayList中
      addToLinkListMap(followee_Map, twitterLink.followee, twitterLink.follower);
    }
    
    links.clear();
    links = null;
    System.gc();
    
    //输出文档 , 注意文档名为id ,实际内容也为id
    //文档分为两个文件夹 follower 和  followee
    //使用follower-followerList，将其分为两部分，keepData和testData
    shuffleListToDocuments(follower_Map, PathConfig.followerPath); //将上一步已经处理好的Map输出到文件中，随机输出到followerPath
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
    
    BufferedWriter bufferedWriter = null;  //Writes text to a character-output stream, buffering characters so as to provide for the efficient writing of single characters, arrays, and strings.
//The buffer size may be specified, or the default size may be accepted. The default is large enough for most purposes.


    String testDataPath = PathConfig.testDataPath;   //导出到用作测试集的一部分数据 约占10%
    BufferedWriter testDataBufferedWriter = new BufferedWriter(new FileWriter(testDataPath));  //使用testPath创建Writer
    BufferedWriter keepDataBufferedWriter = new BufferedWriter(new FileWriter(PathConfig.keepDataPath));
    int times = 0 ;
    
    //A map entry (key-value pair). 
    //The Map.entrySet method returns a collection-view of the map, whose elements are of this class. 
    //The only way to obtain a reference to a map entry is from the iterator of this collection-view. 
    //These Map.Entry objects are valid only for the duration of the iteration; 
    //more formally, the behavior of a map entry is undefined if the backing map has been modified after 
    //the entry was returned by the iterator, except through the setValue operation on the map entry
    for(Map.Entry<Integer, ArrayList<Integer> > followEntry : follower_Map.entrySet()) {
    	//遍历follower_map的key-value视图，即follower-followeeList视图
    	
      if(++times % 10000 == 0) {
        System.out.println(times + "links ok.");
      }
      
      String filePath = path + Link.getLinkeeVaule(followEntry.getKey()) + ".txt";  //使用follower的id作为文本名
      bufferedWriter =new BufferedWriter(new FileWriter(
          filePath, true) );
      //shuffle , mark down split point ,save to a different doc.
      //随机， 标记分割点，保存到不同的doc中
      ArrayList<Integer> shuffleList = followEntry.getValue();
      
      //打破list的元素之间的顺序，随机化
      Collections.shuffle(shuffleList);	//Randomly permutes the specified list using a default source of randomness. 
      									//All permutations occur with approximately equal likelihood.
      
      
      int crossOverRemark = shuffleList.size() / 10 ;   //取shuffleList的十分之一，将其内容写到testData中。
      for(int i = 0 ; i < crossOverRemark ; i++) {
        Integer index = shuffleList.get(i);
        testDataBufferedWriter.append(Link.getLinkeeVaule(followEntry.getKey()) + " " + Link.getLinkeeVaule(index) + "\n");
      }
      for (int i = crossOverRemark; i < shuffleList.size(); i++) {  //将shuffleList中剩余的部分写入到keekpData中
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
    for(Map.Entry<Integer, ArrayList<Integer> > followEntry : follower_Map.entrySet()) { //遍历follower_map中的maping
      if(++times % 10000 == 0) {
        System.out.println(times + "links ok.");
      }
      
      try {
        String filePath = path + Link.getLinkeeVaule(followEntry.getKey()) + ".txt";
        bufferedWriter =new BufferedWriter(new FileWriter(
            filePath, true) );
        
        HashSet<Integer> unionSet = new HashSet<Integer>();
        unionSet.addAll(followEntry.getValue());   //先将followEntry中的所有value（此处指的是followeelist）加入到unionSet中
        if(followee_Map.containsKey(followEntry.getKey())){
          ArrayList<Integer> followeeList= followee_Map.get(followEntry.getKey());//使用follower_map的key获取followee_map中对应的follower_list
          
          //Adds all of the elements in the specified collection to this collection (optional operation). 
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
      Map<Integer, ArrayList<Integer>> follow_Map, int executor , int target) {  //executor即key， target即value
    ArrayList<Integer> followList;
    if (!follow_Map.containsKey(executor) ) {
      followList = new ArrayList<Integer>();
      followList.add(target);
      follow_Map.put(executor, followList);  //向map中添加新的follower-followeeList
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
    
	  	//StringTokenizer break string to tokens
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
