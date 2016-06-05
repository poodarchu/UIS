package me.poodar.uis.social.lda.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IDUtil implements Serializable{
  
  /**
   * 
   */
  private static final long serialVersionUID = -3683507757003501398L;
  /**
   * 
   */
  public Map<String, Integer> IDToIndexMap = new HashMap<String , Integer>();
  public ArrayList<String> indexToIDMap = new ArrayList<String>();
  public String path = "data/LdaTrainSet/wb/twMAP";
  
  public int getIndex (String id) {
    if(!IDToIndexMap.containsKey(id)) {
      int index = indexToIDMap.size();
      IDToIndexMap.put(id, index);
      indexToIDMap.add(id);
      return index;
    } else {
      int index = IDToIndexMap.get(id);
      return index;
    }
  }
  
  public String getID (int index) {
    return indexToIDMap.get(index).toString();
  }
  
  public int getSize() {
    return indexToIDMap.size();
  }
  
  public void write() throws FileNotFoundException, IOException {
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
    out.writeObject(this);
    out.flush();
    out.close();
  }
  
  public IDUtil read() throws FileNotFoundException, IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
    IDUtil tw = (IDUtil) in.readObject();
    in.close();
    return tw;
  }
}

