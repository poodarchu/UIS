package me.poodar.uis.mf.utils;

public class TMBased {
  public static double predict (double [] user , double [] item) {
    double r = 0.0;
    for(int k = 0 ; k < Parameter.L ; k++) {
      r += user[k] * item[k];
    }
    return r;
  }
}
