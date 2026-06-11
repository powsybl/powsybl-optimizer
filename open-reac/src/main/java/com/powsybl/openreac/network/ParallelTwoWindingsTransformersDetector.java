/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.network;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.RatioTapChangerStep;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 *       that only transformers performing the same conversion are bundled
 *       together. Connected components of the resulting per-voltage-pair
 *       subgraph yield the sets.</li>
 * </ol>
 *
 * <p>Only transformers carrying a ratio tap changer are returned. Detected
 * bundles that share at least one transformer are merged transitively
 * (if {@code A // B} and {@code B // C}, then {@code A // B // C}).
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class ParallelTwoWindingsTransformersDetector {

    private static final double RHO_INTERSECTION_EPSILON = 1e-4;

    public enum IntersectionStatus { LARGE, POINT, EMPTY }

    /**
     * A detected bundle of parallel transformers, annotated with the intersection
     * status of their <em>effective</em> rho ranges (tap rho scaled by the constant
     * per-unit ratio, see {@link #cstRatio}). The (low, high) values are the raw
     * intersection bounds in effective-ratio space: max(cst_i * rhoMin_i) and
     * min(cst_i * rhoMax_i). For LARGE: low < high. For POINT: low ≈ high.
     * For EMPTY: low > high.
     */
    public record ParallelBundle(Set<String> transformerIds,
                                IntersectionStatus status,
                                double low,
                                double high) { }

    /**
     * The [min, max] rho span of a transformer's ratio tap changer. When the
     * transformer is null or carries no ratio tap changer, both bounds are
     * {@link Double#NaN} and {@link #isPresent()} returns {@code false}.
     */
    public record RhoBounds(double min, double max) {
        public boolean isPresent() {
            return !Double.isNaN(min);
        }
    }

    private ParallelTwoWindingsTransformersDetector() {
        // utility class
    }

    /**
     * @return bundles of parallel ratio-tap-changer-bearing transformers,
     *         each as a set of transformer ids. Singletons are filtered out.
     */
    static List<Set<String>> detect(Network network) {
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
        List<Set<String>> merged = mergeOverlappingSets(all);

        // Deterministic ordering, mirroring the notebook (largest bundles first), with a
        // stable tie-break so the AMPL bundle numbering is reproducible across runs.
        merged.sort(Comparator
                .comparingInt((Set<String> s) -> s.size()).reversed()
                .thenComparing(s -> s.stream().min(Comparator.naturalOrder()).orElse("")));
        return merged;
    }

    public static List<ParallelBundle> detectAndAnalyze(Network network, Set<String> variableTransformerIds) {
        List<Set<String>> rawBundles = detect(network);
        List<ParallelBundle> result = new ArrayList<>(rawBundles.size());
        for (Set<String> bundle : rawBundles) {
            result.add(analyzeBundle(bundle, network, variableTransformerIds));
        }
        return result;
    }

    /**
     * @return the min/max rho over all ratio tap changer steps of {@code twt},
     *         or an absent {@link RhoBounds} if {@code twt} is null or has no
     *         ratio tap changer.
     */
    public static RhoBounds rhoBounds(TwoWindingsTransformer twt) {
        if (twt == null || twt.getRatioTapChanger() == null) {
            return new RhoBounds(Double.NaN, Double.NaN);
        }
        RatioTapChanger rtc = twt.getRatioTapChanger();
        double min = rtc.getAllSteps().values().stream().mapToDouble(RatioTapChangerStep::getRho).min().orElse(Double.NaN);
        double max = rtc.getAllSteps().values().stream().mapToDouble(RatioTapChangerStep::getRho).max().orElse(Double.NaN);
        return new RhoBounds(min, max);
    }

    /**
     * @return the current rho of {@code twt} as a degenerate {@link RhoBounds} (min == max).
     *         Used to pin a transformer that cannot be moved by OpenReac (e.g. a non-variable
     *         member of a bundle) to the single ratio it is frozen at: its current tap. This is
     *         exactly the value AMPL keeps for a fixed-ratio transformer (regl_tap0).
     */
    public static RhoBounds currentRhoBounds(TwoWindingsTransformer twt) {
        if (twt == null || twt.getRatioTapChanger() == null) {
            return new RhoBounds(Double.NaN, Double.NaN);
        }
        double rho = twt.getRatioTapChanger().getCurrentStep().getRho();
        return new RhoBounds(rho, rho);
    }

    /**
     * @return the constant (off-tap) per-unit ratio of {@code twt}, i.e. the value exported by
     *         the AMPL converter in the "cst ratio (pu)" column of {@code ampl_network_branches.txt}:
     *         {@code (ratedU2 / vnom2) / (ratedU1 / vnom1)}. In the ACOPF flow equations the
     *         transformation ratio of a branch is {@code tapRho * cstRatio}; circulating flows
     *         between parallel transformers are driven by mismatches of this <em>effective</em>
     *         ratio, not of the raw tap rho. The bundle analysis is therefore carried out in
     *         effective-ratio space (see {@link #effectiveRhoBounds}).
     *         This formula must stay aligned with the powsybl-core AMPL exporter; the contract
     *         is locked by a dedicated test against the exported branches file.
     */
    public static double cstRatio(TwoWindingsTransformer twt) {
        double vnom1 = twt.getTerminal1().getVoltageLevel().getNominalV();
        double vnom2 = twt.getTerminal2().getVoltageLevel().getNominalV();
        return twt.getRatedU2() / vnom2 / (twt.getRatedU1() / vnom1);
    }

    /**
     * @return {@link #rhoBounds} scaled to effective-ratio space, i.e. multiplied by
     *         {@link #cstRatio}. Two parallel transformers see the same transformation
     *         when their effective ratios coincide; when the constant ratios of the members
     *         are equal (identical units), effective and raw spaces only differ by a common
     *         positive factor and the analysis is unchanged.
     */
    public static RhoBounds effectiveRhoBounds(TwoWindingsTransformer twt) {
        return scaleByCstRatio(rhoBounds(twt), twt);
    }

    /**
     * @return {@link #currentRhoBounds} scaled to effective-ratio space (see {@link #effectiveRhoBounds}).
     */
    public static RhoBounds currentEffectiveRhoBounds(TwoWindingsTransformer twt) {
        return scaleByCstRatio(currentRhoBounds(twt), twt);
    }

    private static RhoBounds scaleByCstRatio(RhoBounds raw, TwoWindingsTransformer twt) {
        if (!raw.isPresent()) {
            return raw;
        }
        // cstRatio is strictly positive (rated and nominal voltages are), so min/max order is preserved.
        double c = cstRatio(twt);
        return new RhoBounds(c * raw.min(), c * raw.max());
    }

    private static ParallelBundle analyzeBundle(Set<String> bundle, Network network, Set<String> variableTransformerIds) {
        double low = Double.NEGATIVE_INFINITY;
        double high = Double.POSITIVE_INFINITY;
        for (String twtId : bundle) {
            TwoWindingsTransformer twt = network.getTwoWindingsTransformer(twtId);
            // The analysis is carried out in effective-ratio space (tap rho * cst ratio), the
            // quantity that actually drives circulating flows; see cstRatio(). A non-variable
            // member cannot be moved by OpenReac: it stays at its current tap. We therefore pin
            // it to a single point (its current effective rho), so the bundle intersection must
            // coincide with that value. A pinned member collapses the interval to a point
            // (POINT) or makes it empty (EMPTY), and can never yield LARGE — which is why a LARGE
            // bundle is always made of optimisable members only.
            boolean variable = variableTransformerIds == null || variableTransformerIds.contains(twtId);
            RhoBounds bounds = variable ? effectiveRhoBounds(twt) : currentEffectiveRhoBounds(twt);
            if (!bounds.isPresent()) {
                continue;
            }
            low = Math.max(low, bounds.min());
            high = Math.min(high, bounds.max());
        }
        IntersectionStatus status;
        if (high < low - RHO_INTERSECTION_EPSILON) {
            status = IntersectionStatus.EMPTY;
        } else if (high - low <= RHO_INTERSECTION_EPSILON) {
            status = IntersectionStatus.POINT;
        } else {
            status = IntersectionStatus.LARGE;
        }
        return new ParallelBundle(bundle, status, low, high);
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
        // Intra-substation lines take part in the graph as plain edges so they can close
        // cycles, exactly like the reference notebook (graph built from get_branches()).
        // Their ids are later dropped by the ratio-tap-changer filter in detect(), so a
        // line never ends up inside a returned bundle; it only contributes connectivity.
        Map<String, List<Line>> intraSubstationLines = new HashMap<>();
        for (Line line : network.getLines()) {
            Substation s1 = line.getTerminal1().getVoltageLevel().getSubstation().orElse(null);
            Substation s2 = line.getTerminal2().getVoltageLevel().getSubstation().orElse(null);
            if (s1 != null && s1 == s2) {
                intraSubstationLines.computeIfAbsent(s1.getId(), k -> new ArrayList<>()).add(line);
            }
        }
        List<Set<String>> result = new ArrayList<>();
        for (Substation substation : network.getSubstations()) {
            IntraSubstationGraph graph = buildIntraSubstationGraph(substation,
                    intraSubstationLines.getOrDefault(substation.getId(), List.of()));
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
        // Edges of the cycle, bundled by their nominal voltage pair
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

    private static IntraSubstationGraph buildIntraSubstationGraph(Substation substation, List<Line> intraSubstationLines) {
        IntraSubstationGraph graph = new IntraSubstationGraph();
        for (TwoWindingsTransformer twt : substation.getTwoWindingsTransformers()) {
            addBranchEdge(graph, twt.getId(), twt.getTerminal1(), twt.getTerminal2());
        }
        for (Line line : intraSubstationLines) {
            addBranchEdge(graph, line.getId(), line.getTerminal1(), line.getTerminal2());
        }
        return graph;
    }

    private static void addBranchEdge(IntraSubstationGraph graph, String branchId, Terminal terminal1, Terminal terminal2) {
        Bus b1 = terminal1.getBusView().getBus();
        Bus b2 = terminal2.getBusView().getBus();
        if (b1 == null || b2 == null || b1.getId().equals(b2.getId())) {
            return;
        }
        double v1 = terminal1.getVoltageLevel().getNominalV();
        double v2 = terminal2.getVoltageLevel().getNominalV();
        graph.addEdge(b1.getId(), b2.getId(), branchId, NominalVoltagePair.of(v1, v2));
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
            List<String> nodes = new ArrayList<>(adjacency.keySet());
            Collections.sort(nodes);
            Map<String, Integer> rank = new HashMap<>();
            for (int i = 0; i < nodes.size(); i++) {
                rank.put(nodes.get(i), i);
            }
            List<List<String>> cycles = new ArrayList<>();
            collectTriangles(nodes, rank, cycles);
            collectChordlessSquares(nodes, rank, cycles);
            return cycles;
        }

        // Triangles are chordless by definition. Emitted once as (a, b, c) with rank(a) < rank(b) < rank(c).
        private void collectTriangles(List<String> nodes, Map<String, Integer> rank, List<List<String>> cycles) {
            for (String a : nodes) {
                int ra = rank.get(a);
                for (String b : adjacency.get(a)) {
                    if (rank.get(b) > ra) {
                        collectTrianglesThrough(a, b, rank, cycles);
                    }
                }
            }
        }

        private void collectTrianglesThrough(String a, String b, Map<String, Integer> rank, List<List<String>> cycles) {
            int rb = rank.get(b);
            for (String c : adjacency.get(b)) {
                if (rank.get(c) > rb && hasEdge(a, c)) {
                    cycles.add(List.of(a, b, c));
                }
            }
        }

        // Chordless squares a-b-c-d-a: a has smallest rank, rank(b) < rank(d), no chord a-c or b-d.
        // Emitted once as (a, b, c, d), a valid cycle walk.
        private void collectChordlessSquares(List<String> nodes, Map<String, Integer> rank, List<List<String>> cycles) {
            for (String a : nodes) {
                collectSquaresAt(a, rank, cycles);
            }
        }

        private void collectSquaresAt(String a, Map<String, Integer> rank, List<List<String>> cycles) {
            int ra = rank.get(a);
            List<String> higherNeighbors = sortedNeighborsAbove(a, ra, rank);
            for (int i = 0; i < higherNeighbors.size(); i++) {
                for (int j = i + 1; j < higherNeighbors.size(); j++) {
                    collectSquare(a, ra, higherNeighbors.get(i), higherNeighbors.get(j), rank, cycles);
                }
            }
        }

        private void collectSquare(String a, int ra, String b, String d, Map<String, Integer> rank, List<List<String>> cycles) {
            if (hasEdge(b, d)) {
                return; // chord b-d: not chordless
            }
            Set<String> neighborsOfD = adjacency.get(d);
            for (String c : adjacency.get(b)) {
                boolean closesChordlessSquare = rank.get(c) > ra
                        && !c.equals(a) && !c.equals(d)
                        && neighborsOfD.contains(c)
                        && !hasEdge(a, c); // no chord a-c
                if (closesChordlessSquare) {
                    cycles.add(List.of(a, b, c, d));
                }
            }
        }

        // Neighbors of {@code node} with rank strictly greater than {@code minRank}, sorted.
        private List<String> sortedNeighborsAbove(String node, int minRank, Map<String, Integer> rank) {
            List<String> result = new ArrayList<>();
            for (String neighbor : adjacency.get(node)) {
                if (rank.get(neighbor) > minRank) {
                    result.add(neighbor);
                }
            }
            Collections.sort(result);
            return result;
        }
    }
}
