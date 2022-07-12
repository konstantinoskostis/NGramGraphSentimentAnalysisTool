/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis;


import gr.demokritos.iit.jinsect.documentModel.representations.DocumentNGramSymWinGraph;
import java.util.ArrayList;
import java.io.PrintStream;

/**
 *
 * @author konstantinos
 */
public class RepresentativeGraph {
    //the documents from which we create a representative graph
    private ArrayList<TextDocument> trainingInstances;
    private DocumentNGramSymWinGraph represenativeGraph;

    
    /**
     * Initialize the representative graph as a DocumentNGramSymWinGraph object
     */
    public RepresentativeGraph(){
        this.trainingInstances = new ArrayList<TextDocument>();
        this.represenativeGraph = new DocumentNGramSymWinGraph();
    }
    
    /**
     * Constructor
     * 
     * @param trainingInstances , a list of training instances of a class (negative,positive,neutral)
     * from which the representative graph will be constructed
     */
    public RepresentativeGraph(ArrayList<TextDocument> trainingInstances){
        this.setTrainingInstances(trainingInstances);
        this.represenativeGraph = null;
        createRepresentativeGraph();
    }

    /**
     * 
     * @param trainingInstances
     * set the training instances to be used for the representative graph
     */
    public void setTrainingInstances(ArrayList<TextDocument> trainingInstances){
        this.trainingInstances = trainingInstances;
    }
    
    /**
     * Merges all the graphs of the training instances to
     * a representative graph
     */
    private void createRepresentativeGraph(){
        System.out.println("Start merging...");

        int mergeCount = 0;
        while(mergeCount < trainingInstances.size()) {
            if(represenativeGraph == null){
                represenativeGraph = trainingInstances.get(mergeCount).getGraph();
            }else{
                represenativeGraph.merge(
                    trainingInstances.get(mergeCount).getGraph(),
                    (1.0/(1.0+mergeCount))
                );
            }
            System.out.println("Merging No "+mergeCount);
            ++mergeCount;
        }

        System.out.println("Merging finished.");
    }
    
    /**
     * 
     * @return the representative graph as a DocumentNGramSymWinGraph object
     */
    public DocumentNGramSymWinGraph getRepresentativeGraph(){
        return represenativeGraph;
    }
   
}
