/*
 * Stringo change this template, choose Stringools | Stringemplates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.utils;

import java.util.ArrayList;

/**
 * This class is used from the implementations of the
 * algorithm interface to represent an instance
 * that it's values were transformed from numerical to strings
 * ( USED By NaiveBayesDiscretize -- DO NOT USE NaiveBayesDiscretize :-) )
 * @author konstantinos
 */
public class Instance {
    
    private ArrayList<String> features; //an arraylist of string features
    private String sentiment; //will be probably used only for the training instances
    
    public Instance(){
        features = new ArrayList<String>();
    }
    
    public void addFeature(String feature){
        features.add(feature);
    }//add a feature to an instance
    
    public ArrayList<String> getFeatures(){
        return features;
    }//return the arraylist of the features of the instance
    
    public void setSentiment(String sentiment){
        this.sentiment = sentiment;
    }//set the sentiment of an instance
    
    public String getSentiment(){
        return this.sentiment;
    }//return the sentiment of an instance
    
    public void set(int index,String value){
        features.set(index, value);
    }//set an element to position i
    
    @Override
    public String toString(){
       String result = "";
       result += "Sentiment: "+getSentiment()+"\n";
       for(int i=0; i<features.size(); i++){
           result += features.get(i)+" ";
       }
       result += "\n*************************************************************************************\n";
       return result;
    }//
}
