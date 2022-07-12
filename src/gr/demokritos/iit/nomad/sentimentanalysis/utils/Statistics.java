/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.utils;

import java.util.ArrayList;

/**
 *
 * @author konstantinos
 */
public class Statistics {
    
    private ArrayList<Double> statistics;
    
    public Statistics(){
        this.statistics = new ArrayList<>();
    }
    
    public void addStatistic(double s){
        this.statistics.add(s);
    }
    
    public ArrayList<Double> getStatistics(){
        return this.statistics;
    }
    
    public String toString(){
        String result = "";
        for(int i=0; i<statistics.size(); i++){
            if(i == statistics.size()-1){
                result += statistics.get(i);
            }else{
                result += statistics.get(i)+",";
            }
        }
        return result;
    }
    
}
