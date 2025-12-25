import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Main {
    static void main(String[] args) {

        String inputFile = "input10.txt";

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {

            // ================== ĐỌC GRAPH ==================
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
                    System.out.println("Cạnh " + u + " " + v + " nằm ngoài phạm vi 1.." + n + " (bỏ qua)");
                    continue;
                }

                if (u == v) {
                    System.out.println("Bỏ qua self-loop: " + u + " " + v);
                    continue;
                }

                g.addEdge(u, v);
            }

            // ================== ĐỌC THAM SỐ GA (NẾU CÓ) ==================
            int popSize          = 20;
            int maxGenerations   = 50;
            double mutationRate  = 0.05;
            double crossoverRate = 0.7;
            int eliteCount       = 2;
            int patience         = 0;
            double diversityThreshold = 0.0;
            double indexMin      = 0.0; // ngưỡng chỉ mục tối thiểu
            double indexMax      = 1.0; // ngưỡng chỉ mục tối đa

            try {
                // Dòng tiếp theo: kích thước quần thể
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    popSize = Integer.parseInt(line.trim());
                }

                // Dòng tiếp theo: số thế hệ tối đa
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    maxGenerations = Integer.parseInt(line.trim());
                }

                // Dòng tiếp theo: tỷ lệ đột biến
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    mutationRate = Double.parseDouble(line.trim());
                }

                // Dòng tiếp theo: tỷ lệ lai ghép
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    crossoverRate = Double.parseDouble(line.trim());
                }

                // Dòng tiếp theo: số cá thể ưu tú
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    eliteCount = Integer.parseInt(line.trim());
                }

                // Dòng tiếp theo: ngưỡng kiên nhẫn (patience)
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    patience = Integer.parseInt(line.trim());
                }

                // Dòng tiếp theo: ngưỡng đa dạng
                line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    diversityThreshold = Double.parseDouble(line.trim());
                }

            } catch (NumberFormatException e) {
                System.out.println("Lỗi định dạng tham số GA trong file, sẽ dùng giá trị mặc định.");
            }

            // ================== TẠO GA VÀ CHẠY 1 LẦN ==================
            GeneticAlgorithm ga = new GeneticAlgorithm(
                    g,
                    popSize,
                    maxGenerations,
                    mutationRate,
                    crossoverRate,
                    eliteCount,
                    patience,
                    diversityThreshold,
                    indexMin,
                    indexMax
            );

            ga.setEnableLogging(true);      // nếu muốn xem log chi tiết trong GA

            System.out.println("\n=== CHẠY CHƯƠNG TRÌNH ===");
            Individual best = ga.run();
            double execTime = ga.getExecutionTime();

            int actualGenerations = ga.getActualGenerations();
            double finalDiversity = ga.getFinalDiversity();
            boolean earlyStopped  = ga.isEarlyStopped();
            String stopReason     = ga.getStopReason();

            // ================== VẼ BIỂU ĐỒ FITNESS THEO THẾ HỆ ==================
            List<Integer> genHistory      = ga.getGenHistory();
            List<Integer> bestFitnessHist = ga.getBestFitnessHistory();

            List<Integer> genTimeMsHist   = ga.getGenTimeMsHistory();
            if (!genHistory.isEmpty()) {
                FitnessLineChart.show(genHistory, bestFitnessHist);
                FitnessLineChart.savePng(genHistory, bestFitnessHist, "fitness_by_generation.png");
                System.out.println("Đã lưu biểu đồ fitness_by_generation.png");
            } else {
                System.out.println("Không có dữ liệu lịch sử fitness để vẽ biểu đồ.");
            }
            // ================== VẼ BIỂU ĐỒ THỜI GIAN THEO THẾ HỆ ==================
            if (!genTimeMsHist.isEmpty() && genTimeMsHist.size() == genHistory.size()) {
                TimeLineChart.show(genHistory, genTimeMsHist);
                TimeLineChart.savePng(genHistory, genTimeMsHist, "time_by_generation.png");
                System.out.println("Đã lưu biểu đồ time_by_generation.png");
            } else if (!genTimeMsHist.isEmpty()) {
                System.out.println("Không vẽ được biểu đồ thời gian vì số điểm dữ liệu không khớp (genHistory="
                        + genHistory.size() + ", genTimeMsHist=" + genTimeMsHist.size() + ").");
            } else {
                System.out.println("Không có dữ liệu lịch sử thời gian theo thế hệ để vẽ biểu đồ.");
            }



            // ================== IN KẾT QUẢ RA MÀN HÌNH ==================

            System.out.println("\n=== Tham số GA ===");
            System.out.println("Kích thước quần thể: " + popSize);
            System.out.println("Số thế hệ tối đa: " + maxGenerations);
            System.out.println("Tỷ lệ đột biến: " + mutationRate);
            System.out.println("Tỷ lệ lai ghép: " + crossoverRate);
            System.out.println("Số cá thể ưu tú: " + eliteCount);
            System.out.println("Ngưỡng kiên nhẫn: " + patience);
            System.out.println("Ngưỡng đa dạng: " + diversityThreshold);

            System.out.println("\n=== Kết quả ===");
            System.out.println("Best fitness: " + best.getFitness());
            System.out.println("Thời gian thực thi: " + String.format("%.4f", execTime) + " s");
            System.out.println("Số thế hệ thực tế: " + actualGenerations);
            System.out.println("Độ đa dạng cuối cùng: " + String.format("%.4f", finalDiversity));
            System.out.println("Dừng sớm?: " + earlyStopped);
            System.out.println("Lý do dừng: " + stopReason);

        } catch (IOException e) {
            System.out.println("Lỗi đọc file input: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
