import sys
import queue

from math import inf
from math import isinf
from math import sqrt

sys.path.append("../")
sys.path.append("../../../")
from python.network.Network import *

#######################################################
# Heuristics for network with lines only (no transfo) #
#######################################################

def cxcv_heuristic_lines_only(network, start_vertex, I_max):
    
    # Verify start vertex has coherent target V
    #if network.pu and (network.target_v[start_vertex] < 0.5 or network.target_v[start_vertex] > 1.5):
    #    print("[ERROR] %f wrong value of target V in pu." % network.target_v[start_vertex])
    #    return None, None, None
    
    if not network.pu and (network.target_v[start_vertex] < 5 or network.target_v[start_vertex] > 450):
        print("[ERROR] %f wrong value of target V in kV." % network.target_v[start_vertex])
        return None, None, None

    # |Z|=|x| between start_vertex and others
    impedance_x = {bus: inf for bus in range(network.num_of_vertices)}
    impedance_x[start_vertex] = 0

    pq = queue.PriorityQueue()
    pq.put((0, start_vertex))

    # Computation of minimal impendance between start_vertex and others
    flag = False
    while not pq.empty():

        (_, current) = pq.get()
        network.visited.add(current)

        for neighbor in range(network.num_of_vertices):
            # Only look neighbors not yet visited
            if network.edges[current][neighbor] and neighbor not in network.visited:
                
                old_cost = impedance_x[neighbor]
                new_cost = impedance_x[current] + network.x[current][neighbor]

                # If the path is shorter, then change for it
                if new_cost < old_cost:
                    pq.put((new_cost, neighbor))
                    impedance_x[neighbor] = new_cost

    # Verify "close X close V" condition
    for i in range(len(impedance_x)):
        if (not isinf(impedance_x[i])) and i != start_vertex:

            diff_v = abs(network.target_v[i] - network.target_v[start_vertex])
            I_current_i = diff_v / impedance_x[i]

            if (i in network.pv_vertex and I_current_i > I_max):
                print("[WARNING] |V_%i - V_%i| = |%f - %f| = %f and Y = %f and I_%i_%i = %f > %f" % 
                      (start_vertex+1, i+1, network.target_v[start_vertex], network.target_v[i], 
                       diff_v, 1 / impedance_x[i], start_vertex+1, i+1, I_current_i, I_max))
                flag = True

    # Void network visited vertices for futur research
    network.visited.clear()

    return _, impedance_x, flag

def czcv_heuristic_lines_only(network, start_vertex, I_max):
    
    # Verify start vertex has coherent target V
    if not network.pu and (network.target_v[start_vertex] < 5 or network.target_v[start_vertex] > 450):
        print("[ERROR] %f wrong value of target V in kV." % network.target_v[start_vertex])
        return None, None, None

    # |Z|=sqrt(|r|^2+|x|^2) between start_vertex and others
    impedance_r = {bus: inf for bus in range(network.num_of_vertices)}
    impedance_r[start_vertex] = 0

    impedance_x = {bus: inf for bus in range(network.num_of_vertices)}
    impedance_x[start_vertex] = 0

    pq = queue.PriorityQueue()
    pq.put((0, start_vertex))

    # Computation of minimal impendance between start_vertex and others
    flag = False
    while not pq.empty():

        (_, current) = pq.get()
        network.visited.add(current)

        for neighbor in range(network.num_of_vertices):
            # Only look neighbors not yet visited
            if network.edges[current][neighbor] and neighbor not in network.visited:
                
                old_cost = sqrt(impedance_r[neighbor]**2 + impedance_x[neighbor]**2)
                new_cost = sqrt((impedance_r[current] + network.r[current][neighbor])**2 + (impedance_x[current] + network.x[current][neighbor])**2)

                # If the path is shorter, then change for it
                if new_cost < old_cost:
                    pq.put((new_cost, neighbor))
                    impedance_r[neighbor] = impedance_r[current] + network.r[current][neighbor]
                    impedance_x[neighbor] = impedance_x[current] + network.x[current][neighbor]

    # Verify "close Z close V" condition
    for i in range(len(impedance_r)):
        if (not isinf(impedance_r[i]) and not isinf(impedance_x[i])) and i != start_vertex:

            diff_v = abs(network.target_v[i] - network.target_v[start_vertex])
            impedance = sqrt(impedance_r[i]**2 + impedance_x[i]**2)
            I_current_i = diff_v / impedance 

            if (i in network.pv_vertex and I_current_i > I_max):
                print("[WARNING] |V_%i - V_%i| = |%f - %f| = %f and Y = %f and I_%i_%i = %f > %f" 
                      % (start_vertex+1, i+1, network.target_v[start_vertex], network.target_v[i], diff_v, 
                        1 / impedance, start_vertex+1, i+1, I_current_i, I_max))
                flag = True

    # Void network visited vertices for futur research
    network.visited.clear()

    return impedance_r, impedance_x, flag





#################################################
# Heuristics for network with lines AND transfo #
#################################################


# Computation is done with coherence of operations (no per unit)
def cxcv_heuristic_transfo_side_1_only(network, start_vertex, I_max):
    
    # Checks 
    # Verify start vertex has coherent target V
    #if network.target_v[start_vertex] < 0.5 or network.target_v[start_vertex] > 1.5:
    #    print("[ERROR] %f wrong value of target V." % network.target_v[start_vertex])
    #    return None, None, None
    
    # Verify start vertex has coherent target V
    if not network.pu and (network.target_v[start_vertex] < 5 or network.target_v[start_vertex] > 450):
        print("[ERROR] %f wrong value of target V in kV." % network.target_v[start_vertex])
        return None, None, None

    # Verify network has no negative value of x
    for bus1, neighbors in enumerate(network.x):
        for bus2, x in enumerate(neighbors):
            if network.edges[bus1][bus2] and x < 0:
                print("[ERROR] network given has negative value of x.")
                return None, None, None

    # |Z|=|x| between start_vertex and others
    rho = {bus: 1 for bus in range(network.num_of_vertices)}
    impedance_x = {bus: inf for bus in range(network.num_of_vertices)}
    impedance_x[start_vertex] = 0

    # Use PriorityQueue to prioritize closest neighbors in the search
    pq = queue.PriorityQueue()
    pq.put((0, start_vertex))

    # Computation of minimal impendance between start_vertex and others
    flag = False
    while not pq.empty():

        (_, current) = pq.get()
        network.visited.add(current)

        for neighbor in range(network.num_of_vertices):
            # Only look neighbors not yet visited
            if network.edges[current][neighbor] and neighbor not in network.visited:
                
                old_cost = impedance_x[neighbor]
                new_cost = impedance_x[current] * network.rho[current][neighbor] + network.x[current][neighbor] / (network.rho[current][neighbor] * rho[current])

                # If the path is shorter, then change for it
                if new_cost < old_cost:
                    pq.put((new_cost, neighbor))
                    impedance_x[neighbor] = new_cost
                    rho[neighbor] *= network.rho[current][neighbor] 

    # print("Ending, impedance of x :")
    # print(impedance_x)

    # Verify "Close Z Close V" condition
    for i in range(len(impedance_x)):
        if (not isinf(impedance_x[i])) and i != start_vertex:

            diff_v = abs(network.target_v[i] * rho[i] - network.target_v[start_vertex])
            I_current_i = diff_v / impedance_x[i]

            if (i in network.pv_vertex and I_current_i > I_max):
                print("[WARNING] |V_%i - V_%i| = |%f - %f| = %f and Y = %f and I_%i_%i = %f > %f" % 
                      (start_vertex+1, i+1, network.target_v[start_vertex], network.target_v[i], 
                       diff_v, 1 / impedance_x[i], start_vertex+1, i+1, I_current_i, I_max))
                flag = True

    # Void network visited vertices for futur research
    network.visited.clear()

    return _, impedance_x, flag


# Computation is done with coherence of operations (no per unit)
def cxcv_heuristic(network, start_vertex, I_max):
    
    # Checks 
    # Verify start vertex has coherent target V
    #if network.target_v[start_vertex] < 0.5 or network.target_v[start_vertex] > 1.5:
    #    print("[ERROR] %f wrong value of target V." % network.target_v[start_vertex])
    #    return None, None, None
    
    # Verify start vertex has coherent target V
    if not network.pu and (network.target_v[start_vertex] < 5 or network.target_v[start_vertex] > 450):
        print("[ERROR] %f wrong value of target V in kV." % network.target_v[start_vertex])
        return None, None, None

    # Verify network has no negative value of x
    for bus1, neighbors in enumerate(network.x):
        for bus2, x in enumerate(neighbors):
            if network.edges[bus1][bus2] and x < 0:
                print("[ERROR] network given has negative value of x.")
                return None, None, None

    # |Z|=|x| between start_vertex and others
    rho_1 = {bus: 1 for bus in range(network.num_of_vertices)}
    rho_2 = {bus: 1 for bus in range(network.num_of_vertices)}
    z_eq = {bus: inf for bus in range(network.num_of_vertices)}
    z_eq[start_vertex] = 0

    # Use PriorityQueue to prioritize closest neighbors in the search
    pq = queue.PriorityQueue()
    pq.put((0, start_vertex))

    # Computation of minimal impendance between start_vertex and others
    flag = False
    while not pq.empty():

        (_, current) = pq.get()
        network.visited.add(current)

        for neighbor in range(network.num_of_vertices):
            # Only look neighbors not yet visited
            if network.edges[current][neighbor] and neighbor not in network.visited:
                
                old_cost = z_eq[neighbor]
                new_cost = z_eq[current] + network.x[current][neighbor] * rho_2[current]**2 / (rho_1[current]**2 * network.rho[current][neighbor]**2)

                # If the path is shorter, then change for it
                if new_cost < old_cost:
                    pq.put((new_cost, neighbor))
                    z_eq[neighbor] = new_cost
                    rho_1[neighbor] = rho_1[current] * network.rho[current][neighbor] 
                    rho_2[neighbor] = rho_2[current] * network.rho[neighbor][current] 

    # print("Ending, impedance of x :")
    # print(impedance_x)

    # Verify "Close Z Close V" condition
    for i in range(len(z_eq)):
        if (not isinf(z_eq[i])) and i != start_vertex and i in network.pv_vertex:
            diff_v = abs(network.target_v[i] * rho_2[i] / rho_1[i] - network.target_v[start_vertex])
            I_current_i = diff_v / z_eq[i]
            if (I_current_i > I_max):
                print("[WARNING] |V_%i - V_%i| = |%f - %f| = %f and Y = %f and I_%i_%i = %f > %f" % 
                      (start_vertex+1, i+1, network.target_v[start_vertex], network.target_v[i], 
                       diff_v, 1 / z_eq[i], start_vertex+1, i+1, I_current_i, I_max))
                flag = True

    # Void network visited vertices for futur research
    network.visited.clear()

    return _, z_eq, flag

# Computation is done with coherence of operations (no per unit)
def cxcv_heuristic_crazy(network, start_vertex, I_max):
    
    # Checks 
    # Verify start vertex has coherent target V
    #if network.target_v[start_vertex] < 0.5 or network.target_v[start_vertex] > 1.5:
    #    print("[ERROR] %f wrong value of target V." % network.target_v[start_vertex])
    #    return None, None, None
    
    # Verify start vertex has coherent target V
    if not network.pu and (network.target_v[start_vertex] < 5 or network.target_v[start_vertex] > 450):
        print("[ERROR] %f wrong value of target V in kV." % network.target_v[start_vertex])
        return None, None, None

    # Verify network has no negative value of x
    for bus1, neighbors in enumerate(network.x):
        for bus2, x in enumerate(neighbors):
            if network.edges[bus1][bus2] and x < 0:
                print("[ERROR] network given has negative value of x.")
                return None, None, None

    # |Z|=|x| between start_vertex and others
    rho_1 = {bus: 1 for bus in range(network.num_of_vertices)}
    rho_2 = {bus: 1 for bus in range(network.num_of_vertices)}
    z_eq = {bus: inf for bus in range(network.num_of_vertices)}
    prod_Y_ij = {bus: 1 for bus in range(network.num_of_vertices)}
    prod_Y_ij_plus_Y_i = {bus: 1 for bus in range(network.num_of_vertices)}
    z_eq[start_vertex] = 0

    # Use PriorityQueue to prioritize closest neighbors in the search
    pq = queue.PriorityQueue()
    pq.put((0, start_vertex))

    # Computation of minimal impendance between start_vertex and others
    flag = False
    while not pq.empty():

        (_, current) = pq.get()
        network.visited.add(current)

        for neighbor in range(network.num_of_vertices):
            # Only look neighbors not yet visited
            if network.edges[current][neighbor] and neighbor not in network.visited:
                
                old_cost = z_eq[neighbor]
                new_cost = z_eq[current] + prod_Y_ij[current] / (prod_Y_ij_plus_Y_i[current] * (1/network.x[current][neighbor] + network.b[current][neighbor])) * rho_2[current]**2 / (rho_1[current]**2 * network.rho[current][neighbor]**2)

                # If the path is shorter, then change for it
                if new_cost < old_cost:
                    pq.put((new_cost, neighbor))
                    z_eq[neighbor] = new_cost
                    rho_1[neighbor] = rho_1[current] * network.rho[current][neighbor] 
                    rho_2[neighbor] = rho_2[current] * network.rho[neighbor][current] 
                    prod_Y_ij[neighbor] = prod_Y_ij[current] * (1 / network.x[current][neighbor])
                    prod_Y_ij_plus_Y_i[neighbor] = prod_Y_ij_plus_Y_i[current] * ((1 / network.x[current][neighbor]) + network.b[current][neighbor])

    # print("Ending, impedance of x :")
    # print(impedance_x)

    # Verify "Close Z Close V" condition
    for i in range(len(z_eq)):
        if (not isinf(z_eq[i])) and i != start_vertex and i in network.pv_vertex:
            diff_v = abs(network.target_v[i] * rho_2[i] * prod_Y_ij[i] / (rho_1[i] * prod_Y_ij_plus_Y_i[i]) - network.target_v[start_vertex])
            I_current_i = diff_v / z_eq[i]
            if (I_current_i > I_max):
                print("[WARNING] |V_%i - V_%i| = |%f - %f| = %f and Y = %f and I_%i_%i = %f > %f" % 
                      (start_vertex+1, i+1, network.target_v[start_vertex], network.target_v[i], 
                       diff_v, 1 / z_eq[i], start_vertex+1, i+1, I_current_i, I_max))
                flag = True

    # Void network visited vertices for futur research
    network.visited.clear()

    return _, z_eq, flag


if __name__ == "__main__":

    g = Network(3)

    # Define pv vertices of g
    g.target_v = [430, 0, 185]
    g.pv_vertex.add(0)
    g.pv_vertex.add(2)

    # Add the two branches
    g.add_branch(0, 1, 0, 2.86, 1.01)
    g.add_branch(1, 2, 0, 0.17, 0.997 / 2)

    _, impedance_x, flag = cxcv_heuristic(g, 2, 2)
    print("Result expected for I_1_3 = " + str(35.764690))

    _, impedance_x, flag = cxcv_heuristic(g, 0, 2)
    print("Result expected for I_1_3 = " + str(18.006985))