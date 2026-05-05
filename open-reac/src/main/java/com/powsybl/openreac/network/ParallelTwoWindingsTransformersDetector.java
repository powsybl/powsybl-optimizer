/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Detects sets of two-windings transformers acting in parallel.
 *
 * <p>Two detection strategies are combined:
 * <ol>
 *   <li><b>Simple parallels</b>: 2WTs sharing the exact same pair of buses
 *       in the {@code BusView}.</li>
 *   <li><b>Complex parallels</b>: 2WTs belonging to a chordless cycle of size
 *       3 or 4 inside a single substation, filtered by nominal voltage pair so
 *       that only transformers performing the same conversion are grouped
 *       together. Connected components of the resulting per-voltage-pair
 *       subgraph yield the sets.</li>
 * </ol>
 *
 * <p>Only transformers carrying a ratio tap changer are returned. Detected
 * groups that share at least one transformer are merged transitively
 * (if {@code A // B} and {@code B // C}, then {@code A // B // C}).
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class ParallelTwoWindingsTransformersDetector {

    private ParallelTwoWindingsTransformersDetector() {
        // utility class
    }

    /**
     * @return groups of parallel ratio-tap-changer-bearing transformers,
     *         each as a set of transformer ids. Singletons are filtered out.
     */
    public static List<Set<String>> detect(Network network) {
        Set<String> ratioTapChangerIds = network.getTwoWindingsTransformerStream()
                .filter(t -> t.getRatioTapChanger() != null)
                .map(Identifiable::getId)
                .collect(Collectors.toSet());

        List<Set<String>> all = new ArrayList<>();
        all.addAll(detectSimpleParallels(network, ratioTapChangerIds));
        all.addAll(detectComplexParallels(network));

        // Keep only RTCs
        all = all.stream()
                .map(s -> {
                    Set<String> filtered = new HashSet<>(s);
                    filtered.retainAll(ratioTapChangerIds);
                    return filtered;
                })
                .filter(s -> s.size() >= 2)
                .collect(Collectors.toList());

        // Merge overlapping sets transitively
        return mergeOverlappingSets(all);
    }

    // ---- Step 1: simple parallels (same pair of BusView buses) ----

    private static List<Set<String>> detectSimpleParallels(Network network, Set<String> ratioTapChangerIds) {
        Map<UnorderedPair, List<String>> byBusPair = new HashMap<>();
        for (TwoWindingsTransformer twt : network.getTwoWindingsTransformers()) {
            if (!ratioTapChangerIds.contains(twt.getId())) {
                continue;
            }
            Bus b1 = twt.getTerminal1().getBusView().getBus();
            Bus b2 = twt.getTerminal2().getBusView().getBus();
            if (b1 == null || b2 == null) {
                continue;
            }
            byBusPair.computeIfAbsent(UnorderedPair.of(b1.getId(), b2.getId()), k -> new ArrayList<>())
                    .add(twt.getId());
        }
        return byBusPair.values().stream()
                .filter(list -> list.size() >= 2)
                .map(HashSet<String>::new)
                .collect(Collectors.toList());
    }

    // ---- Step 2: complex parallels (chordless cycles ≤ 4 inside substations) ----

    private static List<Set<String>> detectComplexParallels(Network network) {
        List<Set<String>> result = new ArrayList<>();
        for (Substation substation : network.getSubstations()) {
            IntraSubstationGraph graph = buildIntraSubstationGraph(substation);
            if (graph.adjacency.size() < 3) {
                continue; // No cycle of length ≥ 3 possible
            }
            for (List<String> cycleNodes : graph.findChordlessCyclesUpToSize4()) {
                processCycle(graph, cycleNodes, result);
            }
        }
        return result;
    }

    private static void processCycle(IntraSubstationGraph graph, List<String> cycleNodes, List<Set<String>> result) {
        // Edges of the cycle, grouped by their nominal voltage pair
        Map<NominalVoltagePair, List<EdgeKey>> edgesByVoltage = new HashMap<>();
        int n = cycleNodes.size();
        for (int i = 0; i < n; i++) {
            EdgeKey edge = EdgeKey.of(cycleNodes.get(i), cycleNodes.get((i + 1) % n));
            NominalVoltagePair vp = graph.edges.get(edge).voltagePair;
            edgesByVoltage.computeIfAbsent(vp, k -> new ArrayList<>()).add(edge);
        }
        // Within each voltage subgraph, find connected components
        for (List<EdgeKey> edgesAtVoltage : edgesByVoltage.values()) {
            for (Set<EdgeKey> component : connectedComponents(edgesAtVoltage)) {
                Set<String> transformerIds = new HashSet<>();
                for (EdgeKey edge : component) {
                    transformerIds.addAll(graph.edges.get(edge).transformerIds);
                }
                if (transformerIds.size() >= 2) {
                    result.add(transformerIds);
                }
            }
        }
    }

    private static List<Set<EdgeKey>> connectedComponents(Collection<EdgeKey> edges) {
        // Build an adjacency from the edge set, then BFS/DFS for components
        Map<String, Set<String>> adj = new HashMap<>();
        for (EdgeKey e : edges) {
            adj.computeIfAbsent(e.busId1, k -> new HashSet<>()).add(e.busId2);
            adj.computeIfAbsent(e.busId2, k -> new HashSet<>()).add(e.busId1);
        }
        Map<String, Integer> compOf = new HashMap<>();
        int compIdx = 0;
        for (String start : adj.keySet()) {
            if (compOf.containsKey(start)) {
                continue;
            }
            Deque<String> stack = new ArrayDeque<>();
            stack.push(start);
            while (!stack.isEmpty()) {
                String node = stack.pop();
                if (compOf.putIfAbsent(node, compIdx) != null) {
                    continue;
                }
                for (String nbr : adj.getOrDefault(node, Set.of())) {
                    if (!compOf.containsKey(nbr)) {
                        stack.push(nbr);
                    }
                }
            }
            compIdx++;
        }
        List<Set<EdgeKey>> components = new ArrayList<>();
        for (int i = 0; i < compIdx; i++) {
            components.add(new HashSet<>());
        }
        for (EdgeKey e : edges) {
            components.get(compOf.get(e.busId1)).add(e);
        }
        return components;
    }

    // ---- Graph construction ----

    private static IntraSubstationGraph buildIntraSubstationGraph(Substation substation) {
        IntraSubstationGraph graph = new IntraSubstationGraph();
        for (TwoWindingsTransformer twt : substation.getTwoWindingsTransformers()) {
            Bus b1 = twt.getTerminal1().getBusView().getBus();
            Bus b2 = twt.getTerminal2().getBusView().getBus();
            if (b1 == null || b2 == null || b1.getId().equals(b2.getId())) {
                continue;
            }
            double v1 = twt.getTerminal1().getVoltageLevel().getNominalV();
            double v2 = twt.getTerminal2().getVoltageLevel().getNominalV();
            graph.addEdge(b1.getId(), b2.getId(), twt.getId(), NominalVoltagePair.of(v1, v2));
        }
        return graph;
    }

    // ---- Step 3: merge overlapping sets transitively ----

    static List<Set<String>> mergeOverlappingSets(List<Set<String>> input) {
        List<Set<String>> merged = new ArrayList<>();
        for (Set<String> current : input) {
            Set<String> combined = new HashSet<>(current);
            List<Set<String>> remaining = new ArrayList<>();
            for (Set<String> existing : merged) {
                if (!Collections.disjoint(existing, current)) {
                    combined.addAll(existing);
                } else {
                    remaining.add(existing);
                }
            }
            remaining.add(combined);
            merged = remaining;
        }
        return merged;
    }

    // ---- Internal value types ----

    private record UnorderedPair(String a, String b) {
        static UnorderedPair of(String x, String y) {
            return x.compareTo(y) <= 0 ? new UnorderedPair(x, y) : new UnorderedPair(y, x);
        }
    }

    private record NominalVoltagePair(double low, double high) {
        static NominalVoltagePair of(double v1, double v2) {
            return v1 <= v2 ? new NominalVoltagePair(v1, v2) : new NominalVoltagePair(v2, v1);
        }
    }

    private record EdgeKey(String busId1, String busId2) {
        static EdgeKey of(String a, String b) {
            return a.compareTo(b) <= 0 ? new EdgeKey(a, b) : new EdgeKey(b, a);
        }
    }

    private static final class EdgeData {
        final List<String> transformerIds = new ArrayList<>();
        NominalVoltagePair voltagePair;
    }

    private static final class IntraSubstationGraph {

        final Map<String, Set<String>> adjacency = new HashMap<>();
        final Map<EdgeKey, EdgeData> edges = new HashMap<>();

        void addEdge(String busA, String busB, String transformerId, NominalVoltagePair voltagePair) {
            EdgeKey key = EdgeKey.of(busA, busB);
            EdgeData data = edges.computeIfAbsent(key, k -> new EdgeData());
            data.transformerIds.add(transformerId);
            if (data.voltagePair == null) {
                data.voltagePair = voltagePair;
            }
            adjacency.computeIfAbsent(busA, k -> new HashSet<>()).add(busB);
            adjacency.computeIfAbsent(busB, k -> new HashSet<>()).add(busA);
        }

        boolean hasEdge(String busA, String busB) {
            return edges.containsKey(EdgeKey.of(busA, busB));
        }

        /**
         * Enumerates all chordless cycles of length 3 or 4. Each cycle is returned
         * exactly once, as the ordered list of its nodes.
         *
         * <p>Triangles are by definition chordless. Squares are filtered to exclude
         * those carrying a chord (i.e. an edge between opposite vertices).
         *
         * <p>Canonical ordering is enforced by ranking nodes alphabetically and
         * requiring the smallest-rank node to come first; among the two cycle
         * neighbors of that node, we pick the smaller-rank as the second node.
         */
        List<List<String>> findChordlessCyclesUpToSize4() {
            List<List<String>> cycles = new ArrayList<>();
            List<String> nodes = new ArrayList<>(adjacency.keySet());
            Collections.sort(nodes);
            Map<String, Integer> rank = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++) {
                rank.put(nodes.get(i), i);
            }

            // Triangles: (a, b, c) with rank(a) < rank(b) < rank(c)
            for (String a : nodes) {
                int ra = rank.get(a);
                for (String b : adjacency.get(a)) {
                    if (rank.get(b) <= ra) {
                        continue;
                    }
                    for (String c : adjacency.get(b)) {
                        if (rank.get(c) <= rank.get(b)) {
                            continue;
                        }
                        if (hasEdge(a, c)) {
                            cycles.add(List.of(a, b, c));
                        }
                    }
                }
            }

            // Squares chordless: cycle a-b-c-d-a with a = smallest rank, rank(b) < rank(d),
            // and no chord a-c or b-d.
            for (String a : nodes) {
                int ra = rank.get(a);
                List<String> nbrA = new ArrayList<>(adjacency.get(a));
                Collections.sort(nbrA);
                for (int i = 0; i < nbrA.size(); i++) {
                    String b = nbrA.get(i);
                    if (rank.get(b) <= ra) {
                        continue;
                    }
                    for (int j = i + 1; j < nbrA.size(); j++) {
                        String d = nbrA.get(j);
                        if (rank.get(d) <= ra) {
                            continue;
                        }
                        if (hasEdge(b, d)) {
                            continue; // chord b-d
                        }
                        Set<String> nbrD = adjacency.get(d);
                        for (String c : adjacency.get(b)) {
                            if (rank.get(c) <= ra) {
                                continue;
                            }
                            if (c.equals(a) || c.equals(d)) {
                                continue;
                            }
                            if (!nbrD.contains(c)) {
                                continue;
                            }
                            if (hasEdge(a, c)) {
                                continue; // chord a-c
                            }
                            cycles.add(List.of(a, b, c, d));
                        }
                    }
                }
            }
            return cycles;
        }
    }
}
