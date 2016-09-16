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

public class TwitterIDUtil implements Serializable{

	private static final long serialVersionUID = 8975676512972851790L;
  
  	public static Map<String, Integer> IDToIndexMap = new HashMap<String , Integer>();
	public static ArrayList<String> indexToIDMap = new ArrayList<String>();
	public static String path = "data/LdaTrainSet/wb/twMAP";
	
	//如果id不在Map中，将id添加到map中，并为其分配一个index，index是其在ArrayList中的索引
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
		return indexToIDMap.get(index);
	}
	
	public static int getSize() {
	  return indexToIDMap.size();
	}
	
  public void write() throws FileNotFoundException, IOException {
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path));
    out.writeObject(this);
    out.flush(); //刷新流。这将写入任何缓冲的输出字节，并通过底层流刷新。
    out.close(); //Closes the stream. This method must be called to release any resources associated with the stream.
  }
  
  public TwitterIDUtil read() throws FileNotFoundException, IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(path));
    TwitterIDUtil tw = (TwitterIDUtil) in.readObject();
    in.close();
    return tw;
  }
}
