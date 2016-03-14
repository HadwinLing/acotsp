import com.caffinc.acotsp.common.Ant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.caffinc.acotsp.util.Math.*;

/*
 *  === Implementation of ant swarm TSP solver. ===
 *  
 * The algorithm is described in [1, page 8].
 * 
 * == Tweaks/notes == 
 *  - I added a system where the ant chooses with probability
 *    "pr" to go to a purely random town. This did not yield better
 * results so I left "pr" fairly low.
 *  - Used an approximate pow function - the speedup is
 *    more than a factor of 10! And accuracy is not needed
 *    See AcoTsp.pow for details.
 *  
 * == Parameters ==
 * I set the parameters to values suggested in [1]. My own experimentation
 * showed that they are pretty good.
 * 
 * == Usage ==
 * - Compile: javac AcoTsp.java
 * - Run: java AcoTsp <TSP file>
 * 
 * == TSP file format ==
 * Full adjacency matrix. Columns separated by spaces, rows by newline.
 * Weights parsed as doubles, must be >= 0.
 * 
 * == References == 
 * [1] M. Dorigo, The Ant System: Optimization by a colony of cooperating agents
 * ftp://iridia.ulb.ac.be/pub/mdorigo/journals/IJ.10-SMC96.pdf
 * 
 */

public class AcoTsp {
    // Algorithm parameters:
    // original amount of trail
    private static final double c = 1.0;
    // trail preference
    private static final double alpha = 1;
    // greedy preference
    private static final double beta = 5;
    // trail evaporation coefficient
    private static final double evaporation = 0.5;
    // new trail deposit coefficient;
    private static final double Q = 500;
    // number of ants used = numAntFactor*numTowns
    private static final double numAntFactor = 0.8;
    // probability of pure random selection of the next town
    private static final double pr = 0.01;

    // Reasonable number of iterations
    // - results typically settle down by 500
    private static final int maxIterations = 2000;

    public int numCities = 0; // # towns
    public int numAnts = 0; // # ants
    private double graph[][] = null;
    private double trails[][] = null;
    private Ant ants[] = null;
    private Random rand = new Random();
    private double probs[] = null;

    private AtomicInteger currentIndex = new AtomicInteger(0);

    public int[] bestTour;
    public double bestTourLength;

    // Read in graph from a file.
    // Allocates all memory.
    // Adds 1 to edge lengths to ensure no zero length edges.
    public void readGraph(String path) throws IOException {
        FileReader fr = new FileReader(path);
        BufferedReader buf = new BufferedReader(fr);
        String line;
        int i = 0;

        while ((line = buf.readLine()) != null) {
            String splitA[] = line.split(" ");
            LinkedList<String> split = new LinkedList<String>();
            for (String s : splitA)
                if (!s.isEmpty())
                    split.add(s);

            if (graph == null)
                graph = new double[split.size()][split.size()];
            int j = 0;

            for (String s : split)
                if (!s.isEmpty())
                    graph[i][j++] = Double.parseDouble(s) + 1;

            i++;
        }

        numCities = graph.length;
        numAnts = (int) (numCities * numAntFactor);

        // all memory allocations done here
        trails = new double[numCities][numCities];
        probs = new double[numCities];
        ants = new Ant[numAnts];
        for (int j = 0; j < numAnts; j++)
            ants[j] = new Ant(graph.length, currentIndex);
    }

    // Store in probs array the probability of moving to each town
    // [1] describes how these are calculated.
    // In short: ants like to follow stronger and shorter trails more.
    private void probTo(Ant ant) {
        int currentCity = ant.currentCity();

        double denom = 0.0;
        for (int l = 0; l < numCities; l++)
            if (!ant.visited(l))
                denom += pow(trails[currentCity][l], alpha)
                        * pow(1.0 / graph[currentCity][l], beta);


        for (int nextCity = 0; nextCity < numCities; nextCity++) {
            if (ant.visited(nextCity)) {
                probs[nextCity] = 0.0;
            } else {
                double numerator = pow(trails[currentCity][nextCity], alpha)
                        * pow(1.0 / graph[currentCity][nextCity], beta);
                probs[nextCity] = numerator / denom;
            }
        }

    }

    // Given an ant select the next town based on the probabilities
    // we assign to each town. With pr probability chooses
    // totally randomly (taking into account tabu list).
    private int selectNextTown(Ant ant) {
        // sometimes just randomly select
        if (rand.nextDouble() < pr) {
            int t = rand.nextInt(numCities - currentIndex.get()); // random town
            int j = -1;
            for (int i = 0; i < numCities; i++) {
                if (!ant.visited(i))
                    j++;
                if (j == t)
                    return i;
            }

        }
        // calculate probabilities for each town (stored in probs)
        probTo(ant);
        // randomly select according to probs
        double r = rand.nextDouble();
        double tot = 0;
        for (int i = 0; i < numCities; i++) {
            tot += probs[i];
            if (tot >= r)
                return i;
        }

        throw new RuntimeException("Not supposed to get here.");
    }

    // Update trails based on ants tours
    private void updateTrails() {
        // evaporation
        for (int i = 0; i < numCities; i++)
            for (int j = 0; j < numCities; j++)
                trails[i][j] *= evaporation;

        // each ants contribution
        for (Ant ant : ants) {
            double contribution = Q / ant.tourLength();
            for (int i = 0; i < numCities - 1; i++) {
                trails[ant.getTour()[i]][ant.getTour()[i + 1]] += contribution;
            }
            trails[ant.getTour()[numCities - 1]][ant.getTour()[0]] += contribution;
        }
    }

    // Choose the next town for all ants
    private void moveAnts() {
        // each ant follows trails...
        while (currentIndex.get() < numCities - 1) {
            for (Ant a : ants)
                a.visitCity(selectNextTown(a));
            currentIndex.incrementAndGet();
        }
    }

    // numAnts ants with random start city
    private void setupAnts() {
        currentIndex.set(-1);
        for (int i = 0; i < numAnts; i++) {
            ants[i].clear(); // faster than fresh allocations.
            ants[i].visitCity(rand.nextInt(numCities));
        }
        currentIndex.incrementAndGet();

    }

    private void updateBest() {
        if (bestTour == null) {
            bestTour = ants[0].getTour();
            bestTourLength = ants[0].tourLength();
        }
        for (Ant a : ants) {
            if (a.tourLength() < bestTourLength) {
                bestTourLength = a.tourLength();
                bestTour = a.getTour().clone();
            }
        }
    }

    public static String tourToString(int tour[]) {
        String t = "";
        for (int i : tour)
            t = t + " " + i;
        return t;
    }

    public int[] solve() {
        // clear trails
        for (int i = 0; i < numCities; i++)
            for (int j = 0; j < numCities; j++)
                trails[i][j] = c;

        int iteration = 0;
        // run for maxIterations
        // preserve best tour
        while (iteration < maxIterations) {
            setupAnts();
            moveAnts();
            updateTrails();
            updateBest();
            iteration++;
        }
        // Subtract numCities because we added one to edges on load
        System.out.println("Best tour length: " + (bestTourLength - numCities));
        System.out.println("Best tour:" + tourToString(bestTour));
        return bestTour.clone();
    }

    // Load graph file given on args[0].
    // (Full adjacency matrix. Columns separated by spaces, rows by newlines.)
    // Solve the TSP repeatedly for maxIterations
    // printing best tour so far each time. 
    public static void main(String[] args) {
        // Load in TSP data file.
        if (args.length < 1) {
            System.err.println("Please specify a TSP data file.");
            return;
        }
        AcoTsp anttsp = new AcoTsp();
        try {
            anttsp.readGraph(args[0]);
        } catch (IOException e) {
            System.err.println("Error reading graph.");
            return;
        }

        // Repeatedly solve - will keep the best tour found.
        for (; ; ) {
            anttsp.solve();
        }

    }
}