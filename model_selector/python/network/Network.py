import sys
from python.graph.Graph import Graph
from math import inf

class Network(Graph):
    voltages = []
    target_v = []
    pv_vertex = set()
    pu = True

    def __init__(self, num_of_vertices):
        super().__init__(num_of_vertices)
        self.target_v = [inf for _ in range(num_of_vertices)]
        self.voltages = [inf for _ in range(num_of_vertices)]

        self.r = [[-1 for _ in range(num_of_vertices)] for _ in range(num_of_vertices)]
        self.x = [[-1 for _ in range(num_of_vertices)] for _ in range(num_of_vertices)]
        self.rho = [[-1 for _ in range(num_of_vertices)] for _ in range(num_of_vertices)]
        self.b = [[0 for _ in range(num_of_vertices)] for _ in range(num_of_vertices)]
    
    def __str__(self):
        n_str = super().__str__()

        # Add info on voltages
        n_str += "\nVoltages :\n"
        n_str += str(self.voltages) + "\n"

        # Add info on target V
        n_str += "\nTarget V :\n"
        n_str += str(self.target_v)
        return n_str
        
    def add_voltage(self, vertex, value):
        self.voltages[vertex] = value

    def add_target_v(self, vertex, value):
        self.target_v[vertex] = value
        self.pv_vertex.add(vertex)

    def add_branch(self, vertex1, vertex2, r, x, rho=1, b=0):
        if x >= 0:
            self.edges[vertex1][vertex2] = True
            self.edges[vertex2][vertex1] = True
            self.r[vertex1][vertex2] = r
            self.r[vertex2][vertex1] = r
            self.x[vertex1][vertex2] = x
            self.x[vertex2][vertex1] = x
            self.rho[vertex1][vertex2] = rho
            self.rho[vertex2][vertex1] = 1
            self.b[vertex1][vertex2] = b
            self.b[vertex2][vertex1] = 0
        else:
            print("Try to add edge with negative x !")


def import_network(df_sub, df_buses, df_branches, df_generators, df_svc, df_tct, df_rtc, df_ptc, pu=True):
    
    network = import_network_pu(df_buses, df_branches, df_generators, df_svc, df_tct, df_rtc, df_ptc)

    if pu:
        return network
    
    # Remove per unit for...

    # TODO : add remove for voltages

    # ... target v values
    for num_bus, value_pu in enumerate(network.target_v): # num_bus = df_buses["num"] - 1
        num_sub = df_buses["substation"].get(num_bus)
        nom_v = df_sub[df_sub["num"] == num_sub]["nomV (KV)"].head(1).item()
        network.target_v[num_bus] = value_pu * nom_v 

    # ... for r, x and rho
    for num_bus_1 in range(network.num_of_vertices):
        for num_bus_2 in range(network.num_of_vertices):
            if network.edges[num_bus_1][num_bus_2] and (df_branches[df_branches["bus1"] == num_bus_1 + 1][df_branches["bus2"] == num_bus_2 + 1].shape[0] > 0):

                num_sub_1 = df_branches[df_branches["bus1"] == num_bus_1 + 1][df_branches["bus2"] == num_bus_2 + 1]["sub.1"].head(1).item()
                num_sub_2 = df_branches[df_branches["bus1"] == num_bus_1 + 1][df_branches["bus2"] == num_bus_2 + 1]["sub.2"].head(1).item()
                
                nom_v_1 = df_sub[df_sub["num"] == num_sub_1]["nomV (KV)"].head(1).item()
                nom_v_2 = df_sub[df_sub["num"] == num_sub_2]["nomV (KV)"].head(1).item()

                if network.r[num_bus_1][num_bus_2] != -1:
                    network.r[num_bus_1][num_bus_2] *= nom_v_2 * nom_v_2 / 100
                    network.r[num_bus_2][num_bus_1] = network.r[num_bus_1][num_bus_2]

                if network.x[num_bus_1][num_bus_2] != -1:
                    network.x[num_bus_1][num_bus_2] *= nom_v_2 * nom_v_2 / 100
                    network.x[num_bus_2][num_bus_1] = network.x[num_bus_1][num_bus_2]

                if network.rho[num_bus_1][num_bus_2] != -1:
                    network.rho[num_bus_1][num_bus_2] *= nom_v_2 / nom_v_1

                if network.b[num_bus_1][num_bus_2] != 0:
                    network.b[num_bus_1][num_bus_2] *= 100 / (nom_v_2 * nom_v_2)
                    #network.rho[num_bus_2][num_bus_1] *= nom_v_2 / nom_v_1

    network.pu = False

    return network
    # Else, return the network de per united


def import_network_pu(df_buses, df_branches, df_generators, df_svc, df_tct, df_rtc, df_ptc):
    g = Network(df_buses.shape[0])

    for _, row in df_branches.iterrows():
        if row["bus1"] != -1 and row["bus2"] != -1:
            r = row["r (pu)"]
            x = row["x (pu)"]
            rho = row["cst ratio (pu)"]
            b = row["b1 (pu)"]
            if row["ratio tc"] != -1:
                num_rtc = row["ratio tc"]
                num_table, num_tap = df_rtc["table"].get(num_rtc - 1), df_rtc["tap"].get(num_rtc - 1)
                x = df_tct[df_tct["num"] == num_table][df_tct["tap"] == num_tap]["x (pu)"].head(1).item()
                rho *= df_tct[df_tct["num"] == num_table][df_tct["tap"] == num_tap]["var ratio"].head(1).item()

            if row["phase tc"] != -1:
                num_ptc = row["phase tc"]
                num_table, num_tap = df_ptc["table"].get(num_ptc - 1), df_ptc["tap"].get(num_ptc - 1)
                x = df_tct[df_tct["num"] == num_table][df_tct["tap"] == num_tap]["x (pu)"].head(1).item()
                rho *= df_tct[df_tct["num"] == num_table][df_tct["tap"] == num_tap]["var ratio"].head(1).item()

            # If a branch already links bus 1 and bus 2, add the branch only if x is less than the one of current branch
            if g.edges[row["bus1"]-1][row["bus2"]-1]:
                if x < g.x[row["bus1"]-1][row["bus2"]-1]:
                    g.add_branch(row["bus1"]-1, row["bus2"]-1, r, x, rho, b)
            else:
                g.add_branch(row["bus1"]-1, row["bus2"]-1, r, x, rho, b)

    for _, row in df_buses.iterrows():
        g.add_voltage(row["num"]-1, row["v (pu)"])

    eps = 0.1
    for _, row in df_generators.iterrows():
        regulate = row["v regul."] and not (abs(row["Q (MVar)"] - row["maxQ0 (MVar)"]) <= eps or abs(row["Q (MVar)"] - row["minQ0 (MVar)"]) <= eps)
        if regulate and row["bus"] != -1:
            g.add_target_v(row["bus"]-1, row["targetV (pu)"])

    for _, row in df_svc.iterrows():
        regulate = row["v regul."]
        if regulate and row["bus"] != -1:
            g.add_target_v(row["bus"]-1, row["targetV (pu)"])

    return g