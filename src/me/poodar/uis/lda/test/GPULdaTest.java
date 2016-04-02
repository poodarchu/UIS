package me.poodar.uis.lda.test;

import java.io.IOException;
import java.util.Scanner;

import me.poodar.uis.lda.conf.LDAParameter;
import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.model.LDA_GPU;
import me.poodar.uis.lda.model.LDA_Original;
import me.poodar.uis.lda.util.Documents;
import me.poodar.uis.social.lda.util.GeneralizedPolyaMetric;

public class GPULdaTest {

  /**
   * @param args
   */
  public static void main(String[] args) {

    //Documents DocsIn = new Documents ();
    //read files from path
    //DocsIn.readDocs(PathConfig.LdaTrainSetPath);
    //read structured file 
    Scanner scanner = new Scanner(System.in);
    System.out.println("How many Topics?");
    LDAParameter.K = scanner.nextInt();
    
    System.out.println("enter threshold:");
    LDA_GPU.threshold = scanner.nextDouble();
    
    GeneralizedPolyaMetric metric ;
    Documents documents = new Documents();
//    0 for folowee, 1 for folower, others for all
    if(args[0].equals("0")) {
      documents.readDocs(PathConfig.followeePath);
    } else if (args[0].equals("1")) {
      documents.readDocs(PathConfig.followerPath);
    } else {
      documents.readDocs(PathConfig.followerPath);
      documents.readDocs(PathConfig.followeePath);
    }
    //使用读入的documents，生成对应的metric
    metric= new GeneralizedPolyaMetric(documents);
    
    LDA_GPU lda = new LDA_GPU();
    
    //根据输入，使用从ExtractLatentLink中得到的latentFollower.txt得到相应的schema
    if(args[0].equals("0")) {
      lda.setSaveIndicator("followees");
      metric.getSchema(PathConfig.latentFollowerPath);
    } else if (args[0].equals("1")) {
      lda.setSaveIndicator("followers");
      metric.getSchema(PathConfig.latentFolloweePath);
    } else {
      lda.setSaveIndicator("all");
      metric.getSchema(0.4);
    }
    
    lda.setMetric(metric.matrix);
    lda.initializeModel(documents);
    
    
    try {
      lda.inferenceModel();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
