package chosen.social.lda.util;

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
import java.util.concurrent.ConcurrentHashMap;

public class TwitterIDUtil implements Serializable{
	
  private static final long serialVersionUID = 8975676512972851790L;
  public static Map<String, Integer> IDToIndexMap = new ConcurrentHashMap<String, Integer>();
	public static ArrayList<String> indexToIDMap = new ArrayList<String>();
	public static String path = "data/LdaTrainSet/wb/twMAP";
	
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
	
	public static String getID (int index) {
		return indexToIDMap.get(index).toString();
	}
	
	public static int getSize() {
	  return indexToIDMap.size();
	}
	
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
