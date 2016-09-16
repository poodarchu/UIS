package me.poodar.uis.mf.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommunityData implements Serializable{
  
  private static final long serialVersionUID = 9218113212757908227L;
  
  public String path = "data/LdaTrainSet/wb/";
  public IDUtil tIDUtil ; //
  public HashMap<Integer, Integer> docIndexMap ;  //arrayList for sorting 
  public double [][] phi ; //
  public double [][] theta ; //
  public List<List<Integer> > CF; // followers for community
  public List<List<Integer> > CG; // followees for community 
  
  public CommunityData(String mediumName) {
    tIDUtil = new IDUtil();
    docIndexMap = new HashMap<Integer, Integer>();
    path += mediumName + ".CommunityData";
    CF = new ArrayList<List<Integer>>();
    CG = new ArrayList<List<Integer>>();
  }
  
  public void setMedium(String mediumName) {
    path += mediumName + ".CommunityData";
  }
  
  public String getID (int index) {
    return tIDUtil.indexToIDMap.get(index).toString();
  }
  
  public int getSize() {
    return tIDUtil.indexToIDMap.size();
  }
  
  public double getTheta(int follow , int k) {
    if(docIndexMap.get(follow) != null)
      follow = docIndexMap.get(follow);
    else 
      return 0;
    return theta[follow][k];
  }
  
  public double getPhi(int k , int term) {
    return phi[k][tIDUtil.IDToIndexMap.get(term)];
  }
  
  public void write(CommunityData cData) throws FileNotFoundException, IOException {
    tIDUtil.write();
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
    out.writeObject(cData);
    out.flush();
    out.close();
  }
  
  public CommunityData read() throws FileNotFoundException, IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
    CommunityData cData = (CommunityData) in.readObject();
    in.close();
    return cData;
  }
}
