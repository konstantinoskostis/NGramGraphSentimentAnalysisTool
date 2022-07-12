/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis;

import gr.demokritos.iit.nomad.sentimentanalysis.algorithms.Algorithm;
import gr.demokritos.iit.nomad.sentimentanalysis.algorithms.AlgorithmID;
import gr.demokritos.iit.nomad.sentimentanalysis.algorithms.NaiveBayesGaussian;
import gr.demokritos.iit.nomad.sentimentanalysis.algorithms.KNearestNeighbors;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.List;
import java.util.Random;

/**
 *
 * @author konstantinos
 */
public class SentimentAnalyzer {
    private int NUM_OF_FOLDS; //indicates the number of folds
    private float trainPercentageForRepr; //the float that indicates the percentage of training set that should be used for representative graphs
    private InstancesReader reader; //the object that reads instaces 
    private TreeMap<String,ArrayList<TextDocument>> dataSets; // a hashmap/treemap where key is class(eg: neg,pos etc) and value is an arraylist that contains docs of a class
    private TreeMap<String,Integer> howManyInFoldForSentiment; //how many instances of every class when we divide by 10
    private TreeMap<String,Integer> howManyForRepresentativeGraphsForSentiment; //how many instances should be used for representative graphs for every class/sentiment
    private String resultsDirectory;
    private PrintStream trainStream; //initialize it evert time we need to write a new trainData file
    private PrintStream testStream; //initialize it evert time we need to write a new testData file
    private PrintStream trueSentimentWriter; //writes true categories of test instances to a file
    private Random trainingSelection; //random selector of instances for representative graphs
    private String dirToRead;
    private int algorithmID; //the id of the algorithm that user wants to apply
    
    public SentimentAnalyzer(String directory, String resultsDirectory, float trainPercentageForRepr, int folds, int algorithmID) {
        this.dirToRead = directory;
        this.NUM_OF_FOLDS = folds;
        this.resultsDirectory = resultsDirectory; //name of directory to write the comparisons of instances with representative graphs
        this.trainPercentageForRepr = trainPercentageForRepr;

        reader = new InstancesReader(directory); //init the instances reader
        reader.readAll(); //read all the instances neg and pos
        dataSets = reader.getAllData();

        checkDivisionByNumOfFolds();
        checkTrainPercentage();

        trainingSelection = new Random();
        
        howManyInFoldForSentiment = new TreeMap<>();
        howManyForRepresentativeGraphsForSentiment = new TreeMap<>();
        
        for(String key : dataSets.keySet()) {
            //set the number of instances in every fold for a given class
            int howManyInFold = (int)(dataSets.get(key).size() / NUM_OF_FOLDS);
            howManyInFoldForSentiment.put(key, howManyInFold);

            //subtract the number of test instances
            int howManyForRepresentativeGraphs = (int)(this.trainPercentageForRepr * (dataSets.get(key).size() - howManyInFold));
            howManyForRepresentativeGraphsForSentiment.put(key, howManyForRepresentativeGraphs);
        }

        createResultsDirectory();

        this.algorithmID = algorithmID; //set the id of the algorithm
    }
    
    public float getTrainPercentageForRepr() {
        return trainPercentageForRepr;
    }

    private void createResultsDirectory() {
        File results = new File(resultsDirectory);
        if (!results.exists()) {
            results.mkdir();
            System.out.println(resultsDirectory + " is created!!!");
        } else {
            System.out.println(resultsDirectory + " already exists!!!");
        }
    }

    /**
     * this method does not run any machine learning algorithm,
     * it will only output necessary files
     * so that these files can be manipulated by an algorithm
     */
    public void begin() {
        this.beginFolding();
    }
    
    /**
     * this method performs the folding, applies a machine learning
     * algorithm and outputs measures for every iteration
     * essentially this method does the whole analysis
     */
    public void analyze(){
        this.begin();

        Algorithm algo = null;
        
        switch(algorithmID){
            case AlgorithmID.KNearestNeighbors:
                algo = new KNearestNeighbors(dirToRead, resultsDirectory, NUM_OF_FOLDS);
                System.out.println("K-Nearest-Neighbors is the chosen algorithm");
                break;
            case AlgorithmID.GaussianNaiveBayes:
                algo = new NaiveBayesGaussian(dirToRead, resultsDirectory, NUM_OF_FOLDS);
                System.out.println("Gaussian-Naive-Bayes is the chosen algorithm");
                break;
        }
        
        //train ,test and calculate measures
        for(int fold=0; fold<NUM_OF_FOLDS; fold++){
            algo.loadTrainFile("trainData_"+fold);
            algo.train();
            algo.loadTestFile("testData_"+fold);
            algo.test();
            algo.calculateMeasures();
        }
        
        //calculate and output measures on average
        algo.aggregateMeasures();
        
        System.out.println("The analysis is done!\nOpen "+this.resultsDirectory+" to see the results!");
    }

    /**
     * checks if number of instances of each category divided by the number of
     * folds gives 0
     */
    private void checkDivisionByNumOfFolds() {
        for(String key : dataSets.keySet()){
            if(dataSets.get(key).size() % NUM_OF_FOLDS != 0){
                System.err.println("The size of "+key+" documents cannot be divided exactly by "+ NUM_OF_FOLDS+"!!!\n"
                    + "Please fix that!!!\nExiting...");
                System.exit(1);
            }
        }

        System.out.println("Ok!!!The number of documents on each category can be divided by "+ NUM_OF_FOLDS+"!!!");
    }

    /**
     * Exits program if the percentage given is invalid
     */
    private void checkTrainPercentage() {
        if (trainPercentageForRepr <= 0 || trainPercentageForRepr > 1) {
            System.err.println("Train percentage must be a positive float number between [0,1]!!!\nExiting");
            System.exit(1);
        } else {
            System.out.println("Train percenatge is valid!!!");
        }
    }

    private void beginFolding(){
        for (int fold = 0; fold < NUM_OF_FOLDS; fold++) {
            foldOperation(fold);
        }
    }
    
    private void foldOperation(int foldNumber){
        
        TreeMap<String,ArrayList<TextDocument>> testInstances = new TreeMap<String,ArrayList<TextDocument>>();
        TreeMap<String,ArrayList<TextDocument>> trainInstances = new TreeMap<String,ArrayList<TextDocument>>();
        TreeMap<String,ArrayList<TextDocument>> allTrainInstances = new TreeMap<String,ArrayList<TextDocument>>();
        TreeMap<String,RepresentativeGraph> representativeGraphs = new TreeMap<String, RepresentativeGraph>();
        
        for (String key : dataSets.keySet()) {
            int testStartIndex = foldNumber * howManyInFoldForSentiment.get(key);
            int testEndIndex = foldNumber * howManyInFoldForSentiment.get(key) + (howManyInFoldForSentiment.get(key) - 1);

            ArrayList<Integer> indicesInBetween = new ArrayList<Integer>();
            for (int i = testStartIndex; i <= testEndIndex; i++) {
                indicesInBetween.add(i);
            }

            List<TextDocument> testList = (List<TextDocument>) dataSets.get(key).subList(testStartIndex, testEndIndex + 1);

            testInstances.put(key, new ArrayList(testList)); //add the test list of a class to hashmap testInstances
            
            ArrayList<Integer> remainingIndices = new ArrayList<Integer>();
            
            ArrayList<TextDocument> allRemaining = new ArrayList<>();
            
            ArrayList<TextDocument> classTrainSet = new ArrayList<TextDocument>();

            for (int i = 0; i < dataSets.get(key).size(); i++) {
                if (!indicesInBetween.contains(i)) {
                    remainingIndices.add(i);
                    allRemaining.add(dataSets.get(key).get(i)); //all instances but test-instances are excluded
                }
            }

            allTrainInstances.put(key, allRemaining); //these will be compared with the representative graphs
            
            //from remaining indices choose randomly the amount that user wants for training
            for (int i = 0; i < howManyForRepresentativeGraphsForSentiment.get(key); i++) { //we need 'howManyForRepresentativeGraphsForSentiment('className')' training instances for neg/pos/neutral
                //pick randomly an index from remaining indices
                int index = trainingSelection.nextInt(remainingIndices.size()); // an index in [0...length(remainingIndices)-1]
                int indexOfInstance = remainingIndices.get(index); //the true index of an instance
                classTrainSet.add(dataSets.get(key).get(indexOfInstance)); //add the appropriate instance to train using representative graphs
                remainingIndices.remove(index); //remove the element at position index , so that we will not choose it again
            }
            
            trainInstances.put(key, classTrainSet); //add the choosen training set of a class to hashmap trainInstances
            
            representativeGraphs.put(key, new RepresentativeGraph(trainInstances.get(key))); //create the representative graph for this class for this fold
        }

        System.out.println("Representative graphs are created!!!!!!!!!!!!!");
        
        //compare each class with all the representative graphs
        //and write the training and test files
        try{
            trainStream = new PrintStream(new File(resultsDirectory + "/trainData_" + foldNumber));
            new ResultsWriter(trainStream).compareEveryClassWithAllRepresentativeGraphs(allTrainInstances, representativeGraphs, true);
            trainStream.close();
            
            testStream = new PrintStream(new File(resultsDirectory + "/testData_" + foldNumber));
            //create testData
            new ResultsWriter(testStream).compareEveryClassWithAllRepresentativeGraphs(testInstances, representativeGraphs, false);
            testStream.close();
 
            trueSentimentWriter = new PrintStream(new File(resultsDirectory + "/trueSentiments_" + foldNumber));
            new ResultsWriter(trueSentimentWriter).writeTrueSentimentsOfTests(testInstances);
            trueSentimentWriter.close();

            System.out.println("fold_" + foldNumber + " is written!!!");
        }catch(Exception e){
            e.printStackTrace();
        }   
    }
}
