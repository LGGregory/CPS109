package snowman;
import java.util.*;
import java.io.*;

public class SnowmanRunner {
  
    public static final char BLANK = '*';
    
    public static void main(String[] args) {
        
        /* You are allowed to modify the following four variables. */
        final int SEED = 789;
        final String PASSPHRASE = // as illustrated in https://xkcd.com/936/
        "Two short camels carry my dapanzi to the dinner table.";
        final int ROUNDS = 200;
        final boolean VERBOSE = false;
    
        /* DO NOT MODIFY ANYTHING BELOW THIS LINE !!!! */
        final int MINLENGTH = 5;
        final int MAXLENGTH = 15;
        String WORDFILE = "words.txt";
        String ALLOWED = "abcdefghijklmnopqrstuvwxyz";
        
        List<String> wordList = new ArrayList<String>();
        String[] wordArray;
        
        try {
            Scanner s = new Scanner(new File(WORDFILE));
            int totalCount = 0;
            outer:
            while(s.hasNextLine()) {
                String word = s.nextLine();
                totalCount++;
                if(word.length() < MINLENGTH || word.length() > MAXLENGTH) { continue; }
                for(int i = 0; i < word.length(); i++) {
                    if(ALLOWED.indexOf(word.charAt(i)) == -1) { continue outer; }
                }
                wordList.add(word);
            }
            if(VERBOSE) {
                System.out.printf("Read in %d words, of which %d remain after filtering.\n",
                totalCount, wordList.size());
            }
        } catch(Exception e) {
            System.out.println("Error reading file " + WORDFILE + ": " + e);
            return;
        }
        
        wordArray = wordList.toArray(new String[0]);            
        
        try {
            SnowmanPlayerVengeance.startGame(wordArray, MINLENGTH, MAXLENGTH, ALLOWED);                  //CHANGE HERE
        }
        catch(Exception e) {
            System.out.println("startGame crashed with error: " + e);
            System.out.println("Cannot continue the test, exiting.");
            return;
        }
        
        // Scramble the internal wordlist multiple times.
        byte[] scramble = PASSPHRASE.getBytes();
        int idx = 0, scrambleCount = 0;
        Random rng = new Random();
        while(idx < scramble.length) {
            ++scrambleCount;
            long seed = 0;
            for(int j = 0; j < 4; j++) {
                seed = (seed << 8) | scramble[idx < scramble.length? idx++ : 0];
            }
            rng.setSeed(seed);
            for(int j = 0; j < wordList.size() - 1; j++) {
                int k = rng.nextInt(wordList.size());
                String tmp = wordList.get(k);
                wordList.set(k, wordList.get(j));
                wordList.set(j, tmp);
            }
        }
        if(VERBOSE) {
            System.out.printf("Using seed %d to produce %d secret words, scrambled for %d rounds.\n",
            SEED, ROUNDS, scrambleCount); 
        }
        rng = new Random(SEED);
        int currentMisses = 0, totalMisses = 0, crashCount = 0;
        for(int i = 0; i < ROUNDS; i++) {
            try {
                currentMisses = 0;
                String previousGuesses = ""; 
                String secretWord = wordList.get(rng.nextInt(wordList.size())); 
                int previousCorrect = -1;
                
                if(VERBOSE) { System.out.print(secretWord + ": "); }
                SnowmanPlayerVengeance.startNewWord(secretWord.length());                                  //CHANGE HERE

                do {
                    String pattern = "";
                    int correct = 0;
                    for(int j = 0; j < secretWord.length(); j++) {
                        char c = secretWord.charAt(j);
                        if(previousGuesses.indexOf(c) > -1) { pattern += c; correct++; }
                        else { pattern += BLANK; }
                    }
                    if(correct == secretWord.length()) { break; }
                    if(correct == previousCorrect) { 
                        currentMisses++; 
                        if(VERBOSE) { System.out.print('!'); }
                    }
                    
                    char guess = SnowmanPlayerVengeance.guessLetter(pattern, previousGuesses);             //CHANGE HERE
                    if(VERBOSE) { System.out.print(guess); }
                    previousCorrect = correct;
                    previousGuesses += guess;
                } while(currentMisses < ALLOWED.length());
                totalMisses += currentMisses;
                if(VERBOSE) { System.out.println(" (" + currentMisses + ")"); }
            }
            catch(Exception e) {
                crashCount++;
                totalMisses += ALLOWED.length() - currentMisses;
                System.out.println(" CRASH WITH ERROR: " + e + " (" + ALLOWED.length() + ")");
            }
        }
        System.out.println("Code by '" + SnowmanPlayerVengeance.getAuthor() + "' made a total of " 
            + totalMisses + " misses over " + ROUNDS + " words, with " + crashCount + " crashes.");
    }
}