// Copyright (C) 2010 Zeno Gantner, Chris Newell
//
// This file is part of MyMediaLite.
//
// MyMediaLite is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// MyMediaLite is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with MyMediaLite.  If not, see <http://www.gnu.org/licenses/>.

package org.mymedialite;

/**
 * Interface representing iteratively trained models.
 * @version 2.03
 */
public interface IIterativeModel {

  /** Set the number of iterations to run the training */
  void setNumIter(int numIter);

  /** Get the number of iterations to run the training */
  int getNumIter();

  /** Run one iteration (= pass over the training data) */
  void iterate();

  /** 
   * Compute the current loss of the model
   * @return the current loss; -1 if not implemented
   */
  double computeLoss();
  
}