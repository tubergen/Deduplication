/*************************************************************************
 *  Brian Tubergen
 *  05/27/10
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
  
   @Test
   public void test_rt_regex()
   {
       System.out.println("Testing regular expression for rt..");

       Deduplicater d = new Deduplicater("test_rt.csv");

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

   @Test
   public void test_whitehouse_regex()
   {
       System.out.println("Testing regular expression for whitehouse...");

       Deduplicater d = new Deduplicater("test_whitehouse.csv");

       //  All but three ideas should be eliminated
       d.removeWhitehouses();
       assertTrue(d.size() == 3);

       //  Should eliminate all @whitehouse
       for (String idea : d.getIdeas())
           assertTrue(!idea.contains("@whitehouse"));
   }

   @Test
   public void test_hashtag_regex()
   {
       System.out.println("Testing regular expression for hashtag...");

       Deduplicater d = new Deduplicater("test_hashtag.csv");

       //  All but four ideas should be eliminated
       d.removeHashTags();
       assertTrue(d.size() == 4);

       //  Should eliminate all @
       for (String idea : d.getIdeas())
           assertTrue(!idea.contains("#"));
   }

   @Test
   public void test_attext_regex()
   {
       System.out.println("Testing regular expression for @text...");

       Deduplicater d = new Deduplicater("test_attext.csv");

       //  Should remove all but 3
       d.removeAtText();       
       assertTrue(d.size() == 3);

       //  Should elimiante all @
       for (String idea : d.getIdeas())
           assertTrue(!idea.contains("@"));
   }
/*
   public void test_msalganik()
   {
       Deduplicater d = new Deduplicater("test_msalganik");
   }
*/

   /*  Note: This test is kind of trivial since STs don't allow duplicate keys.
    *  Nonetheless, I've left it here in case the Deduplicater implementation
    *  is changed.
    */
   @Test
   public void test_uniqueness_before_removals()
   {
       System.out.println("Testing uniqueness without remove methods" +
               " applied...");

       Deduplicater d = new Deduplicater("test_uniq.csv");

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

   @Test
   public void test_commas()
   {
        System.out.println("Testing commas...");

        Deduplicater d = new Deduplicater("test_commas.csv");

        //  Ensure that ideas that came with commas finish with commas by
        //  marking the ones that started without commas; the rest must contain
        //  commas at the end
        boolean[] indicesWithoutCommas = new boolean[d.size()];
        for (Integer id : d.getIds())
        {
            String idea = d.getIdea(id);
            if (!idea.contains(","))
                indicesWithoutCommas[id] = true;
        }
        
        d.applyAllRemoves(false);

        for (Integer id : d.getIds())
        {
            String idea = d.getIdea(id);
            assertTrue(indicesWithoutCommas[id] || idea.contains(","));
        }
   }

   @Test
   public void test_newlines()
   {
        System.out.println("Testing newlines...");

        Deduplicater d = new Deduplicater("test_newlines.csv");

        //  Ensure that all newlines are eliminated
        for (String idea : d.getIdeas())
            assertTrue(!idea.contains("\n"));

        //  Ensure that removing newlines removes duplicate ideas
        assertTrue(d.size() == 2);

   }
 
}