package me.poodar.uis.lda.utils;

import java.util.ArrayList;
import java.util.Map;

public abstract class DocBase {
    public ArrayList<Document> docs; 
    public Map<String,Integer> termToIndexMap;
    public ArrayList<String> indexToTermMap;
    public Map<String,Integer> termCountMap;
    
    public static class Document {  
      private String docName;
      public int[] docWords;
    }

    public void readStructuredDocs(String ldaStructuredTrainFilePath,
        String delimiter) {
      // TODO Auto-generated method stub
      
    }

    public void readDocs(String ldaTrainSetPath) {
      // TODO Auto-generated method stub
      
    }
}
