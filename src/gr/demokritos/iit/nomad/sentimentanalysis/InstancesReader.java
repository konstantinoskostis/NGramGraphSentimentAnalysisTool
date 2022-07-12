/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.demokritos.iit.nomad.sentimentanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.TreeMap;

/**
 * This class will read text-documents in order to perform sentiment analysis
 *
 * @author konstantinos
 */
public class InstancesReader {

    String directory; //the directory which contains the classes-folders and their documents
    private TreeMap<String, ArrayList<TextDocument>> dataSets;


    public InstancesReader(String directory) {
        dataSets = new TreeMap<String, ArrayList<TextDocument>>();
        this.directory = directory;
    }

    public void readAll() {
        readClasses();
    }

    /**
     *
     * @param category , neg or pos (category the same as class :) )
     * @return the appropriate list
     */
    public ArrayList<TextDocument> getDocuments(String category) {
        return dataSets.get(category);
    }

    /**
     * Fetch all the data as a tree-map.
     * 
     * @return TreeMap<String, ArrayList<TextDocument>>
     */
    public TreeMap<String, ArrayList<TextDocument>> getAllData() {
        return dataSets;
    }

    private void readClasses() {
        File directoryOfClasses = openDirectory();
        File[] classes = directoryOfClasses.listFiles();
        for (File classFolder : classes) {
            readAClassFolder(classFolder);
        }
    }

    /**
     * Opens the directory given as a parameter
     * returns null if given directory does not exist or is not
     * a directory
     */
    private File openDirectory() {
        File directory = new File(this.directory);

        if (directory.exists() && directory.isDirectory()) {
            return directory;
        }

        return null;
    }

    private void readAClassFolder(File classFolder) {
        System.out.println("Attempt to read class-folder " + classFolder.getName());

        //initialize an arrayList that contains text files of a classFolder
        ArrayList<TextDocument> docsOfFolder = new ArrayList<TextDocument>();

        //every folder is a directory that contains text documents
        File textFiles[] = classFolder.listFiles();
        System.out.println("Listing text files...");

        //read all the text files in the specified class folder
        for (int i = 0; i < textFiles.length; i++) {
            readFile(textFiles[i], classFolder.getName(), docsOfFolder);
            textFiles[i] = null;
            System.out.println("Done reading document " + i);
        }
        //once we load all the documents of a folder to a list we add the list to the hashmap
        dataSets.put(classFolder.getName(), docsOfFolder);
        
        System.out.println("class-folder " + classFolder.getName() + " is succesfully read!");
    }


    /**
     * Read a given file of a given category (sentiment)
     * 
     * @param f a file to read, contains text documents
     * @param category the category of the document (negative or positive)
     * @throws FileNotFoundException
     */
    private void readFile(File f, String category, ArrayList<TextDocument> docsOfFolder) {        
        BufferedReader reader = null;
        StringBuffer buffer;

        try {
            reader = new BufferedReader(new FileReader(f));
            buffer = new StringBuffer();
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }

            TextDocument t = new TextDocument(true);
            t.setSentiment(category);
            t.setText(buffer.toString());
            docsOfFolder.add(t);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
