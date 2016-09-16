package me.poodar.uis.lda.test;

public class ExtractTest {
  public static void main(String args[]) {
    ExtractLatentLink extract = new ExtractLatentLink();
    extract.getDocs();
    extract.extract(args[0]);
  }
}
