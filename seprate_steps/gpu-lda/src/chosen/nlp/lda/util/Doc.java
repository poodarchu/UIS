package chosen.nlp.lda.util;

public interface Doc {

  public abstract void readDocs(String docsPath); //read data from file path
  public abstract void readStructuredDocs(String docsPath, String delimiter);//read structured data form file path
  public abstract Integer getIndex(String key); //get index corresponding to the specific ID
  public abstract Integer getWordSize (); //get current word size
  public abstract Boolean contains(String key); //judge whether this doc contains the key(ID)
}