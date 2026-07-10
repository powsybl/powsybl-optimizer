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
import java.util.Optional;
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
 * <p><b>Orientation.</b> The AMPL export assigns bus1/bus2 strictly from the IIDM
 * terminal declaration order, with no normalization: two physically parallel
 * transformers may be declared in opposite directions. Tying the effective ratios
 * of anti-parallel members would enforce a physically wrong condition (the correct
 * one being {@code eff1 * eff2 = 1}), so each member carries its orientation relative
 * to the bundle's canonical direction, defined as the declared direction of the first
 * member in id order (which therefore always gets {@code +1}; a homogeneous bundle is
 * all-{@code +1}). A member is compared to that reference:
 * <ul>
 *   <li>when the two nominal voltages of the bundle differ: same terminal-1 side as
 *       the reference (low or high nominal voltage) means {@code +1}, {@code -1}
 *       otherwise;</li>
 *   <li>when they are equal (coupling transformers) and the bundle is a simple
 *       parallel (single shared bus pair): same terminal-1 bus as the reference
 *       means {@code +1}, {@code -1} otherwise;</li>
 *   <li>when they are equal and the bundle comes from a cycle (no shared bus pair),
 *       the orientation is undecidable: the bundle is returned separately so the
 *       caller can release it with a warning instead of tying it blindly.</li>
 * </ul>
 * The orientation is passed to the AMPL model, which derives the member bounds and
 * tie constraints accordingly (direct members: {@code eff = rho_B}; reversed members:
 * {@code eff * rho_B = 1}).
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public final class ParallelTwoWindingsTransformersDetector {

    private ParallelTwoWindingsTransformersDetector() {
        // utility class
    }

    /**
     * A transformer inside a bundle, with its orientation relative to the bundle's
     * canonical direction (the declared direction of the first member in id order):
     * {@code +1} when declared in the same direction, {@code -1} when reversed.
     */
    public record Member(String transformerId, int orientation) { }

    /** A bundle of parallel transformers, members sorted by id. */
    public record Bundle(List<Member> members) {
        public Bundle {
            members = List.copyOf(members);
        }

        public int size() {
            return members.size();
        }
    }

    /**
     * Result of the detection: the bundles whose member orientations could be
     * established, and the ones (degenerate nominal-voltage pair in a cycle) whose
     * orientation is undecidable and which must be released with a warning.
     */
    public record DetectionResult(List<Bundle> bundles, List<Set<String>> undecidedBundles) {
        public DetectionResult {
            bundles = List.copyOf(bundles);
            undecidedBundles = List.copyOf(undecidedBundles);
        }
    }

    /**
     * @return bundles of parallel ratio-tap-changer-bearing transformers, each member
     *         carrying its orientation relative to the bundle's canonical direction.
     *         Singletons are filtered out. Detection and orientation are purely
     *         topological; the numeric qualification of a bundle (shared-ratio
     *         interval, tie / fix / release) is performed in the AMPL model.
     */
    public static DetectionResult detect(Network network) {
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
                .comparingInt(Set<String>::size).reversed()
                .thenComparing(s -> s.stream().min(Comparator.naturalOrder()).orElse("")));
        return orientBundles(network, merged);
    }

    // ---- Step 4: orientation of each member against the bundle's canonical direction ----

    private static DetectionResult orientBundles(Network network, List<Set<String>> bundles) {
        List<Bundle> oriented = new ArrayList<>();
        List<Set<String>> undecided = new ArrayList<>();
        for (Set<String> bundle : bundles) {
            List<TwoWindingsTransformer> members = bundle.stream().sorted()
                    .map(network::getTwoWindingsTransformer)
                    .toList();
            orientMembers(members).ifPresentOrElse(
                orientedMembers -> oriented.add(new Bundle(orientedMembers)),
                () -> undecided.add(bundle));
        }
        return new DetectionResult(oriented, undecided);
    }

    /**
     * Orients every member against the first one (id order). Members of a bundle all
     * share the same nominal voltage pair (guaranteed by both detection strategies and
     * preserved by the transitive merge): when that pair is non-degenerate, the terminal-1
     * side (low or high nominal voltage) identifies the declared direction; when it is
     * degenerate (coupling transformers), the terminal-1 bus does, provided all members
     * share the same bus pair. Returns empty when undecidable.
     */
    private static Optional<List<Member>> orientMembers(List<TwoWindingsTransformer> members) {
        TwoWindingsTransformer reference = members.get(0);
        double refV1 = reference.getTerminal1().getVoltageLevel().getNominalV();
        double refV2 = reference.getTerminal2().getVoltageLevel().getNominalV();
        return refV1 != refV2
                ? Optional.of(orientByNominalVoltageSide(members, refV1 < refV2))
                : orientBySharedBusPair(members, reference);
    }

    private static List<Member> orientByNominalVoltageSide(List<TwoWindingsTransformer> members, boolean referenceT1OnLowSide) {
        List<Member> result = new ArrayList<>(members.size());
        for (TwoWindingsTransformer twt : members) {
            boolean t1OnLowSide = twt.getTerminal1().getVoltageLevel().getNominalV()
                    < twt.getTerminal2().getVoltageLevel().getNominalV();
            result.add(new Member(twt.getId(), t1OnLowSide == referenceT1OnLowSide ? 1 : -1));
        }
        return result;
    }

    /**
     * Fallback for degenerate nominal-voltage pairs (coupling transformers): only a
     * simple parallel (every member on the same bus pair) can be oriented, by comparing
     * each member's terminal-1 bus to the reference's. A cycle-based bundle has no
     * shared bus pair and is undecidable: returns empty.
     */
    private static Optional<List<Member>> orientBySharedBusPair(List<TwoWindingsTransformer> members, TwoWindingsTransformer reference) {
        Bus refB1 = reference.getTerminal1().getBusView().getBus();
        Bus refB2 = reference.getTerminal2().getBusView().getBus();
        if (refB1 == null || refB2 == null) {
            return Optional.empty();
        }
        UnorderedPair referencePair = UnorderedPair.of(refB1.getId(), refB2.getId());
        List<Member> result = new ArrayList<>(members.size());
        for (TwoWindingsTransformer twt : members) {
            Bus b1 = twt.getTerminal1().getBusView().getBus();
            Bus b2 = twt.getTerminal2().getBusView().getBus();
            if (b1 == null || b2 == null
                    || !referencePair.equals(UnorderedPair.of(b1.getId(), b2.getId()))) {
                return Optional.empty();
            }
            result.add(new Member(twt.getId(), b1.getId().equals(refB1.getId()) ? 1 : -1));
        }
        return Optional.of(result);
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
            if (b1 != null && b2 != null) {
                byBusPair.computeIfAbsent(UnorderedPair.of(b1.getId(), b2.getId()), k -> new ArrayList<>())
                        .add(twt.getId());
            }
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
        // Build an adjacency from the edge set, then flood-fill for components.
        Map<String, Set<String>> adj = buildBusAdjacency(edges);
        Map<String, Integer> compOf = new HashMap<>();
        int compIdx = 0;
        for (String start : adj.keySet()) {
            if (!compOf.containsKey(start)) {
                floodFillComponent(start, compIdx, adj, compOf);
                compIdx++;
            }
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

    private static Map<String, Set<String>> buildBusAdjacency(Collection<EdgeKey> edges) {
        Map<String, Set<String>> adj = new HashMap<>();
        for (EdgeKey e : edges) {
            adj.computeIfAbsent(e.busId1, k -> new HashSet<>()).add(e.busId2);
            adj.computeIfAbsent(e.busId2, k -> new HashSet<>()).add(e.busId1);
        }
        return adj;
    }

    // Iterative flood fill: labels start and everything reachable from it with compIdx.
    private static void floodFillComponent(String start, int compIdx,
                                           Map<String, Set<String>> adj, Map<String, Integer> compOf) {
        Deque<String> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String node = stack.pop();
            if (compOf.putIfAbsent(node, compIdx) == null) {
                for (String nbr : adj.getOrDefault(node, Set.of())) {
                    if (!compOf.containsKey(nbr)) {
                        stack.push(nbr);
                    }
                }
            }
        }
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
