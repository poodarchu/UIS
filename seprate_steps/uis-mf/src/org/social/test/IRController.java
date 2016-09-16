package org.social.test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

public class IRController {

  /**
   * @param args
   */
  public static void execShell(String shell) {

    try {
      Runtime rt = Runtime.getRuntime();
      rt.exec(shell);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static List runShell(String shStr) throws Exception {
    List<String> strList = new ArrayList();

    Process process;
    process = Runtime.getRuntime().exec(shStr);
    // process = Runtime.getRuntime().exec(shStr);
    InputStreamReader ir = new InputStreamReader(process.getInputStream());
    LineNumberReader input = new LineNumberReader(ir);
    String line;
    System.err.print(loadStream(process.getErrorStream())); 
    System.out.print(loadStream(process.getInputStream()));
    process.waitFor();
    process.destroy();
    return strList;
  }

 // read an input-stream into a String
  static String loadStream(InputStream in) throws IOException {
    int ptr = 0;
    in = new BufferedInputStream(in);
    StringBuffer buffer = new StringBuffer();
    while ((ptr = in.read()) != -1) {
      buffer.append((char) ptr);
    }
    return buffer.toString();

  }
  
  public static void main(String[] arg) throws Exception {
    
    String ifmfModel = "java -Xmx80960m -jar IFMF-Fast.jar 2 2 " + arg[0] +" " + arg[1] +" " +arg[2];
    String evaluate = "java -Xmx80960m -jar IFMF-Fast.jar 2 1 " + arg[0] +" " + arg[1] +" " +arg[2];
    
    IRController r = new IRController();
    r.runShell(ifmfModel);
    
    IRController e = new IRController();
    e.runShell(evaluate);
    
  }
}
