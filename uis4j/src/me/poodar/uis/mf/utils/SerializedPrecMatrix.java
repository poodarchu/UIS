package me.poodar.uis.mf.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.mymedialite.datatype.Pair;

import me.poodar.uis.mf.utils.CommunityData;

public class SerializedPrecMatrix implements Serializable {
  
  /**
   * 
   */
  private static final long serialVersionUID = -8398560979578624803L;
  public Map<Integer , List<Pair<Integer, Double>>> prec_Matrix ;
  public String path = "data/LdaResult/wb/IFMF/precMatrix";
  public void write(SerializedPrecMatrix pData , int i ) throws FileNotFoundException, IOException {
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path + "." + i));
    out.writeObject(pData);
    out.flush();
    out.close();
  }
  
  public SerializedPrecMatrix read(int i) throws FileNotFoundException, IOException, ClassNotFoundException {
    ObjectInputStream in = new ObjectInputStream(new FileInputStream(path+ "." + i));
    SerializedPrecMatrix pData = (SerializedPrecMatrix) in.readObject();
    in.close();
    return pData;
  }

}
