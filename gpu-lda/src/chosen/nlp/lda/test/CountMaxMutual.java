package chosen.nlp.lda.test;

import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.util.Documents;
import chosen.social.lda.util.GeneralizedPolyaMetric;

public class CountMaxMutual {

  /**
   * @param args
   */
  public static void main(String[] args) {
    // TODO Auto-generated method stub
    Documents documents = new Documents();
    documents.readDocs(PathConfig.followerPath);
    documents.readDocs(PathConfig.followeePath);
    GeneralizedPolyaMetric gpm = new GeneralizedPolyaMetric(documents);
    System.out.println(gpm.getMax());
  }
  
}
