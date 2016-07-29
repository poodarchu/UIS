package chosen.nlp.lda.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chosen.nlp.lda.util.Documents.Document;

public class DocSentence extends DocBase implements Doc{
  public ArrayList<Document> docs; 
  public Map<String,Integer> termToIndexMap;
  public ArrayList<String> indexToTermMap;
  public Map<String,Integer> termCountMap;
  
  //map for word to doc sent index pair 
  public Map<String, List<MSIPair>> termToMSIPairMap;
  
  public HighFreWords highFreWords = new HighFreWords();
  public Aspects aspects = new Aspects();
  //新加入sentence
  
  public DocSentence() {
    docs = new ArrayList<Document>();
    termToIndexMap = new HashMap<String, Integer>();
    indexToTermMap = new ArrayList<String>();
    termCountMap = new HashMap<String, Integer>();
    
    termToMSIPairMap = new HashMap<String, List<MSIPair>>();
  } 
  
  public void readDocs(String docsPath) {
    for(File docFile : new File(docsPath).listFiles()) {
      //get files form docsPath using listFiles
      Document doc = new Document(docFile.getAbsolutePath(), 
          termToIndexMap, 
          indexToTermMap, 
          termCountMap);
      //get specific file using getAbsolutePath
      docs.add(doc);
    }
  }
  
  public Integer getIndex (String key) {
    return termToIndexMap.get(key);
  }
  
  public Integer getWordSize () {
    return indexToTermMap.size();
  }
  
  @SuppressWarnings("unchecked")
  public void readStructuredDocs(String docsPath ,String delimiter) {
    //读取结构化文本
    //下一步要做的是 结构化document 初始化
    try {
      BufferedReader structuredReviewsReader = new BufferedReader(
          new FileReader(new File(docsPath)));
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
        //replace unrelevent punctuation
        review = review.replaceAll("[^a-zA-Z.!? ]", " ");
        Document doc = new Document(
            i,
            review, 
            highFreWords,
            aspects,
            termToIndexMap, 
            indexToTermMap, 
            termCountMap,
            termToMSIPairMap);
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
  
  public void reduceWordInTermCount(
      String word) {
    
    termCountMap.put(word, termCountMap.get(word) - 1); //词计数值 自减1
  }
  
  public static class Document {  
    private String docName;
    private int doc_index;
    public int[] docWords;
    public int[][] docSentencesWords;
    public int lines;
    public List<Map<Integer,Integer>> termInSentenceCountMap = 
        new ArrayList<Map<Integer ,Integer>>();   //
    
    public Map<String, List<SIPair>> inverseSIPairMap = new HashMap<String, List<SIPair>>();
    
    public Map< String,List<SIPair> > highFredDSPair;
    //map words in highFred to DSPair <sentence,index>
    public Map< String,List<SIPair> > seedDSPair;
    
    public Map< Integer,List<SIPair> > seedDSPairLineMap;
    //
    public List<SIPair> indexMapList = new ArrayList<SIPair>();
    //
    public Map<String, Integer> SIPairIndexMap = new HashMap<String, Integer>();
    
    //public Map<String, Integer> ;
    
    public HighFreWords highFreWords;
    public Aspects aspects;
    
    public Document (
        int docName,String review,
        HighFreWords highFreWords,Aspects aspects,
        Map<String, Integer> termToIndexMap, 
        ArrayList<String> indexToTermMap, 
        Map<String, Integer> termCountMap, 
        Map<String , List<MSIPair>> termToMSIPairMap) {
          //使用string list 初始化
          this.doc_index = docName;
          this.highFreWords = highFreWords;
          this.aspects = aspects;
          //Read file and initialize word index array
          ArrayList<String> docLines = new ArrayList<String>();
          
          initDSPairMap();
          
          FileUtil.readLinesFromString(review, docLines); 
          
          mapLineWords( termToIndexMap, indexToTermMap, termCountMap, docLines, termToMSIPairMap );
          //label the words -> 
          //*** find high-fre words and print
          //update termToIndexMap , indexToTermMap , termCountMap .
          //
          //labelHighFreWord(termToIndexMap, indexToTermMap, termCountMap);
    }
    
    public Document(String docName, 
        Map<String, Integer> termToIndexMap, 
        ArrayList<String> indexToTermMap, 
        Map<String, Integer> termCountMap ) {
      
        this.docName = docName;
          //Read file and initialize word index array
          ArrayList<String> docLines = new ArrayList<String>();
          FileUtil.readLines(docName, docLines); 
          
          //mapLineWords(termToIndexMap, indexToTermMap, termCountMap, docLines);
    }

    private void initDSPairMap() {
      //
      highFredDSPair = new HashMap<String,List<SIPair>>();
      seedDSPair = new HashMap<String,List<SIPair>>();
      seedDSPairLineMap = new HashMap<Integer ,List<SIPair>>();
      for(String highFred : highFreWords.wordsList) {
        List<SIPair> highFredPosList = new ArrayList<SIPair>();
        highFredDSPair.put(highFred, highFredPosList);
      }
      Collection < List<String> > seedwordCollect = Aspects.aspToSeedList.values();
      for(List<String> seedwordList : seedwordCollect) {
        for(String word : seedwordList ) {
          List seedPosList = new ArrayList<SIPair>();
          seedDSPair.put(word, seedPosList);
        }
      }
    }
    
    private void mapLineWords(
        Map<String, Integer> termToIndexMap,
        ArrayList<String> indexToTermMap, 
        Map<String, Integer> termCountMap,
        ArrayList<String> docLines,
        Map<String, List<MSIPair>> termToMSIPairMap) {
      
      ArrayList<String> words ;
      //将内容写入多维数组 sentence to words
      ArrayList< ArrayList<String> > sentenceWords = new ArrayList<ArrayList<String>>();
      Stemmer stemmer = new Stemmer();
      for(String line : docLines) {
        String [] sentences = line.split("[.!?][.!? ]+");
        for(String sentence : sentences) {
          words = new ArrayList<String>();
          FileUtil.sentenceTokenizedStemmedFipped
            (sentence, words, stemmer);
          if(words != null)
            sentenceWords.add(words);
        }
      }
      //写入sentences
      
      int docWordSize = 0 ;
      
      //去停词
      for(int i = 0 ; i < sentenceWords.size() ; i++) {
        words = sentenceWords.get(i);
       /* for(int j = 0; j < words.size(); j++){
          if(Stopwords.isStopword(words.get(j)) || isNoiseWord(words.get(j))){
            words.remove(j);
            j--;
          }
          if(words.get(j).equals("not")) {
            words.remove(j);
            words.set(j, "not_"+words.get(j));
          }
        }*/
        docWordSize += words.size();
      }
      
      //Transfer word to index
      this.docWords = new int[docWordSize];
      docSentencesWords = new int [sentenceWords.size()][];
      int docSentenceIndex = 0;
      int docWordsIndex = 0;
      lines = sentenceWords.size();
      
      for (int sentenceIndex = 0; sentenceIndex < lines; sentenceIndex++) {
        words = sentenceWords.get(sentenceIndex);
        docSentencesWords[sentenceIndex] = new int [words.size()];
        for (int wordIndexInSentence = 0; wordIndexInSentence < words.size(); wordIndexInSentence++) {
          docWordsIndex = docSentenceIndex + wordIndexInSentence;
          indexMapList.add(new SIPair(sentenceIndex,wordIndexInSentence));
          
          SIPairIndexMap.put( ( new SIPair( sentenceIndex,wordIndexInSentence ) ).toString() , docWordsIndex );
          String word = words.get( wordIndexInSentence );
          addToMap(
              termToIndexMap, indexToTermMap, 
              termCountMap, termToMSIPairMap, 
              docWordsIndex, sentenceIndex, wordIndexInSentence, word);
        }
        
        docSentenceIndex += words.size();
        
        Map <Integer,Integer> termSenCountMap = new HashMap<Integer,Integer>();
        for(int j = 0 ; j < words.size() ; j++) {
          int wordIndex = docSentencesWords[sentenceIndex][j];
          if(!termSenCountMap.containsKey(new Integer(wordIndex))) {
            termSenCountMap.put(wordIndex,new Integer(1));
          } else {
            termSenCountMap.put(wordIndex, termSenCountMap.get(wordIndex) + 1);
          }
        }
        termInSentenceCountMap.add(termSenCountMap);
        words.clear();
      }
      sentenceWords.clear();
    }

    public void addToMap(
      Map<String, Integer> termToIndexMap , ArrayList<String> indexToTermMap ,
      Map<String, Integer> termCountMap , Map<String, List<MSIPair>> termToMSIPairMap ,
      int docWordsIndex , int sentenceIndex , int wordIndexInSentence , String word) {
      
      addWordToMap(
          termToIndexMap, indexToTermMap, 
          termCountMap , docWordsIndex ,
          sentenceIndex , wordIndexInSentence , word );            
      
      //build inverse index map
      addToInverMap(termToMSIPairMap, sentenceIndex, wordIndexInSentence, word);
      
      // if words in HighFreWords or SeedWords appear 
      // add them to map,mark thier position ,set their parameter to xxxx
      addPosToMap(sentenceIndex, wordIndexInSentence, word);
    }
    
    //当词变形时,索引没有更新
    public void transformedToMap(
        Map<String, Integer> termToIndexMap , ArrayList<String> indexToTermMap ,
        Map<String, Integer> termCountMap , Map<String, List<MSIPair>> termToMSIPairMap ,
        int docWordsIndex , int sentenceIndex , int wordIndexInSentence , String word) {
        
        addWordToMap(
            termToIndexMap, indexToTermMap, 
            termCountMap , docWordsIndex ,
            sentenceIndex , wordIndexInSentence , word );
      }

    public void addToInverMap(
      Map<String, List<MSIPair>> termToMSIPairMap,
      int sentenceIndex, int wordIndexInSentence, String word) {
      //
      List<MSIPair> inverseList = termToMSIPairMap.get(word);
      if(inverseList == null) {
        inverseList = new ArrayList<MSIPair>();
      }
      inverseList.add(new MSIPair(doc_index,sentenceIndex,wordIndexInSentence));
      termToMSIPairMap .put(word, inverseList);
      List<SIPair> inverSIPairList = inverseSIPairMap.get(word);
      if (inverSIPairList == null) {
        inverSIPairList = new ArrayList<SIPair>();
      }
      inverSIPairList.add(new SIPair(sentenceIndex,wordIndexInSentence));
      inverseSIPairMap.put(word,inverSIPairList);
    }

    public void addWordToMap(Map<String, Integer> termToIndexMap,
        ArrayList<String> indexToTermMap, 
        Map<String, Integer> termCountMap,
        int docWordsIndex, int sentenceIndex, 
        int wordIndexInSentence, String word) {
//      if(termToIndexMap.size() > 2308)
//        System.out.println("***"+word);
      if (!termToIndexMap.containsKey(word)) {
        int newIndex = termToIndexMap.size();   //映射表的大小为新的索引值
        termToIndexMap.put(word, newIndex);     //先对word进行哈希,值为新索引值
        indexToTermMap.add(word);               //存词入链表,index为新索引值
        termCountMap.put(word, new Integer(1)); //初始化词计数值
        docWords[docWordsIndex] = newIndex;     //docWords按词序保存索引值
        docSentencesWords[sentenceIndex][wordIndexInSentence] = newIndex;     //sentenceWords 中的索引
      } else {
        docWords[docWordsIndex] = termToIndexMap.get(word);
        docSentencesWords[sentenceIndex][wordIndexInSentence] = termToIndexMap.get(word);
        termCountMap.put(word, termCountMap.get(word) + 1); //词计数值 自增1
      }
    }
    
    public void addPosToMap(int sentenceIndex, int wordIndexInSentence, String word) {
      if(HighFreWords.isHighFre(word)) {
        List<SIPair> highFrePos = highFredDSPair.get(word);
        if (highFrePos == null) {
          highFrePos = new ArrayList<SIPair>();
        }
        highFrePos.add( new SIPair(sentenceIndex,wordIndexInSentence) );
        highFredDSPair.put(word,highFrePos);
      } else if(Aspects.isSeed(word)) {
        List<SIPair> seedPos = seedDSPair.get(word);
        if(seedPos == null) {
          seedPos = new ArrayList<SIPair>();
        }
        seedPos.add(new SIPair(sentenceIndex,wordIndexInSentence));
        seedDSPair.put(word, seedPos);
        List<SIPair> seedDSPairInSameLineList = seedDSPairLineMap.get(sentenceIndex);
        if (seedDSPairInSameLineList == null) {
          seedDSPairInSameLineList = new ArrayList<SIPair>();
        }
        seedDSPairInSameLineList.add(new SIPair(sentenceIndex, wordIndexInSentence));
        seedDSPairLineMap.put(sentenceIndex, seedDSPairInSameLineList);
      }
    }
    
    public boolean isNoiseWord(String string) {
      // TODO Auto-generated method stub
      string = string.toLowerCase().trim();
      Pattern MY_PATTERN = Pattern.compile(".*[a-zA-Z]+.*");
      Matcher m = MY_PATTERN.matcher(string);
      // filter @xxx and URL
      if(string.matches(".*www\\..*") || 
          string.matches(".*\\.com.*") || 
          string.matches(".*http:.*"))
        return true;
      if (!m.matches()) {
        return true;
      } else
        return false;
    }
    

    @SuppressWarnings("unused")
    private void labelHighFreWord(
        Map<String, Integer> termToIndexMap, 
        ArrayList<String> indexToTermMap, 
        Map<String, Integer> termCountMap ) {
        //for every seed word in sentences
        //label its highFred if there are any ... behind
      for(String highFreWord : highFreWords.wordsList) {
        List <SIPair>highFreDSList = highFredDSPair.get(highFreWord);
        if(highFreDSList == null || highFreDSList.size() == 0)
          continue;
        for(SIPair si : highFreDSList) {
          List<SIPair> seedDSListInSameLine = seedDSPairLineMap.get(si.sent);
          SIPair nearestPair = SIPair.getNearest(si, seedDSListInSameLine);
          if(nearestPair != null) {
            int seedwordIndex = docSentencesWords[nearestPair.sent][nearestPair.index];
            String seedword = indexToTermMap.get(seedwordIndex);
            String aspect = aspects.seedToAspect.get(seedword);
            String highFreWordLabel = highFreWord + "_" + aspect;
            //update map and docs
            int highFreIndex = docSentencesWords[si.sent][si.index];
            Integer highFreTimes = termCountMap.get(highFreWord);
            termCountMap.put(highFreWord,highFreTimes-1);
            int docWordIndex = SIPairIndexMap.get(si.toString());
            addWordToMap(termToIndexMap, indexToTermMap, termCountMap, 
                docWordIndex, si.sent, si.index, highFreWordLabel);
          }
        }
      }
      return;
    }
  }

  @Override
  public Boolean contains(String key) {
    // TODO Auto-generated method stub
    return null;
  }

}
