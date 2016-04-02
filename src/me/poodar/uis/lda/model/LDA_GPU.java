package me.poodar.uis.lda.model;

import me.poodar.uis.lda.conf.LDAParameter;
import me.poodar.uis.lda.conf.PathConfig;
import me.poodar.uis.lda.util.Documents;
import me.poodar.uis.lda.util.Documents.Document;
import me.poodar.uis.lda.util.FileUtil;
import me.poodar.uis.social.lda.util.Community;
import me.poodar.uis.social.lda.util.CommunityData;
import me.poodar.uis.social.lda.util.IndexFreq;
import org.javatuples.Pair;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class LDA_GPU {
  private Documents trainSet; //训练集是Documents对象,里面包含Document的ArrayList,每个Document对象包含docName和int[]类型的docWords
  public String saveIndicator = "";  //保存指示
  
  public IndexFreq[][] weighMatrix; //weighMatrix是权重矩阵,每个weighMatrix[i][j]表示一个IndexFreq对象,里面包含每个word的index和它对应的times
  public int [] weighSum ;          //总权重
 
  int [][] doc;//word index array   int[i][j]表示第i篇文档的第j个词
  int V, K, M;//vocabulary size, topic number, document number
  int [][] z;//topic label array    int[i][j]表示第i篇文档的第j个词对应的topic
  double alpha; //doc-topic dirichlet prior parameter 
  double beta; //topic-word dirichlet prior parameter
  int [][] nmk;//given document m, count times of topic k. M*K  .times of topic j assigned to  document i
  int [][] nkt;//given topic k, count times of term t. K*V      .times of words j assigned to  topic i
  int [] nmkSum;//Sum for each row in nmk                       .number of words in document i
  int [] nktSum;//Sum for each row in nkt                       .number of words in topic i
  double [][] phi;//Parameters for topic-word distribution K*V  .phi_i_j is the probability of word j appear in topic i , phi_i_* is the distribution of topic i
  double [][] theta;//Parameters for doc-topic distribution M*K
  int iterations;//Times of iterations   迭代次数
  int saveStep;//The number of iterations between two saving   每两次save之间间隔的次数
  int beginSaveIters;//Begin save model at this iteration      第一次save从第x次开始
  
  private int maxFollower;

  public static double threshold;
  
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
    beta = 1.0 / V * 100; //beta设置为0.01 / V
    iterations = 1001;
      //K需要用户手动输入
    K = LDAParameter.K;
      //每次save的间隔是100步
    saveStep = 100;
      //从第500步开始save
    beginSaveIters = 500;
  }
  
  public void setMetric(IndexFreq[][] matrix) {
    this.weighMatrix = matrix;
    this.weighSum = new int[matrix.length];  //每一行的IndexFreq[i][*]的和为weighSum
      //每一项weighSum[i]的值由matrix得每一行的各个index的times相加得到
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

      //docs中每个元素是一个Document对象,代表一片文档
      M = docSet.docs.size();
      V = docSet.termToIndexMap.size();
      nmk = new int [M][K];
      nkt = new int[K][V];
      nmkSum = new int[M];
      nktSum = new int[K];
      phi = new double[K][V];
      theta = new double[M][K];
    
      beta = 1.0 / V * 100;
      alpha = 1.0 / K ;   //alpha的取值是1.0/K

      if(docSet.filesNumMark.size() == 2)
          setMaxFollower(docSet.filesNumMark.get(1));
      else
          setMaxFollower(M);
    
      //initialize documents index array, 将docSet的docs[i]中存储的信息转化为使用二维数组存储
      doc = new int[M][];
      for(int m = 0; m < M; m++){
          //Notice the limit of memory
          int N = docSet.docs.get(m).docWords.length;
          doc[m] = new int[N];
          for(int n = 0; n < N; n++)
              doc[m][n] = docSet.docs.get(m).docWords[n];
      }
    
      //initialize topic lable z for each word, 为每个word随机生成一个初始topic z_0
      z = new int[M][];
      for(int m = 0; m < M; m++){
          int N = docSet.docs.get(m).docWords.length;
          z[m] = new int[N];
          nmkSum[m] = N;
          for(int n = 0; n < N; n++) {
              int initTopic = (int) (Math.random() * K);// From 0 to K - 1
              z[m][n] = initTopic;
              //number of words in doc m assigned to topic initTopic add 1
              nmk[m][initTopic]++;
              //number of terms doc[m][n] assigned to topic initTopic add 1

              //+100 ???
              nkt[initTopic][doc[m][n]] += 100;
              int word = doc[m][n];

              for (int i = 0; i < weighMatrix[word].length; i++)
                  nkt[initTopic][weighMatrix[word][i].index] += weighMatrix[word][i].times;
              nktSum[initTopic] += weighSum[word];
              // total number of words assigned to topic initTopic add 1
              nktSum[initTopic] += 100;
          }
      }
  }

  public void inferenceModel() throws IOException {
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
            //save the community docs 到 data/LdaResult/
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

    //将Community存储到/data/LdaResult/
  private void saveCommunity(int iters , Documents docSet) throws FileNotFoundException, IOException {

    String resPath = PathConfig.LdaResultsPath + saveIndicator + "/";
    String modelNameString = "c_";

    Community community = getCommunity(docSet);
      //public CommunityData(String mediumName)
      // path += mediumName + ".CommunityData";
      //public String path = "data/LdaTrainSet/wb/";
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
      
      BufferedWriter writer = new BufferedWriter(new FileWriter(resPath + modelNameString + "F" + k));
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


    //使用docSet获得Community
  public Community getCommunity(Documents docSet) {
    double thresholdForER = threshold ;  //follower的threshold
    double thresholdForEE = 1.0 / 1000;  //followee的threshold

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
        //community的CF和CG严格意义上应该是CFs和CGs
      community.CF.add(CF);
      community.CG.add(CG);
    }

    community.CEdgeList = CEdgeList;
    return community;
  }

  public void saveIteratedModel(int iters, Documents docSet) throws IOException {
      // lda.params lda.phi lda.theta lda.tassign lda.twords
      // lda.params
      //将结果保存到/data/GPU/
      String resPath = PathConfig.GPUResultPath + saveIndicator + "/";
      String modelName = "lda_" + iters;   //使用lda+迭代的次数命名每个model
      ArrayList<String> lines = new ArrayList<String>();  //每次保存的model的格式为一个lines,包含LDA_Parameter的各个属性
      lines.add("alpha = " + alpha);
      lines.add("beta = " + beta);
      lines.add("topicNum = " + K);
      lines.add("docNum = " + M);
      lines.add("termNum = " + V);
      lines.add("iterations = " + iterations);
      lines.add("saveStep = " + saveStep);
      lines.add("beginSaveIters = " + beginSaveIters);
      //将存储好的lines保存到/data/GPUResult/modelName.params
      FileUtil.writeLines(resPath + modelName + ".params", lines);

      // lda.theta M*K
      //将theta二维数组保存到resPath+modelName.theta
      BufferedWriter writer = new BufferedWriter(new FileWriter(resPath + modelName + ".theta"));
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
      writer = new BufferedWriter(new FileWriter(resPath + modelName + ".tassign"));
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

      //找出每个主题中排在前20位的words
      int topNum = LDAParameter.topNum; // Find the top 20 topic words in each topic
      int topHundred = 100;
      int topThousand = 1000;

      for (int i = 0; i < K; i++) {
          //每个topic对应一个tWordsIndex的数组
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
                      writer.write("\n\t"); //5个一行
                  writer.write( trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
                          + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) +" "
                          + nkt[i][tWordsIndexArray.get(t)] / 100 + "\t");
              }
        
              if(t < topHundred) {
                  if (t % 5 == 0)
                      writerHundred.write("\n\t");
                  writerHundred
                          .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
                                  + String.format("%.8f", phi[i][tWordsIndexArray.get(t)])
                                  + " " + nkt[i][tWordsIndexArray.get(t)] / 100 + "\t");
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

