/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.algorithms;


/**
 * This is a simple interface that introduces some
 * contract-methods.
 * Any algorithm must implement this interface
 * 
 * @author konstantinos
 */
public interface Algorithm {
    public void loadTrainFile(String fileName); //loads a file with data to train
    public void train(); //implements/runs a machine learning algorithm given training data
    public void loadTestFile(String fileName); //loads a file with test data
    public void test(); //tries to predict the category of test files
    public void calculateMeasures(); //calculate the number of true positives,true negatives, etc...
    public void aggregateMeasures(); //computes the evaluation measures on average
}
