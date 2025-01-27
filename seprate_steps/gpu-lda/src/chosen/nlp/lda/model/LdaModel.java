package chosen.nlp.lda.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import chosen.nlp.lda.conf.LDAParameter;
import chosen.nlp.lda.conf.PathConfig;
import chosen.nlp.lda.model.variables.PMILabelVariables;
import chosen.nlp.lda.model.variables.TermWeightingVariables;
import chosen.nlp.lda.util.Aspects;
import chosen.nlp.lda.util.Doc;
import chosen.nlp.lda.util.DocBase;
import chosen.nlp.lda.util.DocBase.Document;
import chosen.nlp.lda.util.DocSentence;
import chosen.nlp.lda.util.Documents;
import chosen.nlp.lda.util.FileUtil;
import chosen.nlp.lda.util.SIPair;
import chosen.nlp.lda.util.wordFreq;

public class LdaModel implements LDA {

  // private Documents trainSet;
  private DocSentence trainSet;
  // private DocBase trainSet;
  int[][] doc;// word index array
  int V, K, M;// vocabulary size, topic number, document number
  int LV; // allocate LV in order to accustom to V
  int[][] z;// topic label array
  public double alpha; // doc-topic dirichlet prior parameter
  public double beta[][]; // topic-word dirichlet prior parameter
  public double betaSum[]; 
  int[][] nmk;// given document m, count times of topic k. M*K
  int[][] nkt;// given topic k, count times of term t. K*V
  // int [][][] nmkt;//given doucument m , count times of topic k & t , used for
  // termWeight
  int[] nmkSum;// Sum for each row in nmk
  int[] nktSum;// Sum for each row in nkt
  double[][] theta;// Parameters for doc-topic distribution M*K
  double[][] phi;// Parameters for topic-word distribution K*V
  int iterations;// Times of iterations
  int saveStep;// The number of iterations between two saving
  int beginSaveIters;// Begin save model at this iteration

  public PMILabelVariables plv;

  /*
   * public LdaModel(Documents DocsIn) { // TODO Auto-generated constructor stub
   * trainSet = new Documents(); trainSet.readDocs(PathConfig.LdaTrainSetPath);
   * getParameter(); this.Initialize(DocsIn); }
   * 
   * public LdaModel(Documents DocsIn,String delimiter) { // TODO Auto-generated
   * constructor stub trainSet = DocsIn; getParameter();
   * this.Initialize(DocsIn); }
   */
  public LdaModel(DocSentence DocsIn, String delimiter) {
    // TODO Auto-generated constructor stubs
    trainSet = DocsIn;
    getParameter();
    this.Initialize(DocsIn);
  }

  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#Train()
   */
  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#Train()
   */

  @Override
  public void Train() {
    if (iterations < saveStep + beginSaveIters) {
      System.err
          .println("Error: the number of iterations should be larger than "
              + (saveStep + beginSaveIters));
      System.exit(0);
    }
    for (int i = 0; i < iterations; i++) {
      if (i % 100 == 0)
        System.out.println("Iteration " + i);
      if ((i >= beginSaveIters) && (((i - beginSaveIters) % saveStep) == 0)) {
        // Saving the model
        System.out.println("Saving model at iteration " + i + " ... ");
        // Firstly update parameters
        SaveEstimatedParameters();
        // Secondly print model variables
        try {
          Save(i, trainSet);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      // gibbs sampling
      gibbsSample();
    }
  }
/*
  public void termWeightTrain() { 
    if(iterations < saveStep + beginSaveIters){
    System.err.println("Error: the number of iterations should be larger than "
    + (saveStep + beginSaveIters)); System.exit(0); 
    }
    
    //build variables for PMI(xi,d) 
    double [][] pmi = new double [M][V]; 
    double [][][] pmiNkt = new double [M][K][V]; 
    double [][] pmiNktSum = new double[M][K];
    double [][][] pmiNmkt = new double [M][K][V]; 
    double [][]pmiNmktSum = new double [M][K]; 
    double [] pmiNmkSum = new double [M];
    TermWeightingVariables twv = new TermWeightingVariables();
    
    double nmt = 0; int termCount ; for(int m = 0; m < M ; m++ ) { 
      for (int v =0 ; v < V ; v ++) { 
        for(int k = 0; k < K ; k++) { 
          //nmt += nmkt[m][k][v]; 
          }
    String vString = trainSet.indexToTermMap.get(v); termCount =
    trainSet.termCountMap.get(vString); 
    // log2() 
    pmi [m][v] = - Math.log( nmt
    / termCount) / Math.log(2); } }
    
    for(int m = 0 ; m < M ; m++) { 
      for (int k = 0 ; k < K ; k++) { 
        for(int v = 0 ; v < V ; v++) { 
      pmiNkt[m][k][v] = pmi[m][v] * nkt[k][v]; 
      pmiNktSum[m][k]+= pmiNkt[m][k][v]; 
    //pmiNmkt[m][k][v] = pmi[m][v] * nmkt[m][k][v];
    pmiNmktSum[m][k] += pmiNmkt[m][k][v]; 
    } 
        pmiNmkSum[m] += pmiNktSum[m][k]; }
    } 
    twv.pmi = pmi; twv.pmiNkt = pmiNkt; twv.pmiNktSum = pmiNktSum ;
    twv.pmiNmkSum = pmiNmkSum; twv.pmiNmkt = pmiNmkt; twv.pmiNmktSum =
    pmiNmktSum;
    
    for(int i = 0; i < iterations; i++){ 
      if(i % 100 == 0)
    }
    System.out.println("Iteration " + i); 
    if( (i >= beginSaveIters) && (((i -
      beginSaveIters) % saveStep) == 0) )
    { //Saving the model
      System.out.println("Saving model at iteration " + i +" ... "); 
      //Firstly update parameters 
      SaveEstimatedParameters(); 
      //Secondly print model variables 
      try { Save(i, trainSet); } 
      catch (IOException e) { 
      // TODOAuto-generated catch block 
      e.printStackTrace(); } } 
    //term weighting sample
    for(int m = 0; m < M; m++){ 
      int N = doc[m].length; 
    for(int n = 0; n < N;n++){ 
      // Sample from p(z_i|z_-i, w) 
      int newTopic = termWeightSample(m, n ,twv); 
      z[m][n] = newTopic; 
      } 
    }
    
    } 
  }
*/
  public void labelTrain() {
    int nWords = 0;
    for (int m = 0; m < M; m++) {
      nWords += nmkSum[m];
    }

    if (iterations < saveStep + beginSaveIters) {
      System.err
          .println("Error: the number of iterations should be larger than "
              + (saveStep + beginSaveIters));
      System.exit(0);
    }
    for (int i = 0; i < iterations; i++) {
      if (i % 100 == 0)
        System.out.println("Iteration " + i);
      if ((i >= beginSaveIters) && (((i - beginSaveIters) % saveStep) == 0)) {
        // Saving the model
        System.out.println("Saving model at iteration " + i + " ... ");
        // Firstly update parameters
        SaveEstimatedParameters();
        // Secondly print model variables
        try {
          Save(i, trainSet);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      // gibbs sampling
      gibbsSample();
      // cal PMI
      if (i > 500 && i % 10 == 0) {
        SaveEstimatedParameters();
        calTopicWordPMI();
      }
    }
  }

  public void calTopicWordPMI() {
    for (int k = 0; k < K; k++) {
      double p_kSum = 0;
      for (int m = 0; m < M; m++) {
        p_kSum += theta[m][k];
      }
      double p_k = p_kSum / M;
      for (int v = 0; v < V; v++) {
        String v_word = trainSet.indexToTermMap.get(v);
        double p_v = (double) trainSet.termCountMap.get(v_word) / V;
        plv.PMI[k][v] = phi[k][v] * p_k * Math.log(phi[k][v] / p_v)
            / Math.log(2.0);
        if(Double.isNaN(plv.PMI[k][v]))
          plv.PMI[k][v] = 0;
        plv.PMI_v_Sum[v] += plv.PMI[k][v];
      }
    }
  }

  private void gibbsSample() {
    // Use Gibbs Sampling to update z[][]
    for (int m = 0; m < M; m++) {
      int N = doc[m].length;
      for (int n = 0; n < N; n++) {
        // Sample from p(z_i|z_-i, w)
        int newTopic = Sample(m, n);
        z[m][n] = newTopic;
      }
    }
  }
  
  private void labelGibbsSample() {
    for (int m = 0; m < M; m++) {
      int N = doc[m].length;
      for (int n = 0; n < N; n++) {
        // Sample from p(z_i|z_-i, w)
        int newTopic = labelSample(m, n);
        z[m][n] = newTopic;
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#getParameter()
   */
  @Override
  public void getParameter() {
    K = LDAParameter.K;
    // alpha = 1.0 / K;
    // beta = 1.0 / V;
    iterations = LDAParameter.iterations;
    saveStep = LDAParameter.saveStep;
    beginSaveIters = LDAParameter.beginSaveIters;
  }

  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#Initialize(chosen.nlp.lda.util.Documents)
   */

  @Override
  public void Initialize(Documents docSet) {
    M = docSet.docs.size();
    V = docSet.termToIndexMap.size();
    // autolly set super-parameter
    initVariables();

    doc = new int[M][];
    for (int m = 0; m < M; m++) {
      int N = docSet.docs.get(m).docWords.length;
      doc[m] = new int[N];
      for (int n = 0; n < N; n++) {
        doc[m][n] = docSet.docs.get(m).docWords[n];
      }
    }
    sampleInit();
  }

  @SuppressWarnings("null")
  public void Initialize(DocSentence docSet) {
    M = docSet.docs.size();
    V = docSet.termToIndexMap.size();
    // autolly set super-parameter
    initVariables();

    // reset beta & betaSum for seed words
    int seedTopic = 0;
    Collection<List<String>> seedwordCollect = Aspects.aspToSeedList.values();
    for (List<String> sameAspect : seedwordCollect) {
      if (sameAspect != null || sameAspect.isEmpty()) {
        double seedParameter = 0.3;
        resetBeta(sameAspect, seedTopic, seedParameter);
        seedTopic++;
      }
    }

    doc = new int[M][];
    for (int m = 0; m < M; m++) {
      int N = docSet.docs.get(m).docWords.length;
      doc[m] = new int[N];
      for (int n = 0; n < N; n++) {
        doc[m][n] = docSet.docs.get(m).docWords[n];
      }
    }
    sampleInit();

    seedTopic = 0;
    for (List<String> sameAspect : seedwordCollect) {
      if (sameAspect != null) {
        for (int m = 0; m < this.M; m++) {
          sampleInit(sameAspect, trainSet.docs.get(m).seedDSPair,
              trainSet.docs.get(m).SIPairIndexMap, m, seedTopic);
        }
      }
      seedTopic++;
    }
  }

  private void initVariables() {
    alpha = 1.0 / K;

    beta = new double[K][V];
    betaSum = new double[K];
    for (int i = 0; i < K; i++) {
      for (int j = 0; j < V; j++) {
        beta[i][j] = 1.0 / V;
      }
      betaSum[i] = 1.0;
    }
    nmk = new int[M][K];
    nkt = new int[K][V];
    // nmkt = new int [M][K][V];
    nmkSum = new int[M];
    nktSum = new int[K];
    theta = new double[M][K];
    phi = new double[K][V];
    
    plv = new PMILabelVariables();
    plv.PMI = new double [K][V];
    plv.PMI_v_Sum = new double [V];
  }

  private void sampleInit() {
    // sample topic ramdomly
    z = new int[M][];
    for (int m = 0; m < M; m++) {
      int N = doc[m].length;
      z[m] = new int[N];
      for (int n = 0; n < N; n++) {
        int word = doc[m][n];
        int topic = (int) (Math.random() * K);
        z[m][n] = topic;
        nmk[m][topic]++;
        // nmkt[m][topic][word] ++; // this is added to here for term weighting
        nkt[topic][word]++;
        nktSum[topic]++;
      }
      nmkSum[m] = N;
    }
  }

  @SuppressWarnings("unused")
  private void sampleInit(List<String> sameAspect,
      Map<String, List<SIPair>> seedDSPair,
      Map<String, Integer> SIPairIndexMap, int m, int topic) {
    int former;
    int word;
    for (String seedWord : sameAspect) {
      // for each word in sameAspect,get its position and constrain .
      List<SIPair> siPairs = seedDSPair.get(seedWord);
      if (siPairs == null)
        continue;
      for (SIPair siPair : siPairs) {
        // resample
        int n = SIPairIndexMap.get(siPair.toString());
        word = doc[m][n];
        former = z[m][n];
        nmk[m][former]--;
        nkt[former][word]--;
        nktSum[former]--;

        z[m][n] = topic;
        nmk[m][topic]++;
        nkt[topic][word]++;
        nktSum[topic]++;
      }
    }
  }

  @SuppressWarnings("unused")
  private void resetBeta(List<String> sameAspect, int topic,
      double seedParameter) {
    for (String seedWord : sameAspect) {
      if (!trainSet.termToIndexMap.containsKey(seedWord)) {
        continue;
      }
      int word = this.trainSet.termToIndexMap.get(seedWord);
      for (int i = 0; i < this.K; i++) {
        beta[i][word] = 0;
        betaSum[i] -= 1 / this.V;
      }
      beta[topic][word] = seedParameter;
      betaSum[topic] += seedParameter;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#SaveEstimatedParameters()
   */
  @Override
  public void SaveEstimatedParameters() {
    // update parameter of theta
    for (int k = 0; k < K; k++) {
      for (int t = 0; t < V; t++) {
        phi[k][t] = (nkt[k][t] + beta[k][t]) / (nktSum[k] + betaSum[k]);
      }
    }
    // updae parameter of phi
    for (int m = 0; m < M; m++) {
      for (int k = 0; k < K; k++) {
        theta[m][k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#Save(int, chosen.nlp.lda.util.Documents)
   */
  @Override
  public void Save(int iters) throws IOException {
    // TODO Auto-generated method stub
    // lda.params lda.phi lda.theta lda.tassign lda.twords
    // lda.params
    String resPath = PathConfig.LdaResultsPath;
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

    int topNum = LDAParameter.topNum; // Find the top 20 topic words in each
                                      // topic

    for (int i = 0; i < K; i++) {
      List<Integer> tWordsIndexArray = new ArrayList<Integer>();
      for (int j = 0; j < V; j++) {
        tWordsIndexArray.add(new Integer(j));
      }
      // Construct index of phi[i] sorted by its value
      // which means that index = tWordsIndexArray.get(0) , phi[i][index] is the
      // biggest value
      Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(phi[i]));
      writer.write("\n\n\n" + "topic " + i + "\t:\t" + nktSum[i] + "\t");
      for (int t = 0; t < topNum; t++) {

        writer
            .write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
                + String.format("%.8f", phi[i][tWordsIndexArray.get(t)]) 
                +" "+ nkt[i][tWordsIndexArray.get(t)] + "\t");

        if (t % 5 == 0)
          writer.write("\n\t");
      }
      writer.write("\n");
    }
    writer.close();

    writer = new BufferedWriter(new FileWriter(resPath + modelName
        + ".PMIwords"));

    List<Integer> tWordsIndexArray = new ArrayList<Integer>();
    for (int j = 0; j < V; j++) {
      tWordsIndexArray.add(new Integer(j));
    }
    // Construct index of phi[i] sorted by its value
    // which means that index = tWordsIndexArray.get(0) , phi[i][index] is the
    // biggest value
    Collections.sort(tWordsIndexArray, new LdaModel.TwordsComparable(
        plv.PMI_v_Sum));
    for (int t = V-1; t >= 0; t--) {
      writer.write(trainSet.indexToTermMap.get(tWordsIndexArray.get(t)) + " "
          + String.format("%.8f", plv.PMI_v_Sum[tWordsIndexArray.get(t)])
          + "\t");
      if (t % 5 == 0)
        writer.write("\n\t");
      writer.write("\t");
    }
    writer.close();
  }

  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#Sample(int, int)
   */
  @Override
  public int Sample(int m, int n) {
    int word = doc[m][n];
    // Remove topic label for w_{m,n}
    int formerTopic = z[m][n];
    nmk[m][formerTopic]--;
    nkt[formerTopic][word]--;
    nmkSum[m]--;
    nktSum[formerTopic]--;

    return gibbsSampler(m, word);
  }
  
  public int labelSample(int m, int n) {
    int word = doc[m][n];
    // Remove topic label for w_{m,n}
    int formerTopic = z[m][n];
    nmk[m][formerTopic]--;
    nkt[formerTopic][word]--;
    nmkSum[m]--;
    nktSum[formerTopic]--;

    return gibbsSampler(m, word);
  }

  private int gibbsSampler(int m, int word) {
    // sum of k
    double topicsSum = 0;
    double[] topicArray = new double[K];
    for (int k = 0; k < K; k++) {
      topicArray[k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha)
          * (nkt[k][word] + beta[k][word]) / (nktSum[k] + betaSum[k]);
      topicsSum += topicArray[k];
    }
    //
    int currentTopic = 0;
    double topicCursor = Math.random() * topicsSum;
    for (int k = 0; k < K; k++) {
      topicCursor -= topicArray[k];
      if (topicCursor <= 0) {
        currentTopic = k;
        break;
      }
    }
    nmk[m][currentTopic]++;
    nkt[currentTopic][word]++;
    nmkSum[m]++;
    nktSum[currentTopic]++;
    return currentTopic;
  }
  
  private int labelGibbsSampler(int m, int word) {
    // sum of k
    double topicsSum = 0;
    double[] topicArray = new double[K];
    for (int k = 0; k < K; k++) {
      topicArray[k] = (nmk[m][k] + alpha) / (nmkSum[m] + K * alpha)
          * (nkt[k][word] + beta[k][word]) / (nktSum[k] + betaSum[k]);
      topicsSum += topicArray[k];
    }
    //
    int currentTopic = 0;
    double topicCursor = Math.random() * topicsSum;
    for (int k = 0; k < K; k++) {
      topicCursor -= topicArray[k];
      if (topicCursor <= 0) {
        currentTopic = k;
        break;
      }
    }
    nmk[m][currentTopic]++;
    nkt[currentTopic][word]++;
    nmkSum[m]++;
    nktSum[currentTopic]++;
    return currentTopic;
  }

  /*
   * private int termWeightSample(int m, int n ,TermWeightingVariables twv) {
   * //sample int word = doc[m][n]; //Remove topic label for w_{m,n} int
   * formerTopic = z[m][n]; 
   * nmk[m][formerTopic]--; 
   * nkt[formerTopic][word] --;
   * nmkSum[m]--; 
   * nktSum[formerTopic]--; 
   * nmkt[m][formerTopic][word] --;
   * 
   * return termWeightSampler(m, word ,twv); 
   * }
   */
  /*
   * private int termWeightSampler(int m , int word ,TermWeightingVariables twv)
   * { //update twv 
   * for(int k = 0; k < K ; k++) { 
   * twv.pmiNmkSum[m] -= twv.pmiNktSum[m][k]; 
   * twv.pmiNktSum[m][k] -= twv.pmiNkt[m][k][word];
   * twv.pmiNmktSum[m][k] -= twv.pmiNmkt[m][k][word];
   * 
   * twv.pmiNkt[m][k][word] = twv.pmi[m][word] * nkt[k][word];
   * twv.pmiNmkt[m][k][word] = twv.pmi[m][word] * nmkt[m][k][word];
   * 
   * twv.pmiNktSum[m][k] += twv.pmiNkt[m][k][word]; 
   * twv.pmiNmktSum[m][k] += twv.pmiNmkt[m][k][word]; 
   * twv.pmiNmkSum[m] += twv.pmiNktSum[m][k]; 
   * }
   * 
   * //nmkt should be updated **** //update 
   * double topicsSum = 0; 
   * double [] topicArray = new double [K]; 
   * for(int k = 0; k < K; k++) { 
   * topicArray[k] =
   * (twv.pmiNmktSum[m][k] + alpha) / (twv.pmiNmkSum[m] + K * alpha) *
   * (twv.pmiNkt[m][k][word] + beta[k][word]) / (twv.pmiNktSum[m][k] +
   * betaSum[k]); 
   * topicsSum += topicArray[k]; 
   * } 
   * // 
   * int currentTopic = 0 ; 
   * double topicCursor = Math.random() * topicsSum; 
   * for(int k = 0 ; k < K ; k++) {
   * topicCursor -= topicArray[k]; 
   * if(topicCursor <= 0) { 
   * currentTopic = k;
   * break; 
   * } 
   * } nmk[m][currentTopic] ++ ; nkt[currentTopic][word] ++;
   * nmkSum[m]++; nktSum[currentTopic]++; nmkt[m][currentTopic][word] ++; return
   * currentTopic ; }
   */
  /*
   * (non-Javadoc)
   * 
   * @see chosen.nlp.lda.model.LDA#SampleAll()
   */
  @Override
  public void SampleAll() {
    for (int m = 0; m < M; m++) {
      int N = doc[m].length;
      for (int n = 0; n < N; n++) {
        z[m][n] = this.Sample(m, n);
      }
    }
  }

  public class TwordsComparable implements Comparator<Integer> {

    public double[] sortProb; // Store probability of each word in topic k

    public TwordsComparable(double[] sortProb) {
      this.sortProb = sortProb;
    }

    @Override
    public int compare(Integer o1, Integer o2) {
      // TODO Auto-generated method stub
      // Sort topic word index according to the probability of each word in
      // topic k
      if (sortProb[o1] > sortProb[o2])
        return -1;
      else if (sortProb[o1] < sortProb[o2])
        return 1;
      else
        return 0;
    }
  }

  @Override
  public void Initialize(Doc docSet) {
    // TODO Auto-generated method stub

  }

  @Override
  public void Save(int iters, Documents docSet) throws IOException {
    // TODO Auto-generated method stub
    Save(iters);
  }

  @Override
  public void Save(int iters, DocBase docSet) throws IOException {
    // TODO Auto-generated method stub
    Save(iters);
  }
}
