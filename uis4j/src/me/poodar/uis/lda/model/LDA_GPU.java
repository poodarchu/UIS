package me.poodar.uis.lda.model;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import me.poodar.uis.lda.utils.IndexFreq;
import org.javatuples.Pair;

import me.poodar.uis.lda.conf.LDAParameter;
import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.utils.Documents;
import me.poodar.uis.lda.utils.FileUtil;
import me.poodar.uis.lda.utils.Documents.Document;
import me.poodar.uis.lda.utils.Community;
import me.poodar.uis.lda.utils.CommunityData;
import me.poodar.uis.lda.utils.GeneralizedPolyaMetric;
import me.poodar.uis.lda.utils.IndexFreq;

public class LDA_GPU {
  private Documents trainSet;
  public String saveIndicator = "";
  
  public IndexFreq[][] weighMatrix;
  public int [] weighSum ; 
 
  int [][] doc;//word index array
  int V, K, M;//vocabulary size, topic number, document number
  int [][] z;//topic label array
  double alpha; //doc-topic dirichlet prior parameter 
  double beta; //topic-word dirichlet prior parameter
  int [][] nmk;//given document m, count times of topic k. M*K
  int [][] nkt;//given topic k, count times of term t. K*V
  int [] nmkSum;//Sum for each row in nmk
  int [] nktSum;//Sum for each row in nkt
  double [][] phi;//Parameters for topic-word distribution K*V
  double [][] theta;//Parameters for doc-topic distribution M*K
  int iterations;//Times of iterations
  int saveStep;//The number of iterations between two saving
  int beginSaveIters;//Begin save model at this iteration
  
  private int maxFollower ;
  public static double threshold ;
  
  public static double getThreshold() {
    return threshold;
  }

  public static void setThreshold(double threshold) {
    LDA_GPU.threshold = threshold;
  }

  public void setMaxFollower(int value) {
    maxFollower = value;
  }
  
  public int getMaxFollower() {
    return maxFollower;
  }
  
  public LDA_GPU() {
    // TODO Auto-generated constructor stub
    alpha = LDAParameter.alpha;
    beta = 1.0 / V * 100;
    iterations = 1001;
    K = LDAParameter.K;
    saveStep = 100;
    beginSaveIters = 500;
  }
  
  public void setMetric(IndexFreq[][] matrix) {
    this.weighMatrix = matrix;
    this.weighSum = new int[matrix.length];
    for(int i = 0 ; i < matrix.length ;i ++) {
      for(int j = 0; j < matrix[i].length ; i++) {
        weighSum [i] += weighMatrix[i][j].times;
      }
    }
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
    
    beta = 1.0 / V * 100;
    alpha = 1.0 / K ;
    if(docSet.filesNumMark.size() == 2)
      setMaxFollower(docSet.filesNumMark.get(1));
    else setMaxFollower(M);
    
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
      nmkSum[m] = N;
      for(int n = 0; n < N; n++){
        
        int initTopic = (int)(Math.random() * K);// From 0 to K - 1
        z[m][n] = initTopic;
        //number of words in doc m assigned to topic initTopic add 1
        nmk[m][initTopic]++;
        //number of terms doc[m][n] assigned to topic initTopic add 1
        nkt[initTopic][doc[m][n]] += 100;
        int word = doc[m][n];
        
        for(int i = 0 ; i < weighMatrix[word].length ; i++) {
          nkt[initTopic][weighMatrix[word][i].index] += weighMatrix[word][i].times;
        }
        nktSum[initTopic] += weighSum[word];
        // total number of words assigned to topic initTopic add 1
        nktSum[initTopic] += 100;
      }
      
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
        //save the community docs 
        saveCommunity (i, trainSet);
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
    nkt[oldTopic][doc[m][n]] -= 100;
    nmkSum[m]--;
    nktSum[oldTopic] -= 100;
    int word = doc[m][n];
    for(int i = 0 ; i < weighMatrix[word].length ; i++) {
      nkt[oldTopic][weighMatrix[word][i].index] -= weighMatrix[word][i].times;
    }
    nktSum[oldTopic] -= weighSum[word];
    //Compute p(z_i = k|z_-i, w)
    double topicsSum = 0;
    double [] p = new double[K];
    for(int k = 0; k < K; k++){
      p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
      topicsSum += p[k];
    }
    
    //Sample a new topic label for w_{m, n} like roulette
    //Compute cumulated probability for p
    //
    int newTopic = 0;
    double topicCursor = (double) (Math.random() * topicsSum);
    for (int k = 0; k < K; k++) {
      topicCursor -= p[k];
      if (topicCursor <= 0) {
        newTopic = k;
        break;
      }
    }
    
    //Add new topic label for w_{m, n}
    nmk[m][newTopic]++;
    nkt[newTopic][doc[m][n]] += 100;
    nmkSum[m]++;
    nktSum[newTopic] += 100;
    for(int i = 0 ; i < weighMatrix[word].length ; i++) {
      nkt[newTopic][weighMatrix[word][i].index] += weighMatrix[word][i].times;
    }
    nktSum[newTopic] += weighSum[word];
    return newTopic;
  }
  
  private void saveCommunity(int iters , Documents docSet) throws FileNotFoundException, IOException {

    String resPath = PathConfig.LdaResultsPath + saveIndicator + "/";
    String modelNameString = "c_";

    Community community = getCommunity(docSet);
    CommunityData cData = new CommunityData(saveIndicator);
    
    List< List< Pair<Integer, Integer> > > CEdgeList = community.CEdgeList;
    //arrange the list according to its original value
    
    for (int k = 0; k < CEdgeList.size(); k++) {
      List<Pair<Integer, Integer>> CEdges = CEdgeList.get(k);
      BufferedWriter writer = new BufferedWriter(new FileWriter(resPath
          + modelNameString + k));
      for (Pair<Integer, Integer> CEdge : CEdges) {
        Integer value1 = CEdge.getValue0();
        Integer value2 = CEdge.getValue1();
        writer.write(value1 + " " + value2 + " \n");
      }
      writer.close();
    }

    for (int k = 0; k < community.CF.size(); k++) {
      List<Integer> CFs = community.CF.get(k);
      List<Integer> rankCFs = new ArrayList<Integer>();
      
      BufferedWriter writer = new BufferedWriter(new FileWriter(resPath
          + modelNameString + "F" + k));
      for (Integer follower : CFs) {
        
//        try{
//          int i = Integer.parseInt(value1);
//        }catch(NumberFormatException ex){ // handle your exception
//          System.out.println(value1);
//        }
        rankCFs.add(follower);
        writer.write(follower + " \n");
      }
      Collections.sort(rankCFs);
      cData.CF.add(rankCFs);
      writer.close();
    }

    for (int k = 0; k < community.CG.size(); k++) {
      List<Integer> CGs = community.CG.get(k);
      List<Integer> rankCGs = new ArrayList<Integer>();
      BufferedWriter writer = new BufferedWriter(new FileWriter(resPath
          + modelNameString + "G" + k));
      for (Integer followee : CGs) {
        rankCGs.add(followee);
        writer.write(followee + " \n");
      }
      Collections.sort(rankCGs);
      cData.CG.add(rankCGs);
      writer.close();
    }

    
    cData.phi = phi;
    cData.theta = theta;
    cData.tIDUtil.IDToIndexMap = docSet.termToIndexMap; 
    cData.tIDUtil.indexToIDMap = docSet.indexToTermMap;
    for (int i = 0; i < cData.CF.size(); i++) {
      if (cData.CF.get(i) == null || cData.CG.get(i) == null)
        continue;
      Collections.sort(cData.CF.get(i));
      Collections.sort(cData.CG.get(i));
    }
    //map document index to m'exact index in doc theta'
    Map<Integer, Integer> indexList = cData.docIndexMap;
    
    int index = 0;
    for(Document d : docSet.docs) {
      indexList.put(Integer.parseInt(d.docName), index ++);
    }
    
    cData.write(cData);
  }


  public Community getCommunity(Documents docSet) {
    double thresholdForER = threshold ;
    double thresholdForEE = 1.0 / 1000;
    List<Integer> CF; // followers for community
    List<Integer> CG; // followees for community 
    
    Community community = new Community();
    
    List<Integer> docIndex;
    List< List< Pair<Integer, Integer> > > CEdgeList = 
        new ArrayList< List <Pair<Integer,Integer> > >(); //edge for community
    
    for(int k = 0 ; k < K ; k ++) {
      CF = new ArrayList<Integer>();
      CG = new ArrayList<Integer>();
      docIndex = new ArrayList<Integer>();
      
      List< Pair<Integer, Integer>> CEdges = new ArrayList<Pair<Integer,Integer>>();
      for(int m = 0 ; m < maxFollower ; m++) {
        //**** 需要对documents 排序
        String docName = docSet.docs.get(m).docName;
        int follower = Integer.parseInt(docName);
        if (theta[m][k] > thresholdForER && docSet.docs.get(m).docWords.length > 1) {
          CF.add(follower);
          docIndex.add(m);
        }
        
        if (!this.saveIndicator.equals("all") && theta[m][k] > thresholdForER)
          CG.add(follower); // follower is the same as followee
        
      }
      
      for (int m = maxFollower; m < M; m++) {
        String docName = docSet.docs.get(m).docName;
        int followee = Integer.parseInt(docName);
        if (theta[m][k] > thresholdForER && docSet.docs.get(m).docWords.length > 1) {
          CG.add(followee);
          docIndex.add(m);
        }
      }
      
      
      Collections.sort(CG);
      
      for(int j = 0; j < CF.size(); j++) {
        //bug here...
        int mInteger = CF.get(j);
        int dindex = docIndex.get(j);
        int N = trainSet.docs.get(dindex).docWords.length;
        for(int v= 0 ; v < N ; v++) {
          String ee = docSet.indexToTermMap.get(doc[dindex][v]);
          int eeInt = Integer.parseInt(ee);
          int ee_flag = Collections.binarySearch(CG,eeInt);
          if(ee_flag >= 0 ) { 
            CEdges.add(new Pair<Integer, Integer>(mInteger, eeInt)) ;
          }
        }
      }
      Collections.sort(CF);
      CEdgeList.add(CEdges);
      community.CF.add(CF);
      community.CG.add(CG);
    }

    community.CEdgeList = CEdgeList;
    return community;
  }

  public void saveIteratedModel(int iters, Documents docSet) throws IOException {
    // TODO Auto-generated method stub
    // lda.params lda.phi lda.theta lda.tassign lda.twords
    // lda.params
    String resPath = PathConfig.GPUResultPath + saveIndicator + "/";
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
      writer.write(i + "    ");
      for (int j = 0; j < K; j++) {
        writer.write(j+":"+String.format("%.8f",theta[i][j]) + " ");
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
      Collections.sort(tWordsIndexArray, new LDA_GPU.TwordsComparable(phi[i]));
      writer.write("\n\n\n" + "topic " + i + "\t:\t Total words in this topic : " + nktSum[i] + "\t");
      writerHundred.write("\n\n\n" + "topic " + i + "\t:\t Total words in this topic : " + nktSum[i] + "\t");
      writerThousand.write("\n\n\n" + "topic " + i + "\t:\t Total words in this topic : " + nktSum[i] + "\t");
      
      for (int t = 0; t < topThousand; t++) {
        if (t >= V ) break;
        
        if(t < topNum) {
          if (t % 5 == 0)
            writer.write("\n\t");
          
          writer
              .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
                  + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) 
                  +" "+ nkt[i][tWordsIndexArray.get(t)] / 100 + "\t");
        }
        
        if(t < topHundred) {
           if (t % 5 == 0)
             writerHundred.write("\n\t");
           writerHundred
            .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
              + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) 
              +" "+ nkt[i][tWordsIndexArray.get(t)] / 100+ "\t");
        }
        
        if(t < topThousand) {
          if (t % 5 == 0)
            writerThousand.write("\n\t");
          writerThousand
           .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
             + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) 
             +" "+ nkt[i][tWordsIndexArray.get(t)] / 100 + "\t");
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

