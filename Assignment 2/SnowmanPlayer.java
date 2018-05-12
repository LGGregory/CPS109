package snowman;

import java.util.*;
import java.util.regex.*;

/**
 * Snowman Player
 *
 * @author Liam Gregory
 * @version Entropius 3.1 (6.1)
 */
public class SnowmanPlayer {

    private static ArrayList<String> wordList;                                                                          // ArrayList containing every word 
    private static ArrayList<String> currentList;                                                                       // ArrayList containing the words current round
    private static Map<Integer, ArrayList<String>> wordListByLength = new HashMap<Integer, ArrayList<String>>();        // Map of ArrayLists indexed by wordlength
    private static HashMap<Character, Integer> letterOccurrence = new HashMap<Character, Integer>();                    // Map of letter Occurrence indexed by character
    private static HashMap<Character, Integer> letterNonOccurrence = new HashMap<Character, Integer>();                 // Map of letter NonOccurrence indexed by character
    private static HashMap<Character, Double> letterEntropy = new HashMap<Character, Double>();                         // Map of entropy indexed by character
    private static char[] allowed;                                                                                      // Array of characters given in startGame
    private static String allowedSaved;                                                                                 // String given in startGame, saved for later use
    private static int minLengthSaved;                                                                                  // minLength given in startGame, saved for later use
    private static int maxLengthSaved;                                                                                  // maxLength given in startGame, saved for later use
    private static char[] firstGuess;                                                                                   // Array of character that are an easy first guess for each word length
    private static int wordLength;                                                                                      // length given in startNewWorld, saved for later use

    //DEPRECATED VARIABLES
    private static String guessedSaved;
    private static int guessNumber;

    /**
     * Initializes some maps. Filters wordLists by length for faster use later. Analyzes a quick first guess for each word length.
     *
     * @param words an array containing every valid word
     * @param minLength the minimum length of words in words
     * @param maxLength the maximum length of words in words
     * @param allowedChars the string of allowed characters
     */
    public static void startGame(String[] words, int minLength, int maxLength, String allowedChars) {
        minLengthSaved = minLength;
        maxLengthSaved = maxLength;
        wordList = new ArrayList<>(Arrays.asList(words));
        allowed = allowedChars.toCharArray();
        allowedSaved = allowedChars;

        String allowedletters = "abcdeghijklmnopqstuvwxyz";
        char[] frequencyofAllLetters = new char[26];
        for (String word : words) {
            for (int i = 0; i < allowedletters.length(); i++) {
                if (word.indexOf(allowedletters.charAt(i)) >= 0) {
                    frequencyofAllLetters[i]++;
                }
            }
        }

        firstGuess = mostOccurrentLetter(wordList, minLength, maxLength);
        for (char a : allowed) {
            letterOccurrence.put(a, 0);
            letterNonOccurrence.put(a, 0);
            letterEntropy.put(a, 0.0);

        }
        populateLengthList();

    }

    /**
     * Starts a new word. Resets several variables.
     *
     * @param length length of the current word
     */
    public static void startNewWord(int length) {
        currentList = new ArrayList<>(wordListByLength.get(length));
        guessNumber = 1;
        wordLength = length;
    }

    /**
     * Finds the character to guess based on current pattern
     *
     * @param pattern current pattern in the form "***x**"
     * @param previousGuesses all previous guesses
     * @return the character to guess
     */
    public static char guessLetter(String pattern, String previousGuesses) {
        // Makes a likely first guess to quickly gain information
        if (guessNumber == 0) {
            guessNumber++;
            return firstGuess[wordLength - minLengthSaved];
        }

        // Resets all per Guess maps.
        clearMap(letterOccurrence, 0);
        clearMap(letterNonOccurrence,0);
        clearMap(letterEntropy,0.0);

        guessedSaved = previousGuesses;
        char guess;

        // Builds a regex to match the current pattern
        String regexPattern = regexBuilder(pattern, previousGuesses);
        Pattern regexTerm = Pattern.compile(regexPattern);

        // filters the current List, leaving behind only words that match the regex term. Also builds the per Guess Maps.
        currentList = filterList(currentList, regexTerm);

        // Calculates information gained by guessing each possible character
        //for (char a : top5Occurrence(allowedSaved, previousGuesses)) {
        for (char a : allowed) {
            if (previousGuesses.indexOf(a) < 0 && letterOccurrence.get(a) > 0) {
                letterEntropy.put(a, bitGainer(pattern, a, previousGuesses));
            }

        }

        // guess the letter that would result in the highest information gained, weighted by miss chance
        guess = mostEntropicLetter();
        guessNumber++;
        return guess;
    }

    /**
     * Builds a regex term that will match the pattern given.
     *
     * @param pattern current pattern in the form "***x**"
     * @param previousGuesses all previous misses
     * @return the regex term as a string
     */
    public static String regexBuilder(String pattern, String previousGuesses) {
        StringBuilder regexTermBuilder = new StringBuilder();
        char[] pString = pattern.toCharArray();
        if (previousGuesses.isEmpty() && pattern.matches(
                "\\*+")) {
            for (int i = 0; i < pattern.length(); i++) {
                regexTermBuilder.append('.');
            }
        } else {
            String regexPossibility = "[^" + previousGuesses + "]";
            for (int i = 0; i < pString.length; i++) {
                if (pString[i] == '*') {
                    regexTermBuilder.append(regexPossibility);
                } else {
                    regexTermBuilder.append(pString[i]);
                }
            }
        }
        //System.out.println(regexTermBuilder.toString());
        return regexTermBuilder.toString();

    }

    /**
     * Adds the letter information in the given word to the global maps letterNonOccurrence and letterOccurrence
     *
     * @param word the word
     */
    public static void filterRisk(String word) {
        for (char a : allowed) {
            if (word.indexOf(a) < 0) {
                letterNonOccurrence.put(a, letterNonOccurrence.get(a) + 1);
            } else {
                letterOccurrence.put(a, letterOccurrence.get(a) + 1);
            }
        }
    }
//TODO 

    /**
     * Filters out words from an ArrayList that do not match the current pattern by means of a regex term given
     *
     * @param currList the ArrayList
     * @param regexPattern the Regex Pattern
     * @return a new ArrayList
     */
    public static ArrayList<String> filterList(ArrayList<String> currList, Pattern regexPattern) {
        ArrayList<String> newList = new ArrayList<>();
        currList.forEach((String word) -> {
            Matcher regMatcher = regexPattern.matcher(word);
            if (regMatcher.matches()) {
                newList.add(word);
                filterRisk(word);

            }
        });
        return newList;
    }

    /**
     * Finds each unique pattern of a given character in the global ArrayList<String> currentList. Returns a HashMap of the number of instances of each indexed to a unique hash of the pattern.
     *
     * @param testee the character to be checked
     * @return a map containing the instances of each pattern
     */
    private static HashMap<Integer, Integer> wordListPatternHash(char testee) {
        HashMap<Integer, Integer> wordListContainingGuess = new HashMap<>();
        currentList.stream().map((word) -> {
            int hashPoint = 0;
            for (int i = 0; i < word.length(); i++) {
                if (word.charAt(i) == testee) {
                    hashPoint += 1 << i;
                }
            }
            return hashPoint;
        }).map((hashPoint) -> {
            if (wordListContainingGuess.get(hashPoint) == null) {
                wordListContainingGuess.put(hashPoint, 0);
            }
            return hashPoint;
        }).forEachOrdered((hashPoint) -> {
            wordListContainingGuess.put(hashPoint, wordListContainingGuess.get(hashPoint) + 1);
        });
        return wordListContainingGuess;
    }

    /**
     * Finds the information gained from guessing a given letter, then weighs it by the possibility the guess is wrong. Then does some cargo cult programming because it just wasn't working how I
     * thought it should.
     *
     * @param pattern the pattern in the form "***x**"
     * @param testee the character to be tested
     * @param previousGuesses the string containing all previous guesss
     * @return the weighted information gained by guessing this character
     */
    public static double bitGainer(String pattern, char testee, String previousGuesses) {
        HashMap<Integer, Integer> patternsForLetter = wordListPatternHash(testee);
        // System.out.println("Pattern Matches for " + testee + " = " + Arrays.toString(patternMatches));
        //  System.out.println(testee + " list  " + patternsForLetter.toString());
        double bitsGained;
        double d = 0;
        double b = 0;

        if (patternsForLetter.get(0) != null) {
            patternsForLetter.remove(0);
        }
        //Entropy math except slightly off. TODO?
        for (Map.Entry<Integer, Integer> entry : patternsForLetter.entrySet()) {
            b = (entry.getValue() * 1.0) / currentList.size();
            d += b * log2(b);

        }
        if (currentList.size() - letterOccurrence.get(testee) == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        double n = (((currentList.size() - letterOccurrence.get(testee)) * 1.0) / currentList.size());
        bitsGained = (-1.0 * d);
        bitsGained = bitsGained / n;
        bitsGained *= (letterOccurrence.get(testee) * 1.0 / currentList.size());

        bitsGained *= (letterOccurrence.get(testee) * 1.0 / currentList.size());

        return bitsGained;

    }

   /**
    * Sets all values in a given map to a specific value.
    * 
    * @param <T> the type of the value of the map
    * @param map the map to be manipulated
    * @param value the value to be entered
    */
    private static <T> void clearMap(Map<?, T> map, T value) {
        map.entrySet().forEach((entry) -> {
            entry.setValue(value);
        });
    }

    /**
     * Loops through letterEntropy to find the highest valued character
     *
     * @return the character with highest weighted entropy
     */
    public static char mostEntropicLetter() {
        char guess = '?';
        double entropyMin = Double.NEGATIVE_INFINITY;

        for (Map.Entry<Character, Double> entry : letterEntropy.entrySet()) {

            if (entry.getValue() > entropyMin) {

                entropyMin = entry.getValue();
                guess = entry.getKey();
            }

        }

        return guess;
    }

    /**
     * Fills wordListByLength with ArrayLists, each containing the length of word that is the index value of the list.
     */
    private static void populateLengthList() {
        for (int i = minLengthSaved; i <= maxLengthSaved; i++) {
            wordListByLength.put(i, new ArrayList<>());
        }
        wordList.forEach((s) -> {
            wordListByLength.get(s.length()).add(s);
        });
    }

    /**
     * For each value [min, max], returns the most occurrent letter in all words with that length in an array
     *
     *
     * @param passedList ArrayList containing every word
     * @param min Minimum length of words
     * @param max Maximum length of words
     * @return the array
     */
    public static char[] mostOccurrentLetter(ArrayList<String> passedList, int min, int max) {
        int[][] occurrentLettersByWorldLength = new int[(max - min) + 1][26];
        char[] mostOccurrentLettersByWorldLength = new char[(max - min) + 1];
        for (int[] ocl : occurrentLettersByWorldLength) {
            for (int i : ocl) {
                i = 0;
            }
        }
        passedList.forEach((word) -> {
            for (int i = 0; i < word.length(); i++) {
                occurrentLettersByWorldLength[word.length() - min][allowedSaved.indexOf(word.charAt(i))]++;
            }
        });
        for (int k = 0; k < occurrentLettersByWorldLength.length; k++) {
            int value = 0;
            for (int i = 0; i < occurrentLettersByWorldLength[k].length; i++) {
                if (occurrentLettersByWorldLength[k][i] > value) {
                    //System.out.println("blah");
                    value = occurrentLettersByWorldLength[k][i];
                    mostOccurrentLettersByWorldLength[k] = allowedSaved.charAt(i);
                }
            }
        }

        //System.out.println(mostOccurrentLettersByWorldLength);
        return mostOccurrentLettersByWorldLength;
    }

    /**
     * Returns the log base 2 of a number
     *
     * @param a the number
     * @return the log
     */
    public static double log2(double a) {
        return Math.log(a) / Math.log(2);
    }

    /**
     * Returns the name of the author.
     *
     * @return name of the author
     */
    public static String getAuthor() {
        return "Gregory, Liam";
    }

}
