package cpen221.mp3.wikimediator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpen221.mp3.fsftbuffer.FSFTBuffer;
import cpen221.mp3.fsftbuffer.NotFoundException;
import cpen221.mp3.fsftbuffer.Page;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.Wiki;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class WikiMediator {

    /*The default capacity of the WikiMediator's cache is 100*/
    private static final int CAPACITY = 100;
    /*The default timeout for items in the cache is 3600*/
    private static final int TIMEOUT = 3600;
    /*The default timer for trending and peakLoad30s is 30*/
    private static final int TIMER = 30;
    /*The file in which statistical data is stored*/
    private static final String LOG = "./local/log.txt";

    private final Wiki wiki;
    private final FSFTBuffer<Page> pageCache;

    private ConcurrentHashMap<String, Long> trendingCache;
    private ConcurrentHashMap<String, Integer> trendingHistory;
    private ConcurrentHashMap<String, Integer> searchHistory;

    private final CopyOnWriteArrayList<Long> timeList;
    private int peak;
    /**
     * Rep Invariant:
     * wiki, pageCache, trendingCache, trendingHistory, searchHistory,
     *      timeList, peak, are not null
     * Every key in trendingHistory.keySet() must also be in
     *      searchHistory.keySet() and vice-versa
     * For all trendingCache.get(i), it must appear as a key in both
     *      searchHistory.keySet() and trending.keySet()
     * For all duplicate keys k shared by trendingHistory.keySet()
     *      and searchHistory.keySet(),
     *      searchHistory.get(k) >= trendingHistory.get(k)
     * For all timeList.get(i), timeList.get(i) <= System.currentTimeMillis()
     * peak >= 0
     * peak cannot decrement
     */
    /**
     * Abstraction Function:
     * WikiMediator caches Wikipedia pages and collects
     *      statistical data on its public methods.
     * Every page accessed by getPage is cached for
     *      faster subsequent use.
     * Public method calls are tracked by trendingCache,
     *      searchHistory, trendingHistory, timeList, and peak.
     * trendingCache records all queries passed to getPage and search
     *      in the last 30 seconds
     * trendingHistory records the number of uses of each query
     *      passed to getPage and search if they have been used
     *      in the past 30 seconds
     * searchHistory tracks the number of uses of each query
     *      passed to getPage and search for the entire duration
     *      of the instance's existence.
     * timeList records the requests of all public methods and
     *      stores the time at which they were requested. If
     *      peakLoad30s is called then all entries in timeList
     *      that are older than 30 seconds are removed at
     *      the end of the method call
     * peak represents the highest frequency of public method
     *      calls in any 30 second interval
     *
     */
    /**
     * Thread Safety Argument:
     * all methods are synchronized
     * All mutable datatypes are only modified in
     *       synchronized blocks
     * the mutable datatypes used in this implementation:
     *       FSFTBuffer, ConcurrentHashMap, CopyOnWriteArrayList,
     *       and CopyOnWriteArraySet are all thread-safe
     * the exception to the above is the log method, which
     *      uses non thread-safe datatypes. This is fine as
     *      log is intended to only record the most recent data
     *      for the representation of WikiMediator, which is
     *      synchronized across all threads.
     */

    /**
     * Create a new instance of WikiMediator. The constructor
     * attempts to read prior statistical data from a data file.
     * If the file does not exist, it creates one. If the file
     * exists but is invalid, it overwrites
     */
    public WikiMediator() {
        this.wiki = new Wiki.Builder().withDomain("en.wikipedia.org").build();
        this.pageCache = new FSFTBuffer<>(CAPACITY, TIMEOUT);
        this.timeList = new CopyOnWriteArrayList<>();

        try {
            File log = new File(LOG);
            if (!log.exists()) {
                log.createNewFile();
            }
            FileReader fr = new FileReader(LOG);
            JsonElement json = JsonParser.parseReader(fr);
            if (json.isJsonObject()) {
                JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
                JsonArray trendingCacheKeys =
                    jsonObject.get("trendingCacheKeys").getAsJsonArray();
                JsonArray trendingCacheTimes =
                    jsonObject.get("trendingCacheTimes").getAsJsonArray();
                JsonArray trendingHistoryKeys =
                    jsonObject.get("trendingHistoryKeys").getAsJsonArray();
                JsonArray trendingFrequency =
                    jsonObject.get("trendingFrequency").getAsJsonArray();
                JsonArray searchHistoryKeys =
                    jsonObject.get("searchHistoryKeys").getAsJsonArray();
                JsonArray searchFrequency =
                    jsonObject.get("searchFrequency").getAsJsonArray();
                JsonArray timeList = jsonObject.get("timeList").getAsJsonArray();
                this.peak = jsonObject.get("peak").getAsInt();

                Map<String, Long> trendCache = new HashMap<>();
                arrayToMapLong(trendCache, trendingCacheKeys, trendingCacheTimes);
                this.trendingCache = new ConcurrentHashMap<>(trendCache);

                Map<String, Integer> trendHistory = new HashMap<>();
                arrayToMapInteger(trendHistory, trendingHistoryKeys, trendingFrequency);
                this.trendingHistory = new ConcurrentHashMap<>(trendHistory);

                Map<String, Integer> searchHistory = new HashMap<>();
                arrayToMapInteger(searchHistory, searchHistoryKeys, searchFrequency);
                this.searchHistory = new ConcurrentHashMap<>(searchHistory);

                for (int i = 0; i < timeList.size(); i++) {
                    this.timeList.add(timeList.get(i).getAsLong());
                }
            } else {
                this.peak = 0;
                this.trendingCache = new ConcurrentHashMap<>();
                this.trendingHistory = new ConcurrentHashMap<>();
                this.searchHistory = new ConcurrentHashMap<>();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Given a map and two arrays, this method modifies map such that it takes arr1
     * and sets it to be the keys of map as Strings and arr2 is set to the values as Integers.
     * Each entry of the map has entries such that (key=arr1[i],value=arr2[i]).
     *
     * @param map  A map that has a String Integer pair as the entries
     * @param arr1 the array of jsonElements to be converted to String keys for the map
     * @param arr2 the array of jsonElements to be converted to Integer values
     */
    synchronized private void arrayToMapInteger(Map<String, Integer> map, JsonArray arr1,
                                                JsonArray arr2) {
        for (int i = 0; i < arr1.size(); i++) {
            map.put(arr1.get(i).getAsString(), arr2.get(i).getAsInt());
        }
    }

    /**
     * Given a map and two arrays, this method modifies map such that it takes arr1
     * and sets it to be the keys of map as Strings and arr2 is set to the values as Longs.
     * Each entry of the map has entries such that (key=arr1[i],value=arr2[i]).
     *
     * @param map  A map that has a string integer pair as the entries
     * @param arr1 the array of jsonElements to be converted to string keys for the map
     * @param arr2 the array of jsonElements to be converted to Long values
     */
    synchronized private void arrayToMapLong(Map<String, Long> map, JsonArray arr1,
                                             JsonArray arr2) {
        for (int i = 0; i < arr1.size(); i++) {
            map.put(arr1.get(i).getAsString(), arr2.get(i).getAsLong());
        }
    }


    /**
     * Given a query, return up to limit page titles that match the
     * query string (per Wikipedia's search service).
     *
     * @param query is the query string
     * @param limit is the maximum number of search results returned
     */
    synchronized public List<String> search(String query, int limit) {
        timeList.add(System.currentTimeMillis());
        zeitgeistTracker(query);
        trendingTracker(query);
        if (limit == 0) {
            return new ArrayList<>();
        }
        return wiki.search(query, limit, NS.MAIN);
    }

    /**
     * Given a pageTitle, returns the text associated with the
     * Wikipedia page that matches pageTitle.
     *
     * @param pageTitle is the title of the Wikipedia page
     * @return the contents of the Wikipedia page
     */
    synchronized public String getPage(String pageTitle) {
        timeList.add(System.currentTimeMillis());
        zeitgeistTracker(pageTitle);
        trendingTracker(pageTitle);
        try {
            return pageCache.get(pageTitle).getContent();
        } catch (NotFoundException nfe) {
            String content = wiki.getPageText(pageTitle);
            Page page = new Page(pageTitle, content);
            pageCache.put(page);
            return content;
        }
    }

    /**
     * Returns the most common Strings used in search and getPage requests, with
     * items being sorted in non-increasing order. Returns up to 'limit' items.
     *
     * @param limit the maximum number of items returned
     * @return a List of the most common Strings used in search and getPage.
     */
    synchronized public List<String> zeitgeist(int limit) {
        timeList.add(System.currentTimeMillis());
        CopyOnWriteArrayList<String> queries = new CopyOnWriteArrayList<>(searchHistory.keySet());
        queries.sort((a, b) -> searchHistory.get(b).compareTo(searchHistory.get(a)));
        if (queries.size() > limit) {
            return queries.subList(0, limit);
        }
        return queries;
    }

    /**
     * Returns the most common Strings used in search and getPage requests in the
     * last 30 seconds. Returns up to 'limit' items.
     *
     * @param limit the maximum number of items returned
     * @return a List of the most common search and getPage requests
     */
    synchronized public List<String> trending(int limit) {
        timeList.add(System.currentTimeMillis());
        timeoutTrends();
        CopyOnWriteArrayList<String> trendingQueries =
            new CopyOnWriteArrayList<>(trendingCache.keySet());
        trendingQueries.sort((a, b) -> trendingHistory.get(b).compareTo(trendingHistory.get(a)));
        if (trendingQueries.size() > limit) {
            return trendingQueries.subList(0, limit);
        }
        return trendingQueries;
    }

    /**
     * This method returns the maximum number of requests made to public
     * methods in this class over any 30 second interval.
     *
     * @return max number of requests over a 30 second interval
     */
    synchronized public int peakLoad30s() {
        long currentTime = System.currentTimeMillis();
        timeList.add(currentTime);
        processPeak(currentTime);
        return peak;
    }

    /**
     * Determines the max number of requests made up to the currentTime in any 30 second interval
     * by processing the timesList field variable
     *
     * @param currentTime the time peakLoad30s was called at, to be used as a reference as the maximum time
     */
    synchronized private void processPeak(long currentTime) {
        timeList.sort(Long::compare);
        CopyOnWriteArrayList<Long> newTimeList = new CopyOnWriteArrayList<>();
        for (int i = 0; i < timeList.size(); i++) {
            long time = timeList.get(i);
            int count = 0;
            if (currentTime - time <= TIMER * 1000) {
                newTimeList.add(time);
            }
            for (int j = i; j < timeList.size(); j++) {
                long nextTime = timeList.get(j);
                if (nextTime - time < TIMER * 1000) {
                    count++;
                } else {
                    break;
                }
            }
            peak = Math.max(count, peak);
        }
        timeList.clear();
        timeList.addAll(newTimeList);
    }

    /**
     * Keeps track of the number of times the same String is passed
     * to the search and getPage methods.
     *
     * @param entry the String passed to search or getPage
     */
    synchronized private void zeitgeistTracker(String entry) {
        if (searchHistory.containsKey(entry)) {
            int value = searchHistory.get(entry) + 1;
            searchHistory.replace(entry, value);
        } else {
            searchHistory.put(entry, 1);
        }
    }

    /**
     * Keeps track of the number of times the same String is passed
     * to the search and getPage methods in the last 30 seconds.
     *
     * @param query the String passed to search or getPage
     */
    synchronized private void trendingTracker(String query) {
        timeoutTrends();
        if (trendingCache.containsKey(query)) {
            int value = trendingHistory.get(query) + 1;
            trendingHistory.replace(query, value);
        } else {
            trendingHistory.put(query, 1);
        }
        trendingCache.put(query, System.currentTimeMillis() + TIMER * 1000);
    }

    /**
     * Removes all the values from the trendingCache that have not been
     * searched in the past 30 seconds. Modifies the trendingCache.
     */
    synchronized private void timeoutTrends() {
        ConcurrentLinkedQueue<String> timeoutQueue = new ConcurrentLinkedQueue<>();
        for (String key : trendingCache.keySet()) {
            if (trendingCache.get(key) < System.currentTimeMillis()) {
                timeoutQueue.add(key);
            }
        }
        while (!timeoutQueue.isEmpty()) {
            String key = timeoutQueue.poll();
            trendingCache.remove(key);
        }
    }

    /**
     * This method records the current state of the field variables into a text file called
     * log.txt in the local folder. This folder is written to in the traditional JSON format
     * in order to allow for ease when reading the state.
     */
    synchronized public void log() {
        File log = new File(LOG);
        try {
            FileWriter fw = new FileWriter(log);
            String[] trendingCacheKeys = new String[trendingCache.size()];
            Long[] trendingCacheTimes = new Long[trendingCache.size()];
            String[] trendingHistoryKeys = new String[trendingHistory.size()];
            Integer[] trendingFrequency = new Integer[trendingHistory.size()];
            String[] searchHistoryKeys = new String[searchHistory.size()];
            Integer[] searchFrequency = new Integer[searchHistory.size()];
            Long[] timeList = this.timeList.toArray(new Long[this.timeList.size()]);

            processPeak(System.currentTimeMillis());

            mapToArray(trendingCache, trendingCacheKeys, trendingCacheTimes);
            mapToArray(trendingHistory, trendingHistoryKeys, trendingFrequency);
            mapToArray(searchHistory, searchHistoryKeys, searchFrequency);

            Gson gson = new Gson();
            String jsonString = gson.toJson(
                new Formatter(trendingCacheKeys, trendingCacheTimes, trendingHistoryKeys,
                    trendingFrequency, searchHistoryKeys, searchFrequency, timeList, peak));

            fw.write(jsonString);
            fw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Modifies arr1, and arr2 by obtaining the keys of map and inputting them into arr1
     * and obtaining the values of map and inputting them into arr2. The i'th key-value pair in map
     * corresponds to the key in arr1[i] and the value at arr2[i]
     *
     * @param map  the map we want to separate into it's corresponding key and value arrays
     * @param arr1 an array of String keys from map
     * @param arr2 an array of generic type values from map
     * @param <T>  generic type
     */
    synchronized private <T> void mapToArray(Map<String, T> map, String[] arr1, T[] arr2) {
        int i = 0;
        for (String key : map.keySet()) {
            arr1[i] = key;
            arr2[i] = map.get(key);
            i++;
        }
    }

    private class Formatter {
        private final String[] trendingCacheKeys;
        private final Long[] trendingCacheTimes;
        private final String[] trendingHistoryKeys;
        private final Integer[] trendingFrequency;
        private final String[] searchHistoryKeys;
        private final Integer[] searchFrequency;
        private final Long[] timeList;
        private final int peak;
        /**
         * Rep Invariant:
         * trendingCacheKeys, trendingCacheTimes, trendingHistoryKeys,
         * trendingFrequency, searchHistoryKeys, searchFrequency,
         * timeList, peak are not null
         */
        /**
         * Abstraction Function:
         * This class' sole purpose is to take all
         * the the field variables such that it is easier
         * to format a JSON string of all the field variables
         * in a relatively simple way in the log() method
         */
        /**
         * Thread safety argument:
         * This class only assigns final variable references once.
         */

        /**
         * Creates a new instance of Formatter with all the necessary
         * statistical information for a WikiMediator instance.
         * @param trendingCacheKeys the keys of the WikiMediator trendingCache
         * @param trendingCacheTimes the values of the WikiMediator trendingCache
         * @param trendingHistoryKeys the keys of the WikiMediator trendingHistory
         * @param trendingFrequency the values of the WikiMediator trendingHistory
         * @param searchHistoryKeys the keys of the WikiMediator searchHistory
         * @param searchFrequency the values of the WikiMediator searchHistory
         * @param timeList the timeList of WikiMediator which contains access times
         * @param peak the peak 30 second load of WikiMediator
         */
        public Formatter(String[] trendingCacheKeys, Long[] trendingCacheTimes,
                         String[] trendingHistoryKeys, Integer[] trendingFrequency,
                         String[] searchHistoryKeys, Integer[] searchFrequency, Long[] timeList,
                         int peak) {
            this.trendingCacheKeys = trendingCacheKeys;
            this.trendingCacheTimes = trendingCacheTimes;
            this.trendingHistoryKeys = trendingHistoryKeys;
            this.trendingFrequency = trendingFrequency;
            this.searchHistoryKeys = searchHistoryKeys;
            this.searchFrequency = searchFrequency;
            this.timeList = timeList;
            this.peak = peak;
        }
    }

    /**
     * Did not attempt to finish task 5 so this is left here.
     * If finished this would have allowed for searches with
     * criteria given with a valid structure.
     *
     * @param query the search criteria
     * @return null
     */
    public List<String> executeQuery(String query){
        return null;
    }
}
