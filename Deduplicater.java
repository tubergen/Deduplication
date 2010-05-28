import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*************************************************************************
 *  Brian Tubergen
 *  05/24/10
 *
 *  Compilation:  javac Deduplicater.java
 *  Execution:    java Deduplicater input.csv output.csv
 *  Dependencies: ST.java, opencsv-2.1.jar
 *
 *  Reads in data from a CSV file and removes duplicate idea entries. Some
 *  of the data structures could be eliminated based on what we want.
 *
 *  OpenCSV:      http://opencsv.sourceforge.net/
 *
 *************************************************************************/

public class Deduplicater {

    //   Given an idea "post_text," gives the id associated with that idea.
    private ST<String, Integer> ideaToId = new ST<String, Integer>();

    /*  Given an id, gives the idea/username/network associated with that id
     *  Right now I only keep the information for the first time the idea
     *  is encountered. Could keep information for all instances of an idea
     *  by replacing the "String" with a set.
     */
    private ST<Integer, String> idToIdea = new ST<Integer, String>();
    private ST<Integer, String> idToUsername = new ST<Integer, String>();
    private ST<Integer, String> idToNetwork = new ST<Integer, String>();

    //  Biggest id number
    private int maxId;
    //  Number of ideas
    private int N = 0;

    public Deduplicater(String inputFile)
    {
        try
        {
            processData(inputFile);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }

    private void processData(String inputFile) throws IOException
    {
        CSVReader reader = new CSVReader(new FileReader(inputFile));

        //  Keep track of which spreadsheet row data came from; use this as id
        //  Technically id = row - 1
        int id = 0;

        //  Read in the fields and assume they come in the format "network,
        //  author_username, post_text"
        String[] currentLine;
        while ( (currentLine = reader.readNext()) != null)
        {
            String network = currentLine[0].trim();
            String username = currentLine[1].trim();
            //  Change tabs and newlines to single spaces
            String idea = whitespaceToSpace(currentLine[2].trim());

            if (!ideaToId.contains(idea))
            {
                ideaToId.put(idea, id);
                idToIdea.put(id, idea);
                idToUsername.put(id, username);
                idToNetwork.put(id, network);
                N++;
            }
            id++;
        }

        maxId = id;
    }

    //  Will remove @anytext from all ideas and remove duplicates
    public void removeAtText()
    {
        remove("[ ]*@[^ ]*", true, false);
    }

    //  Will remove nonalphanumeric entries from the BEGINNING of all ideas
    //  and delete duplicates
    public void removeInitialNonAlphanumericEntries()
    {
        //  False is the 2nd argument here because we only want to match at the
        //  beginning
        remove("[^0-9a-z]+", true, true);
        //remove("[^a]+", true, true);
    }

    //  Removes "RT @username" from the beginnning of all ideas and deletes
    //  duplicates
    public void removeRTs()
    {
        //  Regular expression to match and eliminate "RT @username" from ideas
        //  Will not eliminate ideas where "RT" does not occur at the beginning
        //  of the string
        //  Will not eliminate "RTarbitarytext"
        //  Will not eliminate @username if it's not proceeded by RT
        String rtElim = "RT( @[^ ]*)* ";

        //  Regular expression to eliminate the above but not @whitehouse
        String rtElimWH = "RT( @[^((W|w)hitehouse)][^ ]*)* ";

        //  Try to recognize if RT is followed by ": "
        //  Will not eliminate RT:@username
        //String rtImproved = "RT:*[ ]*( @[^((W|w)hitehouse)][^ ]*)+";

        String rtImproved = "RT:*([ ]*@[^((W|w)hitehouse[ ]*)]*[^ ]*)+";

        String rtSimplified = "RT:*([ ]*@[^ ]*)+";

        remove(rtSimplified, false, true);
        removeRTtext();
    }

    //  Will remove "RTarbitrary_text " from the beginning of all ideas
    public void removeRTtext()
    {
        remove("RT[^ ]+ ", false, true);
        remove("RT[ ]+", false, true);
    }

    //  Removes "@whitehouse" from all ideas and deletes duplicates
    public void removeWhitehouses()
    {
        remove("@whitehouse", true, false);
    }

    //  Removes #tag from all ideas and deletes duplicates
    public void removeHashTags()
    {
        remove("#[^ ]*", true, false);
    }

    //  Removes anything with "is looking for Grand Challenges in science and
    //  technology, via Twitter"
    public void removeUselessTweets()
    {
        remove(".*is looking for Grand Challenges in science and tech.*," +
                " via Twitter.*", true, false);
    }

    //  Applies all of the remove methods implemented. uselessOn determines
    //  whether removeUselessTweets() will be applied as well.
    public void applyAllRemoves(boolean uselessOn)
    {
        //  Remove hash tags first in case they come before an RT
        removeHashTags();
        removeRTs();
        removeWhitehouses();
        removeAtText(); //  should probably be applied only after removeRTs()
        removeInitialNonAlphanumericEntries();
        removeRTs();    //  reapply in case we've uncovered some more RTs

        //  This wasn't requested, and I acknowledge that there's a small
        //  chance that we could miss an idea with this method. Nonetheless,
        //  the bugged the crap out of me, so I decided to implement it.
        if (uselessOn)
            removeUselessTweets();
    }

    //  Removes a given regular expression from all ideas and deletes duplicates
    //  Insensitive = true -> case insensitive
    //  Insensitive = false -> case sensitive
    private void remove(String regex, boolean insensitive,
            boolean matchOnlyStart)
    {
        Pattern myPattern = null;
        if (insensitive)
            myPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        else    //  we're dealing with RT
            myPattern = Pattern.compile(regex);

        for (int id = 0; id <= maxId; id++)
        {
            String idea = idToIdea.get(id);
            if (idea != null)
            {
                String simplifiedIdea = null;
                int newStartIndex = 0;

                Matcher myMatcher = myPattern.matcher(idea);
                while (myMatcher.find())
                {                  
                    if (!matchOnlyStart)
                    {
                        simplifiedIdea = myMatcher.replaceAll("").trim();
                    }
                    else if (myMatcher.start() == newStartIndex
                            && matchOnlyStart)
                    {
                        newStartIndex = myMatcher.end();
                        simplifiedIdea = myMatcher.replaceFirst("").trim();
                    }
                }

                if (simplifiedIdea != null)
                    updateForDuplicates(id, idea, simplifiedIdea);
            }
        }
    }

    //  Helper method to check if the new idea is now a duplicate
    private void updateForDuplicates(Integer id, String idea,
            String simplifiedIdea)
    {
        String username = idToUsername.get(id);
        String network = idToNetwork.get(id);

        if (!ideaToId.contains(simplifiedIdea))
        {
            ideaToId.put(simplifiedIdea, id);
            idToIdea.put(id, simplifiedIdea);
            //  These don't do anything I don't think:
            idToUsername.put(id, username);
            idToNetwork.put(id, network);

        }
        else //  this idea is a duplicate of something we already had
        {
            idToIdea.delete(id);
            idToUsername.delete(id);
            idToNetwork.delete(id);
            N--;
        }

        //  Always get rid of the old idea
        ideaToId.delete(idea);
    }

    //  Changes newlines and tabs to a single space
    private String whitespaceToSpace(String idea)
    {
       idea = idea.replaceAll("\t", " ");
       idea = idea.replaceAll("\n", " ");
       return idea;
    }

    //  Returns number of ideas
    public int size()
    {
        return N;
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

    //  Returns the idea associated with a given id
    public String getIdea(int id)
    {
        return idToIdea.get(id);
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
        Deduplicater d = new Deduplicater(args[0]);

        //  args[2] = 1 -> apply the removeUselessTweets() method
        //  args[2] != 1 -> don't apply the removeUselessTweets() method
        if (Integer.parseInt(args[2]) == 1)
            d.applyAllRemoves(true);
        else
            d.applyAllRemoves(false);

        try
        {
            d.writeOutput(args[1]);
        }
        catch(Exception e)
        {
            System.out.println(e);
        }
    }
}
