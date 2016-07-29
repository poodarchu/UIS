package chosen.nlp.lda.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import chosen.nlp.lda.conf.PathConfig;

public class QuickFilter {
  Set<String> idSet = new HashSet<String>();

  public static void main(String args []) throws IOException {
    // path for save follower & ee doc
    QuickFilter qf = new QuickFilter();
    String path = PathConfig.twitterUserLinksFile;
    BufferedReader bufferedReader = new BufferedReader(new FileReader(
          new File(path)));
    int transferTimes = 0;
    String line ;
    while ((line = bufferedReader.readLine()) != null) {
      String[] tokens = line.split("\t");
      
      if(++transferTimes % 1000000 == 0 ) {
        System.out.println( (transferTimes/1000000) + " M lines has been loaded in");
      }
      qf.addToSet(tokens[0]);
    }
    bufferedReader.close();
    
    bufferedReader = new BufferedReader(new FileReader(
        new File(path)));
    BufferedWriter bw = new BufferedWriter(new FileWriter(
        new File(path + ".clean")));
    transferTimes = 0;
    while ((line = bufferedReader.readLine()) != null) {
      String[] tokens = line.split("\t");
      if(qf.inSet(tokens[1])) bw.write(line + "\n");
      if(++transferTimes % 1000000 == 0 ) {
        System.out.println( (transferTimes) + " M lines has been written");
      }
    }
    bw.close();
    bufferedReader.close();
  }
  
  public void addToSet (String s) {
    idSet.add(s);
  }
  public boolean inSet(String s){
    return idSet.contains(s);
  }
}
