/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis.algorithms;

import gr.demokritos.iit.nomad.sentimentanalysis.TextDocument;
import gr.demokritos.iit.nomad.sentimentanalysis.utils.Features;
import gr.demokritos.iit.nomad.sentimentanalysis.utils.Instance;
import gr.demokritos.iit.nomad.sentimentanalysis.utils.Probabilities;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/**
 * This class was never used during experiments
 *
 * @author konstantinos
 */
public class NaiveBayesDiscretize implements Algorithm {

    private ArrayList<TextDocument> trainingSet; //the arraylist that contains all info from trainData files
    private ArrayList<Instance> trainInstances; // the above list is transformed to nominals and this info goes to trainInstances list
    private ArrayList<TextDocument> testSet; //the arraylist that contains all info from testData files
    private ArrayList<Instance> testInstances; //the above list is tranformed to numerical to nominals
    private Scanner trainScanner; //will scan the the trainData files
    private Scanner testScanner; //will scan testData files
    private Scanner modelScanner; //will read a model
    private PrintStream modelWriter; //will write a model to a file
    private PrintStream testResultWriter; //will write the result of tests to a file
    private String directory; //the directory where we read from and where we save to
    private String suffix; //is the suffix of the file we read eg: _0,_1,_2 etc(we will remove the '_')
    private HashMap<String, Probabilities> conditionalProbabilities; //will hold the conditional probabilities of nominal strings
    private Features feats;//object that reads categories of sentiments from a directory
    private String[] columns; //names of feature columns
    private HashMap<String, Integer> frequenciesOfSentiments;

    public NaiveBayesDiscretize(String resultsDirectory, String categoriesDirectory) {

        this.directory = resultsDirectory;
        feats = new Features(categoriesDirectory);
        columns = feats.getFeatures();
    }

    @Override
    public void loadTrainFile(String fileName) {
        try {
            //init the arraylist which will store the file we read
            trainingSet = new ArrayList<TextDocument>();

            //keep the suffix 
            String copy = fileName;
            String disconnect[] = copy.split("_");
            suffix = disconnect[1];
            //load the training file to an arraylist
            trainScanner = new Scanner(new File(this.directory + "/" + fileName));
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
        //init the list that will hold the transformed instances
        trainInstances = new ArrayList<Instance>();

        //create as many instances as text documents are
        for (int i = 0; i < trainingSet.size(); i++) {
            Instance instance = new Instance();
            instance.setSentiment(trainingSet.get(i).getSentiment());
            trainInstances.add(instance);
        }

        ArrayList<Double> tempFeatureColumn = new ArrayList<Double>(); // an arraylist that reperesents a column

        //first we must tranform the numerical values to string labels
        for (int feature = 0; feature < columns.length; feature++) {
            for (int numInstance = 0; numInstance < trainingSet.size(); numInstance++) {
                tempFeatureColumn.add(trainingSet.get(numInstance).getSimilarities().get(feature));
            }
            transformColumn(tempFeatureColumn, feature, true);
            tempFeatureColumn.clear();
        }

        System.out.println("Transformation from numerical data to nominals is done.");

        //compute the probability model of naive bayes

        //now we must find the conditional probabilities of the nominals

        //initialize a Hashmap , key -> nominal , value -> ProbaProbabilities objectbilities object
        conditionalProbabilities = new HashMap<String, Probabilities>();

        //initialize a Hashmap , key -> sentiment , value -> frequency
        frequenciesOfSentiments = new HashMap<String, Integer>();

        //an arraylist of sentiments as a collumn
        ArrayList<String> sentiments = new ArrayList<String>();
        for (int numInstance = 0; numInstance < trainInstances.size(); numInstance++) {
            sentiments.add(trainInstances.get(numInstance).getSentiment());
        }

        //find all different sentiments and count their frequency
        HashSet<String> differentSentiments = new HashSet<String>(sentiments); //hashset contains all different sentiments
        //now find their frequencies
        for (String key : differentSentiments) {
            frequenciesOfSentiments.put(key, findSentimentFrequency(key));
        }

        //iterate over the features and calculate conditional probabilities
        ArrayList<String> tempColumnNominals = new ArrayList<String>();

        for (int feature = 0; feature < columns.length; feature++) {
            for (int numInstance = 0; numInstance < trainInstances.size(); numInstance++) {
                tempColumnNominals.add(trainInstances.get(numInstance).getFeatures().get(feature)); //fill the arraylist with nominals from a column
                //findConditionalProbabilities(tempColumnNominals, sentiments);
            }
            findConditionalProbabilities(tempColumnNominals, sentiments);
            tempColumnNominals.clear();
        } //now we have found conditional probabilities of the nominals

        //add the probabilities of sentiments
        for (String key : frequenciesOfSentiments.keySet()) {
            Probabilities classProbability = new Probabilities();
            classProbability.addProbability(findSentimentProbability(key));
            conditionalProbabilities.put(key, classProbability);
        }

        System.out.println("Model is calculated!!!");

        //now the hashmap 'conditionalProbabilities contains the model', we will write it to a file
        String modelFileName = directory + "/model_" + suffix; //this is the name of the file that we will store the model of the training file we read
        try {
            System.out.println("Start writing model...");
            modelWriter = new PrintStream(new File(modelFileName));
            for (String key : conditionalProbabilities.keySet()) {
                modelWriter.println(key + " " + conditionalProbabilities.get(key).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            modelWriter.close();
            System.out.println("Model is written to file!!!");
            /*System.out.println("Clearing resources...");
             clearResources();
             System.out.println("Resources are cleared...");*/
        }

    }

    @Override
    public void loadTestFile(String fileName) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        try {

            String copy = fileName;
            String disconnect[] = copy.split("_");
            suffix = disconnect[1];

            //load the test file to an arraylist
            testSet = new ArrayList<>();
            testScanner = new Scanner(new File(this.directory + "/" + fileName));
            String line;
            TextDocument t;
            while (testScanner.hasNextLine()) {
                line = testScanner.nextLine(); //read a line from a training file , contains similarities and a sentiment
                t = new TextDocument(false);
                String[] features = line.split(","); //split line
                for (int feature = 0; feature < features.length; feature++) {

                    t.addSimilarity(Double.parseDouble(features[feature])); //a double indicating a kind of similarity
                }
                testSet.add(t);
            }
            //close stream
            testScanner.close();

            System.out.println("Test file is loaded!!!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void test() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        try {
            //initialize an arraylist to store the transformed test data
            testInstances = new ArrayList<>();

            //create as many instances as testSet contains
            for (int i = 0; i < testSet.size(); i++) {
                Instance instance = new Instance();
                testInstances.add(instance);
            }

            ArrayList<Double> tempFeatureColumn = new ArrayList<Double>(); // an arraylist that reperesents a column

            //first we must tranform the numerical values to string labels
            for (int feature = 0; feature < columns.length; feature++) {
                for (int numInstance = 0; numInstance < testSet.size(); numInstance++) {
                    tempFeatureColumn.add(testSet.get(numInstance).getSimilarities().get(feature));
                }
                transformColumn(tempFeatureColumn, feature,false);
                tempFeatureColumn.clear();
            }
            System.out.println("Transformation from numerical data to nominals is done.");
            
            //now load a model from a file to a hashmap
            
            //initialize hashmap
            conditionalProbabilities = new HashMap<>();
            
            //initialize model reader
            modelScanner = new Scanner(new File(this.directory+"/model_"+suffix));
            String line;
            while(modelScanner.hasNextLine()){
                line = modelScanner.nextLine();
                String probs[] = line.split(" ");
                String probString = probs[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void calculateMeasures() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public void aggregateMeasures(){
        
    }
    
    /**
     * ************************************************* SOME PRIVATE HELPER
     * FUNCTIONS *********************************************
     */
    //clear the arraylist and close the stream
    private void clearResources() {
        try {
            trainingSet.clear();
            trainInstances.clear();
            frequenciesOfSentiments.clear();
            conditionalProbabilities.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //find min and max of a column
    private double[] findMinMax(ArrayList<Double> featureColumn) {
        double[] minMax = new double[2];

        //find min value
        double v = featureColumn.get(0);

        for (int i = 1; i < featureColumn.size(); i++) {
            if (featureColumn.get(i) < v) {
                v = featureColumn.get(i);
            }
        }

        //put min to minMax[0]
        minMax[0] = v;

        //find max value
        v = featureColumn.get(0);

        for (int i = 1; i < featureColumn.size(); i++) {
            if (featureColumn.get(i) > v) {
                v = featureColumn.get(i);
            }
        }

        minMax[1] = v;

        return minMax;
    }

    private void transformColumn(ArrayList<Double> featureColumn, int index, boolean train) {
        //min and max of the feature column
        double minMax[] = findMinMax(featureColumn);
        //now devide the values to 10 teams , width = (max-min)/10
        double max = minMax[1];
        double min = minMax[0];
        //find width of buckets
        double width = (max - min) / 10;
        //transform double value to nominal
        for (int i = 0; i < featureColumn.size(); i++) {
            int bucketIndex = (int) Math.floor((float) (featureColumn.get(i) - min) / width); //hash the double value to a bucket
            String label = columns[index] + "_" + bucketIndex; //create the nominal value
            if(train){
                trainInstances.get(i).addFeature(label);//insert the nominal value to the appropriate position
            }else{
                testInstances.get(i).addFeature(label);
            }
        }
    }//boolean train=true means that we will add the nominal to trainInstances , otherwise we will add the nominal to testInstances

    private int findSentimentFrequency(String sentiment) {
        int sentimentFrequency = 0;
        for (int i = 0; i < trainInstances.size(); i++) {
            if (trainInstances.get(i).getSentiment().equalsIgnoreCase(sentiment)) {
                ++sentimentFrequency;
            }
        }
        return sentimentFrequency;
    }//find how many times we saw this sentiment

    private float findSentimentProbability(String sentiment) {
        return (float) findSentimentFrequency(sentiment) / trainInstances.size();
    }//dividing the frequency of a sentiment by the total size of instances we find the probability of a sentiment

    private void findConditionalProbabilities(ArrayList<String> columnNominals, ArrayList<String> sentiments) {
        int possibleDifferentValues = 0; //will be used for laplacia estimator
        ArrayList<String> unique = new ArrayList<String>();
        for (int i = 0; i < columnNominals.size(); i++) {
            if (!unique.contains(columnNominals.get(i))) {
                ++possibleDifferentValues;
                unique.add(columnNominals.get(i));
            }
        }

        for (String u : unique) { //for every unique nominal
            Probabilities p = new Probabilities();
            for (String key : frequenciesOfSentiments.keySet()) { //for every sentiment , calculate conditional probability
                int uSentimentCooccurence = valueSentimentCooccurence(columnNominals, u, sentiments, key);
                double conditionalProbability = (double) (1 + uSentimentCooccurence) / (double) (possibleDifferentValues + frequenciesOfSentiments.get(key));
                p.addProbability(conditionalProbability); // add the conditional probability of u and sentiment to a list
            }
            conditionalProbabilities.put(u, p);
        }

        //clear resource
        unique.clear();
    }

    private int valueSentimentCooccurence(ArrayList<String> featureColumn, String value, ArrayList<String> sentiments, String sentiment) {
        int cooccurence = 0;
        for (int i = 0; i < featureColumn.size(); i++) {
            if (featureColumn.get(i).equalsIgnoreCase(value) && sentiments.get(i).equalsIgnoreCase(sentiment)) {
                ++cooccurence;
            }
        }
        return cooccurence;
    }//find how many times a nominal and a sentiment co-occur
}