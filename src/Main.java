import java.io.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Đọc input từ file
        String inputFile = "input2.txt";
        String outputFile = "output.txt";

        try {
            // --- Đọc dữ liệu từ file ---
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            String line;

            // Đọc số đỉnh
            line = reader.readLine();
            if (line == null) {
                System.out.println("File input không có dữ liệu");
                return;
            }
            int n = Integer.parseInt(line.trim());
            if (n < 1) {
                System.out.println("Số đỉnh phải >= 1");
                reader.close();
                return;
            }

            Graph g = new Graph(n);

            // Đọc số cạnh
            line = reader.readLine();
            if (line == null) {
                System.out.println("Thiếu số cạnh trong file");
                reader.close();
                return;
            }
            int m = Integer.parseInt(line.trim());
            if (m <= 0) {
                System.out.println("Số cạnh phải > 0");
                reader.close();
                return;
            }

            // Đọc các cạnh - đỉnh bắt đầu từ 1
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
                // Kiểm tra đỉnh từ 1 đến n
                if (u < 1 || u > n || v < 1 || v > n) {
                    System.out.println("Chỉ số đỉnh không hợp lệ: " + u + " " + v + " (bỏ qua cạnh này)");
                    continue;
                }
                if (u == v) {
                    System.out.println("Bỏ qua self-loop: " + u + " " + v);
                    continue;
                }
                g.addEdge(u, v);
            }

            // Đọc tham số GA
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

            reader.close();

            // --- Khởi tạo và chạy GA ---
            GeneticAlgorithm ga = new GeneticAlgorithm(
                    g,
                    popSize,
                    maxGenerations,
                    mutationRate,
                    crossoverRate,
                    eliteCount,
                    patience,
                    diversityThreshold
            );

            System.out.println("\nBắt đầu chạy Genetic Algorithm...");
            Individual best = ga.run();


            // Lấy toàn bộ population sau khi chạy xong GA
            List<Individual> finalPop = ga.getPopulation();


            int maxFitness = 0;
            for (Individual ind : finalPop) {
                if (ind.getFitness() > maxFitness) {
                    maxFitness = ind.getFitness();
                }
            }

            // --- Ghi kết quả ra file ---
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));

            writer.write("\n" + "=".repeat(50) + "\n");
            writer.write("=".repeat(50) + "\n\n");

            writer.write("Độ thích nghi: " + best.getFitness() + "\n");

            // Ghi danh sách đỉnh có gene = 1 - SỬA: đúng cách xử lý BitSet từ 1 đến n
            writer.write("Đỉnh có trong tập: ");
            int count = 0;
            for (int i = 1; i <= g.size(); i++) { // SỬA: từ 1 đến n
                if (best.getGenes().get(i)) {
                    writer.write(i + " "); // SỬA: ghi trực tiếp i (đã là đỉnh từ 1)
                    count++;
                }
            }
            writer.write("\n");
            writer.write("Tổng số đỉnh được chọn: " + count + "/" + g.size() + "\n");

            // Ghi thông tin genes - SỬA: từ 1 đến n
            writer.write("Genes: ");
            for (int i = 1; i <= g.size(); i++) { // SỬA: từ 1 đến n
                writer.write(best.getGenes().get(i) ? "1" : "0");
                if (i < g.size()) writer.write(" ");
            }
            writer.write("\n");
            writer.close();

            // --- In kết quả ra console ---
            System.out.println("\n==== KẾT QUẢ TỐI ƯU ====");
            System.out.println("Độ thích nghi: " + best.getFitness());
            System.out.print("Đỉnh có trong tập: ");
            count = 0;
            for (int i = 1; i <= g.size(); i++) { // SỬA: từ 1 đến n
                if (best.getGenes().get(i)) {
                    System.out.print(i + " "); // SỬA: ghi trực tiếp i
                    count++;
                }
            }
            System.out.println();
            System.out.println("Tổng số đỉnh được chọn: " + count + "/" + g.size());
            System.out.println("Kết quả đã được ghi vào file: " + outputFile);

        } catch (IOException e) {
            System.out.println("Lỗi đọc/ghi file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}