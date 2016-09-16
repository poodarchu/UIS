package chosen.social.lda.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.javatuples.Pair;

public class Community implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -3200069514002950050L;
  public List<List<Integer> > CF = new ArrayList<List<Integer>>(); // followers for community
  public List<List<Integer> > CG = new ArrayList<List<Integer>>(); // followees for community 
  
  public List< List< Pair<Integer, Integer> > > CEdgeList = 
      new ArrayList< List <Pair<Integer,Integer> > >(); //edge for community
}
