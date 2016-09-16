package me.poodar.uis.lda.conf;

public class LDAParameter {
  public static int K = 15; //topic number
  public static int topNum = 20;
  public static float alpha = (float) 0.01; //doc-topic dirichlet prior parameter 
  public static float beta = (float) 0.01;//topic-word dirichlet prior parameter
  public static int iterations = 201;//Times of iterations
  public static int saveStep = 100 ;//The number of iterations between two saving
  public static int beginSaveIters = 100;//Begin save model at this iteration
  
  public static double seedParameter = 0.6;
  
  public LDAParameter() {
    // TODO Auto-generated constructor stub
  }
  
  public void getParameter(String parameterPath) {
    //get parameter from file
  }
}
