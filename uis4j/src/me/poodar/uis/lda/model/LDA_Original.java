package me.poodar.uis.lda.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.poodar.uis.lda.conf.LDAParameter;
import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.utils.Documents;
import me.poodar.uis.lda.utils.FileUtil;

public class LDA_Original {
  private Documents trainSet;
  public String saveIndicator = "";
  
  int [][] doc;//word index array
  int V, K, M;//vocabulary size, topic number, document number
  int [][] z;//topic label array
  float alpha; //doc-topic dirichlet prior parameter 
  float beta; //topic-word dirichlet prior parameter
  int [][] nmk;//given document m, count times of topic k. M*K
  int [][] nkt;//given topic k, count times of term t. K*V
  int [] nmkSum;//Sum for each row in nmk
  int [] nktSum;//Sum for each row in nkt
  double [][] phi;//Parameters for topic-word distribution K*V
  double [][] theta;//Parameters for doc-topic distribution M*K
  int iterations;//Times of iterations
  int saveStep;//The number of iterations between two saving
  int beginSaveIters;//Begin save model at this iteration
  
  public LDA_Original() {
    // TODO Auto-generated constructor stub
    alpha = LDAParameter.alpha;
    beta = LDAParameter.beta;
    iterations = LDAParameter.iterations;
    K = LDAParameter.K;
    saveStep = LDAParameter.saveStep;
    beginSaveIters = LDAParameter.beginSaveIters;
  }

  public void setSaveIndicator(String whichDoc) {
    saveIndicator = whichDoc;
  }
  
  public void initializeModel(Documents docSet) {
    // TODO Auto-generated method stub
    trainSet = docSet;
    
    M = docSet.docs.size();
    V = docSet.termToIndexMap.size();
    nmk = new int [M][K];
    nkt = new int[K][V];
    nmkSum = new int[M];
    nktSum = new int[K];
    phi = new double[K][V];
    theta = new double[M][K];
    
    //initialize documents index array
    doc = new int[M][];
    for(int m = 0; m < M; m++){
      //Notice the limit of memory
      int N = docSet.docs.get(m).docWords.length;
      doc[m] = new int[N];
      for(int n = 0; n < N; n++){
        doc[m][n] = docSet.docs.get(m).docWords[n];
      }
    }
    
    //initialize topic lable z for each word
    z = new int[M][];
    for(int m = 0; m < M; m++){
      int N = docSet.docs.get(m).docWords.length;
      z[m] = new int[N];
      for(int n = 0; n < N; n++){
        int initTopic = (int)(Math.random() * K);// From 0 to K - 1
        z[m][n] = initTopic;
        //number of words in doc m assigned to topic initTopic add 1
        nmk[m][initTopic]++;
        //number of terms doc[m][n] assigned to topic initTopic add 1
        nkt[initTopic][doc[m][n]]++;
        // total number of words assigned to topic initTopic add 1
        nktSum[initTopic]++;
      }
       // total number of words in document m is N
      nmkSum[m] = N;
    }
  }

  public void inferenceModel() throws IOException {
    // TODO Auto-generated method stub
//    if(iterations < saveStep + beginSaveIters){
//      System.err.println("Error: the number of iterations should be larger than " + (saveStep + beginSaveIters));
//      System.exit(0);
//    }
    for(int i = 0; i < iterations; i++){
      System.out.println("Iteration " + i);
      if((i >= beginSaveIters) && (((i - beginSaveIters) % saveStep) == 0)){
        //Saving the model
        System.out.println("Saving model at iteration " + i +" ... ");
        //Firstly update parameters
        updateEstimatedParameters();
        //Secondly print model variables
        saveIteratedModel(i, trainSet);
      }
      
      //Use Gibbs Sampling to update z[][]
      for(int m = 0; m < M; m++){
        int N = trainSet.docs.get(m).docWords.length;
        for(int n = 0; n < N; n++){
          // Sample from p(z_i|z_-i, w)
          int newTopic = sampleTopicZ(m, n);
          z[m][n] = newTopic;
        }
      }
    }
  }
  
  private void updateEstimatedParameters() {
    // TODO Auto-generated method stub
    for(int k = 0; k < K; k++){
      for(int t = 0; t < V; t++){
        phi[k][t] = (nkt[k][t] + beta) / (nktSum[k] + V * beta);
      }
    }
    
    for(int m = 0; m < M; m++){
      for(int k = 0; k < K; k++){
        theta[m][k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
      }
    }
  }

  private int sampleTopicZ(int m, int n) {
    // TODO Auto-generated method stub
    // Sample from p(z_i|z_-i, w) using Gibbs upde rule
    
    //Remove topic label for w_{m,n}
    int oldTopic = z[m][n];
    nmk[m][oldTopic]--;
    nkt[oldTopic][doc[m][n]]--;
    nmkSum[m]--;
    nktSum[oldTopic]--;
    
    //Compute p(z_i = k|z_-i, w)
    double [] p = new double[K];
    for(int k = 0; k < K; k++){
      p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
    }
    
    //Sample a new topic label for w_{m, n} like roulette
    //Compute cumulated probability for p
    for(int k = 1; k < K; k++){
      p[k] += p[k - 1];
    }
    double u = Math.random() * p[K - 1]; //p[] is unnormalised
    int newTopic;
    for(newTopic = 0; newTopic < K; newTopic++){
      if(u < p[newTopic]){
        break;
      }
    }
    
    //Add new topic label for w_{m, n}
    nmk[m][newTopic]++;
    nkt[newTopic][doc[m][n]]++;
    nmkSum[m]++;
    nktSum[newTopic]++;
    return newTopic;
  }

  public void saveIteratedModel(int iters, Documents docSet) throws IOException {
    // TODO Auto-generated method stub
    // lda.params lda.phi lda.theta lda.tassign lda.twords
    // lda.params
    String resPath = PathConfig.LdaResultsPath + saveIndicator + "/";
    String modelName = "lda_" + iters;
    ArrayList<String> lines = new ArrayList<String>();
    lines.add("alpha = " + alpha);
    lines.add("beta = " + beta);
    lines.add("topicNum = " + K);
    lines.add("docNum = " + M);
    lines.add("termNum = " + V);
    lines.add("iterations = " + iterations);
    lines.add("saveStep = " + saveStep);
    lines.add("beginSaveIters = " + beginSaveIters);
    FileUtil.writeLines(resPath + modelName + ".params", lines);

    // lda.theta M*K
    BufferedWriter writer = new BufferedWriter(new FileWriter(resPath
        + modelName + ".theta"));
    for (int i = 0; i < M; i++) {
      for (int j = 0; j < K; j++) {
        writer.write(theta[i][j] + "\t");
      }
      writer.write("\n");
    }
    writer.close();

    // lda.phi K*V
    writer = new BufferedWriter(new FileWriter(resPath + modelName + ".phi"));
    for (int i = 0; i < K; i++) {
      for (int j = 0; j < V; j++) {
        writer.write(phi[i][j] + "\t");
      }
      writer.write("\n");
    }
    writer.close();

    // lda.tassign
    writer = new BufferedWriter(
        new FileWriter(resPath + modelName + ".tassign"));
    for (int m = 0; m < M; m++) {
      for (int n = 0; n < doc[m].length; n++) {
        writer.write(doc[m][n] + ":" + z[m][n] + "\t");
      }
      writer.write("\n");
    }
    writer.close();

    // lda.twords phi[][] K*V
    writer = new BufferedWriter(new FileWriter(resPath + modelName + ".twords"));
    BufferedWriter writerHundred = new BufferedWriter(
        new FileWriter(resPath + modelName + "-100.twords"));
    BufferedWriter writerThousand = new BufferedWriter(
        new FileWriter(resPath + modelName + "-1000.twords"));
    
    int topNum = LDAParameter.topNum; // Find the top 20 topic words in each
                                      // topic
    int topHundred = 100;
    int topThousand = 1000;
    
    for (int i = 0; i < K; i++) {
      List<Integer> tWordsIndexArray = new ArrayList<Integer>();
      for (int j = 0; j < V; j++) {
        tWordsIndexArray.add(new Integer(j));
      }
      // Construct index of phi[i] sorted by its value
      // which means that index = tWordsIndexArray.get(0) , phi[i][index] is the
      // biggest value
      Collections.sort(tWordsIndexArray, new LDA_Original.TwordsComparable(phi[i]));
      writer.write("\n\n\n" + "topic " + i + "\t:\t Total words in this topic : " + nktSum[i] + "\t");
      writerHundred.write("\n\n\n" + "topic " + i + "\t:\t Total words in this topic : " + nktSum[i] + "\t");
      writerThousand.write("\n\n\n" + "topic " + i + "\t:\t Total words in this topic : " + nktSum[i] + "\t");
      
      for (int t = 0; t < topThousand; t++) {
        if(t < topNum) {
          if (t % 5 == 0)
            writer.write("\n\t");
          
          writer
              .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
                  + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) 
                  +" "+ nkt[i][tWordsIndexArray.get(t)] + "\t");
        }
        
        if(t < topHundred) {
           if (t % 5 == 0)
             writerHundred.write("\n\t");
           writerHundred
            .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
              + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) 
              +" "+ nkt[i][tWordsIndexArray.get(t)] + "\t");
        }
        
        if(t < topThousand) {
          if (t % 5 == 0)
            writerThousand.write("\n\t");
          writerThousand
           .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
             + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) 
             +" "+ nkt[i][tWordsIndexArray.get(t)] + "\t");
       }
      }
      writer.write("\n");
      writerHundred.write("\n");
      writerThousand.write("\n");
    }
    writer.close();
    writerHundred.close();
    writerThousand.close();

  }
  
  public class TwordsComparable implements Comparator<Integer> {
    
    public double [] sortProb; // Store probability of each word in topic k
    
    public TwordsComparable (double[] sortProb){
      this.sortProb = sortProb;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
      // TODO Auto-generated method stub
      //Sort topic word index according to the probability of each word in topic k
      if(sortProb[o1] > sortProb[o2]) return -1;
      else if(sortProb[o1] < sortProb[o2]) return 1;
      else return 0;
    }
  }
}

