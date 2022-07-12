/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.utils;

import java.util.ArrayList;

/**
 * This class will hold the conditional probabilities of nominal strings
 * @author konstantinos
 */
public class Probabilities {
    
    private ArrayList<Double> conditionalProbabilities;
    
    public Probabilities(){
        conditionalProbabilities = new ArrayList<Double>();
    }
    
    public void addProbability(double probability){
        conditionalProbabilities.add(probability);
    }
    
    public ArrayList<Double> getProbabilities(){
        return conditionalProbabilities;
    }
    
    
    public String toString(){
        String result = "";
        for(int i=0; i<conditionalProbabilities.size(); i++){
            if(i == conditionalProbabilities.size()-1){
                result += conditionalProbabilities.get(i);
            }else{
                result += conditionalProbabilities.get(i)+",";
            }
        }
        return result;
    }
    
}
