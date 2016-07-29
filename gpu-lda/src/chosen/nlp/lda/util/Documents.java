package chosen.nlp.lda.util;

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

//Doc is an interface, DocBase is the parent class
public class Documents extends DocBase implements Doc , Serializable{
	private static final long serialVersionUID = -735384947188934217L;
	
	public ArrayList<Document> docs; 
	public Map<String,Integer> termToIndexMap;
	public ArrayList<String> indexToTermMap;
	public Map<String,Integer> termCountMap;
	
	public List<Integer> filesNumMark;
	
	//constructor
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
		//return the number of elements in the list.(actually it's the follower's followeeList's lenth)
		return indexToTermMap.size();
	}
	
	public Boolean contains(String key){
		return termToIndexMap.containsKey(key);
	}
	
	//@override indicates that a method declaration is intend to override a method declaration in the superType.
	@Override
	public void readDocs(String docsPath){
		filesNumMark.add(docs.size());
		for(File docFile : new File(docsPath).listFiles()) {
			//get files form docsPath using listFiles
			Document doc = new Document(docFile.getAbsolutePath(), termToIndexMap, indexToTermMap, termCountMap, docFile.getName());
			//get specific file using getAbsolutePath
			docs.add(doc);
		}
	}

	@Override
	//Indicates that the named compiler warnings should be suppressed in the annotated element 
	//(and in all program elements contained in the annotated element).
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
			String [] reviewArray = fileData.toString().split(delimiter);
			List<String> reviewList = new ArrayList<String>();
			Collections.addAll(reviewList, reviewArray);
      
			for(int i = 0;i < reviewList.size();i++) {
				String review = reviewList.get(i);
				//replace punctuation
				//review = review.replaceAll("[^a-zA-Z ]", " ");
				Document doc = new Document("doc_" + i,review, termToIndexMap, indexToTermMap, termCountMap);
				docs.add(doc);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
public static class Document implements Serializable {	
	private static final long serialVersionUID = -7174806303706851314L;
    public String docName;
	public Integer[] docWords;
		
	public 
	Document (
	    String docName,
	    String review,
	    Map<String, Integer> termToIndexMap, 
        ArrayList<String> indexToTermMap, 
        Map<String, Integer> termCountMap) 
	{	
		//使用string list 初始化
		this.docName = docName;
		//Read file and initialize word index array
		ArrayList<String> docLines = new ArrayList<String>();
		FileUtil.readLinesFromString(review, docLines); 
      
		mapLineWords(termToIndexMap, indexToTermMap, termCountMap, docLines);
	}
		
	public 
	Document(String docName, 
			Map<String, Integer> termToIndexMap, 
		    ArrayList<String> indexToTermMap, 
		    Map<String, Integer> termCountMap,
		    String fileName)
	{
		//Read file and initialize word index array
		ArrayList<String> docLines = new ArrayList<String>();
		FileUtil.readLines(docName, docLines); 
      
		//add filename into termMap
		fileName = fileName.replace(".txt", "");
      
		this.docName = fileName;
      
		mapToIndexMap(termToIndexMap, indexToTermMap, termCountMap, fileName);  
		mapLineWords(termToIndexMap, indexToTermMap, termCountMap, docLines);
	}

	private void 
	mapLineWords(Map<String, Integer> termToIndexMap,
									ArrayList<String> indexToTermMap, 
									Map<String, Integer> termCountMap,
									ArrayList<String> docLines) 
	{  
		ArrayList<String> words = new ArrayList<String>();
		for(String line : docLines){
			FileUtil.tokenizeAndLowerCase(line, words);
		}
      
		//Remove stop words and noise words
		for(int i = 0; i < words.size(); i++){
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

	private void 
	mapToIndexMap(Map<String, Integer> termToIndexMap,
								ArrayList<String> indexToTermMap, 
								Map<String, Integer> termCountMap,
								String word) 
	{
		if(!termToIndexMap.containsKey(word)){
			int newIndex = termToIndexMap.size();               //映射表的大小为新的索引值
			termToIndexMap.put(word, newIndex);                 //先对word进行哈希,值为新索引值
			indexToTermMap.add(word);                           //存词入链表,index为新索引值
			termCountMap.put(word, new Integer(1));             //初始化词计数值                            //docWords按词序保存索引值
		} else {
			termCountMap.put(word, termCountMap.get(word) + 1); //词计数值 自增1
		}
    }
		
	public boolean 
	isNoiseWord(String string) 
	{
		// TODO Auto-generated method stub
		string = string.toLowerCase().trim();
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
