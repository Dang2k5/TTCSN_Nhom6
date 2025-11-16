package model;

public class Graph {
    private final int n;
    private final boolean[][] adj;

    public Graph(int n) {
        this.n = n;
        this.adj = new boolean[n][n];
    }

    public int size() {
        return n;
    }

    public void addEdge(int u, int v) {
        if (u < 0 || u >= n || v < 0 || v >= n) {
            throw new IllegalArgumentException("Vertex index out of bounds");
        }
        adj[u][v] = adj[v][u] = true;
    }

    public boolean isEdge(int u, int v) {
        if (u < 0 || u >= n || v < 0 || v >= n) {
            throw new IllegalArgumentException("Vertex index out of bounds");
        }
        return adj[u][v];
    }
}
