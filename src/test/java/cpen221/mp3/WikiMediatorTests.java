package cpen221.mp3;

import cpen221.mp3.wikimediator.WikiMediator;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class WikiMediatorTests {

    @Test
    public void searchTest1() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> search1 = wm.search("ubc", 3);

        assertEquals("University of British Columbia", search1.get(0));
        assertEquals("UBC (disambiguation)", search1.get(1));
        assertEquals("UBC Exchange", search1.get(2));
        assertEquals(3, search1.size());
    }

    @Test
    public void searchEmpty() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> search = wm.search("test", 0);
        assertEquals(search, new ArrayList<>());
    }

    @Test
    public void testGetPage() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> search = wm.search("ubc", 2);
        String UBC = search.get(1);
        assertEquals("'''UBC''' may refer to:\n" +
            "\n" +
            "{{TOC right}}\n" +
            "== Universities ==\n" +
            "* [[University of British Columbia]], a major Canadian university " +
            "with its main campus in Greater Vancouver\n" +
            ":* [[UBC Okanagan]], the campus in Kelowna, British Columbia\n" +
            ":* [[UBC Thunderbirds]], the athletic program of the main campus\n" +
            "\n" +
            "== Athletics ==\n" +
            "* [[Union Boat Club]], a rowing and sports club in Boston, Massachusetts\n" +
            "* [[United Basketball Conference]], a college basketball scheduling alliance\n" +
            "* [[University Barge Club]], an amateur rowing club in Philadelphia, " +
            "Pennsylvania\n" +
            "\n" +
            "== Businesses ==\n" +
            "* [[Unclaimed Baggage Center]], a retail store in Scottsboro, Alabama\n" +
            "* [[Union Bank of California]], in the United States\n" +
            "* [[Union Banking Corporation]], " +
            "a former banking corporation in the United States\n" +
            "* [[United Bank Card]], a payment and transaction processor in Pennsylvania \n" +
            "* [[United Brotherhood of Carpenters]], one of the largest trade unions in the " +
            "United States\n" +
            "\n" +
            "== Broadcasting ==\n" +
            "* [[UBC Media Group]], a radio services company in London, UK\n" +
            "* [[Ukrainian Business Channel]], a Ukrainian television channel\n" +
            "* [[Uganda Broadcasting Corporation]], the state broadcast\n" +
            "* [[Ulsan Broadcasting Corporation]], a television station in Ulsan, " +
            "South Korea\n" +
            "* [[United Broadcasting Company]], UBC was a US radio network\n" +
            "* [[United Broadcasting Corporation]], a cable television company in " +
            "Thailand\n" +
            "\n" +
            "== Science and technology ==\n" +
            "* [[Ubiquitin C]], a human gene\n" +
            "* [[Ubiquitin]]-conjugating enzyme\n" +
            "* [[Ultra Bright Colour]], a mobile phone display technology\n" +
            "* [[Unipolar brush cell]], a class of excitatory glutamatergic interneuron\n" +
            "\n" +
            "== Other uses==\n" +
            "* Uncorrectable Block Count, number of blocks that can't be corrected with an " +
            "[[Error Correction]] algorithm\n" +
            "* [[Unified Braille Code]], an English Braille code\n" +
            "* [[Uniform Building Code]], a model building code\n" +
            "* Union of Baltic Cites, a [[Euroregion Baltic#EU Strategy for the Baltic Sea " +
            "Region|cross-border cooperation organization in the Baltic Sea region]]\n" +
            "* [[United Baptist Convention]], a generic term for certain ministries in the " +
            "United Baptist tradition\n" +
            "* [[Universal background check]], a regularly debated topic in " +
            "gun politics in the United States\n" +
            "* [[University Baptist Church (disambiguation)]]\n" +
            "\n" +
            "{{disambiguation}}", wm.getPage(UBC));
    }

    @Test
    public void testZeitgeist1() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> search1 = wm.search("messi", 1);
        List<String> search2 = wm.search("ronaldo", 1);
        List<String> search3 = wm.search("messi", 1);
        List<String> search4 = wm.search("messi", 1);
        List<String> history = wm.zeitgeist(10);
        assertEquals(history.get(0), "messi");
        assertEquals(history.get(1), "ronaldo");
    }

    @Test
    public void testZeitgeist2() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> search1 = wm.search("messi", 1);
        List<String> search2 = wm.search("ronaldo", 1);
        List<String> search3 = wm.search("messi", 1);
        List<String> search4 = wm.search("messi", 1);
        List<String> search5 = wm.search("ronaldo", 1);
        List<String> search6 = wm.search("soccer", 1);
        List<String> history = wm.zeitgeist(10);
        assertEquals(history.get(0), "messi");
        assertEquals(history.get(1), "ronaldo");
        assertEquals(history.get(2), "soccer");
    }

    @Test
    public void testZeitgeist3() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        for (int i = 0; i < 100; i++) {
            wm.getPage("english");
            if (i % 10 == 0) {
                wm.getPage("spanish");
            }
            if (i % 5 == 0) {
                wm.getPage("french");
            }
        }
        List<String> history = wm.zeitgeist(10);
        assertEquals(history.get(0), "english");
        assertEquals(history.get(1), "french");
        assertEquals(history.get(2), "spanish");

        history = wm.zeitgeist(2);
        assertEquals(2, history.size());
    }

    @Test
    public void testTrending1() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        wm.search("messi", 1);
        wm.search("ronaldo", 1);
        wm.search("messi", 1);
        wm.search("messi", 1);
        wm.search("ronaldo", 1);
        wm.search("soccer", 1);
        wm.search("soccer", 1);
        wm.getPage("ronaldo");
        List<String> trend = wm.trending(2);

        assertFalse(trend.contains("soccer"));

    }

    @Test
    public void testTrending2() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        wm.search("messi", 1);
        wm.search("ronaldo", 1);
        wm.search("messi", 1);
        wm.search("messi", 1);
        wm.search("ronaldo", 1);
        wm.search("soccer", 1);
        wm.search("soccer", 1);
        wm.getPage("ronaldo");
        wm.getPage("ronaldo");
        List<String> trend = wm.trending(1);

        assertFalse(trend.contains("soccer"));
        assertFalse(trend.contains("messi"));

    }

    @Test
    public void testTrending3() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        wm.getPage("sathish");
        Thread.sleep(30 * 1000);
        wm.getPage("cpen221");
        List<String> trend = wm.trending(2);
        assertTrue(trend.contains("cpen221"));
        assertFalse(trend.contains("sathish"));
    }

    @Test
    public void testTrending4() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        wm.getPage("sathish");
        wm.getPage("cpen221");
        wm.getPage("ubc");
        wm.getPage("sathish");
        wm.getPage("cpen221");
        wm.getPage("sathish");
        List<String> trend = wm.trending(2);
        assertEquals(trend.get(0), "sathish");
        assertEquals(trend.get(1), "cpen221");
        assertEquals(trend.size(), 2);
    }

    @Test
    public void peakLoadTest1() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> search1 = wm.search("messi", 1);
        List<String> search2 = wm.search("ronaldo", 1);
        List<String> search3 = wm.search("messi", 1);
        List<String> search4 = wm.search("messi", 1);
        assertEquals(5, wm.peakLoad30s());
    }

    @Test
    public void peakLoadTest2() throws InterruptedException {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        for (int i = 0; i < 100; i++) {
            wm.getPage("english");
            if (i == 24) {
                Thread.sleep(30 * 1000);
            }
        }
        assertEquals(76, wm.peakLoad30s());
    }

    @Test
    public void logTest() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> search1 = wm.search("messi", 1);
        List<String> search2 = wm.search("ronaldo", 1);
        List<String> search3 = wm.search("messi", 1);
        List<String> search4 = wm.search("messi", 1);
        List<String> history = wm.zeitgeist(10);
        assertEquals(history.get(0), "messi");
        assertEquals(history.get(1), "ronaldo");
        wm.log();
        WikiMediator wm2 = new WikiMediator();
        List<String> history2 = wm2.zeitgeist(10);
        assertEquals(history2.get(0), "messi");
        assertEquals(history2.get(1), "ronaldo");
    }

    @Test
    public void noRequest() {
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        wm.log();
        wm = new WikiMediator();
        assertEquals(1, wm.peakLoad30s());
    }

    @Test
    public void threadSafety1(){
        try {
            PrintWriter pw = new PrintWriter("./local/log.txt");
            pw.write("");
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        WikiMediator wm = new WikiMediator();
        List<String> zeitgeistActual = new ArrayList<>();

        Thread thread1=new Thread(()->{
            wm.getPage("cpen 221");
            wm.getPage("cpen 221");
        });

        Thread thread2= new Thread(()->{
           wm.getPage("hello");
           wm.getPage("messi");
        });

        Thread thread3= new Thread(()->{
           try{
               Thread.sleep(1000);
               zeitgeistActual.addAll(wm.zeitgeist(3));
           }catch (InterruptedException e){
               e.printStackTrace();
               fail();
           }
        });

        thread1.start();
        thread2.start();
        thread3.start();

        try{
            thread1.join();
            thread2.join();
            thread3.join();
            assertEquals(3, zeitgeistActual.size());
            assertEquals("cpen 221", zeitgeistActual.get(0));
            assertEquals(6, wm.peakLoad30s());
        }catch (InterruptedException e){
            e.printStackTrace();
            fail();
        }
        
    }

}
