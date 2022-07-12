/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis;

import java.io.PrintStream;
import java.util.ArrayList;
import gr.demokritos.iit.jinsect.documentModel.comparators.NGramCachedGraphComparator;
import gr.demokritos.iit.jinsect.structs.GraphSimilarity;
import java.util.TreeMap;

/**
 *
 * @author konstantinos
 */
public class ResultsWriter {

    private PrintStream ps; //the stream to write to
    private NGramCachedGraphComparator graphComparator; //the graph comparator
    private GraphSimilarity graphSimilarity;

    public ResultsWriter(PrintStream ps) {
        this.ps = ps;
        this.graphComparator = new NGramCachedGraphComparator();
    }

    
    public void compareEveryClassWithAllRepresentativeGraphs(TreeMap<String,ArrayList<TextDocument>> dataSets,
            TreeMap<String,RepresentativeGraph> representativeGraphs, boolean training){
        System.out.println("Comparing...");

        for(String key : dataSets.keySet()){
            writeInstancesOfClass(dataSets.get(key), representativeGraphs, training);
        }

        System.out.println("End Comparing...");
    }

    public void writeInstancesOfClass(ArrayList<TextDocument> classInstances, 
            TreeMap<String, RepresentativeGraph> representativeGraphs, boolean training) {
        //compare the arraylist with every representative graph
        
        for (TextDocument t : classInstances) { // for every text instance
            for (String key : representativeGraphs.keySet()) { //for every representative graph
                RepresentativeGraph graph = representativeGraphs.get(key); //get a representative graph 
                this.graphSimilarity = this.graphComparator.getSimilarityBetween(t.getGraph(), graph.getRepresentativeGraph());
                //compare instance with all classes and set the similarities
                t.addSimilarity(this.graphSimilarity.ValueSimilarity);
                t.addSimilarity(this.graphSimilarity.ContainmentSimilarity);
                t.addSimilarity(this.graphSimilarity.getOverallSimilarity());
                double NVS = (this.graphSimilarity.SizeSimilarity == 0.d) ? 0.d : (this.graphSimilarity.ValueSimilarity / this.graphSimilarity.SizeSimilarity);
                t.addSimilarity(NVS);
            }

            //write the instance to file
            String featureVector = "";
            for (int i = 0; i < t.getSimilarities().size(); i++) {
                if (i == t.getSimilarities().size() - 1) {
                    featureVector += t.getSimilarities().get(i);
                } else {
                    featureVector += t.getSimilarities().get(i) + ",";
                }
            }//add all the similarities as features

            if (training == true) {
                featureVector += "," + t.getSentiment();
            }//if this boolean training is true then write the senetiment to the file , else we have a test instance and we don't want to write it's sentiment

            ps.println(featureVector);
            ps.flush();
        }
    }
    
    public void writeTrueSentimentsOfTests(TreeMap<String,ArrayList<TextDocument>> test){
        
        for(String key : test.keySet()){
            for(TextDocument testInstance : test.get(key)){
                ps.println(testInstance.getSentiment());
            }
        }
        
    }
}
