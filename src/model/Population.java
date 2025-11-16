package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Population {
    private List<Individual> individuals;

    public Population(Graph graph, int size) {
        if(size <= 0){
            throw  new IllegalArgumentException("Population size must be greater than 0");
        }
        individuals = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Individual ind = new Individual(graph);
            individuals.add(ind);
        }
        sortByFitness();
    }

    public void sortByFitness() {
        individuals.sort((a, b) -> b.getFitness() - a.getFitness());
    }

    public List<Individual> getIndividuals() {
        return individuals;
    }

    public Individual getBest() {
        return individuals.get(0);
    }

    public Individual getWorst() {
        return individuals.get(individuals.size() - 1);
    }

//    public Individual getFittest() {
//        return Collections.max(individuals, (a, b) -> Integer.compare(a.getFitness(), b.getFitness()));
//    }
}
