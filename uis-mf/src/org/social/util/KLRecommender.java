package org.social.util;

public class KLRecommender {
  public static double predict (double [] user , double [] item) {
    double r = 0.0;
    for(int k = 0 ; k < Parameter.L ; k++) {
      r += user[k] * Math.log(user[k] / item[k]);
    }
    return r;
  }
}
