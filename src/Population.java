import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Population {
    private final List<Individual> individuals;

    /**
     * Khởi tạo quần thể gồm size cá thể, mỗi cá thể là một chuỗi nhị phân ngẫu nhiên.
     * Sử dụng đối tượng Random bên ngoài để đồng nhất nguồn ngẫu nhiên trong toàn bộ GA.
     */
    public Population(Graph graph, int size, Random rand) {
        if (size <= 0) {
            throw new IllegalArgumentException("Population size must be greater than 0");
        }
        individuals = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Individual ind = new Individual(graph, rand);
            individuals.add(ind);
        }
        sortByFitness();
    }

    /**
     * Constructor giữ lại để tương thích với mã cũ (sử dụng Random mới cho mỗi Population).
     * Khuyến nghị: trong GA hãy dùng constructor có Random.
     */
    public Population(Graph graph, int size) {
        this(graph, size, new Random());
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
