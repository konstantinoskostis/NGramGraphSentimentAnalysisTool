package gr.demokritos.iit.nomad.sentimentanalysis;

import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import java.util.ArrayList;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author konstantinos
 * 
 * This class represents a text document.
 */

public class TextDocument {
    
    
    /**
     * the source of the document
     * like Facebook , Tweeter , Google+, or a blog
     */
    private String source;
    
    /**
     * the sentiment can be categorized as
     * positive,neutral or negative
     */
    private String sentiment;
    
    /**
     * when representing a text document with an n-gram graph and
     * comparing this graph with a representative graph then, there
     * exists a similarity between the two. This similarity may be
     * the value similarity or the containment similarity or NVS or 
     * the overall similarity or all of them.
     */
    
    private ArrayList<Double> similarities;
    private double valueSimilarity;
    private double containmentSimilarity;
    private double overallSimilarity;
    private double NVS;
    
    /**
     * every text document is represented by a graph
     */
    private DocumentNGramSymWinGraph documentGraph;
    
    /**
     * this boolean will be true, when reading files (aka during training)
     * but should be false during classification/prediction.
     */
    private boolean createGraph; //will be true when we read the files that contain text , eg: during classification the "TextDocument" data type will be used but createGraph will be false
    
    public TextDocument(){
        documentGraph = new DocumentNGramSymWinGraph();
        similarities = new ArrayList();
    }
    
    public TextDocument(boolean createGraph){
        similarities = new ArrayList();
        this.createGraph = false;
        if(createGraph == true){
            documentGraph = new DocumentNGramSymWinGraph();
            this.createGraph = true;
        }
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the sentiment
     */
    public String getSentiment() {
        return sentiment;
    }

    /**
     * @param sentiment the sentiment to set
     */
    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public void setValueSimilarity(double valueSimilarity){
        this.valueSimilarity = valueSimilarity;
    }
    
    public double getValueSimilarity(){
        return this.valueSimilarity;
    }
    
    public void setContainmentSimilarity(double containmentSimilarity){
        this.containmentSimilarity = containmentSimilarity;
    }
    
    public double getContainmentSimilarity(){
        return this.containmentSimilarity;
    }
    
    public void setOverallSimilarity(double overallSimilarity){
        this.overallSimilarity = overallSimilarity;
    }
    
    public double getOverallSimilarity(){
        return this.overallSimilarity;
    }
    
    public void setNVS(double NVS){
        this.NVS = NVS;
    }
    
    public double getNVS(){
        return this.NVS;
    }

    public void setText(String text){
        documentGraph.setDataString(text);
    }
    
    /**
     * 
     * @param from , the file to load the data string
     */
    public void loadText(String from){
        try{
            documentGraph.loadDataStringFromFile(from);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getText(){
        return documentGraph.getDataString();
    }
    
    public DocumentNGramSymWinGraph getGraph(){
        if(this.createGraph == true){
            return documentGraph;
        }
        return null;
    }
    
    public void addSimilarity(double similarity){
        similarities.add(similarity);
    }
    
    public ArrayList<Double> getSimilarities(){
        return similarities;
    }
    
    @Override
    public String toString(){
        String result = "";
        result += "Sentiment: " +getSentiment()+"\n";
        if(!getSimilarities().isEmpty()){
           for(Double d : getSimilarities()){
               result += d.toString()+" ";
           } 
        }
        result += "\n***************************************************************\n";
        return result;
    }
    
}
