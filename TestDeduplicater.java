import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.IOException;

/*************************************************************************
 *  Brian Tubergen
 *  05/28/10
 *
 *  Compilation:  javac TestDeduplicater.java
 *  Execution:    java org.junit.runner.JUnitCore TestDeduplicater
 *  Dependencies: Duplicater.java, junit-4.8.2.jar
 *
 *  http://code.google.com/p/t2framework/wiki/JUnitQuickTutorial
 *
 *************************************************************************/

import org.junit.* ;
import static org.junit.Assert.* ;

public class TestDeduplicater {    

    /* Ensure that the regular expression meant to remove RT's at the beginning
     * of ideas functions properly by testing that all ideas marked with "keep"
     * in the specified text file are actually kept.
     */
    @Test
    public void test_rt_regex()
    {
        System.out.println("Testing regular expression for rt..");

        Deduplicater d = new Deduplicater("test_csvfiles/test_rt.csv");

        d.removeRTs();

        //  The entries with "keep" should keep an RT that doesn't appear
        //  at the beginning; the others should have none
        for (String idea : d.getIdeas())
        {
            if (idea.contains("keep"))
                assertTrue(idea.contains("RT"));
            else
                assertTrue(!idea.contains("RT"));
        }
   }

    /* Ensure that the regular expression meant to remove @whitehosue functions
     * properly by testing that all @whitehouse is eliminated and that all but
     * three of the ideas in the particular text file specified are removed as
     * duplicates.
     */
    @Test
    public void test_whitehouse_regex()
    {
        System.out.println("Testing regular expression for whitehouse...");

        Deduplicater d = new Deduplicater("test_csvfiles/test_whitehouse.csv");
        d.removeWhitehouses();

        //  Should eliminate all @whitehouse
        for (String idea : d.getIdeas())
            assertTrue(!idea.contains("@whitehouse"));

        //  All but three ideas should be eliminated
        assertTrue(d.size() == 3);
    }

    /* Ensure that the regular expression meant to remove #text functions
     * properly by testing that all # is eliminated and that all but four of
     * the ideas in the particular text file specified are removed as duplicates.
     */
    @Test
    public void test_hashtag_regex()
    {
        System.out.println("Testing regular expression for hashtag...");

        Deduplicater d = new Deduplicater("test_csvfiles/test_hashtag.csv");
        d.removeHashTags();

        //  Should eliminate all #
        for (String idea : d.getIdeas())
            assertTrue(!idea.contains("#"));

        //  All but four ideas should be eliminated
        assertTrue(d.size() == 4);
    }

    /* Ensure that the regular expression meant to remove @text functions
     * properly by testing that all @ is eliminated and that all but three of
     * the ideas in the particular text file specified are removed as duplicates.
     */
    @Test
    public void test_attext_regex()
    {
        System.out.println("Testing regular expression for @text...");

        Deduplicater d = new Deduplicater("test_csvfiles/test_attext.csv");
        d.removeAtText();

        //  Should elimiante all @
        for (String idea : d.getIdeas())
            assertTrue(!idea.contains("@"));

        //  Should remove all but 3 ideas
        assertTrue(d.size() == 3);
    }

    /*  This test is kind of trivial since STs don't allow duplicate keys.
     *  Nonetheless, I've left it here in case the Deduplicater implementation
     *  is changed.
     */
    @Test
    public void test_uniqueness_before_removals()
    {
        System.out.println("Testing uniqueness without remove methods" +
                " applied...");

        Deduplicater d = new Deduplicater("test_csvfiles/test_uniq.csv");

        //  Sets do not allow duplicates
        SET<String> ideaSET = new SET<String>();
        for (String idea : d.getIdeas())
        {
            assertTrue(!ideaSET.contains(idea));
            ideaSET.add(idea);
        }

        //  The last idea in uniq.csv gives the number of unique ideas
        int numUnique = Integer.parseInt(ideaSET.min());
        assertTrue((Integer) ideaSET.size() == ideaSET.size());
    }

    /* Ensure that all the ideas that are entered which contain commas within
     * the idea retain those commas at the end. (Ensure that they are not split
     * in parsing the CSV file)
     */
    @Test
    public void test_commas()
    {
        System.out.println("Testing commas...");

        Deduplicater d = new Deduplicater("test_csvfiles/test_commas.csv");

        //  Mark the ideas that started without commas; the rest must contain
        //  commas at the end
        boolean[] indicesWithoutCommas = new boolean[d.size()];
        for (Integer id : d.getIds())
        {
            String idea = d.getIdea(id);
            if (!idea.contains(","))
                indicesWithoutCommas[id] = true;
        }
        
        d.applyAllRemoves(false);

        //  Check that the ideas that came with commas finish with commas
        for (Integer id : d.getIds())
        {
            String idea = d.getIdea(id);
            assertTrue(indicesWithoutCommas[id] || idea.contains(","));
        }
    }

    /* Ensures that newlines are eliminated from idea output and that ideas
     * that become identical after newlines are removed are deduplicated. Note
     * that newline removal is automatic; there is no separate removeNewlines
     * method.
     */
    @Test
    public void test_newlines()
    {
        System.out.println("Testing newlines...");

        Deduplicater d = new Deduplicater("test_csvfiles/test_newlines.csv");

        //  Ensure that all newlines are eliminated
        for (String idea : d.getIdeas())
            assertTrue(!idea.contains("\n"));

        //  Ensure that removing newlines removes duplicate ideas
        assertTrue(d.size() == 2);
    }

    /* Compare outputs generated by a Deduplicator to the ideas that we know
     * are the correct outputs for Matt Salganik's prescribed corner cases.
     */
    @Test
    public void test_msalganik() throws IOException
    {
        System.out.println("Testing Matt's corner cases...");

        String inputFile = "test_csvfiles/test_msalganik.csv";
        String correctOutputFile = "test_csvfiles/test_msalganik_output.csv";

        Deduplicater d = new Deduplicater(inputFile);
        d.applyAllRemoves(false);

        CSVReader reader = new CSVReader(new FileReader(correctOutputFile));

        //  Store the correct idea outputs in a SET
        SET<String> correctIdeas = new SET<String>();
        String[] currentLine;
        while ( (currentLine = reader.readNext()) != null)
            correctIdeas.add(currentLine[2]);

        //  Ensure we got the expected results from d
        for (String idea : d.getIdeas())
             assertTrue(correctIdeas.contains(idea));
    }
}