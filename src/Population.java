import java.util.ArrayList;
import java.util.List;

public class Population {
    private final List<Individual>  individuals;

    public Population(Graph graph, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Population size must be greater than 0");
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

}
