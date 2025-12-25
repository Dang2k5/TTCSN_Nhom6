public class Graph {
    private final int n;
    private final boolean[][] adj;

    public Graph(int n) {
        if(n <= 0) {
                throw new IllegalArgumentException("Number of vertices must be positive.");
            }
        this.n = n;
        // Tạo ma trận kề với kích thước n+1 để đỉnh chạy từ 1 đến n
        this.adj = new boolean[n + 1][n + 1];
    }

    public int size() {
        return n;
    }

    public void addEdge(int u, int v) {
        // Sửa: đỉnh bắt đầu từ 1 đến n
        if (u < 1 || u > n || v < 1 || v > n) {
            throw new IllegalArgumentException("Vertex index out of bounds: " + u + ", " + v + " (valid: 1-" + n + ")");
        }
        adj[u][v] = adj[v][u] = true;
    }

    public boolean isEdge(int u, int v) {
        // Sửa: đỉnh bắt đầu từ 1 đến n
        if (u < 1 || u > n || v < 1 || v > n) {
            throw new IllegalArgumentException("Vertex index out of bounds: " + u + ", " + v + " (valid: 1-" + n + ")");
        }
        return adj[u][v];
    }

}
