/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.utils;

/**
 * It will contain two values of type double
 * mean and standard deviation
 * @author konstantinos
 */
public class Stats {
    
    private double mean;
    private double standardDeviation;
    
    public Stats(){
        this(0.d,0.d);
    }
    
    public Stats(double mean,double standardDeviation){
        setMean(mean);
        setStandardDeviation(standardDeviation);
    }
    
    public void setMean(double mean){
        this.mean = mean;
    }
    
    public double getMean(){
        return this.mean;
    }
    
    public void setStandardDeviation(double standardDeviation){
        this.standardDeviation = standardDeviation;
    }
    
    public double getStandardDeviation(){
        return this.standardDeviation;
    }
    
    @Override
    public String toString(){
        return mean+","+standardDeviation;
    }
}
