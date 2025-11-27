import java.util.*;

public class Individual {
    private final BitSet genes;
    private int fitness;
    private final Graph graph;

    public Individual(Graph graph) {
        this.graph = graph;
        this.genes = new BitSet(graph.size() + 1);
        // khởi tạo ngẫu nhiên
        for (int i = 1; i <= graph.size(); i++) {
            genes.set(i, Math.random() < 0.5);
        }
        calculateFitness(new Random());
    }

    // Tính fitness: kích thước clique nếu hợp lệ, ngược lại 0
    public void calculateFitness(Random rand) {
        repairToClique();
        greedyExpand(rand);
        fitness = genes.cardinality();
    }


    public BitSet getGenes() {
        return genes;
    }

    public int getFitness() {
        return fitness;
    }

    // Đột biến
    public void mutate(double mutationRate) {
        for (int i = 1; i <= graph.size(); i++) {
            if (Math.random() < mutationRate) {
                genes.flip(i);
            }
        }
    }

    // Lai tạo đơn điểm
    public static Individual crossover(Individual parent1, Individual parent2, Random rand) {
        Graph graph = parent1.graph;
        Individual offspring = new Individual(graph);

        // Reset genes ban đầu
        offspring.getGenes().clear();
        int point = 1 + (int)(Math.random() * (graph.size() - 1));

        for (int i = 1; i <= graph.size(); i++) {
            if (i < point) offspring.getGenes().set(i, parent1.getGenes().get(i));
            else offspring.getGenes().set(i, parent2.getGenes().get(i));
        }

        offspring.calculateFitness(rand);
        return offspring;
    }
    /** Check if a BitSet of vertices is a clique */
    private boolean isClique(BitSet set) {
        for (int i = set.nextSetBit(0); i >= 1; i = set.nextSetBit(i + 1)) {
            for (int j = set.nextSetBit(i + 1); j >= 1; j = set.nextSetBit(j + 1)) {
                if (!graph.isEdge(i, j)) return false;
            }
        }
        return true;
    }

    /** Find vertex with minimum connections inside current selected set */
    private int findWorstVertexInCurrentSet() {
        int worstV = -1;
        int worstInternalDegree = Integer.MAX_VALUE;

        for (int v = genes.nextSetBit(1); v >= 1; v = genes.nextSetBit(v + 1)) {
            int internalDegree = 0;
            for (int u = genes.nextSetBit(0); u >= 1; u = genes.nextSetBit(u + 1)) {
                if (u != v && graph.isEdge(u, v)) internalDegree++;
            }
            if (internalDegree < worstInternalDegree) {
                worstInternalDegree = internalDegree;
                worstV = v;
            }
        }
        return worstV;
    }


    private void repairToClique() {
        if (genes.isEmpty()) return;

        while (!isClique(genes)) {
            int vertexToRemove = findWorstVertexInCurrentSet();
            if (vertexToRemove == -1) break;
            genes.clear(vertexToRemove);

            if (genes.isEmpty()) break;
        }
    }

    private void greedyExpand(Random rand) {
        int n = graph.size();
        List<Integer> candidates = new ArrayList<>();
        // SỬA: duyệt từ đỉnh 1 đến n
        for (int v = 1; v <= n; v++) {
            if (!genes.get(v)) candidates.add(v);
        }
        Collections.shuffle(candidates, rand);

        for (int v : candidates) {
            if (canAddVertex(v)) genes.set(v);
        }
    }

    private boolean canAddVertex(int v) {
        for (int u = genes.nextSetBit(1); u >= 1; u = genes.nextSetBit(u + 1)) {
            if (!graph.isEdge(u, v)) return false;
        }
        return true;
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
        for (int i = 1; i <= graph.size(); i++) {
            sb.append(genes.get(i) ? "1" : "0");
        }
        sb.append(", Fitness: ").append(fitness);
        return sb.toString();
    }
}
