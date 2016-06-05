package me.poodar.uis.lda.model;

import me.poodar.uis.lda.conf.LDAParameter;
import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.model.variables.UnionInterestSocialLDA_Variables;
import me.poodar.uis.lda.util.Documents;
import me.poodar.uis.lda.util.Documents.Document;
import me.poodar.uis.lda.util.FileUtil;
import me.poodar.uis.social.lda.util.Community;
import me.poodar.uis.social.lda.util.CommunityData;
import me.poodar.uis.social.lda.util.IndexFreq;
import org.javatuples.Pair;

import java.io.*;
import java.util.*;

public class UIS_LDA_Seperated extends UIS_LDA implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -1679600151879194809L;

  UnionInterestSocialLDA_Variables uisVariables ;
  
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
  
  int [] nmk_I_Sum;//Sum for each row in nmk
  int [] nmk_S_Sum;
  
  int [] nktSum;//Sum for each row in nkt
  double [][] phi;//Parameters for topic-word distribution K*V
  double [][] theta;//Parameters for doc-topic distribution M*K
  int iterations;//Times of iterations
  int saveStep;//The number of iterations between two saving
  int beginSaveIters;//Begin save model at this iteration
  
  static int S_up = UnionInterestSocialLDA_Variables.S_up;
  static int S_down = UnionInterestSocialLDA_Variables.S_down;
  int follower_doc_map[];
  public String serialPath ;
  int I ;
  
  Community community ;
  private int numFollowee;
  public  double threshold ;
  
  public  double getThreshold() {
    return threshold;
  }

  public void setThreshold(double threshold) {
    this.threshold = threshold;
  }

  public void setNumFollowee(int value) {
    numFollowee = value;
  }
  
  public int getNumFollowee() {
    return numFollowee;
  }
  
  
  public UIS_LDA_Seperated(int topicDivision) {
    super(topicDivision);
    // TODO Auto-generated constructor stub
    alpha = LDAParameter.alpha;
    beta = 1.0 / V * 100;
    iterations = LDAParameter.iterations;
    K = LDAParameter.K;
    saveStep = LDAParameter.saveStep;
    beginSaveIters = LDAParameter.beginSaveIters;
    
    uisVariables = new UnionInterestSocialLDA_Variables(topicDivision);
    I = topicDivision;
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
    trainSet = docSet;
    
    if(docSet.filesNumMark.size() == 2)
      setNumFollowee(docSet.docs.size() - docSet.filesNumMark.get(1));
    else setNumFollowee(0);
    
    int MSum = docSet.docs.size(); // sum of follower & followee docs
    M = docSet.filesNumMark.get(1);
    V = docSet.termToIndexMap.size();
    nmk = new int [MSum][K];
    nkt = new int[K][V];
    nmk_I_Sum = new int[MSum];
    nmk_S_Sum = new int[MSum];
    
    nktSum = new int[K];
    phi = new double[K][V];
    theta = new double[MSum][K];
    
    uisVariables .initParam(M);
    // map  followers to docIndex
    follower_doc_map = new int [V];
    for(int m = 0 ; m < M ; m++) {
      String name = docSet.docs.get(m).docName;
      int nameInt = docSet.getIndex(name);
      follower_doc_map[nameInt] = m;
    }
    
    beta = 1.0 / V * 100;
    alpha = 1.0 / K ;
    
    //initialize documents index array
    doc = new int[MSum][];
    for(int m = 0; m < MSum; m++){
      //Notice the limit of memory
      int N = docSet.docs.get(m).docWords.length;
      doc[m] = new int[N];
      for(int n = 0; n < N; n++){
        doc[m][n] = docSet.docs.get(m).docWords[n];
      }
    }
    // balance chance for interest and social topics
    double [] p = new double[K];
    double pSum = 0.0;
    for(int i = 0 ; i < K ; i++) {
      if(i < uisVariables.division) 
        p[i] = 1000.0 / uisVariables.division ;
      else 
        p[i] = 1000.0 / (K - uisVariables.division);
      pSum += p[i];
    }
    
    //initialize topic lable z for each word
    z = new int[M][];
    uisVariables.initializeSwitcher(M);
    for(int m = 0; m < M; m++) {
      int N = docSet.docs.get(m).docWords.length;
      z[m] = new int[N];
      uisVariables.nmsSum[m] = N;
      uisVariables.switcher[m] = new int [N];
      
      for(int n = 0; n < N; n++){
        double topicCusor = Math.random() * pSum;
        int initTopic = 0;
        for(int i = 0 ; i < K ; i ++ ) {
          topicCusor -= p[i];
          if(topicCusor <= 0) {
            initTopic = i;
          }
        }
        
        z[m][n] = initTopic;
        //number of words in doc m assigned to topic initTopic add 1
        nmk[m][initTopic]++;
        //number of terms doc[m][n] assigned to topic initTopic add 1
        
        if(initTopic < uisVariables.division) {
          nkt[initTopic][doc[m][n]] += 100;
          nktSum[initTopic] += 100;
          uisVariables.switcher[m][n] = S_down;
          uisVariables.nms[m][S_down]++;
          nmk_I_Sum[m]++;
          continue;
        }
        
        nkt[initTopic][doc[m][n]] += 100;
        int word = doc[m][n];
        
        for(int i = 0 ; i < weighMatrix[word].length ; i++) {
          nkt[initTopic][weighMatrix[word][i].index] += weighMatrix[word][i].times;
        }
        nktSum[initTopic] += weighSum[word];
        // total number of words assigned to topic initTopic add 1
        nktSum[initTopic] += 100;
        uisVariables.switcher[m][n] = S_up;
        uisVariables.nms[m][S_up] ++;
        nmk_S_Sum[m]++;
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
        saveCommunity (trainSet);
        //
        outputCommunitySize(i, community);
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
    
    UIS_LDA_Seperated.write(this, serialPath);
  }
  
  private void updateEstimatedParameters() {
    // TODO Auto-generated method stub
    for(int k = 0; k < K; k++){
      for(int t = 0; t < V; t++){
        phi[k][t] = (nkt[k][t] + beta) / (nktSum[k] + V * beta);
      }
    }
    
    int I = uisVariables.division;
    
    for(int m = 0; m < M; m++){
      for(int k = 0; k < K; k++){
        if(k < I)
          theta[m][k] = (nmk[m][k] + alpha) / (nmk_I_Sum[m] + nmk_S_Sum[m] + K * alpha);
        else 
          theta[m][k] = (nmk[m][k] + alpha) / (nmk_S_Sum[m]  + nmk_I_Sum[m] + K * alpha);
      }
    }
  }

  private int sampleTopicZ(int m, int n) {
    // TODO Auto-generated method stub
    // Sample from p(z_i|z_-i, w) using Gibbs upde rule
    
    //Remove topic label for w_{m,n}
    int oldTopic = z[m][n];
    // sample switcher first...
    if(oldTopic < uisVariables.division) {
      nmk[m][oldTopic]--;
      nkt[oldTopic][doc[m][n]] -= 100;
      nmk_I_Sum[m]--;
      nktSum[oldTopic] -= 100;
    } else {
      nmk[m][oldTopic]--;
      nkt[oldTopic][doc[m][n]] -= 100;
      nmk_S_Sum[m]--;
      nktSum[oldTopic] -= 100;
      int word = doc[m][n];
      for(int i = 0 ; i < weighMatrix[word].length ; i++) {
        nkt[oldTopic][weighMatrix[word][i].index] -= weighMatrix[word][i].times;
      }
      nktSum[oldTopic] -= weighSum[word];
    }
    
    int oldSwithcer = uisVariables.switcher[m][n];
    uisVariables.nms[m][oldSwithcer] -- ; 
    uisVariables.nmsSum[m] --;
    
    int word = doc[m][n];
    //Compute p(z_i = k|z_-i, w)
    double topicsSum = 0;
    double [] p = new double[K];
    for(int k = 0; k < uisVariables.division; k++){
      p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * 
          (nmk[m][k] + alpha) / (nmk_I_Sum[m] + I * alpha) *
          (uisVariables.nms[m][0] + uisVariables.delta[0]) / 
          (uisVariables.nmsSum[m] + uisVariables.delataSum);
      topicsSum += p[k];
    }
    for(int k = uisVariables.division; k < K; k++){
      p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * 
          (nmk[m][k] + alpha) / (nmk_S_Sum[m] + (K-I) * alpha) * 
          (uisVariables.nms[m][1] + uisVariables.delta[1]) / 
          (uisVariables.nmsSum[m] + uisVariables.delataSum);
      topicsSum += p[k];
    }
    
    int newTopic = 0;
    double topicCursor = (double) (Math.random() * topicsSum);
    for (int k = 0; k < K; k++) {
      topicCursor -= p[k];
      if (topicCursor <= 0) {
        newTopic = k;
        break;
      }
    }
    
    int newSwithcer;
    if(newTopic < uisVariables.division) {
      newSwithcer = 0;
      //Add new topic label for w_{m, n}
      nmk[m][newTopic]++;
      nkt[newTopic][doc[m][n]] += 100;
      nmk_I_Sum[m]++;
      nktSum[newTopic] += 100;
    } else {
      newSwithcer = 1;
      //Add new topic label for w_{m, n}
      nmk[m][newTopic]++;
      nkt[newTopic][doc[m][n]] += 100;
      nmk_S_Sum[m]++;
      nktSum[newTopic] += 100;
      for(int i = 0 ; i < weighMatrix[word].length ; i++) {
        nkt[newTopic][weighMatrix[word][i].index] += weighMatrix[word][i].times;
      }
      nktSum[newTopic] += weighSum[word];
    }
    
    uisVariables.nms[m][newSwithcer] ++ ; 
    uisVariables.nmsSum[m] ++;
    uisVariables.switcher[m][n] = newSwithcer;
    return newTopic;
  }

  public int GibbsSamplerForInterest(int m, int n, int oldTopic) {
    //Compute p(z_i = k|z_-i, w)
    double topicsSum = 0;
    double [] p = new double[K];
    for(int k = 0; k < uisVariables.division; k++){
      p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmk_I_Sum[m] + I * alpha);
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
    nmk_I_Sum[m]++;
    nktSum[newTopic] += 100;
    return newTopic;
  }
  
  public int GibbsSamplerForSocial(int m, int n, int oldTopic) {
    //Compute p(z_i = k|z_-i, w)
    double topicsSum = 0;
    double [] p = new double[K];
    for(int k = uisVariables.division; k < K; k++){
      p[k] = (nkt[k][doc[m][n]] + beta) / (nktSum[k] + V * beta) * (nmk[m][k] + alpha) / (nmk_S_Sum[m] + (K -I) * alpha);
      topicsSum += p[k];
    }
    
    //Sample a new topic label for w_{m, n} like roulette
    //Compute cumulated probability for p
    //
    int newTopic = 0;
    double topicCursor = (double) (Math.random() * topicsSum);
    for (int k = uisVariables.division; k < K; k++) {
      topicCursor -= p[k];
      if (topicCursor <= 0) {
        newTopic = k;
        break;
      }
    }
    
    int word = doc[m][n];
    //Add new topic label for w_{m, n}
    nmk[m][newTopic]++;
    nkt[newTopic][doc[m][n]] += 100;
    nmk_S_Sum[m]++;
    nktSum[newTopic] += 100;
    for(int i = 0 ; i < weighMatrix[word].length ; i++) {
      nkt[newTopic][weighMatrix[word][i].index] += weighMatrix[word][i].times;
    }
    nktSum[newTopic] += weighSum[word];
    return newTopic;
  }
  
  public void saveCommunity() throws FileNotFoundException, IOException {
    saveCommunity(this.trainSet);
  }
  
  private void saveCommunity(Documents docSet) throws FileNotFoundException, IOException {

    String resPath = PathConfig.LdaResultsPath + saveIndicator + "/";
    String modelNameString = "c_";

    community = getCommunity(docSet);
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
    
    for(int i = 0 ; i < M ; i++) {
      Document d = trainSet.docs.get(i);
      indexList.put(Integer.parseInt(d.docName), i);
    }
    
    cData.write(cData);
  }


  public Community getCommunity(Documents docSet) {
    double thresholdForER = threshold ;
    double thresholdForEE = 0.06;
    List<Integer> CF; // followers for community
    List<Integer> CG; // followees for community 
    
    Community community = new Community();
    
    List<Integer> docIndex;
    //follow link edge for community
    List< List< Pair<Integer, Integer> > > CEdgeList = 
        new ArrayList< List <Pair<Integer,Integer> > >(); 
    
    double [][] followeeTopic_p = new double [numFollowee][K] ;
    //需要建立一张 follower -> m 的表, ---OK---
    for (int m = M; m < M + numFollowee; m++) {
      double p_sum_interst  = 0;
      double p_sum_social = 0;
      for(int k = 0 ; k < K ; k ++) {
        //先把   每个followee 文档的总各个topic概率  找出来
        for(int n = 0; n < doc[m].length ; n++) {
          int follower_doc_index = follower_doc_map[doc[m][n]];
          followeeTopic_p[m-M][k] += theta[follower_doc_index][k];
          if( k  < uisVariables.division)
            p_sum_interst += theta[follower_doc_index][k];
          else
            p_sum_social += theta[follower_doc_index][k];
        }
      }
      // Normalization
      for(int k = 0 ; k < K ; k ++) {
        if(k < uisVariables.division) 
          followeeTopic_p[m-M][k] /= p_sum_interst;
        else 
          followeeTopic_p[m-M][k] /= p_sum_social;
      }
    }
    /*
    //compute deviation & apply Chebyshev Law for setting threshold .
    double [] averCG = new double [K];
    double [] averCF = new double [K];
    double [] deviCG = new double [K];
    double [] deviCF = new double [K];
    
    double magicThr = 0.0001;
    for(int k = 0 ; k < K ; k ++) {
      int Fsize = 0;
      for(int m = 0 ; m < M ; m++) {
        if(theta[m][k] > magicThr) {
          averCF[k] += theta[m][k];
          Fsize ++ ;
        }
      }
      averCF[k] = averCF[k] / Fsize;
      
      for(int m = 0 ; m < M ; m++) {
        if(theta[m][k] > magicThr) {
          deviCF[k] += (theta[m][k] - averCF[k]) * (theta[m][k] - averCF[k]);
        }
      }
      deviCF [k] = Math.sqrt(deviCF[k] / Fsize);
      
      int Gsize = 0 ;
      for(int m = M ; m < M + numFollowee; m++) {
        if(followeeTopic_p[m-M][k] > magicThr) {
          averCG[k] += followeeTopic_p[m-M][k];
          Gsize ++ ;
        }
      }
      averCG[k] = averCG[k] / Gsize;
      
      for(int m = M ; m < M + numFollowee; m++) {
        if(followeeTopic_p[m-M][k] > magicThr) {
          deviCG[k] += (followeeTopic_p[m-M][k] - averCG[k]) * (followeeTopic_p[m-M][k] - averCG[k]);
        }
      }
      deviCG [k] = Math.sqrt(deviCG[k] / Gsize);
    }
    
    */
    
    for(int k = 0 ; k < K ; k ++) {
      CF = new ArrayList<Integer>();
      CG = new ArrayList<Integer>();
      docIndex = new ArrayList<Integer>();
      
      List< Pair<Integer, Integer>> CEdges = new ArrayList<Pair<Integer,Integer>>();
      for(int m = 0 ; m < M ; m++) {
        //**** 需要对documents 排序
        String docName = docSet.docs.get(m).docName;
        int follower = Integer.parseInt(docName);
        
        //setting thr using chebyshey law
        //thresholdForER = averCF[k] - 2  * deviCF[k];
        if (theta[m][k] > thresholdForER && docSet.docs.get(m).docWords.length > 1) {
          CF.add(follower);
          docIndex.add(m);
        }
      }
      
      for (int m = M; m < M + numFollowee; m++) {
        String docName = docSet.docs.get(m).docName;
        int followee = Integer.parseInt(docName);
        //thresholdForEE = averCG[k] - 2  * deviCG[k];
        if (followeeTopic_p[m-M][k] > thresholdForEE &&docSet.docs.get(m).docWords.length > 1) {
          CG.add(followee);
          docIndex.add(m);
        } else {
          if(k < uisVariables.division) {
            if (followeeTopic_p[m-M][k] > thresholdForER 
                &&docSet.docs.get(m).docWords.length > 1) {
              CG.add(followee);
              docIndex.add(m);
            }
          }
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

  public void outputCommunitySize (int iters, Community c) throws IOException {
    String resPath = PathConfig.GPUResultPath + saveIndicator + "/";
    String modelName = "lda_" + iters;
    BufferedWriter writer = new BufferedWriter(new FileWriter(resPath
        + modelName + ".CommunitySize"));
    for(int i = 0 ; i < K; i++) {
      int erSize = c.CF.get(i).size();
      int eeSize = c.CG.get(i).size();
      writer.write("Community "+ i +"follwer size :" + erSize + "followee size : " +eeSize + "\n");
    }
    writer.close();
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
      Collections.sort(tWordsIndexArray, new TwordsComparable(phi[i]));
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
  
  public static void write(UIS_LDA_Seperated uisData , String path) throws FileNotFoundException, IOException {
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
    out.writeObject(uisData);
    out.flush();
    out.close();
  }
  
  public static UIS_LDA_Seperated read(String path) throws FileNotFoundException, IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
    UIS_LDA_Seperated cData = (UIS_LDA_Seperated) in.readObject();
    in.close();
    return cData;
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

