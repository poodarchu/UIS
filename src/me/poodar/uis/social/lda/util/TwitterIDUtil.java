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

public class TwitterIDUtil implements Serializable{

  private static final long serialVersionUID = 8975676512972851790L;
  
  //每个Index对应一个ID，所以该Map的基本结构是<Identifier:Index>
  public static Map<String, Integer> IDToIndexMap = new HashMap<String , Integer>();  
  public static ArrayList<String> indexToIDMap = new ArrayList<String>();
  public static String path = "data/LdaTrainSet/wb/twMAP";
	
 //返回id对应的index，如果id不存在，则新建一个，将index设置为indexToIDMap.size()
  public static int getIndex (String id) {
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
	
//  根据Index获得对应的ID
  public static String getID (int index) {
	  return indexToIDMap.get(index).toString();
  }
	
//  获得indexToIDMap的size
  public static int getSize() {
	  return indexToIDMap.size();
  }
	
  //写入文件
  public void write() throws FileNotFoundException, IOException {
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
    out.writeObject(this);
    out.flush();
    out.close();
  }
  
  public TwitterIDUtil read() throws FileNotFoundException, IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
    TwitterIDUtil tw = (TwitterIDUtil) in.readObject();
    in.close();
    return tw;
  }
}
