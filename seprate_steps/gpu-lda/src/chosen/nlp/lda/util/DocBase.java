package chosen.nlp.lda.util;

import java.util.ArrayList;
import java.util.Map;

public abstract class DocBase {
	//DocBase contains:
	//1. docName & content
	//2. termToIndexMap: Map<String, Integer>  ID->index
	//3. indexToTermMap: index->ID
	//4. termCountMap:Map<String, Integer> ID->count
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
      
    }

    public void readDocs(String ldaTrainSetPath) {      
    }
}
