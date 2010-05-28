//  Imports so that OpenCSV works
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.util.List;

/*************************************************************************
 *  Brian Tubergen
 *  05/24/10
 *
 *  Compilation:  javac Deduplicater_OpenCSV.java
 *  Execution:    java Deduplicater_OpenCSV input.csv output.csv
 *  Dependencies: ST.java, opencsv-2.1.jar
 *
 *  Reads in data from a CSV file and removes duplicate idea entries. Some
 *  of the data structures could be eliminated based on what we want.
 *
 *************************************************************************/

public class Deduplicater_OpenCSV {
    
    //   Given an idea "post_text," gives the id associated with that idea.
    private ST<String, Integer> ideaToId = new ST<String, Integer>();

    //  Can be used to answer question, "What did this user submit?"
    private ST<String, Integer> usernameToId = new ST<String, Integer>();

    /*  Given an id, gives the idea/username/network associated with that id
     *  Right now I only keep the information for the first time the idea 
     *  is encountered. Could keep information for all instances of an idea
     *  by replacing the "String" with a set.
     */
    private ST<Integer, String> idToIdea = new ST<Integer, String>();
    private ST<Integer, String> idToUsername = new ST<Integer, String>();
    private ST<Integer, String> idToNetwork = new ST<Integer, String>();

   
    public Deduplicater_OpenCSV(String inputFile, String outputFile)
    {
        try
        {
            processData(inputFile);
            writeOutput(outputFile);
        }
        catch(Exception e)
        {
            System.out.println("Input file dne." + e);
        }
    }

    public void processData(String inputFile) throws IOException
    {      
        CSVReader reader = new CSVReader(new FileReader(inputFile));

        //  Keep track of which spreadsheet row data came from; use this as id
        int id = 0;

        //  Read in the fields and assume they come in the format "network,
        //  author_username, post_text"
        String[] currentLine;
        while ( (currentLine = reader.readNext()) != null)
        {
            String network = currentLine[0];
            String username = currentLine[1];
            String idea = currentLine[2];

            if (!alreadyStored(idea))
            {
                ideaToId.put(idea, id);
                usernameToId.put(username, id);

                idToIdea.put(id, idea);
                idToUsername.put(id, username);
                idToNetwork.put(id, network);
            }
            id++;
        }
    }

    private boolean alreadyStored(String idea)
    {
        boolean b = false;
        if (idea.matches("RT.*"))
            if (ideaToId.contains(idea.substring(2, idea.length())))
                b = true;
        
        if (ideaToId.contains(idea))
            b = true;

        return b;
    }

    //  Returns an iterable of unique ideas
    public Iterable<String> getIdeas()
    {
        return ideaToId.keys();
    }

    //  Returns an iterable of unique ids
    public Iterable<Integer> getIds()
    {
        return idToIdea.keys();
    }

    //  Gives unique ideas in format: network, author_username, post_text
    public void writeOutput(String outputFile) throws IOException
    {
        CSVWriter writer = new CSVWriter(new FileWriter(outputFile));

        for (Integer id : getIds())
        {
            String idea = idToIdea.get(id);
            String username = idToUsername.get(id);
            String network = idToNetwork.get(id);
            
            String[] entries = {network, username, idea};
            writer.writeNext(entries);
        }
        writer.close();
    }

    public static void main(String[] args)
    {
        Deduplicater_OpenCSV d = new Deduplicater_OpenCSV(args[0], args[1]);
    }
}
