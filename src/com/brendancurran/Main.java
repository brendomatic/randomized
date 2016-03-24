package com.brendancurran;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AC1: Generate a randomized dataset of 997940 numbers containing values between 1 and 20
 * AC2: Dataset should conform to provided specification regarding distribution of results
 * AC3: No two consecutive numbers should be the same
 * AC4: Output results to file test.output
 * AC5: Print all lines to console which contain the value "20"
 */

/**
 * 64 Bit XorShift based fast RNG
 */
class Random {
    private long seed;

    /**
     * Default Constructor which generates the seed based on system time.
     */
    public Random(){
        seed = System.nanoTime();
    }

    /**
     * Additional constructor
     * @param seed The value to start the random number generator.
     * A seed value of 0 generates a random seed based on system time
     * which is the same as the default constructor.
     *
     */
    public Random(long seed){
        if (seed == 0){
            seed = System.nanoTime();
        }
        this.seed = seed;
    }

    /**
     * Uses an XORShift to produce a 63 bit random number.
     * Java's variables are all signed so the 64th bit is lost.
     * Period is 2^64 - 1
     * @return A pseudo-random 63 bit number
     */
    private long rand() {
        seed ^= (seed << 21);
        seed ^= (seed >>> 35);
        seed ^= (seed << 4);
        return Math.abs(seed - 1);
    }

    /**
     * Allows the user to assign a min and max value for the numbers
     * they want returned.
     * @param min Minimum value to be returned (Can be negative)
     * @param max Maximum value to be returned (Can be negative)
     * @return A long random value between min and max
     */
    public long rand(long min, long max){
        if(min > max){
            return rand(max,min);
        }
        if (min == max) {
            return min;
        }
        return (rand() % (max + 1 - min)) + min;
    }

    /**
     * Returns the current seed for the random number generator
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Sets the current seed to seed
     */
    public void setSeed(final long seed) {
        this.seed = seed;
    }
}

class DistributionControl {
    private final Integer MIN = 1, MAX = 20;
    private final Integer CARDINALITY_1_TO_12 = 83000;

    // We will use this map to keep track of in-memory state
    private Map<Integer,Integer> distribution = null;

    public DistributionControl(){
        distribution = new HashMap<>();
        initializeDistribution();
    }

    /**
     * Initialize distribution matrix to initial values representing an untouched sterile input field.
     */
    private void initializeDistribution(){
        distribution.put(1,CARDINALITY_1_TO_12);
        distribution.put(2,CARDINALITY_1_TO_12);
        distribution.put(3,CARDINALITY_1_TO_12);
        distribution.put(4,CARDINALITY_1_TO_12);
        distribution.put(5,CARDINALITY_1_TO_12);
        distribution.put(6,CARDINALITY_1_TO_12);
        distribution.put(7,CARDINALITY_1_TO_12);
        distribution.put(8,CARDINALITY_1_TO_12);
        distribution.put(9,CARDINALITY_1_TO_12);
        distribution.put(10,CARDINALITY_1_TO_12);
        distribution.put(11,CARDINALITY_1_TO_12);
        distribution.put(12,CARDINALITY_1_TO_12);
        distribution.put(13,1000);
        distribution.put(14,500);
        distribution.put(15,250);
        distribution.put(16,100);
        distribution.put(17,50);
        distribution.put(18,25);
        distribution.put(19,10);
        distribution.put(20,5);
    }

    /**
     * This function decrements the corresponding entry to the distribution map.
     * This function protects the distribution from negative values by not decrementing past ZERO
     */
    public void adjustDistribution(Integer index) {
        Integer numUsed = distribution.get(index);
        if( numUsed > 0 ) {
            distribution.put(index, numUsed - 1);
        }
    }

    public boolean available(Integer index) {
        if(distribution.get(index) > 0){
            return true;
        }
        else
            return false;
    }

    /**
     * Returns true if the entire distribution is exhausted
     * @return
     */
    public boolean isEmpty(){
        boolean result = true;
        for( int i = 1; i <= 20; i++){
            result = result || distribution.get(i) != 0;
        }
        return !result;
    }
}

public class Main {
    private static final Integer MIN = 1, MAX = 20, SIZE = 997940;
    private static List<Integer> numbers = null;
    private static Random random = null;
    private static DistributionControl distributionControl= null;

    public static void main(String[] args) {
        //THESE STEPS WOULD OTHERWISE BE BROKEN INTO ATOMIC SUBFUNCTIONS TO SUPPORT ENCAPSULATION, SEPARATION OF LOGIC,
        //COHESION, AND PRINCIPLES OF CODE RE-USE. FOR CONVENIENCE AND TIME THEY HAVE BEEN INCLUDED DIRECTLY IN THE MAIN
        //CLASS AS STATIC REFERENCES. BOUNDARIES THAT WOULD BE FUNCTION BOUNDARIES ARE MARKED WITH COMMENTS.

        //Initialize our helper objects and member objects
        random = new Random();
        numbers = new ArrayList<>(SIZE);
        distributionControl = new DistributionControl();

        //Retrieve initial value
        int prevValue = Math.abs((int) random.rand(MIN,MAX));
        numbers.add(0,prevValue);
        distributionControl.adjustDistribution(prevValue);

        for(int i=1; i<SIZE; i++) {
            if (!distributionControl.isEmpty()) {
                //Randomly generate next value
                int nextValue = (int) random.rand(MIN, MAX);

                //Avoid contiguous repetition of values by iterating over our distribution until we find a new value
                //in the case that our current value is the same as our the value immediately to our left
                while (nextValue == prevValue ) {
                    nextValue = (int) random.rand(MIN, MAX);
                }

                //LIMIT SOLUTION SPACE IN QUICK AND DIRTY WAY TO IMPROVE PERFORMANCE BY IMMEDIATELY SHIFTING UNAVAILABLE
                //VALUES RATHER THAN ENDLESSLY RE-TRYING RANDOM GENERATION AT EACH UNAVAILABLE NUMBER FOUND.
                while(!distributionControl.available(nextValue)){
                    nextValue = (nextValue == 1) ? MAX : nextValue-1;
                }

                numbers.add(i, nextValue);                                      //Store the value in our resultset
                distributionControl.adjustDistribution(nextValue);              //Decrement the appropriate index's remaining usages
                prevValue = nextValue;                                          //Reset this value as the prevValue to check next time through
            }
            else{
                //Normally we would handle exeptions by terminating the program in question or throwing control to an
                //upline calling method. Here we just print and exit the loop since we are not in an application server anyway
                //and further
                System.out.println("Error condition found, distribution exhausted before it was supposed to, terminating.");
                continue;
            }
        }

        buildFile(numbers);
    }

    /**
     * Build and populate file with results
     * @param numbers
     */
    public static void buildFile(List<Integer> numbers){
        String filename = "test.output";

        //Convert numbers list to strings in preparation for output to file. In this case we will take the performance
        //and space hit required to transform the Integers because optimization at this level is not worth it in an interview
        ArrayList<String> strings = new ArrayList<>(numbers.size());
        for( Integer n : numbers ){
            if( n.equals(20)) {
                System.out.println(n);    //Document location of the number 20 by printing in console
            }
            strings.add( n.toString() );
        }
        //Write the file
        try {
            Files.write(Paths.get(filename), strings, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch(IOException e){
            System.out.println( "IOException found: " + e.getStackTrace() );
        }
    }
}

