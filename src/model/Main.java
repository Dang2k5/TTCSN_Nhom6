package model;

public class Main {
    public static void main(String[] args) {
        // Tạo đồ thị ví dụ
        Graph graph = new Graph(5);
        graph.addEdge(0,1);
        graph.addEdge(0,2);
        graph.addEdge(0,3);
        graph.addEdge(0,4);
        graph.addEdge(1,2);
        graph.addEdge(1,3);
        graph.addEdge(1,4);
        graph.addEdge(2,3);
        graph.addEdge(3,4);

        GeneticAlgorithm ga = new GeneticAlgorithm(
                graph,
                20,     // population size
                50,     // max generations
                0.05,   // mutation rate
                0.7,    // crossover rate
                2       // elite count
        );

        Individual best = ga.run();
        System.out.println("Best Solution Found:");
        System.out.println(best);
    }
}
