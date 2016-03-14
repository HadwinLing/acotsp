package com.caffinc.acotsp.common;


import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains tour information of one ant
 *
 * @author Sriram
 */
public class Ant {
    // Tour taken by this ant
    private int tour[];
    // Tracks the visited cities
    public boolean visited[];
    // Current index of the iteration
    private AtomicInteger currentIndex;

    public Ant(int numCities, AtomicInteger currentIndex) {
        this.tour = new int[numCities];
        this.visited = new boolean[numCities];
        this.currentIndex = currentIndex;
    }

    public void visitCity(int city) {
        tour[currentIndex.get() + 1] = city;
        visited[city] = true;
    }

    public int currentCity() {
        return tour[currentIndex.get()];
    }

    public int[] getTour() {
        return tour;
    }

    public boolean visited(int city) {
        return visited[city];
    }

    public double tourLength() {
        double length = graph[tour[n - 1]][tour[0]];
        for (int i = 0; i < n - 1; i++) {
            length += graph[tour[i]][tour[i + 1]];
        }
        return length;
    }

    public void clear() {
        for (int i = 0; i < visited.length; i++)
            visited[i] = false;
    }
}
