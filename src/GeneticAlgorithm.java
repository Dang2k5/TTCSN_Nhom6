import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GeneticAlgorithm {

    private final Graph graph;
    private final int populationSize;
    private final int maxGenerations;
    private final double mutationRate;
    private final double crossoverRate;
    private final int eliteCount;
    private final int patience;
    private final double diversityThreshold;

    private final double indexMin;
    private final double indexMax;

    private final Random rand = new Random();

    private double executionTimeSec;
    private int actualGenerations;
    private double finalDiversity;
    private boolean earlyStopped;
    private String stopReason;

    // Lịch sử fitness để Main tự vẽ biểu đồ
    private final List<Integer> genHistory = new ArrayList<>();
    private final List<Integer> bestFitnessHistory = new ArrayList<>();

    private final List<Integer> genTimeMsHistory = new ArrayList<>();
    // Flag
    private boolean enableLogging = true;  // ghi file output.txt

    // ============================================================
    // CONSTRUCTOR
    // ============================================================
    public GeneticAlgorithm(Graph graph,
                            int populationSize,
                            int maxGenerations,
                            double mutationRate,
                            double crossoverRate,
                            int eliteCount,
                            int patience,
                            double diversityThreshold,
                            double indexMin,
                            double indexMax) {

        this.graph = graph;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.eliteCount = eliteCount;
        this.patience = Math.max(0, patience);
        this.diversityThreshold = Math.max(0.0, diversityThreshold);

        this.indexMin = indexMin;
        this.indexMax = indexMax;

        this.executionTimeSec = 0.0;
        this.actualGenerations = 0;
        this.finalDiversity = 0.0;
        this.earlyStopped = false;
        this.stopReason = "Đạt số thế hệ tối đa";
    }

    // ============================================================
    // SETTER
    // ============================================================
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    // ============================================================
    // CHẠY GA 1 LẦN
    // ============================================================
    public Individual run() {

        // Thống kê 1 lần chạy
        long startTimeMillis = System.currentTimeMillis();
        genHistory.clear();
        bestFitnessHistory.clear();

        genTimeMsHistory.clear();
        earlyStopped = false;
        stopReason = "Đạt số thế hệ tối đa";
        actualGenerations = 0;

        // Khởi tạo quần thể
        Population population = new Population(graph, populationSize, rand);

        Individual globalBest = population.getBest().cloneIndividual();
        int noImproveCount = 0;

        BufferedWriter bw = null;

        try {
            if (enableLogging) {
                bw = new BufferedWriter(new FileWriter("output.txt"));
            }

            for (int gen = 0; gen < maxGenerations; gen++) {
                actualGenerations = gen + 1;


                long genStartNs = System.nanoTime();
                List<Individual> newIndividuals = new ArrayList<>(populationSize);

                // ===== 1) ELITISM =====
                int elites = Math.min(eliteCount, populationSize);
                for (int i = 0; i < elites; i++) {
                    newIndividuals.add(population.getIndividuals().get(i).cloneIndividual());
                }

                // ===== 2) SINH NGẪU NHIÊN CÓ CHỌN LỌC =====
                while (newIndividuals.size() < populationSize) {
                    Individual p1 = selectParentWithIndex(population);
                    Individual p2 = selectParentWithIndex(population);

                    Individual child;

                    if (rand.nextDouble() < crossoverRate) {
                        child = Individual.crossover(p1, p2, rand);
                    } else {
                        child = p1.cloneIndividual();
                    }

                    double mr = mutationRateFor(child);
                    child.mutate(mr, rand);
                    child.calculateFitness(rand);

                    newIndividuals.add(child);
                }

                // ===== 3) CẬP NHẬT QUẦN THỂ =====
                population.getIndividuals().clear();
                population.getIndividuals().addAll(newIndividuals);
                population.sortByFitness();

                // ===== 4) CẬP NHẬT BEST =====
                Individual currentBest = population.getBest();
                if (currentBest.getFitness() > globalBest.getFitness()) {
                    globalBest = currentBest.cloneIndividual();
                    noImproveCount = 0;
                } else {
                    noImproveCount++;
                }

                // ===== 5) GHI LỊCH SỬ =====
                int bestFit = currentBest.getFitness();
                double diversity = computeDiversity(population);

                genHistory.add(actualGenerations);
                bestFitnessHistory.add(bestFit);
                // ===== 6) DỪNG SỚM =====
                boolean stop1 = (patience > 0 && noImproveCount >= patience);
                boolean stop2 = (diversityThreshold > 0 && diversity < diversityThreshold);

                // ===== THỜI GIAN / GENERATION (ms) =====
                long genEndNs = System.nanoTime();
                int genTimeMs = (int) Math.max(0, Math.round((genEndNs - genStartNs) / 1_000_000.0));
                genTimeMsHistory.add(genTimeMs);

                if (enableLogging && bw != null) {
                    bw.write("Gen " + actualGenerations +
                            " | Best=" + bestFit +
                            " | Diversity=" + String.format("%.4f", diversity) +
                            " | TimeMs=" + genTimeMs + "\n");
                }

                if (stop1 || stop2) {
                    earlyStopped = true;

                    if (stop1) stopReason = "Không cải thiện sau " + patience + " thế hệ";
                    if (stop2) stopReason = "Đa dạng thấp: " + String.format("%.4f", diversity);

                    break;
                }
            }

            finalDiversity = computeDiversity(population);

        } catch (Exception e) {
            System.err.println("Lỗi GA: " + e.getMessage());
        }
        finally {
            try { if (bw != null) bw.close(); } catch (Exception ignored) {}
        }

        // Thời gian chạy (giây)
        executionTimeSec = (System.currentTimeMillis() - startTimeMillis) / 1000.0;

        return globalBest;
    }

    // ============================================================
    // HÀM HỖ TRỢ
    // ============================================================

    private boolean isIndexInRange(Individual ind) {
        if (indexMax <= indexMin) return true;
        double score = ind.getIndexScore();
        return score >= indexMin && score <= indexMax;
    }

    private double mutationRateFor(Individual ind) {
        if (indexMax <= indexMin) return mutationRate;

        double idx = ind.getIndexScore();
        double t = (idx - indexMin) / (indexMax - indexMin);

        t = Math.max(0, Math.min(1, t));

        double minM = mutationRate * 0.5;
        double maxM = mutationRate * 2.0;

        return maxM - t * (maxM - minM);
    }

    private Individual rankSelection(Population pop) {
        List<Individual> inds = pop.getIndividuals();
        int n = inds.size();
        double sumRank = n * (n + 1) / 2.0;
        double[] cum = new double[n];

        double acc = 0;
        for (int i = 0; i < n; i++) {
            int rank = n - i;
            acc += rank / sumRank;
            cum[i] = acc;
        }

        double r = rand.nextDouble();
        for (int i = 0; i < n; i++) {
            if (r <= cum[i]) return inds.get(i);
        }
        return inds.get(n - 1);
    }

    private Individual selectParentWithIndex(Population pop) {
        Individual last = null;
        for (int i = 0; i < 10; i++) {
            last = rankSelection(pop);
            if (isIndexInRange(last)) return last;
        }
        return last != null ? last : rankSelection(pop);
    }

    private double computeDiversity(Population pop) {
        Set<String> uniq = new HashSet<>();
        for (Individual ind : pop.getIndividuals()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= graph.size(); i++) {
                sb.append(ind.getGenes().get(i) ? '1' : '0');
            }
            uniq.add(sb.toString());
        }
        return (double) uniq.size() / pop.getIndividuals().size();
    }

    // ============================================================
    // GETTER
    // ============================================================

    public double getExecutionTime() { return executionTimeSec; }
    public int getActualGenerations() { return actualGenerations; }
    public double getFinalDiversity() { return finalDiversity; }
    public boolean isEarlyStopped() { return earlyStopped; }
    public String getStopReason() { return stopReason; }

    public List<Integer> getGenHistory() { return genHistory; }
    public List<Integer> getBestFitnessHistory() { return bestFitnessHistory; }
    public List<Integer> getGenTimeMsHistory() { return genTimeMsHistory; }
}
