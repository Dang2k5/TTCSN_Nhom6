package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm {

    private final Graph graph;
    private final int populationSize;
    private final int maxGenerations;
    private final double mutationRate;
    private final double crossoverRate;
    private final int eliteCount;
    private final Random rand = new Random();


    public GeneticAlgorithm(Graph graph, int populationSize, int maxGenerations, double mutationRate, double crossoverRate, int eliteCount) {
        this.graph = graph;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.eliteCount = eliteCount;
    }

    public Individual run() {
        Population population = new Population(graph, populationSize);

        int bestFitness = population.getBest().getFitness();
        Individual globalBest = population.getBest();

        System.out.println("Generation 0:");
        printPopulation(population);
        for (int gen = 0; gen < maxGenerations; gen++) {
            List<Individual> newIndividuals = new ArrayList<>();

            for(int i = 0; i < eliteCount; i++) {
                newIndividuals.add(population.getIndividuals().get(i).cloneIndividual());
            }

            while (newIndividuals.size() < populationSize) {
                Individual parent1 = tournamentSelection(population);
                Individual parent2 = tournamentSelection(population);
                Individual offspring;

                if (Math.random() < crossoverRate) {
                    offspring = Individual.crossover(parent1, parent2);
                } else {
                    offspring = new Individual(graph);
                    offspring.getGenes().or(parent1.getGenes());
                    offspring.calculateFitness();
                }

                offspring.mutate(mutationRate);
                newIndividuals.add(offspring);
            }

            population.getIndividuals().clear();
            population.getIndividuals().addAll(newIndividuals);

            Individual fittest = population.getBest();
            if (fittest.getFitness() > globalBest.getFitness()) {
                globalBest = fittest;
            }
            // Optional: in ra tiến trình
//            System.out.println("Generation " + gen + ": Best Fitness = " + globalBest.getFitness());
            // In ra quần thể và best fitness của thế hệ
            System.out.println("=== Generation " + gen + " ===");
            printPopulation(population);
            System.out.println("Best Fitness so far: " + globalBest.getFitness());
            System.out.println("--------------------------------------");
        }

        return globalBest;
    }

    private Individual tournamentSelection(Population population) {
        int tournamentSize = 3;
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual ind = population.getIndividuals().get(rand.nextInt(populationSize));
            if (best == null || ind.getFitness() > best.getFitness()) {
                best = ind;
            }
        }
        return best;
    }
    private void printPopulation(Population population) {
        int idx = 0;
        for (Individual ind : population.getIndividuals()) {
            System.out.println("[" + idx + "] " + ind);
            idx++;
        }
    }
}
