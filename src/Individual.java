import java.util.BitSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Individual {
    // Chuỗi gen: mỗi bit đại diện cho một đỉnh (1..n)
    private final BitSet genes;
    // Độ thích nghi (fitness): số đỉnh trong clique sau khi sửa
    private int fitness;
    // Chỉ mục đánh giá cấu trúc (index) dựa trên mật độ liên kết bên trong
    private double indexScore;
    // Đồ thị tương ứng
    private final Graph graph;

    // ====== CONSTRUCTOR ======

    /**
     * Khởi tạo cá thể với gen nhị phân ngẫu nhiên (0/1) bằng Random bên ngoài truyền vào.
     * Mỗi bit i (1..n): rand.nextDouble() > 0.5 => 1, ngược lại 0.
     * Sau đó tính luôn fitness + index.
     */
    public Individual(Graph graph, Random rand) {
        this.graph = graph;
        this.genes = new BitSet(graph.size() + 1);
        for (int i = 1; i <= graph.size(); i++) {
            genes.set(i, rand.nextDouble() > 0.5);
        }
        calculateFitness(rand);
    }

    /**
     * Constructor riêng để clone / tạo offspring với BitSet đã được chuẩn bị sẵn.
     */
    private Individual(Graph graph, BitSet genes, int fitness, double indexScore) {
        this.graph = graph;
        this.genes = genes;
        this.fitness = fitness;
        this.indexScore = indexScore;
    }

    // ====== GETTER ======

    public BitSet getGenes() {
        return genes;
    }

    public int getFitness() {
        return fitness;
    }

    public double getIndexScore() {
        return indexScore;
    }

    // ====== ĐỘT BIẾN (FLIP-BIT) ======

    /**
     * Đột biến flip-bit: với xác suất mutationRate cho từng gen, ta lật bit 0<->1.
     * Dùng chung đối tượng Random của GA.
     */
    public void mutate(double mutationRate, Random rand) {
        int n = graph.size();
        for (int i = 1; i <= n; i++) {
            if (rand.nextDouble() < mutationRate) {
                genes.flip(i);
            }
        }
    }

    // ====== LAI GHÉP ĐƠN ĐIỂM (ONE-POINT CROSSOVER) ======

    /**
     * Lai ghép đơn điểm: chọn vị trí cắt (1..n-1), trước điểm cắt lấy gen bố 1, sau điểm cắt lấy gen bố 2.
     * Sau khi lai ghép xong, tính lại fitness + index cho con.
     */
    public static Individual crossover(Individual parent1, Individual parent2, Random rand) {
        Graph graph = parent1.graph;
        int n = graph.size();

        BitSet childGenes = new BitSet(n + 1);

        // điểm cắt từ 1..n-1
        int point = 1 + rand.nextInt(Math.max(1, n - 1));

        for (int i = 1; i <= n; i++) {
            if (i < point) {
                childGenes.set(i, parent1.genes.get(i));
            } else {
                childGenes.set(i, parent2.genes.get(i));
            }
        }

        Individual offspring = new Individual(graph, childGenes, 0, 0.0);
        offspring.calculateFitness(rand);
        return offspring;
    }

    // ====== TÍNH FITNESS + INDEX ======

    /**
     * Tính fitness:
     *  - B1: sửa chuỗi gen hiện tại thành một clique hợp lệ (repairToClique)
     *  - B2: mở rộng greedy để thêm các đỉnh còn thiếu nếu vẫn giữ được clique (greedyExpand)
     *  - B3: fitness = số bit 1 sau khi đã sửa (kích thước clique)
     *  - B4: tính thêm chỉ mục indexScore dựa trên mật độ cạnh bên trong
     */
    public void calculateFitness(Random rand) {
        repairToClique();
        greedyExpand(rand);
        fitness = genes.cardinality();
        calculateIndex();
    }

    /**
     * Tính chỉ mục indexScore dựa trên:
     *  - k: số đỉnh đang chọn
     *  - edgesInside: số cạnh bên trong tập đỉnh đang chọn
     *  - maxEdges = k * (k - 1) / 2
     *  - density = edgesInside / maxEdges (∈ [0,1])
     *  - indexScore = density / k (ưu tiên clique nhỏ nhưng dày đặc)
     */
    public void calculateIndex() {
        int k = genes.cardinality();
        if (k <= 1) {
            indexScore = 0.0;
            return;
        }

        int edgesInside = 0;
        for (int i = genes.nextSetBit(1); i >= 1; i = genes.nextSetBit(i + 1)) {
            for (int j = genes.nextSetBit(i + 1); j >= 1; j = genes.nextSetBit(j + 1)) {
                if (graph.isEdge(i, j)) {
                    edgesInside++;
                }
            }
        }

        int maxEdges = k * (k - 1) / 2;
        if (maxEdges == 0) {
            indexScore = 0.0;
            return;
        }

        double density = (double) edgesInside / maxEdges;
        // indexScore: càng ít đỉnh nhưng càng dày đặc thì càng cao
        indexScore = density / k;
    }

    // ====== CÁC HÀM HỖ TRỢ CLIQUE ======

    /** Kiểm tra tập đỉnh (BitSet) có tạo thành clique hay không */
    private boolean isClique(BitSet set) {
        for (int i = set.nextSetBit(1); i >= 1; i = set.nextSetBit(i + 1)) {
            for (int j = set.nextSetBit(i + 1); j >= 1; j = set.nextSetBit(j + 1)) {
                if (!graph.isEdge(i, j)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Tìm đỉnh có bậc trong (internal degree) nhỏ nhất trong tập đang chọn */
    private int findWorstVertexInCurrentSet() {
        int worstV = -1;
        int worstInternalDegree = Integer.MAX_VALUE;

        for (int v = genes.nextSetBit(1); v >= 1; v = genes.nextSetBit(v + 1)) {
            int internalDegree = 0;
            for (int u = genes.nextSetBit(1); u >= 1; u = genes.nextSetBit(u + 1)) {
                if (u != v && graph.isEdge(u, v)) {
                    internalDegree++;
                }
            }
            if (internalDegree < worstInternalDegree) {
                worstInternalDegree = internalDegree;
                worstV = v;
            }
        }
        return worstV;
    }

    /**
     * Sửa chuỗi gen hiện tại thành một clique:
     *  - Nếu chưa là clique, liên tục xóa đỉnh "tệ nhất" (internal degree nhỏ nhất)
     *    cho đến khi tập còn lại là clique hoặc rỗng.
     */
    private void repairToClique() {
        if (genes.isEmpty()) {
            return;
        }

        while (!isClique(genes)) {
            int vertexToRemove = findWorstVertexInCurrentSet();
            if (vertexToRemove == -1) {
                break;
            }
            genes.clear(vertexToRemove);

            if (genes.isEmpty()) {
                break;
            }
        }
    }

    /**
     * Mở rộng clique hiện tại một cách tham lam:
     *  - Lấy danh sách các đỉnh chưa chọn,
     *  - Trộn ngẫu nhiên,
     *  - Thử thêm từng đỉnh nếu vẫn giữ được clique.
     */
    private void greedyExpand(Random rand) {
        int n = graph.size();
        List<Integer> candidates = new ArrayList<>();
        for (int v = 1; v <= n; v++) {
            if (!genes.get(v)) {
                candidates.add(v);
            }
        }
        Collections.shuffle(candidates, rand);

        for (int v : candidates) {
            if (canAddVertex(v)) {
                genes.set(v);
            }
        }
    }

    /** Kiểm tra có thể thêm đỉnh v vào clique hiện tại hay không (v nối với mọi đỉnh trong clique) */
    private boolean canAddVertex(int v) {
        for (int u = genes.nextSetBit(1); u >= 1; u = genes.nextSetBit(u + 1)) {
            if (!graph.isEdge(u, v)) {
                return false;
            }
        }
        return true;
    }

    // ====== CLONE ======

    public Individual cloneIndividual() {
        BitSet clonedGenes = (BitSet) this.genes.clone();
        return new Individual(this.graph, clonedGenes, this.fitness, this.indexScore);
    }

    // ====== DEBUG / LOG ======

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Genes: ");
        for (int i = 1; i <= graph.size(); i++) {
            sb.append(genes.get(i) ? '1' : '0');
        }
        sb.append(", Fitness: ").append(fitness);
        sb.append(", IndexScore: ").append(String.format("%.6f", indexScore));
        return sb.toString();
    }
}
