package org.mymedialite.itemrec;

import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Collections;


  // TODO
  // - fix numerical issues
  //   - bold driver?
  //   - annealing?
  // - run on epinions dataset
  // - support incremental updates
  // - add bias modeling
  // - more fine-grained regularization
  // - parallelize
  // - implement MAP optimization
  
  /// <summary>Collaborative Less-is-More Filtering Matrix Factorization</summary>
  /// <remarks>
  ///   WARNING: Implementation of this recommender is not finished yet.
  /// </remarks>
  /// <exception cref='NotImplementedException'>
  /// Is thrown when a requested operation is not implemented for a given type.
  /// </exception>
  public class CLiMF extends MF
  {
    /// <summary>Regularization parameter</summary>
    public float Regularization ;
    /// <summary>Learning rate; step size for gradient descent updates</summary>
    public float LearnRate ;

    /// <summary>Default constructor</summary>
    public CLiMF()
    {
      Regularization = 0.0f;
      LearnRate = 0.01f;
    }

    double Sig(float d)
    {
      return 1.0 / (1.0 + Math.exp(-d));
    }
    
    @Override
    public void iterate() {
      IntList users = feedback.allUsers();
      Collections.shuffle(users);

      for (int user_id : users)
        UpdateForUser(user_id);
    }
    
    void UpdateForUser(int user_id)
    {
      // compute user gradient ...
      double[] user_gradient = new double[numFactors];
      for(int index : feedback.byUser().get(user_id)) {
        int item_id = feedback.items().getInt(index);
        double sig_neg_score = Sig((float)-predict(user_id, item_id));
        
        for (int f = 0; f < numFactors; f++)
          user_gradient[f] += sig_neg_score * itemFactors.get(item_id, f);
        
        for (int other_index : feedback.byUser().get(user_id) ){
          int other_item_id = feedback.items().getInt (other_index);
          float score_diff = (float) (predict(user_id, other_item_id) - predict(user_id, item_id));
          double sig_score_diff = Sig(score_diff);
          double deriv_sig_score_diff = Sig(-score_diff) * sig_score_diff;
          for (int f = 0; f < numFactors; f++)
            user_gradient[f] += (deriv_sig_score_diff / (1 - sig_score_diff)) * (itemFactors.get(item_id, f) - itemFactors.get(other_item_id, f));
        }
      }
      // ... update user factors
      for (int f = 0; f < numFactors; f++) {
        double value = userFactors.get(user_id, f);
        value -= (float) (LearnRate * (user_gradient[f] - Regularization * userFactors.get(user_id, f) ));
        userFactors.set(user_id, f, value);
      }

      for  (int index : feedback.byUser().get(user_id))
      {
        int item_id = feedback.items().get(index);
        // compute item gradient ...
        double sig_neg_score = Sig((float)-predict(user_id, item_id)); // TODO speed up: score every item just ince
        double[] item_gradient = new double[numFactors];
        for (int f = 0; f < numFactors; f++)
          item_gradient[f] = sig_neg_score;
        for (int other_index : feedback.byUser().get(user_id))
        {
          int other_item_id = feedback.items().getInt(other_index);
          float score_diff = (float) (predict(user_id, item_id) - predict(user_id, other_item_id));
          double sig_score_diff = Sig(score_diff);
          double sig_score_neg_diff = Sig(-score_diff);
          double deriv_sig_score_diff = Sig(-score_diff) * sig_score_diff;
          double a = 1.0 / (1.0 - sig_score_neg_diff);
          double b = 1.0 / (1.0 - sig_score_diff);
          double x = deriv_sig_score_diff * (a - b);
          for (int f = 0; f < numFactors; f++)
            item_gradient[f] += x;
        }
        for (int f = 0; f < numFactors; f++)
          item_gradient[f] *= userFactors.get(user_id, f);

        // ... update item gradient
        for (int f = 0; f < numFactors; f++) {
          double value = itemFactors.get(item_id, f);
          value -= (float) (LearnRate * (item_gradient[f] - Regularization * itemFactors.get(item_id, f) ) );
          itemFactors.set(item_id, f, value);
        }
      }
    }

/*
    @Override
    public String toString()
    {
      return String.Format(
        CultureInfo.InvariantCulture,
        "{0} num_factors={1} regularization={2} num_iter={3} learn_rate={4}",
        this.GetType().Name, num_factors, Regularization, NumIter, LearnRate);
    }
*/
    @Override
    public double computeLoss() {
      // TODO Auto-generated method stub
      return 0;
    }
  }

