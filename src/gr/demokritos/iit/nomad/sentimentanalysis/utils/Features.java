/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author konstantinos
 */
public class Features {
    
    private ArrayList<String> sentiments; //all the sentiments(positive,negative,neutral or a subset of these)
    private String directory; //directory of examples , use it to retrieve the names of folders because these names represent the names of sentiments
    
    public Features(String directory){
        this.directory = directory;
        sentiments = new ArrayList<String>();
        loadSentiments(sentiments);
        Collections.sort(sentiments);
    }
    
    private File openDirectory() {
        File dir = new File(this.directory);
        if (dir.exists() && dir.isDirectory()) {
            return dir; //return the File everything is ok
        }
        return null; //else return null
    }
    
    private void loadSentiments(ArrayList<String> sentiments){
        File dir = openDirectory();
        File [] categories = dir.listFiles();
        for(File f : categories){
            sentiments.add(f.getName()); //get the name of each folder/sentiment
        }
    }
    
    public ArrayList<String> getSentiments(){
        return sentiments;
    }
    
    public String[] getFeatures(){
        
        ArrayList<String> features = new ArrayList<String>();
        //for each sentiment we have VS(value similarity),CS(Containment similarity)
        //OS(Overall similarity) and NVS(Normalized value similarity)
        for(String sentiment : sentiments){
            features.add(sentiment+"ReprVS");
            features.add(sentiment+"ReprCS");
            features.add(sentiment+"ReprOS");
            features.add(sentiment+"ReprNVS");
        }
        
        String [] allFeatures = new String[features.size()];
        for(int i=0; i<features.size(); i++){
            allFeatures[i] = features.get(i);
        }
        return allFeatures;
    }
    
}
