package model;

import java.util.BitSet;

public class Individual {
    private final BitSet genes;
    private int fitness;
    private final Graph graph;

    public Individual(Graph graph) {
        this.graph = graph;
        this.genes = new BitSet(graph.size());
        // khởi tạo ngẫu nhiên
        for (int i = 0; i < graph.size(); i++) {
            genes.set(i, Math.random() < 0.5);
        }
        calculateFitness();
    }

    // Khởi tạo cá thể ngẫu nhiên
//    public void randomize() {
//        for (int i = 0; i < graph.size(); i++) {
//            genes.set(i, Math.random() < 0.5);
//        }
//        calculateFitness();
//    }

    // Tính fitness: kích thước clique nếu hợp lệ, ngược lại 0
    public void calculateFitness() {
        int size = genes.cardinality();
        if (size <= 1) {
            fitness = size;
            return;
        }

        boolean valid = true;
        for (int i = 0; i < graph.size(); i++) {
            if (!genes.get(i)) continue;
            for (int j = i + 1; j < graph.size(); j++) {
                if (!genes.get(j)) continue;
                if (!graph.isEdge(i, j)) {
                    valid = false;
                    break;
                }
            }
            if (!valid) break;
        }

        fitness = valid ? size : 0;
    }

    public BitSet getGenes() {
        return genes;
    }

    public int getFitness() {
        return fitness;
    }

    // Đột biến
    public void mutate(double mutationRate) {
        for (int i = 0; i < graph.size(); i++) {
            if (Math.random() < mutationRate) {
                genes.flip(i);
            }
        }
        calculateFitness();
    }

    // Lai tạo đơn điểm
    public static Individual crossover(Individual parent1, Individual parent2) {
        Graph graph = parent1.graph;
        Individual offspring = new Individual(graph);

        // Reset genes ban đầu
        offspring.getGenes().clear();
        int point = 1 + (int)(Math.random() * (graph.size() - 2));

        for (int i = 0; i < graph.size(); i++) {
            if (i < point) offspring.getGenes().set(i, parent1.getGenes().get(i));
            else offspring.getGenes().set(i, parent2.getGenes().get(i));
        }

        offspring.calculateFitness();
        return offspring;
    }
    public Individual cloneIndividual() {
        Individual clone = new Individual(this.graph);
        clone.getGenes().clear();
        clone.getGenes().or(this.genes); // copy nội dung
        clone.fitness = this.fitness;
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Genes: ");
        for (int i = 0; i < graph.size(); i++) {
            sb.append(genes.get(i) ? "1" : "0");
        }
        sb.append(", Fitness: ").append(fitness);
        return sb.toString();
    }
}
