package chosen.nlp.lda.model.variables;

import java.io.Serializable;

public class UnionInterestSocialLDA_Variables implements Serializable {
  private static final long serialVersionUID = 2648328676821019686L;
  public final static int S_up = 1;
  public final static int S_down = 0;
  
  public int division ; //topic number less than division is assumed to be interest topic
  
  public int switcher[][]; //switcher like z[][] in LDA
  
  public int nms[][];
  public int nmsSum[];
  public double delta [];
  public double delataSum = 1;
  
  public UnionInterestSocialLDA_Variables(int division) {
    this.division = division;
  }

  public void initParam(int docNum) {
    int switcherNum = 2; 
    nms = new int [docNum][];
    nmsSum = new int [docNum];
    for(int i = 0 ; i < docNum ; i++) {
      nms[i] = new int [switcherNum];
    }
    delta = new double[2];
    delta[0] = 0.5;
    delta[1] = 1 - delta[0];
  }
  
  public void initializeSwitcher(int M) {
    switcher = new int [M][];
  }
  
}
