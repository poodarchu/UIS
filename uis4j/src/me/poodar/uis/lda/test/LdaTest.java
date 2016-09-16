package me.poodar.uis.lda.test;

import java.io.IOException;
import java.util.Scanner;

import me.poodar.uis.lda.conf.LDAParameter;
import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.model.LDA_Community;
import me.poodar.uis.lda.utils.Documents;

public class LdaTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub
    //Documents DocsIn = new Documents ();
    //read files from path
    //DocsIn.readDocs(PathConfig.LdaTrainSetPath);
    //read structured file 
    Scanner scanner = new Scanner(System.in);
    System.out.println("How many Topics?");
    LDAParameter.K = scanner.nextInt();
    
    System.out.println("enter threshold:");
    LDA_Community.threshold = scanner.nextDouble();
    scanner.close();
    
    Documents documents = new Documents();
    if(args[0].equals("0")) {
      documents.readDocs(PathConfig.followeePath);
    } else if (args[0].equals("1")) {
      documents.readDocs(PathConfig.followerPath);
    } else {
      documents.readDocs(PathConfig.followerPath);
      documents.readDocs(PathConfig.followeePath);
    }
    
    LDA_Community lda = new LDA_Community();
    
    if(args[0].equals("0")) {
      lda.setSaveIndicator("followees");
    } else if (args[0].equals("1")) {
      lda.setSaveIndicator("followers");
    } else {
      lda.setSaveIndicator("all");
    }

    lda.initializeModel(documents);
    try {
      lda.inferenceModel();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
