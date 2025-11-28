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
    private final Random rand = new Random();

    private Population population;

    // === THEO DÕI THỰC NGHIỆM (CHO 1 RUN) ===
    private long startTime;
    private int actualGenerations;
    private double finalDiversity;
    private boolean earlyStopped;
    private String stopReason;

    // === HISTORY ĐỂ VẼ BIỂU ĐỒ FITNESS THEO GENERATION (CHO 1 RUN) ===
    private final List<Integer> genHistory = new ArrayList<>();
    private final List<Integer> bestFitnessHistory = new ArrayList<>();

    // === HISTORY ĐỂ VẼ BIỂU ĐỒ THỜI GIAN THEO SỐ LẦN CHẠY (N RUNS) ===
    private final List<Integer> runHistory = new ArrayList<>();
    private final List<Double> runTimeHistory = new ArrayList<>();

    // === CONTROL FLAGS ===
    private boolean enableLogging = true;      // ghi output.txt theo generation
    private boolean drawFitnessChart = true;   // vẽ chart fitness (bar/line) sau run

    public GeneticAlgorithm(Graph graph,
                            int populationSize,
                            int maxGenerations,
                            double mutationRate,
                            double crossoverRate,
                            int eliteCount,
                            int patience,
                            double diversityThreshold) {

        this.graph = graph;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.eliteCount = eliteCount;

        this.patience = Math.max(0, patience);
        this.diversityThreshold = Math.max(0.0, diversityThreshold);

        this.earlyStopped = false;
        this.stopReason = "Đạt số thế hệ tối đa";
        this.actualGenerations = 0;
        this.finalDiversity = 0.0;
    }

    // ================== SETTERS ĐỂ MAIN TẮT/BẬT ==================

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public void setDrawFitnessChart(boolean drawFitnessChart) {
        this.drawFitnessChart = drawFitnessChart;
    }

    // ================== CHẠY 1 LẦN ==================

    public Individual run() {
        startTime = System.currentTimeMillis();

        // reset history cho 1 run
        genHistory.clear();
        bestFitnessHistory.clear();

        // reset trạng thái theo dõi
        earlyStopped = false;
        stopReason = "Đạt số thế hệ tối đa";
        actualGenerations = 0;
        finalDiversity = 0.0;

        // Khởi tạo quần thể ban đầu
        population = new Population(graph, populationSize);
        population.sortByFitness();

        Individual globalBest = population.getBest().cloneIndividual();
        int noImprovementCounter = 0;

        // Nếu tắt logging => bw = null và không ghi file
        BufferedWriter bw = null;

        try {
            if (enableLogging) {
                bw = new BufferedWriter(new FileWriter("output.txt"));
            }

            for (int gen = 0; gen < maxGenerations; gen++) {
                actualGenerations = gen + 1;

                List<Individual> newIndividuals = new ArrayList<>(populationSize);

                // 1) Elitism
                int elites = Math.min(eliteCount, populationSize);
                for (int i = 0; i < elites; i++) {
                    newIndividuals.add(population.getIndividuals().get(i).cloneIndividual());
                }

                // 2) Sinh cá thể mới
                while (newIndividuals.size() < populationSize) {
                    Individual parent1 = tournamentSelection(population);
                    Individual parent2 = tournamentSelection(population);

                    Individual offspring;

                    if (rand.nextDouble() < crossoverRate) {
                        offspring = Individual.crossover(parent1, parent2, rand);
                    } else {
                        // clone đúng nghĩa
                        offspring = parent1.cloneIndividual();
                    }

                    offspring.mutate(mutationRate);
                    offspring.calculateFitness(rand);

                    newIndividuals.add(offspring);
                }

                // 3) Update quần thể
                population.getIndividuals().clear();
                population.getIndividuals().addAll(newIndividuals);
                population.sortByFitness();

                // 4) Update best & no-improve counter
                Individual currentBest = population.getBest();
                if (currentBest.getFitness() > globalBest.getFitness()) {
                    globalBest = currentBest.cloneIndividual();
                    noImprovementCounter = 0;
                } else {
                    noImprovementCounter++;
                }

                // 5) Diversity
                double diversity = computeDiversity(population);

                // 6) Lưu history (gen -> best fitness)
                genHistory.add(gen + 1);
                bestFitnessHistory.add(currentBest.getFitness());

                // 7) Log nếu bật
                if (enableLogging && bw != null) {
                    logGeneration(bw, gen, population, noImprovementCounter, diversity);
                }

                // 8) Early stop
                boolean stopByPatience = (patience > 0 && noImprovementCounter >= patience);
                boolean stopByDiversity = (diversityThreshold > 0 && diversity < diversityThreshold);

                if (stopByPatience || stopByDiversity) {
                    earlyStopped = true;

                    if (stopByPatience) {
                        stopReason = "Không cải thiện sau " + patience + " thế hệ liên tiếp";
                    }

                    if (stopByDiversity) {
                        stopReason = "Độ đa dạng quá thấp (" + String.format("%.4f", diversity) +
                                ") < ngưỡng (" + diversityThreshold + ")";
                    }

                    System.out.println("Dừng sớm tại thế hệ " + actualGenerations + " - " + stopReason);
                    break;
                }
            }

            finalDiversity = computeDiversity(population);

        } catch (Exception e) {
            System.err.println("Lỗi trong quá trình chạy thuật toán hoặc ghi log: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
            } catch (Exception ignored) {}
        }

        // Vẽ fitness chart (bar/line) nếu bật
        if (drawFitnessChart) {
            try {
                FitnessLineChart.show(genHistory, bestFitnessHistory);
                FitnessLineChart.savePng(genHistory, bestFitnessHistory, "fitness_by_generation_line.png");
            } catch (Exception e) {
                System.err.println("Không thể vẽ/lưu biểu đồ fitness: " + e.getMessage());
            }
        }

        return globalBest;
    }

    // ================== CHẠY N LẦN + VẼ BIỂU ĐỒ THỜI GIAN ==================
    // Trả về best tốt nhất trong N runs (fitness lớn nhất).
    public Individual runMultiple(int runCount, boolean drawRuntimeChart) {
        runHistory.clear();
        runTimeHistory.clear();

        Individual bestOverall = null;
        double bestFitness = -1;

        // Khi benchmark: tắt log + tắt fitness chart để không spam
        boolean oldLog = enableLogging;
        boolean oldChart = drawFitnessChart;

        enableLogging = false;
        drawFitnessChart = false;

        for (int i = 1; i <= runCount; i++) {
            Individual bestThisRun = run();
            double timeSec = getExecutionTime();

            runHistory.add(i);
            runTimeHistory.add(timeSec);

            if (bestThisRun.getFitness() > bestFitness) {
                bestFitness = bestThisRun.getFitness();
                bestOverall = bestThisRun;
            }

            System.out.println("Run " + i + " | time=" + String.format("%.4f", timeSec)
                    + "s | bestFitness=" + bestThisRun.getFitness());
        }

        // restore flags
        enableLogging = oldLog;
        drawFitnessChart = oldChart;

        // Vẽ runtime chart nếu muốn
        if (drawRuntimeChart) {
            try {
                RunTimeLineChart.show(runHistory, runTimeHistory);
                RunTimeLineChart.savePng(runHistory, runTimeHistory, "runtime_by_run.png");
            } catch (Exception e) {
                System.err.println("Không thể vẽ/lưu biểu đồ runtime: " + e.getMessage());
            }
        }

        return bestOverall;
    }

    // ================= GETTERS =================

    public double getExecutionTime() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }

    public int getActualGenerations() {
        return actualGenerations;
    }

    public double getFinalDiversity() {
        return finalDiversity;
    }

    public boolean isEarlyStopped() {
        return earlyStopped;
    }

    public String getStopReason() {
        return stopReason;
    }

    public List<Integer> getGenHistory() {
        return new ArrayList<>(genHistory);
    }

    public List<Integer> getBestFitnessHistory() {
        return new ArrayList<>(bestFitnessHistory);
    }

    public List<Integer> getRunHistory() {
        return new ArrayList<>(runHistory);
    }

    public List<Double> getRunTimeHistory() {
        return new ArrayList<>(runTimeHistory);
    }

    public List<Individual> getPopulation() {
        if (this.population == null || this.population.getIndividuals() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.population.getIndividuals());
    }

    // ================= GA HELPERS =================

    private Individual tournamentSelection(Population population) {
        int tournamentSize = 3;

        Individual best = null;
        List<Individual> inds = population.getIndividuals();

        for (int i = 0; i < tournamentSize; i++) {
            Individual ind = inds.get(rand.nextInt(inds.size()));
            if (best == null || ind.getFitness() > best.getFitness()) {
                best = ind;
            }
        }

        return best;
    }

    private double computeDiversity(Population pop) {
        List<Individual> inds = pop.getIndividuals();
        if (inds == null || inds.isEmpty()) return 0.0;

        Set<String> unique = new HashSet<>();
        for (Individual ind : inds) {
            unique.add(ind.getGenes().toString());
        }

        return (double) unique.size() / (double) inds.size();
    }

    private void logGeneration(BufferedWriter bw,
                               int generation,
                               Population population,
                               int noImprovementCounter,
                               double diversity) {
        try {
            String lineSep = System.lineSeparator();
            StringBuilder sb = new StringBuilder();

            sb.append("===== GENERATION ").append(generation + 1).append(" =====").append(lineSep).append(lineSep);

            sb.append("Population Details:").append(lineSep);
            int index = 0;
            for (Individual ind : population.getIndividuals()) {
                sb.append("[").append(index).append("] ")
                        .append(ind.toString())
                        .append(lineSep);
                index++;
            }

            sb.append("Best Fitness: ")
                    .append(population.getBest().getFitness())
                    .append(lineSep);

            sb.append("Population Diversity: ")
                    .append(String.format("%.4f", diversity))
                    .append(lineSep);

            sb.append("Số thế hệ liên tiếp không cải thiện: ")
                    .append(noImprovementCounter)
                    .append(lineSep);

            sb.append("----------------------------------------").append(lineSep);

            bw.write(sb.toString());
            bw.flush();

        } catch (Exception e) {
            System.err.println("Lỗi ghi log generation " + (generation + 1) + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
