import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        String inputFile  = "input2.txt";
        String outputFile = "output.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {

            // ===== ĐỌC GRAPH =====
            String line = reader.readLine();
            if (line == null) {
                System.out.println("File input không có dữ liệu");
                return;
            }

            int n = Integer.parseInt(line.trim());
            if (n < 1) {
                System.out.println("Số đỉnh phải >= 1");
                return;
            }

            Graph g = new Graph(n);

            line = reader.readLine();
            if (line == null) {
                System.out.println("Thiếu số cạnh trong file");
                return;
            }

            int m = Integer.parseInt(line.trim());
            if (m <= 0) {
                System.out.println("Số cạnh phải > 0");
                return;
            }

            System.out.println("Đang đọc các cạnh từ file...");
            for (int i = 0; i < m; i++) {
                line = reader.readLine();
                if (line == null) {
                    System.out.println("Thiếu cạnh thứ " + (i + 1));
                    break;
                }

                String[] tokens = line.trim().split("\\s+");
                if (tokens.length < 2) {
                    System.out.println("Dòng không hợp lệ: " + line + " (bỏ qua)");
                    continue;
                }

                int u = Integer.parseInt(tokens[0]);
                int v = Integer.parseInt(tokens[1]);

                if (u < 1 || u > n || v < 1 || v > n) {
                    System.out.println("Chỉ số đỉnh không hợp lệ: " + u + " " + v + " (bỏ qua)");
                    continue;
                }
                if (u == v) {
                    System.out.println("Bỏ qua self-loop: " + u + " " + v);
                    continue;
                }

                g.addEdge(u, v);
            }

            // ===== ĐỌC THAM SỐ GA =====
            int popSize = 20;
            int maxGenerations = 50;
            double mutationRate = 0.05;
            double crossoverRate = 0.7;
            int eliteCount = 2;
            int patience = 0;
            double diversityThreshold = 0.0;

            try {
                line = reader.readLine();
                if (line != null) popSize = Integer.parseInt(line.trim());

                line = reader.readLine();
                if (line != null) maxGenerations = Integer.parseInt(line.trim());

                line = reader.readLine();
                if (line != null) mutationRate = Double.parseDouble(line.trim());

                line = reader.readLine();
                if (line != null) crossoverRate = Double.parseDouble(line.trim());

                line = reader.readLine();
                if (line != null) eliteCount = Integer.parseInt(line.trim());

                line = reader.readLine();
                if (line != null) patience = Integer.parseInt(line.trim());

                line = reader.readLine();
                if (line != null) diversityThreshold = Double.parseDouble(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Lỗi định dạng tham số GA, sử dụng giá trị mặc định");
            }

            // =========================================================
            // BENCHMARK 10 RUNS -> CHỌN RUN TỐT NHẤT + VẼ BIỂU ĐỒ
            // =========================================================
            int runCount = 10;

            List<Integer> runIdx   = new ArrayList<>();
            List<Double>  runTimes = new ArrayList<>();

            Individual bestOverall = null;
            int bestRunIndex = -1;
            int bestFitness = Integer.MIN_VALUE;

            double bestExecTime = Double.MAX_VALUE;
            int bestActualGenerations = 0;
            double bestFinalDiversity = 0.0;
            boolean bestEarlyStopped = false;
            String bestStopReason = "";

            List<Integer> bestGenHistory = null;
            List<Integer> bestFitnessHistory = null;

            System.out.println("\n=== BENCHMARK: chạy " + runCount + " lần, chọn run tốt nhất ===");

            for (int run = 1; run <= runCount; run++) {
                GeneticAlgorithm gaBench = new GeneticAlgorithm(
                        g, popSize, maxGenerations, mutationRate, crossoverRate,
                        eliteCount, patience, diversityThreshold
                );

                // tắt log + tắt auto-draw trong GA để tránh spam cửa sổ/ghi file
                gaBench.setEnableLogging(false);
                gaBench.setDrawFitnessChart(false);

                Individual bestThisRun = gaBench.run();
                double timeSec = gaBench.getExecutionTime();

                runIdx.add(run);
                runTimes.add(timeSec);

                System.out.println("Run " + run + " | time=" + String.format("%.4f", timeSec) +
                        "s | bestFitness=" + bestThisRun.getFitness());

                // Chọn best: fitness cao nhất; nếu hòa => time nhỏ hơn
                if (bestThisRun.getFitness() > bestFitness ||
                        (bestThisRun.getFitness() == bestFitness && timeSec < bestExecTime)) {

                    bestFitness = bestThisRun.getFitness();
                    bestRunIndex = run;

                    bestOverall = bestThisRun.cloneIndividual();

                    bestExecTime = timeSec;
                    bestActualGenerations = gaBench.getActualGenerations();
                    bestFinalDiversity = gaBench.getFinalDiversity();
                    bestEarlyStopped = gaBench.isEarlyStopped();
                    bestStopReason = gaBench.getStopReason();

                    bestGenHistory = gaBench.getGenHistory();
                    bestFitnessHistory = gaBench.getBestFitnessHistory();
                }
            }

            // ===== VẼ runtime theo run (10 điểm) =====
            RunTimeLineChart.show(runIdx, runTimes);
            RunTimeLineChart.savePng(runIdx, runTimes, "runtime_by_run.png");
            System.out.println("Đã lưu biểu đồ runtime: runtime_by_run.png");

            // ===== VẼ fitness theo generation của BEST RUN =====
            if (bestGenHistory != null && bestFitnessHistory != null && !bestGenHistory.isEmpty()) {
                FitnessLineChart.show(bestGenHistory, bestFitnessHistory);
                FitnessLineChart.savePng(bestGenHistory, bestFitnessHistory, "best_run_fitness_by_generation.png");
                System.out.println("Đã lưu biểu đồ fitness best run: best_run_fitness_by_generation.png");
            }

            // =========================================================
            // IN RA KẾT QUẢ CỦA BEST RUN (KHÔNG CHẠY RUN CHÍNH)
            // =========================================================
            if (bestOverall == null) {
                System.out.println("Không tìm được bestOverall (lỗi).");
                return;
            }

            // ===== IN RA CONSOLE =====
            System.out.println("\n" + "=".repeat(70));
            System.out.println("KẾT QUẢ BEST RUN #" + bestRunIndex + "/" + runCount);
            System.out.println("=".repeat(70));
            System.out.println("Best Fitness: " + bestOverall.getFitness());
            System.out.println("Time (sec): " + String.format("%.4f", bestExecTime));
            System.out.println("Actual Generations: " + bestActualGenerations + "/" + maxGenerations);
            System.out.println("Final Diversity: " + String.format("%.4f", bestFinalDiversity));
            System.out.println("Early Stopped: " + (bestEarlyStopped ? "CÓ" : "KHÔNG"));
            System.out.println("Stop Reason: " + bestStopReason);

            System.out.print("\nĐỉnh có trong tập: ");
            int count = 0;
            for (int i = 1; i <= g.size(); i++) {
                if (bestOverall.getGenes().get(i)) {
                    System.out.print(i + " ");
                    count++;
                }
            }
            System.out.println();
            System.out.println("Tổng số đỉnh được chọn: " + count + "/" + g.size());

            System.out.print("Genes: ");
            for (int i = 1; i <= g.size(); i++) {
                System.out.print(bestOverall.getGenes().get(i) ? "1" : "0");
                if (i < g.size()) System.out.print(" ");
            }
            System.out.println();

            // ===== GHI RA output.txt (CHỈ BEST RUN) =====
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                writer.write("\n" + "=".repeat(70) + "\n");
                writer.write("BEST RUN #" + bestRunIndex + "/" + runCount + " - " + java.time.LocalDateTime.now() + "\n");
                writer.write("=".repeat(70) + "\n\n");

                writer.write("Best Fitness: " + bestOverall.getFitness() + "\n");
                writer.write("Time (sec): " + String.format("%.4f", bestExecTime) + "\n");
                writer.write("Actual Generations: " + bestActualGenerations + "/" + maxGenerations + "\n");
                writer.write("Final Diversity: " + String.format("%.4f", bestFinalDiversity) + "\n");
                writer.write("Early Stopped: " + (bestEarlyStopped ? "CÓ" : "KHÔNG") + "\n");
                writer.write("Stop Reason: " + bestStopReason + "\n");

                writer.write("\nĐỉnh có trong tập: ");
                count = 0;
                for (int i = 1; i <= g.size(); i++) {
                    if (bestOverall.getGenes().get(i)) {
                        writer.write(i + " ");
                        count++;
                    }
                }
                writer.write("\nTổng số đỉnh được chọn: " + count + "/" + g.size() + "\n");

                writer.write("Genes: ");
                for (int i = 1; i <= g.size(); i++) {
                    writer.write(bestOverall.getGenes().get(i) ? "1" : "0");
                    if (i < g.size()) writer.write(" ");
                }
                writer.write("\n");

                writer.write("\nTHÔNG TIN ĐỒ THỊ:\n");
                writer.write("Số đỉnh: " + g.size() + "\n");
                writer.write("Số cạnh: " + m + "\n");

                writer.write("\nTHAM SỐ GA ĐÃ SỬ DỤNG:\n");
                writer.write("Kích thước quần thể: " + popSize + "\n");
                writer.write("Số thế hệ tối đa: " + maxGenerations + "\n");
                writer.write("Tỷ lệ đột biến: " + mutationRate + "\n");
                writer.write("Tỷ lệ lai ghép: " + crossoverRate + "\n");
                writer.write("Số cá thể ưu tú: " + eliteCount + "\n");
                writer.write("Ngưỡng kiên nhẫn: " + patience + "\n");
                writer.write("Ngưỡng đa dạng: " + diversityThreshold + "\n");
            }

            System.out.println("\nĐã ghi output chỉ BEST RUN vào file: " + outputFile);

        } catch (IOException e) {
            System.out.println("Lỗi đọc/ghi file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
