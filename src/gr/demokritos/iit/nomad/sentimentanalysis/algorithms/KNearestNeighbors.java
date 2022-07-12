/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.algorithms;

import gr.demokritos.iit.nomad.sentimentanalysis.TextDocument;
import gr.demokritos.iit.nomad.sentimentanalysis.utils.Features;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *
 * @author konstantinos
 */
public class KNearestNeighbors implements Algorithm {

    private ArrayList<TextDocument> trainingSet; //a list with text documents, each textDocument contains a similarities list and a sentiment
    private ArrayList<TextDocument> testSet; //same as above
    private ArrayList<String> uniqueSentiments; //discrete categories of sentiments
    private Features feats;//object that reads categories of sentiments from a directory
    private String[] columns; //names of feature 
    private String documentsDirectory; //the directory that contains all the folders with their text documents
    private String resultsDirectory; //the directory where the results are written
    private String suffix; //an integer indicating the number of file/fold_number
    private Scanner trainScanner; //reads the training files
    private Scanner testScanner; //reads the tests files
    private Scanner trueSentimentsScanner; //reads the file that contains the true sentiments of the test files
    private ArrayList<String> trueSentiments; //will hold the true sentiments of a file once we read it
    private Scanner predictedSentimentsScanner; //reads the file that contains the predicted sentiments of the test files
    private ArrayList<String> predictedSentiments; //will hold the predicted sentiments of a file once we read it
    private PrintStream predictedSentimentsWriter; //will write the predicted sentiments of test files
    private PrintStream measuresWriter; //will write measures to a file
    private Scanner measuresReader; //will read all the files that contain measures
    private PrintStream onAverageMeasuresWriter; //will write measures from all folds and the measures on average in a file
    private final int K = 5; //number of nearest neighbours
    private int NUM_OF_FOLDS;

    public KNearestNeighbors(String documentsDirectory, String resultsDirectory, int numberOfFolds) {
        this.documentsDirectory = documentsDirectory;
        this.resultsDirectory = resultsDirectory;
        this.NUM_OF_FOLDS = numberOfFolds;
        feats = new Features(this.documentsDirectory);
        columns = feats.getFeatures();
        uniqueSentiments = feats.getSentiments();
    }

    @Override
    public void loadTrainFile(String fileName) {
        try {
            trainingSet = new ArrayList<>();

            //keep the suffix 
            String copy = fileName;
            String disconnect[] = copy.split("_");
            suffix = disconnect[1];
            //load the training file to an arraylist
            trainScanner = new Scanner(new File(this.resultsDirectory + "/" + fileName));
            String line;
            TextDocument t;

            while (trainScanner.hasNextLine()) {
                line = trainScanner.nextLine(); //read a line from a training file , contains similarities and a sentiment
                t = new TextDocument(false);
                String[] features = line.split(","); //split line
                for (int feature = 0; feature < features.length; feature++) {
                    if (feature == features.length - 1) {
                        t.setSentiment(features[feature]);//the last feature is the sentiment of the post
                    } else {
                        t.addSimilarity(Double.parseDouble(features[feature])); //a double indicating a kind of similarity
                    }
                }
                //add the the training instance to a collection
                trainingSet.add(t);
            }
            //close stream
            trainScanner.close();

            System.out.println("Τrain file is loaded!!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void train() {
        System.out.println("KNearestNeighbors does not need training");
    }

    @Override
    public void loadTestFile(String fileName) {
        try {
            testSet = new ArrayList<>(); //will contain the test instances

            //keep the suffix 
            String copy = fileName;
            String disconnect[] = copy.split("_");
            suffix = disconnect[1];
            //load the training file to an arraylist
            testScanner = new Scanner(new File(this.resultsDirectory + "/" + fileName));
            String line;
            TextDocument t;

            while (testScanner.hasNextLine()) {
                line = testScanner.nextLine(); //read a line from a training file , contains similarities and a sentiment
                t = new TextDocument(false);
                String[] features = line.split(","); //split line
                for (int feature = 0; feature < features.length; feature++) {
                    t.addSimilarity(Double.parseDouble(features[feature])); //a double indicating a kind of similarity
                }
                //add the the training instance to a collection
                testSet.add(t);
            }
            //close stream
            testScanner.close();
            
            System.out.println("Test file is loaded");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void test() {
        try {
            predictedSentimentsWriter = new PrintStream(new File(this.resultsDirectory + "/KNNpredicted_" + suffix));
            ArrayList<String> sentimentsOfTrainInstances = new ArrayList<>(); //will hold the sentiment of every training instance
  
            for (TextDocument testInstance : testSet) {
                ArrayList<String> kNearestSentiments = new ArrayList<>(); //will hold the sentiments of the k nearest neighbours of testInstance
                ArrayList<Double> allDistances = new ArrayList<>(); //will hold all distances of testInstace from all the trainInstances
                TreeMap<String, Integer> frequenciesOfNearestSentiments = new TreeMap<>(); //wiil hold the frequencies of the nearest sentiments , using this we will decide what sentiment shoud we give to the testInstance
    
                // initialize frequencies with zero
                for (String sentiment : uniqueSentiments) {
                    frequenciesOfNearestSentiments.put(sentiment, 0);
                }

                for (int trainIndex = 0; trainIndex < trainingSet.size(); trainIndex++) {
                    //fill sentiment
                    sentimentsOfTrainInstances.add(trainingSet.get(trainIndex).getSentiment());
                    allDistances.add(euclideanDistance(testInstance, trainingSet.get(trainIndex))); //find distances
                }

                //sort lists
                comb_sort(allDistances, sentimentsOfTrainInstances);

                //choose the top K sentiments
                for (int i = 0; i < K; i++) {
                    kNearestSentiments.add(sentimentsOfTrainInstances.get(i));
                }

                predictedSentimentsWriter.println(assignSentiment(frequenciesOfNearestSentiments, kNearestSentiments));
                predictedSentimentsWriter.flush();
            }

            predictedSentimentsWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    public void calculateMeasures() {
        //first load the file of true sentiments and then the file of predicted sentiments , to lists
        try {
            trueSentimentsScanner = new Scanner(new File(this.resultsDirectory + "/trueSentiments_" + suffix));
            trueSentiments = new ArrayList<>();
            String trueSentimentLine;

            while (trueSentimentsScanner.hasNextLine()) {
                trueSentimentLine = trueSentimentsScanner.nextLine();
                if (!trueSentimentLine.isEmpty()) {
                    trueSentiments.add(trueSentimentLine);
                }
            }

            trueSentimentsScanner.close();

            predictedSentimentsScanner = new Scanner(new File(this.resultsDirectory + "/KNNpredicted_" + suffix));
            predictedSentiments = new ArrayList<>();
            String predictedSentimentLine;

            while (predictedSentimentsScanner.hasNextLine()) {
                predictedSentimentLine = predictedSentimentsScanner.nextLine();
                if (!predictedSentimentLine.isEmpty()) {
                    predictedSentiments.add(predictedSentimentLine);
                }
            }

            predictedSentimentsScanner.close();

            // find true and false (neg,pos and/or neural)
            TreeMap<String, Double> measures = new TreeMap<>();
            String trueLabel = "true_";
            String falseLabel = "false_";
            for (String key : uniqueSentiments) {
                measures.put(trueLabel + key, 0.d);
                measures.put(falseLabel + key, 0.d);
            }//add entries to the measures map

            for (String key : uniqueSentiments) {
                for (int index = 0; index < predictedSentiments.size(); index++) {
                    if (predictedSentiments.get(index).equalsIgnoreCase(key)) { //if predicted sentiment matches the one we are looking for then we have two cases
                        if (trueSentiments.get(index).equalsIgnoreCase(predictedSentiments.get(index))) { //if true sentiment and predicted sentiment match
                            measures.put(trueLabel + key, measures.get(trueLabel + key) + 1); //add 1 to the true of this category if prediction was correct
                        } else { //if true sentiment and predicted sentiment do not match
                            measures.put(falseLabel + key, measures.get(falseLabel + key) + 1); //add 1 to the false of this category if prediction was incorrect
                        }
                    }
                }
            } //for every sentiment calculate true and false

            //find accuracy
            double accuracy;
            int numerator = 0;
            int denominator = 0;

            for (String key : measures.keySet()) {
                if (key.startsWith("true")) {
                    numerator += measures.get(key);
                }
                denominator += measures.get(key);
            }

            if (denominator == 0) {
                accuracy = 0.d;
            } else {
                accuracy = (double) numerator / denominator;
            }
            measures.put("accuracy", accuracy);

            //write measures to a file
            measuresWriter = new PrintStream(new File(this.resultsDirectory + "/KNNmeasures_" + suffix));
            for (String key : measures.keySet()) {
                measuresWriter.println(key + " " + measures.get(key));
            }
            measuresWriter.close();

            System.out.println("KNN measures file written!!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void aggregateMeasures() {
        try {
            //clear resources
            this.clearResources();

            TreeMap<String, Double> measures = new TreeMap<>(); //will store values of measures
            onAverageMeasuresWriter = new PrintStream(new File(this.resultsDirectory + "/KNNmeasuresOnAverage"));

            for (int iteration = 0; iteration < NUM_OF_FOLDS; iteration++) {
                measuresReader = new Scanner(new File(this.resultsDirectory + "/KNNmeasures_" + iteration)); //open file
                String line;
                while (measuresReader.hasNextLine()) {
                    line = measuresReader.nextLine();//read a line
                    onAverageMeasuresWriter.println(line); //write the line to results file
                    if (!line.isEmpty()) {
                        String components[] = line.split(" ");
                        String measureName = components[0];
                        Double measureValue = Double.parseDouble(components[1]);
                        if (!measures.containsKey(measureName)) {
                            measures.put(measureName, measureValue); //add a new key
                        } else {
                            measures.put(measureName, measures.get(measureName) + measureValue); //update value of key
                        }
                    }
                }

                //flush the stream we write at
                onAverageMeasuresWriter.println("--------------------------------------------------------------------------------");
                onAverageMeasuresWriter.flush();
            }

            //we read all files and in measures tree map we have summed all the different measures
            //divide all the sums by the number of folds and write this value as an average value of a measure to the results file
            onAverageMeasuresWriter.println("Measures on average:");

            for (String key : measures.keySet()) {
                if(key.equalsIgnoreCase("accuracy")){
                    onAverageMeasuresWriter.println(key + " " + measures.get(key)/(double) NUM_OF_FOLDS);
                }else{
                    onAverageMeasuresWriter.println(key + " " + measures.get(key));
                }  
            }

            onAverageMeasuresWriter.flush();
            onAverageMeasuresWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * **************************** HELPERS ************************************
     */
    /**
     *
     * @param testInstance a testInstance
     * @param trainInstance a training instance
     * @return the Euclidean distance between two instances
     */
    private double euclideanDistance(TextDocument testInstance, TextDocument trainInstance) {
        double distance = 0.d;
        for (int feature = 0; feature < columns.length; feature++) {
            distance += Math.pow((testInstance.getSimilarities().get(feature) - trainInstance.getSimilarities().get(feature)), 2);
        }
        return Math.sqrt(distance);
    }

    /**
     *
     * @param distances , list of Euclidean distances
     * @param sentiments , list of sentiments
     */
    private void sort(ArrayList<Double> distances, ArrayList<String> sentiments) {
        for (int pass = 0; pass < distances.size() - 1; pass++) {
            for (int j = 0; j < distances.size() - 1; j++) {
                if (distances.get(j) > distances.get(j + 1)) {
                    //swap distances
                    double distance = distances.get(j);
                    distances.set(j, distances.get(j + 1));
                    distances.set(j + 1, distance);

                    //also swap sentiments
                    String sentiment = sentiments.get(j);
                    sentiments.set(j, sentiments.get(j + 1));
                    sentiments.set(j + 1, sentiment);
                }
            }
        }
    }//bubble-sort
    
    /**
     * improves on bubble-sort
     * @param distances
     * @param sentiments 
     */
    private void comb_sort(ArrayList<Double> distances, ArrayList<String> sentiments){
        float shrink = 1.3f;
        int gap = distances.size();
        boolean swapped = false;
        int size = distances.size();
        
        while((gap > 1) || swapped){
            if(gap > 1){
                gap = (int)((float)gap/shrink);
            }
            swapped = false;
            for(int i = 0; i+gap < size; ++i){
                if(distances.get(i) - distances.get(i+gap) > 0){
                    //swap distances
                    double distance = distances.get(i);
                    distances.set(i, distances.get(i + gap));
                    distances.set(i+gap, distance);

                    //also swap sentiments
                    String sentiment = sentiments.get(i);
                    sentiments.set(i, sentiments.get(i + gap));
                    sentiments.set(i+gap, sentiment);
                    
                    swapped = true;
                }
            }//for
        }//while
    }

    /**
     *
     * @param frequenciesOfNearestSentiments , a tree map to keep frequency of
     * every sentiment
     * @param sentiments , a list of sentiments
     * @return the sentiment that appears more frequently
     */
    private String assignSentiment(TreeMap<String, Integer> frequenciesOfNearestSentiments, ArrayList<String> sentiments) {
        for (String key : frequenciesOfNearestSentiments.keySet()) {
            int frequency = 0;
            for (int sentimentIndex = 0; sentimentIndex < sentiments.size(); sentimentIndex++) {
                if (key.equalsIgnoreCase(sentiments.get(sentimentIndex))) { //if key exists in sentiments then count how many times it appears in the list
                    ++frequency;
                }
            }
            frequenciesOfNearestSentiments.put(key, frequency);
        }
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<Integer> frequencies = new ArrayList<>();
        for (String key : frequenciesOfNearestSentiments.keySet()) {
            keys.add(key);
            frequencies.add(frequenciesOfNearestSentiments.get(key));
        }
        //find max
        int maxPos = 0;
        int maxElement = frequencies.get(maxPos);
        for (int i = 1; i < frequencies.size(); i++) {
            if (frequencies.get(i) > maxElement) {
                maxPos = i;
                maxElement = frequencies.get(i);
            }
        }
        String sentiment = keys.get(maxPos);

        return sentiment;
    }
    
    private void clearResources(){
        trainingSet.clear();
        trainingSet = null;
        
        testSet.clear();
        testSet = null;
        
        trueSentiments.clear();
        trueSentiments = null;
        
        predictedSentiments.clear();
        predictedSentiments = null;
        
        System.gc();
        
        System.out.println("Cleared resources!!!");
    }
}
