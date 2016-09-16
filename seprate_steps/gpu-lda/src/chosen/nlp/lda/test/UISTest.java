package chosen.nlp.lda.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.model.UIS_LDA;
import chosen.nlp.lda.model.UIS_LDA_Seperated;
import chosen.nlp.lda.util.Documents;
import chosen.social.lda.util.GeneralizedPolyaMetric;

public class UISTest {
  public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, IOException {
    // TODO Auto-generated method stub
    //Documents DocsIn = new Documents ();
    //read files from path
    //DocsIn.readDocs(PathConfig.LdaTrainSetPath);
    //read structured file 
    Scanner scanner = new Scanner(System.in);
    System.out.println("How many Topics?");
    LDAParameter.K = scanner.nextInt();
    
    System.out.println("How many Interest Topic?");
    int division = scanner.nextInt();
    
    System.out.println("enter threshold:");
    Double threshold = scanner.nextDouble();
    
    System.out.println("enter weigh:");
    Double weigh = scanner.nextDouble();
    
    scanner.close();
    
    if(args.length > 1 && args[1].equals("read") ) {
      String aS = args[0];
      aS = PathConfig.serialPath + "UIS." + aS;
      UIS_LDA lda = UIS_LDA_Seperated.read(aS);
      lda.threshold = threshold;
      lda.saveCommunity();
    } else {
      try {
        UIS_LDA_Seperated lda = train(args, division, threshold, weigh);
        lda.inferenceModel();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  public static UIS_LDA_Seperated train(String[] args,int division , double threshold, double weigh) {
    GeneralizedPolyaMetric metric ;
    Documents documents = new Documents();
    if(args[0].equals("0")) {
      documents.readDocs(PathConfig.followeePath);
    } else if (args[0].equals("1")) {
      documents.readDocs(PathConfig.followerPath);
    } else {
      documents.readDocs(PathConfig.followerPath);
      documents.readDocs(PathConfig.followeePath);
    }
    metric= new GeneralizedPolyaMetric(documents);
    
    UIS_LDA_Seperated lda = new UIS_LDA_Seperated(division);
    String aS = args[0];
    aS = PathConfig.serialPath + "UIS." + aS;
    lda.threshold = threshold;
    lda.serialPath = aS;
    
    if(args[0].equals("0")) {
      lda.setSaveIndicator("followees");
      metric.getSchema(PathConfig.latentFollowerPath);
    } else if (args[0].equals("1")) {
      lda.setSaveIndicator("followers");
      metric.getSchema(PathConfig.latentFolloweePath);
    } else {
      lda.setSaveIndicator("all");
      metric.getSchema(weigh);
    }
    
    lda.setMetric( metric.matrix);
    lda.initializeModel(documents);
    return lda;
  }
}
