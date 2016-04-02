package me.poodar.uis.lda.model;

import me.poodar.uis.lda.util.Doc;

public class TsmEM {
  //Doc d , Word w , Topic j
  //F for feature ,P for positive ,N for negative 
  //c(w,d) for counting number for Word w in document d
  
  private Doc trainSet;
  int [][] doc;//word index array
  int V, J, M;//vocabulary size, topic number, document number
  int c_wd[][];
  
  double lambdaB ; //empirically constant
  double lambdaB_un; // 1 - lambdaB
  double p_dwjf ;  //possibility for pdwjf need not be stored 
  double p_dwjp ;  //as it is in pc_f
  double p_dwjn ;
  double p_dwj [][][];
  
  double pi_dj[][];
  double delta_djf[][];
  double delta_djp[][];
  double delta_djn[][];
  
  double theta_b;
  double theta_jw[][];
  double theta_p[];
  double theta_n[];
  
  double topic_dw[][]; // representing for topic possibility for doc d & word w;
  
  double pc_f [][][];
  double pc_p [][][];
  double pc_n [][][];
  double pc [][][];
  
  double pcSum_W  [][]; //∑_w pc
  double pcSum_W_f [][];//∑_w pc_f
  double pcSum_W_p [][];//∑_w pc_p
  double pcSum_W_n [][];//∑_w pc_n
  
  double pcSum_D_f[][]; //[j][w]
  double pcSum_DJ_p[];
  double pcSum_DJ_n[];
  
  double pcSum_WC []; // ∑_w ∑_c pc_f
  double pcSum_WCJ_p;
  double pcSum_WCJ_n;
  
  double pcSum_JW [];   //∑_j (∑_w pc)
  double pcSum_CJW ;    //∑_d (∑_j (∑_w pc))
  
  double pcSum_CJ_f[][];   // for each topic j & word w
  double pcSum_CJ_p[];    //for each word w
  double pcSum_CJ_n[];
  
  double mu_p;  //empirically parameter
  double mu_n;  //
  double mu_j;  //
  
  private void init() {
    //c_wd ,initiate other values
  }
  
  private void Estep() {
    //compute topic_dw
    for(int d = 0 ; d < M ; d++) {
      //
      for(int w = 0 ; w < V ; w ++) {
        //
        topic_dw[d][w] = 0;
        for(int j = 0; j < J ; j++) {
          topic_dw[d][w] += pi_dj[d][j] * 
              (delta_djf[d][j] * theta_jw[j][w] +
               delta_djp[d][j] * theta_p[w]+
               delta_djn[d][j] * theta_n[w]); 
        }
      }
    }
    
    //set pcSum_D_f..to zero
    for(int w = 0 ; w < V ; w++) {
      pcSum_DJ_p[w] = 0;
      pcSum_DJ_n[w] = 0;
      for(int j = 0 ; j < J ; j++ ) {
        pcSum_D_f[j][w]=0;
      }
    }
    
    //
    for(int j = 0; j < J ; j++) {
      pcSum_WC[j] = 0;
    }
    pcSum_WCJ_n = 0;
    pcSum_WCJ_p = 0;
    //
    pcSum_CJW = 0;
    for(int d = 0 ; d < M ; d++) {
      //
      pcSum_JW[d] = 0;
      for(int j = 0; j < J ; j++) {
        //
        pcSum_W[d][j] = 0;
        pcSum_W_f[d][j] = 0;
        pcSum_W_n[d][j] = 0;
        pcSum_W_p[d][j] = 0;
        for(int w = 0 ; w < V ; w ++) {
          //
          double denominator = lambdaB * theta_b + lambdaB_un * topic_dw[d][w];
          double commonFactor = (lambdaB_un * pi_dj[d][j]) / denominator;
          
          p_dwjf = commonFactor * delta_djf[d][j] * theta_jw[j][w];
          p_dwjp = commonFactor * delta_djp[d][j] * theta_p[w];
          p_dwjn = commonFactor * delta_djn[d][j] * theta_n[w];
          p_dwj[d][w][j] = p_dwjf + p_dwjn + p_dwjp;
          
          pc_f[d][w][j] = c_wd[w][d] * p_dwjf;
          pc_n[d][w][j] = c_wd[w][d] * p_dwjn;
          pc_p[d][w][j] = c_wd[w][d] * p_dwjp;
          
          pcSum_W[d][j] += pc[d][w][j];
          pcSum_W_f [d][j] += pc_f[d][w][j];
          pcSum_W_n [d][j] += pc_n[d][w][j];
          pcSum_W_p [d][j] += pc_p[d][w][j];
          
          pcSum_WC[j] += pc_f[d][w][j];
          pcSum_WCJ_n += pc_n[d][w][j];
          pcSum_WCJ_p += pc_p[d][w][j];
          
          pcSum_D_f[j][w] += pc_f[d][w][j];
          pcSum_DJ_p[w]+= pc_p[d][w][j];
          pcSum_DJ_n[w]+= pc_n[d][w][j];
          
        }
        pcSum_JW[d] += pcSum_W[d][j];
      }
      pcSum_CJW += pcSum_JW[d];
    }
  }
  
  private void Mstep() {
    //estimate pi,delta,theta...
    for(int d = 0 ; d < M ; d++) {
      for(int j = 0 ; j < J ; j++) {
        //compute pi,delta
        pi_dj[d][j] = pcSum_W[d][j] / pcSum_JW[d];
        delta_djf[d][j] = pcSum_W_f[d][j] / pcSum_W[d][j] ;
        delta_djp[d][j] = pcSum_W_p[d][j] / pcSum_W[d][j] ;
        delta_djn[d][j] = pcSum_W_n[d][j] / pcSum_W[d][j] ;
      }
    }
    
    for(int w = 0 ; w < V ; w ++) {
      for(int j = 0; j < J ; j++) {
        theta_jw[j][w] = pcSum_D_f[j][w] / pcSum_WC[w];
        theta_p[w] = pcSum_DJ_p[w] / pcSum_WCJ_p;
        theta_n[w] = pcSum_DJ_n[w]/ pcSum_WCJ_n;
      }
    }
  }
  
}
