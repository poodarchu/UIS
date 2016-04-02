package me.poodar.uis.lda.util;

import java.util.ArrayList;
import java.util.Map;

public abstract class DocBase {
    public ArrayList<Document> docs;   //文档集合，这里的docs就是communities， 其中每一篇doc其实就是一个community
    public Map<String,Integer> termToIndexMap;   //
    public ArrayList<String> indexToTermMap;
    public Map<String,Integer> termCountMap;
    
    public static class Document {  
      private String docName;
      public int[] docWords;
    }

    public void readStructuredDocs(String ldaStructuredTrainFilePath,
        String delimiter) {
      
    }

    public void readDocs(String ldaTrainSetPath) {
      
    }
}
