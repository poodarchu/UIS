package chosen.social.lda.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.util.Doc;
import chosen.nlp.lda.util.Documents;

public class GeneralizedPolyaMetric {
  
  public static Doc doc;
  
  public IndexFreq[][] matrix;
  
  public GeneralizedPolyaMetric(Documents documents) {
    doc = documents;
  }
  
  public int getMax() {
    String mutualData = readData(PathConfig.mutualLinkPath);
    IndexFreq[][] mutual = new IndexFreq [doc.getWordSize()][];
    toMatrix(mutualData, mutual , 0.3);
    int j = 0;
    for(int i = 0 ; i < mutual.length ; i ++) {
      if(mutual[i].length > j) j = mutual[i].length;
    }
    return j;
  }
  
  public void getSchema(Double weigh){
    String mutualData = readData(PathConfig.mutualLinkPath);
    IndexFreq[][] mutual = new IndexFreq [doc.getWordSize()][];
    toMatrix(mutualData, mutual ,  weigh);
    matrix = new IndexFreq[mutual.length][];
    
    /*
    String latentIndexData = readData(PathConfig.latentFollowerPath);
    IndexFreq[][] FollowerFreqs = new IndexFreq [doc.getWordSize()][];
    toMatrix(latentIndexData, FollowerFreqs);
    
    latentIndexData = readData(PathConfig.latentFollowerPath);
    IndexFreq[][] FolloweeFreqs = new IndexFreq [doc.getWordSize()][];
    toMatrix(latentIndexData, FolloweeFreqs);
    */
    for(int i = 0 ; i < mutual.length ; i++) {
      for(int j = 0 ; j < mutual[i].length ; j++) {
        int swapIndex = (int) (Math.random() * mutual[i].length);
        IndexFreq a = mutual[i][j];
        mutual[i][j] = mutual[i][swapIndex];
        mutual[i][swapIndex] = a;
      }
      int size = mutual[i].length > 40 ? 40 : mutual[i].length;
      matrix[i] = new IndexFreq[size];
      for(int j = 0 ; j< size ; j ++ ) matrix[i][j] = mutual[i][j];
      /*
      int size = mutual[i].length > 15 ? 15 : mutual[i].length;
      matrix[i] = new IndexFreq[size + FolloweeFreqs[i].length + FollowerFreqs[i].length];
      for(int j = 0 ; j< size ; j ++ ) matrix[i][j] = mutual[i][j];
      for(int j = size ; j < size + FolloweeFreqs[i].length ; j++) matrix[i][j] = FolloweeFreqs[i][j-size];
      for(int j = size + FolloweeFreqs[i].length ; j < matrix[i].length ; j++) matrix[i][j] =
          FollowerFreqs[i][j - size - FolloweeFreqs[i].length];
    */
    }
    for(int i = 0 ; i < matrix.length ; i++) {
      if(matrix[i] == null) matrix[i] = new IndexFreq[0];
    }
  }
  
  public void getSchema(String path) {
    //read doc
    //add it to map
    String mutualData = readData(PathConfig.mutualLinkPath);
    IndexFreq[][] mutual = new IndexFreq [doc.getWordSize()][];
    toMatrix(mutualData, mutual ,  0.1);
    
    String latentIndexData = readData(path);
    IndexFreq[][] latentFreqs = new IndexFreq [doc.getWordSize()][];
    toMatrix(latentIndexData, latentFreqs);
    
    // merge two matrix
    matrix = new IndexFreq[doc.getWordSize()][];
    for(int i = 0 ; i < doc.getWordSize() ; i ++) {
      List<IndexFreq> freqs = new ArrayList<IndexFreq>(); 
      
      for(int j = 0 ; j < mutual[i].length ; j++) {
        int swapIndex = (int) (Math.random() * mutual[i].length);
        IndexFreq a = mutual[i][j];
        mutual[i][j] = mutual[i][swapIndex];
        mutual[i][swapIndex] = a;
      }
      for(int j = 0; j < mutual[i].length ; j ++) {
        if(mutual[i][j] != null) {
          if (j > 15) break;
          freqs.add(mutual[i][j]);
        } else {
          break;
        }
      }
      
      if(latentFreqs[i] != null) {
        freqs.addAll(Arrays.asList(latentFreqs[i]));
        freqs.remove(null);
      }
      matrix[i] = freqs.toArray(new IndexFreq[freqs.size()]);
    }
  }

  public void toMatrix(String data, IndexFreq[][] matrix ,Double weigh) {
    
    String[] mutualDataLine = data.split("#");
    for (String line : mutualDataLine) {
      int index ;
      //String[] tokens;
      if (line != null) {
        StringTokenizer strTok = new StringTokenizer(line);
        if (strTok.hasMoreTokens() ) {
          String next = strTok.nextToken();
          if(doc.contains(next)) {
            index = getIndex(next);
          }
          else {
            continue;
          }
        } else {
          continue;
        }
        int t = strTok.countTokens();
        matrix[index] = new IndexFreq[t];
        int sIndex = 0;
        while (strTok.hasMoreTokens()) {
          String token = strTok.nextToken();
          if(doc.contains(token))
            matrix[index][sIndex ++ ] = new IndexFreq(doc.getIndex(token),(int) (weigh * 100)); 
        }
      }
    }
  }
  
  public static void toMatrix(String data, IndexFreq[][] matrix) {
    String[] mutualDataLine = data.split("#");
    for (String line : mutualDataLine) {
      int index ;
      //String[] tokens;
      if (line != null) {
        StringTokenizer strTok = new StringTokenizer(line);
        if (strTok.hasMoreTokens()) {
          index = getIndex(strTok.nextToken());
        } else {
          continue;
        }
        int t = 0;
        matrix[index] = new IndexFreq[strTok.countTokens() > 10 ? 10 : strTok.countTokens()];
        int sIndex = 0;
        while (strTok.hasMoreTokens()) {
          if(t++ > 9) break;
          String token = strTok.nextToken();
          matrix[index][sIndex ++ ] = new IndexFreq(token,doc); 
        }
      }
    }
  }

  public static String readData(String path) {
    try {
      BufferedReader bReader = new BufferedReader(new FileReader ( new File(path) ) ) ;
      StringBuffer fileData = new StringBuffer();
      int bufferSize = 1024 * 1024;
      char[] buffer = new char[bufferSize];
      int numRead = 0;
      int transferTimes = 0;
      try {
        while ((numRead = bReader.read(buffer)) != -1) {
          String readData = String.valueOf(buffer, 0, numRead);
          fileData.append(readData);
          if(++transferTimes % 100 == 0 )
            System.out.println( (++transferTimes) + " M block has been loaded in");
        }
      }catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      bReader.close();
      
      String data = fileData.toString();
      
      return data;
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return null; 
    }
    
  }
  
  
  public static int getIndex(String key) {
    if (doc.contains(key)) {
      return doc.getIndex(key);
    }
    return 0;
  }
}
