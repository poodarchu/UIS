package chosen.nlp.lda.model;

import java.io.IOException;

import chosen.nlp.lda.util.Doc;
import chosen.nlp.lda.util.DocBase;
import chosen.nlp.lda.util.Documents;

public interface LDA {

  /* (non-Javadoc)
   * @see chosen.nlp.lda.model.LDA#Train()
   */
  public abstract void Train();

  public abstract void getParameter();

  /* (non-Javadoc)
   * @see chosen.nlp.lda.model.LDA#Initialize(chosen.nlp.lda.util.Documents)
   */
  public abstract void Initialize(Documents docSet);

  public abstract void SaveEstimatedParameters();

  public abstract void Save(int iters, Documents docSet) throws IOException;

  public abstract int Sample(int m, int n);

  public abstract void SampleAll();

  void Initialize(Doc docSet);

  void Save(int iters, DocBase docSet) throws IOException;

  void Save(int iters) throws IOException;

}