package me.poodar.uis.lda.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Documents extends DocBase implements Doc , Serializable{
  private static final long serialVersionUID = -735384947188934217L;
  public ArrayList<Document> docs; 
	public Map<String,Integer> termToIndexMap;
	public ArrayList<String> indexToTermMap;
	public Map<String,Integer> termCountMap;
	
	public List<Integer> filesNumMark;
	
	public Documents(){
		docs = new ArrayList<Document>();
		termToIndexMap = new HashMap<String, Integer>();
		indexToTermMap = new ArrayList<String>();
		termCountMap = new HashMap<String, Integer>();
		filesNumMark = new ArrayList<Integer>();
	} 
	
	public Integer getIndex (String key) {
	  return termToIndexMap.get(key);
	}
	
	public Integer getWordSize () {
	  return indexToTermMap.size();
	}
	
	public Boolean contains(String key){
	  return termToIndexMap.containsKey(key);
	}
	

	@Override
  public void readDocs(String docsPath){
	  filesNumMark.add(docs.size());  //append the specified element to the end of the list、将docs的文件数目加入filesNumMark做标记
		for(File docFile : new File(docsPath).listFiles()) { //an array of File objects is returned, one for each file or directory in the directory. 
		  //get files form docsPath using listFiles
			Document doc = new Document(
			    docFile.getAbsolutePath(), 
			    termToIndexMap, 
			    indexToTermMap, 
			    termCountMap,
			    docFile.getName());   //Returns: The name of the file or directory denoted by this abstract pathname,
			//get specific file using getAbsolutePath
			docs.add(doc);
		}
	}
	
	/* (non-Javadoc)
   * @see chosen.nlp.lda.util.Doc#readStructuredDocs(java.lang.String, java.lang.String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public void readStructuredDocs(String docsPath ,String delimiter) {
	  //读取结构化文本
	  //下一步要做的是 结构化document 初始化
	  try {
      BufferedReader structuredReviewsReader = new BufferedReader(new FileReader(new File(docsPath)));
      StringBuffer fileData = new StringBuffer();
      int bufferSize = 1024;
      char [] buffer = new char[bufferSize];
      int numRead = 0;
      
      try {
        while( (numRead = structuredReviewsReader.read(buffer)) != -1) {
          String readData = String.valueOf(buffer,0,numRead);
          fileData.append(readData);
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      structuredReviewsReader.close();
      
      //将读取进来的fileData先用delimiter分割，在将其组织成String的List
      String [] reviewArray = fileData.toString().split(delimiter);   
      List<String> reviewList = new ArrayList<String>();
      Collections.addAll(reviewList, reviewArray);
      
      //将上一步组织成List的review遍历，每个List组织成一个Document对象，加入docs
      for(int i = 0;i < reviewList.size();i++) {
        String review = reviewList.get(i);
        //replace punctuation
        //review = review.replaceAll("[^a-zA-Z ]", " ");
        Document doc = new Document(
            "doc_" + i,
            review, 
            termToIndexMap, 
            indexToTermMap, 
            termCountMap);
        docs.add(doc);
      }
      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
	  
	}
	
	public static class Document implements Serializable {	
		/**
     * 
     */
		private static final long serialVersionUID = -7174806303706851314L;
		public String docName;
		public Integer[] docWords;
		
		public Document ( String docName, String review, Map<String, Integer> termToIndexMap, ArrayList<String> indexToTermMap, 
        Map<String, Integer> termCountMap) {
				//使用string list 初始化
				this.docName = docName;
				//Read file and initialize word index array
				ArrayList<String> docLines = new ArrayList<String>();
				FileUtil.readLinesFromString(review, docLines);   //read lines of review into docLines
      
				mapLineWords(termToIndexMap, indexToTermMap, termCountMap, docLines);
		}
		
		public Document(String docName, 
		    Map<String, Integer> termToIndexMap, 
		    ArrayList<String> indexToTermMap, 
		    Map<String, Integer> termCountMap,
		    String fileName){
		  
			
			//Read file and initialize word index array
			ArrayList<String> docLines = new ArrayList<String>();
			FileUtil.readLines(docName, docLines); 
      
			//add filename into termMap
			fileName = fileName.replace(".txt", "");
      
			this.docName = fileName;
      
			mapToIndexMap(termToIndexMap, indexToTermMap, termCountMap, fileName);
      
			mapLineWords(termToIndexMap, indexToTermMap, termCountMap, docLines);
		}

		
	//将docLines中的字符串分割为单个的word，再存入termToIndexMap, indexToTermMap 和 termCountMap	
    private void mapLineWords(	Map<String, Integer> termToIndexMap, ArrayList<String> indexToTermMap, 
    							Map<String, Integer> termCountMap,   ArrayList<String> docLines)
    {
      
    	ArrayList<String> words = new ArrayList<String>();
    	for(String line : docLines){  //先将docLines中的字符串分割为一个个token，然后存入words列表（List）中
    		FileUtil.tokenizeAndLowerCase(line, words);
    	}
      
    	//Remove stop words and noise words
    	for(int i = 0; i < words.size(); i++){
    		//Stopwords  can test whether a given string is a stop word, lowercase all before the test
    		if(Stopwords.isStopword(words.get(i))){
    			words.remove(i);
    			i--;
    		}
    	}
      
    	//Transfer word to index
    	this.docWords = new Integer[words.size()];
    	for(int i = 0; i < words.size(); i++){
    		String word = words.get(i);
    		if(!termToIndexMap.containsKey(word)){
    			int newIndex = termToIndexMap.size();               //映射表的大小为新的索引值
    			termToIndexMap.put(word, newIndex);                 //先对word进行哈希,值为新索引值
    			indexToTermMap.add(word);                           //存词入链表,index为新索引值
    			termCountMap.put(word, new Integer(1));             //初始化词计数值
    			docWords[i] = newIndex;                             //docWords按词序保存索引值
    		} else {
    			docWords[i] = termToIndexMap.get(word);
    			termCountMap.put(word, termCountMap.get(word) + 1); //词计数值 自增1
    		}
    	}
    	words.clear();
    }

    private void mapToIndexMap( Map<String, Integer> termToIndexMap, ArrayList<String> indexToTermMap, 
    							Map<String, Integer> termCountMap,   String word) {
      if(!termToIndexMap.containsKey(word)){
        int newIndex = termToIndexMap.size();               //映射表的大小为新的索引值
        termToIndexMap.put(word, newIndex);                 //先对word进行哈希,值为新索引值
        indexToTermMap.add(word);                           //存词入链表,index为新索引值
        termCountMap.put(word, new Integer(1));             //初始化词计数值                            //docWords按词序保存索引值
      } else {
        termCountMap.put(word, termCountMap.get(word) + 1); //词计数值 自增1
      }
    }
		
    //判断是否是noise word, 比如网址的前缀后缀等
		public boolean isNoiseWord(String string) {
			// TODO Auto-generated method stub
			string = string.toLowerCase().trim();   //remove the string without any leading or trailing whitespace
			Pattern MY_PATTERN = Pattern.compile(".*[a-zA-Z]+.*");
			Matcher m = MY_PATTERN.matcher(string);
			// filter @xxx and URL
			if(string.matches(".*www\\..*") || string.matches(".*\\.com.*") || 
					string.matches(".*http:.*") || Stopwords.isStopword(string))
				return true;
			if (!m.matches()) {
				return true;
			} else
				return false;
		}
		
	}
}
