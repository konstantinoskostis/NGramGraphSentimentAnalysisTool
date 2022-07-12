/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.algorithms;

import gr.demokritos.iit.nomad.sentimentanalysis.TextDocument;
import gr.demokritos.iit.nomad.sentimentanalysis.utils.Features;
import gr.demokritos.iit.nomad.sentimentanalysis.utils.Stats;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.TreeMap;

/**
 *
 * @author konstantinos
 */
public class NaiveBayesGaussian implements Algorithm {

    private ArrayList<TextDocument> trainingSet;
    private ArrayList<TextDocument> testSet;
    private ArrayList<String> uniqueSentiments;
    private TreeMap<String, Double> sentimentsProbabilities;
    private ArrayList<TreeMap<String, Stats>> stats;
    private Scanner trainScanner;
    private Scanner testScanner;
    private Scanner sentimentsProbabilitiesScanner;
    private Scanner statisticsScanner;
    private PrintStream statisticsWriter;
    private PrintStream sentimentsProbabilityWriter;
    private PrintStream predictedSentimentsWriter;
    private Scanner trueSentimentsScanner; //reads the file that contains the true sentiments of the test files
    private ArrayList<String> trueSentiments; //will hold the true sentiments of a file once we read it
    private Scanner predictedSentimentsScanner; //reads the file that contains the predicted sentiments of the test files
    private ArrayList<String> predictedSentiments; //will hold the predicted sentiments of a file once we read it
    private PrintStream measuresWriter; //will write measures to a file
    private Scanner measuresReader; //will read all the files that contain measures
    private PrintStream onAverageMeasuresWriter; //will write measures from all folds and the measures on average in a file
    private int NUM_OF_FOLDS;
    private String documentsDirectory;
    private String resultsDirectory;
    private String suffix;
    private Features feats;//object that reads categories of sentiments from a directory
    private String[] columns; //names of feature columns

    public NaiveBayesGaussian(String documentsDirectory, String resultsDirectory, int numberOfFolds) {
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
        try {
            //no need to transform numerical data to nominal strings, we can use gaussian distribution
            stats = new ArrayList<>();
            //init a list with the sentiments as they appear
            ArrayList<String> sentiments = new ArrayList<>();
            for (int i = 0; i < trainingSet.size(); i++) {
                sentiments.add(trainingSet.get(i).getSentiment());
            }

            //For every feature/column and sentiment find the mean and standard deviation
            ArrayList<Double> column = new ArrayList<>();
            for (int feature = 0; feature < columns.length; feature++) {
                for (int numInstance = 0; numInstance < trainingSet.size(); numInstance++) {
                    column.add(trainingSet.get(numInstance).getSimilarities().get(feature));
                }
                findStatistics(column, sentiments);
                column.clear();
            }

            //now write the statistics to a file
            statisticsWriter = new PrintStream(new File(this.resultsDirectory + "/statistics_" + suffix));
            for (int i = 0; i < stats.size(); i++) {
                String toWrite = columns[i] + " ";
                for (String sentiment : uniqueSentiments) {
                    toWrite += sentiment + ":" + stats.get(i).get(sentiment).toString() + " ";
                }
                statisticsWriter.println(toWrite);
                statisticsWriter.flush();
            }
            statisticsWriter.close();
            System.out.println("Statistics file is written!!!");

            //calculate and write the probabilities of sentiments
            sentimentsProbabilities = new TreeMap<>();
            for (String sentiment : uniqueSentiments) {
                sentimentsProbabilities.put(sentiment, findSentimentProbability(sentiment, sentiments));
            }
            sentimentsProbabilityWriter = new PrintStream(new File(this.resultsDirectory + "/probabilities_" + suffix));
            for (String key : sentimentsProbabilities.keySet()) {
                sentimentsProbabilityWriter.println(key + " " + sentimentsProbabilities.get(key));
                sentimentsProbabilityWriter.flush();
            }
            sentimentsProbabilityWriter.close();
            System.out.println("Probabilities file is written!!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadTestFile(String fileName) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
                for (String feature : features) {
                    t.addSimilarity(Double.parseDouble(feature)); //a double indicating a kind of similarity
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
            //load the probabilities
            sentimentsProbabilities = new TreeMap<>();
            sentimentsProbabilitiesScanner = new Scanner(new File(this.resultsDirectory + "/probabilities_" + suffix));
            String line;
            while (sentimentsProbabilitiesScanner.hasNextLine()) {
                line = sentimentsProbabilitiesScanner.nextLine();
                String components[] = line.split(" ");
                sentimentsProbabilities.put(components[0], Double.parseDouble(components[1]));
            }
            sentimentsProbabilitiesScanner.close();
            System.out.println("Probabilities file is loaded!!!");

            //load the statistics
            stats = new ArrayList<>();
            statisticsScanner = new Scanner(new File(this.resultsDirectory + "/statistics_" + suffix));
            while (statisticsScanner.hasNextLine()) {
                line = statisticsScanner.nextLine();
                String components[] = line.split(" ");

                TreeMap<String, Stats> statsOfSentiments = new TreeMap<>();

                for (int i = 1; i < components.length; i++) { //element at 0 is the name of the feature
                    String statsComponentLine[] = components[i].split(":");
                    String sentiment = statsComponentLine[0]; //this is a sentiment
                    //now get mean and standard deviation
                    String descriptiveStats[] = statsComponentLine[1].split(",");
                    statsOfSentiments.put(sentiment, new Stats(Double.parseDouble(descriptiveStats[0]), Double.parseDouble(descriptiveStats[1])));
                }

                stats.add(statsOfSentiments);
            }

            statisticsScanner.close();

            System.out.println("Statistics file is loaded!!!");

            //initialize stream to write predictions
            predictedSentimentsWriter = new PrintStream(new File(this.resultsDirectory + "/GNBpredicted_" + suffix));
            System.out.println("Start writing predicted sentiments...");
            System.out.println("Iteration " + suffix);
            for (int i = 0; i < testSet.size(); i++) {
                processTestInstance(testSet.get(i), predictedSentimentsWriter);
            }
            predictedSentimentsWriter.close();

            System.out.println("predictedSetiments file is written!!!");

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
            String sentimentLine;
            while (trueSentimentsScanner.hasNextLine()) {
                sentimentLine = trueSentimentsScanner.nextLine();
                if (!sentimentLine.isEmpty()) {
                    trueSentiments.add(sentimentLine);
                }
            }
            trueSentimentsScanner.close();

            predictedSentimentsScanner = new Scanner(new File(this.resultsDirectory + "/GNBpredicted_" + suffix));
            predictedSentiments = new ArrayList<>();
            while (predictedSentimentsScanner.hasNextLine()) {
                sentimentLine = predictedSentimentsScanner.nextLine();
                if (!sentimentLine.isEmpty()) {
                    predictedSentiments.add(sentimentLine);
                }
            }
            predictedSentimentsScanner.close();

            // find true and false (neg,pos and/or neu)
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
                            measures.put(falseLabel + key, measures.get(falseLabel + key) + 1); //add 1 to the fakse of this category if prediction was incorrect
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
            measuresWriter = new PrintStream(new File(this.resultsDirectory + "/GNBmeasures_" + suffix));
            for (String key : measures.keySet()) {
                measuresWriter.println(key + " " + measures.get(key));
            }
            measuresWriter.close();
            System.out.println("GNB measures file written!!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void aggregateMeasures() {
        try {
            
            //clear all resources used before
            this.clearResources();
            
            TreeMap<String, Double> measures = new TreeMap<>(); //will store values of measures
            onAverageMeasuresWriter = new PrintStream(new File(this.resultsDirectory + "/GNBmeasuresOnAverage"));

            for (int iteration = 0; iteration < NUM_OF_FOLDS; iteration++) {
                measuresReader = new Scanner(new File(this.resultsDirectory + "/GNBmeasures_" + iteration)); //open file
                String line;
                while (measuresReader.hasNextLine()) {
                    line = measuresReader.nextLine();//read a line
                    onAverageMeasuresWriter.println(line); //write the line to results file
                    onAverageMeasuresWriter.flush();
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
     * ************************************* HELPER FUNCTIONS ************************************************
     */
    private void findStatistics(ArrayList<Double> column, ArrayList<String> sentiments) {

        TreeMap<String, Stats> statsOfSentiment = new TreeMap<>();

        for (String sentiment : uniqueSentiments) {
            double mean;
            double stdev;
            double sum_mean = 0;
            double sum_dev = 0;
            double factor;
            int size = 0; //number of instances that their sentiment matches the sentiment we care about
            //find mean
            for (int numInstance = 0; numInstance < column.size(); numInstance++) {
                if (sentiment.equalsIgnoreCase(sentiments.get(numInstance))) {
                    ++size;
                    sum_mean += column.get(numInstance);
                }
            }
            mean = (double) sum_mean / size;

            //find standard deviation
            for (int numInstance = 0; numInstance < column.size(); numInstance++) {
                if (sentiment.equalsIgnoreCase(sentiments.get(numInstance))) {
                    factor = (column.get(numInstance) - mean);
                    sum_dev += factor * factor;
                }
            }
            stdev = Math.sqrt((double) sum_dev / size);

            statsOfSentiment.put(sentiment, new Stats(mean, stdev));
        }

        stats.add(statsOfSentiment);
    }

    private int findSentimentFrequency(String sentiment, ArrayList<String> sentiments) {
        int sentimentFrequency = 0;
        for (int i = 0; i < sentiments.size(); i++) {
            if (sentiment.equalsIgnoreCase(sentiments.get(i))) {
                ++sentimentFrequency;
            }
        }
        return sentimentFrequency;
    }//find how many times we saw this sentiment

    private double findSentimentProbability(String sentiment, ArrayList<String> sentiments) {
        return (double) findSentimentFrequency(sentiment, sentiments) / sentiments.size();
    }//dividing the frequency of a sentiment by the total size of instances we find the probability of a sentiment

    private void processTestInstance(TextDocument testInstance, PrintStream writer) {
        TreeMap<String, Double> finalProbabilities = new TreeMap<>();

        for (String sentiment : uniqueSentiments) { //for every sentiment compute a final probability
            double pr = 1.0d;

            for (int attribute = 0; attribute < columns.length; attribute++) {
                double mean = stats.get(attribute).get(sentiment).getMean();
                double stdev = stats.get(attribute).get(sentiment).getStandardDeviation();
                double valueOfTestInstance = testInstance.getSimilarities().get(attribute);
                double gaussian = gaussian(valueOfTestInstance, mean, stdev);
                pr *= gaussian;
            }//we have found all aposteriori probs for a sentiment ie all P(vi|sentiment) for a test instance

            pr *= sentimentsProbabilities.get(sentiment);
            finalProbabilities.put(sentiment, pr);
        }

        //now that we have found the probabilities for all the sentiments for the testInstance, we must assign a sentiment to it
        writer.println(sentimentAssignment(finalProbabilities));
        writer.flush();
    }

    private double gaussian(double value, double mean, double stdev) {
        double numeratorExp = -((value - mean) * (value - mean));
        double denominatorExp = 2.0d * stdev * stdev;
        double exponent = Math.exp((double) numeratorExp / denominatorExp);

        double gaussian = (double) 1 / (Math.sqrt( 2 * Math.PI * stdev * stdev));
        gaussian = gaussian * exponent;

        return gaussian;
    }

    private String sentimentAssignment(TreeMap<String, Double> probabilities) {
        ArrayList<Double> values = new ArrayList<>(probabilities.values());
        ArrayList<String> sentiments = new ArrayList<>(probabilities.keySet());
        int maxPosition = 0;
        double maxElement = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i) > maxElement) {
                maxElement = values.get(i);
                maxPosition = i;
            }
        }
        return sentiments.get(maxPosition);
    }
    
    private void clearResources(){
        trainingSet.clear();
        trainingSet = null;
        
        testSet.clear();
        testSet = null;
        
        stats.clear();
        stats = null;
        
        sentimentsProbabilities.clear();
        sentimentsProbabilities = null;
        
        trueSentiments.clear();
        trueSentiments = null;
        
        predictedSentiments.clear();
        predictedSentiments = null;
        
        System.gc();
        
        System.out.println("Cleared resources!!!");
    }
}
