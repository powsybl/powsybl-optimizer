import queue
import sys

def dijkstra(graph, first_vertex):
    distances = {vertex:float('inf') for vertex in range(graph.num_of_vertices)}
    distances[first_vertex] = 0

    pq = queue.PriorityQueue()
    pq.put((0, first_vertex))

    # While there exist a vertex for which shortest path is not evaluated...
    while not pq.empty():
        (_, current_vertex) = pq.get()
        graph.visited.add(current_vertex)

        # For all the neighbors of the vertex
        for neighbor in range(graph.num_of_vertices):
            if graph.edges[current_vertex][neighbor] and neighbor not in graph.visited :

                old_cost = distances[neighbor]
                new_cost = distances[current_vertex] + graph.weights[current_vertex][neighbor]

                # If the path is shorter, then change for it
                if new_cost < old_cost:
                    pq.put((new_cost, neighbor))
                    distances[neighbor] = new_cost

    return distances

if __name__ == "__main__":

    sys.path.append("../")
    from Graph import Graph

    # Build the graph
    g = Graph(9)
    g.add_edge(0, 1, 4)
    g.add_edge(0, 6, 7)
    g.add_edge(1, 6, 11)
    g.add_edge(1, 7, 20)
    g.add_edge(1, 2, 9)
    g.add_edge(2, 3, 6)
    g.add_edge(2, 4, 2)
    g.add_edge(3, 4, 10)
    g.add_edge(3, 5, 5)
    g.add_edge(4, 5, 15)
    g.add_edge(4, 7, 1)
    g.add_edge(4, 8, 5)
    g.add_edge(5, 8, 12)
    g.add_edge(6, 7, 1)
    g.add_edge(7, 8, 3)

    # Compute the shortest path between node 0 and all the others
    distances = dijkstra(g, 0)

    assert(distances[0] == 0)
    assert(distances[1] == 4)
    assert(distances[2] == 11)
    assert(distances[3] == 17)
    assert(distances[4] == 9)
    assert(distances[5] == 22)
    assert(distances[6] == 7)
    assert(distances[7] == 8)
    assert(distances[8] == 11)

    print(g)
