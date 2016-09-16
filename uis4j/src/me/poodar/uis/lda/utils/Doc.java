package me.poodar.uis.lda.utils;

public interface Doc {

  public abstract void readDocs(String docsPath);

  public abstract void readStructuredDocs(String docsPath, String delimiter);
  
  public abstract Integer getIndex(String key) ;
  
  public abstract Integer getWordSize ();
  
  public abstract Boolean contains(String key);
}