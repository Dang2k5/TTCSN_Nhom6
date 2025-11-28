import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

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
    
    // === THÊM CÁC TRƯỜNG MỚI ĐỂ THEO DÕI ===
    private long startTime;
    private int actualGenerations;
    private double finalDiversity;
    private boolean earlyStopped;
    private String stopReason;

    public GeneticAlgorithm(Graph graph, int populationSize, int maxGenerations, double mutationRate, double crossoverRate
            , int eliteCount, int patience, double diversityThreshold) {
        this.graph = graph;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.eliteCount = eliteCount;
        this.patience = Math.max(0, patience);
        this.diversityThreshold = Math.max(diversityThreshold, 0.0);
        
        // Khởi tạo các biến theo dõi
        this.earlyStopped = false;
        this.stopReason = "Đạt số thế hệ tối đa";
        this.actualGenerations = 0;
    }

    public Individual run() {
        // Bắt đầu đo thời gian
        startTime = System.currentTimeMillis();
        
        // Khởi tạo quần thể ban đầu
        population = new Population(graph, populationSize);
        population.sortByFitness();

        // best toàn cục
        Individual globalBest = population.getBest().cloneIndividual();
        int noImprovementCounter = 0;

        // Ghi log ra file 1 lần trong suốt quá trình
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt"))) {

            for (int gen = 0; gen < maxGenerations; gen++) {
                actualGenerations = gen + 1;  // Ghi nhận số thế hệ thực tế
                
                List<Individual> newIndividuals = new ArrayList<>();

                // 1. Giữ lại elite
                for (int i = 0; i < eliteCount && i < populationSize; i++) {
                    newIndividuals.add(population.getIndividuals().get(i).cloneIndividual());
                }

                // 2. Sinh thêm cá thể mới tới đủ populationSize
                while (newIndividuals.size() < populationSize) {
                    Individual parent1 = tournamentSelection(population);
                    Individual parent2 = tournamentSelection(population);
                    Individual offspring;

                    // Lai ghép
                    if (Math.random() < crossoverRate) {
                        offspring = Individual.crossover(parent1, parent2, rand);
                    } else {
                        // Không lai: clone parent1
                        offspring = new Individual(graph);
                        offspring.getGenes().or(parent1.getGenes());
                        offspring.calculateFitness(rand);
                    }

                    // Đột biến + tính lại fitness
                    offspring.mutate(mutationRate);
                    offspring.calculateFitness(rand);
                    newIndividuals.add(offspring);
                }

                // 3. Cập nhật quần thể
                population.getIndividuals().clear();
                population.getIndividuals().addAll(newIndividuals);
                population.sortByFitness();

                // 4. Cập nhật best & bộ đếm không cải thiện
                Individual currentBest = population.getBest();
                if (currentBest.getFitness() > globalBest.getFitness()) {
                    globalBest = currentBest.cloneIndividual();
                    noImprovementCounter = 0;   // reset vì vừa cải thiện
                } else {
                    noImprovementCounter++;     // không cải thiện
                }

                // 5. Tính đa dạng
                double diversity = computeDiversity(population);

                // 6. Ghi log thế hệ hiện tại
                logGeneration(bw, gen, population, noImprovementCounter, diversity);

                // 7. Điều kiện dừng sớm
                boolean stopByPatience = (patience > 0 && noImprovementCounter >= patience);
                boolean stopByDiversity = (diversityThreshold > 0 && diversity < diversityThreshold);

                if (stopByPatience || stopByDiversity) {
                    earlyStopped = true;
                    if (stopByPatience) {
                        stopReason = "Không cải thiện sau " + patience + " thế hệ liên tiếp";
                        System.out.println(
                                "Dừng sớm tại thế hệ " + actualGenerations +
                                        " - " + stopReason
                        );
                    }
                    if (stopByDiversity) {
                        stopReason = "Độ đa dạng quá thấp (" + String.format("%.4f", diversity) + ") < ngưỡng (" + diversityThreshold + ")";
                        System.out.println(
                                "Dừng sớm tại thế hệ " + actualGenerations +
                                        " - " + stopReason
                        );
                    }
                    break; // THOÁT KHỎI FOR, KHÔNG CHẠY THÊM THẾ HỆ NÀO NỮA
                }
            }
            
            // Ghi nhận độ đa dạng cuối cùng
            finalDiversity = computeDiversity(population);
            
        } catch (Exception e) {
            System.err.println("Lỗi trong quá trình chạy thuật toán hoặc ghi log: " + e.getMessage());
            e.printStackTrace();
        }

        return globalBest;
    }

    // === THÊM CÁC PHƯƠNG THỨC GETTER ĐỂ LẤY THÔNG TIN THỰC NGHIỆM ===
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

    private double computeDiversity(Population pop) {
        Set<String> unique = new HashSet<>();
        for (Individual ind : pop.getIndividuals()) {
            String s = ind.getGenes().toString();
            unique.add(s);
        }
        if (pop.getIndividuals().isEmpty()) return 0.0;
        return (double) unique.size() / (double) pop.getIndividuals().size();
    }

    private void logGeneration(BufferedWriter bw,
                               int generation,
                               Population population,
                               int noImprovementCounter,
                               double diversity) {
        try {
            String lineSep = System.lineSeparator();
            StringBuilder sb = new StringBuilder();

            sb.append("===== GENERATION ").append(generation + 1).append(" =====").append(lineSep)
                    .append(lineSep);

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

            sb.append("----------------------------------------")
                    .append(lineSep);

            // Ghi 1 lần
            bw.write(sb.toString());
            bw.flush();

            System.out.println("Đã ghi Generation " + (generation + 1) + " vào file output.txt");

        } catch (Exception e) {
            System.err.println("Lỗi ghi log generation " + generation + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Individual> getPopulation() {
        if (this.population == null || this.population.getIndividuals() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.population.getIndividuals());
    }
}