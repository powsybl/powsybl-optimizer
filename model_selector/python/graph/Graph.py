class Graph:
    def __init__(self, num_of_vertices):
        self.num_of_vertices = num_of_vertices
        self.edges = [[False for _ in range(num_of_vertices)] for _ in range(num_of_vertices)]
        self.weights = [[-1 for _ in range(num_of_vertices)] for _ in range(num_of_vertices)]
        self.visited = set()

    def __str__(self):
        g_str = "\nAdjancy matrix :\n"
        for neighbors in self.edges:
            g_str += str(neighbors) + "\n"
        return g_str

    def add_edge(self, u, v, weight):
        self.edges[u][v] = True
        self.edges[v][u] = True
        self.weights[u][v] = weight
        self.weights[v][u] = weight

    def add_pos_edge(self, u, v, weight):
        if weight < 0:
            print("Tried to add negative edge on positive graph. Edge refused.")
        else:
            self.add_edge(u, v, weight)

    def add_neg_edge(self, u, v, weight):
        if weight > 0:
            print("Tried to add positive edge on negative graph. Edge refused.")
        else:
            self.add_edge(u, v, weight)