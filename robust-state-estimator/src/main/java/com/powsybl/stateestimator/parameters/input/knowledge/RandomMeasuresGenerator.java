/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.knowledge;

import com.powsybl.iidm.network.*;

import java.util.*;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class RandomMeasuresGenerator {

    public static final ArrayList<String> ALL_MEASUREMENT_TYPES = new ArrayList<>() {{
        add("PfSide1");
        add("PfSide2");
        add("QfSide1");
        add("QfSide2");
        add("P");
        add("Q");
        add("V");
    }};
    public static final Map<String, Double> RELATIVE_STD_BY_MEAS_TYPE = new HashMap<>() {{
        put("Pf", 0.011);
        put("Qf", 0.022);
        put("P", 0.011);
        put("Q", 0.022);
        put("V", 0.0051);
    }};
    // Relative standard deviation values for measurements, see Master Thesis' report

    // Thresholds used for computation of measurement variances
    public static final double MIN_ACTIVE_POWER_MW = 1; // in MW
    public static final double MIN_REACTIVE_POWER_MVAR = 1; // in MVar
    public static final double MIN_VOLTAGE_KV = 1; // in kV


    // By default, the number of measurements generated will be 4 times the number of buses in the network (ensure observability)
    public static final double DEFAULT_RATIO_MEASURES_TO_BUSES = 4.0;

    /**
     * This method generates random measurements out of the Load Flow results obtained on a network.
     * The measurements generated are added to the "knowledge" instance.
     * The number of measurement generated is large by default (4 times the number of buses) and distributed enough to ensure network observability.
     * It is possible to skew the distribution to pick more often buses with higher voltages ("double roll, pick better" method),
     * to emulate the fact that it is more likely to have measurement devices on the biggest nodes than on the smallest ones.
     * <p>
     * Note 1 : this method should be used for testing purposes only.
     * Note 2 : the sign of injected powers (P, Q) is inverted.
     * Note 3 : variance for measurements is given by (std_MeasType_% x |measurementValue|)^2
     * </p>
     *
     * @param knowledge The knowledge object that will store the random measurements generated
     * @param network The network (LF run previously) for which random measurements must be generated
     * @param seed (optional) The seed used by the random generator
     * @param ratioMeasuresToBuses (optional) The ratio "number of measures"/"number of buses in the network" used to compute the number of measures generated
     * @param addNoise (optional) If "true", add gaussian noise (based on measurement type's variance) to the measurement value
     * @param noiseAmplitude (optional) A number k (default 1) defining noise amplitude : noise will be pick in [-K x sigma + measValue; K x sigma + measValue]
     * @param noPickBranchID (optional) The ID of a branch for which no Pf nor Qf measurements will be picked
     * @param ensureRealisticObservability (optional) Mode of placement of measures, where every node has V measure and Pf/Qf measures on one of its lines, but one line can not have Pf/Qf on both sides
     */
    public static void generateRandomMeasurements(StateEstimatorKnowledge knowledge, Network network,
                                                  Optional<Integer> seed, Optional<Double> ratioMeasuresToBuses,
                                                  Optional<Boolean> addNoise, Optional<Double> noiseAmplitude,
                                                  Optional<String> noPickBranchID, Optional<Boolean> ensureRealisticObservability)
            throws IllegalArgumentException {

        // Initialize new Random (use the seed if provided) to pick measurements
        Random random = seed.map(Random::new).orElseGet(() -> new Random(System.currentTimeMillis()));

        // Initialize a boolean stating if noise must be added to measurements,
        // and a Random for gaussian noise (use constant seed for repeatability)
        boolean withNoise = addNoise.isPresent() && addNoise.get().equals(true);
        Random noise = new Random(0);
        // If given, control the size of the interval [-k sigma; +k sigma] in which noise will be picked at random (uniform distribution, sigma : std of measurement)
        double noiseCoef = 1;
        if (noiseAmplitude.isPresent() && noiseAmplitude.get() > 0) {
            noiseCoef = noiseAmplitude.get();
        }

        // Initialize lists in which to pick measurement locations (bus or branch) (one list per type of measurement)
        List<Branch> listOfBranchesPfSide1 = new ArrayList<>();
        List<Bus> listOfBusesV = new ArrayList<>();
        // Add only buses and branches from main connected component (make sure each element is unique)
        for (Branch branch : network.getBranches()) {
            Bus b1 = branch.getTerminal1().getBusView().getConnectableBus();
            Bus b2 = branch.getTerminal2().getBusView().getConnectableBus();
            if (b1.isInMainConnectedComponent()
            && b2.isInMainConnectedComponent()) {
                listOfBranchesPfSide1.add(branch);
                if (!listOfBusesV.contains(b1)) {
                    listOfBusesV.add(b1);
                }
                if (!listOfBusesV.contains(b2)) {
                    listOfBusesV.add(b2);
                }
            }
        }
        // If a "no-pick" branch is given, remove it from these lists
        if (noPickBranchID.isPresent() && network.getBranchStream().map(Identifiable::getId).toList().contains(noPickBranchID.get())) {
            listOfBranchesPfSide1.removeIf(branch -> branch.getId().equals(noPickBranchID.get()));
        }
        List<Branch> listOfBranchesPfSide2 = new ArrayList<>(listOfBranchesPfSide1);
        List<Branch> listOfBranchesQfSide1 = new ArrayList<>(listOfBranchesPfSide1);
        List<Branch> listOfBranchesQfSide2 = new ArrayList<>(listOfBranchesPfSide1);
        List<Bus> listOfBusesP = new ArrayList<>();
        List<Bus> listOfBusesQ = new ArrayList<>();
        // For active and reactive power injections, remove from the list zero-injection buses (this information is known for sure : it is not a measure associated with uncertainty)
        for (Bus bus : listOfBusesV) {
            if (!knowledge.getZeroInjectionBuses().containsValue(bus.getId())) {
                listOfBusesP.add(bus);
                listOfBusesQ.add(bus);
            }
        }

        // Find the starting measurement number, such that no new measure created here will be given the number of an already existing measure
        ArrayList<Integer> allExistingMeasNumbers = new ArrayList<>();
        allExistingMeasNumbers.add(0);
        allExistingMeasNumbers.addAll(knowledge.getActivePowerInjectedMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getReactivePowerInjectedMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getActivePowerFlowMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getReactivePowerFlowMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getVoltageMagnitudeMeasures().keySet());
        int startingMeasurementNumber = Collections.max(allExistingMeasNumbers) + 1;

        // If ensureObservability = true, do:
        // For each bus b1, build the list L(b1) of branches linked to it + "injection" branch
        // For each bus b1:
        //      - pick branch b1-b2 at random from L(b1)
        //      - add measures V(b1), Pf(b1->b2) and Qf(b1->b2)
        //      - remove branch b1-b2 from L(b1) and L(b2)
        boolean ensureObservability = ensureRealisticObservability.isPresent() && ensureRealisticObservability.get().equals(true);
        if (ensureObservability) {
            // Take into account noPickBranchID
            String noPickBranch = "";
            if (noPickBranchID.isPresent()) {
                noPickBranch = noPickBranchID.get();
            }
            // Build "adjacency list": find for each bus the list of linked branches (one occurrence in the list)
            Map<String, List<String>> adjacencyList = new HashMap<>();
            for (Bus bus1 : network.getBusView().getBuses()) {
                Set<String> linkedBranches = new HashSet<>();
                if (!knowledge.getZeroInjectionBuses().containsValue(bus1.getId())) {
                    linkedBranches.add("MeasuresP/Q");
                }
                for (Line l : bus1.getLines()) {
                    linkedBranches.add(l.getId());
                }
                for (TwoWindingsTransformer twt : bus1.getTwoWindingsTransformers()) {
                    linkedBranches.add(twt.getId());
                }
                // Remove noPickBranchID (if provided)
                linkedBranches.remove(noPickBranch);
                // TODO : add other types of branches (three windings transformers, dangling lines, etc)
                adjacencyList.put(bus1.getId(), List.copyOf(linkedBranches));
            }
            // Shuffle the list of buses in adjacency list (based on seed provided)
            List<String> allBusesShuffled = new ArrayList<>(adjacencyList.keySet().stream().toList());
            Collections.shuffle(allBusesShuffled, random);
            // For each bus b1 (following previous random order), pick a branch b1-b2 at random and add measures
            for (String busID : allBusesShuffled) {

                // Try to add measure V(b1) (might already exist)
                Map<String, String> measureV = new HashMap<>();
                measureV.put("Type", "V");
                measureV.put("BusID", busID);
                double measureVValue = network.getBusView().getBus(busID).getV();
                double measureVVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("V")
                                * Math.max(Math.abs(measureVValue), MIN_VOLTAGE_KV)
                        , 2);
                measureV.put("Variance", String.valueOf(measureVVariance));
                if (withNoise) {
                    measureVValue += -noiseCoef * Math.sqrt(measureVVariance) + 2 * noiseCoef * Math.sqrt(measureVVariance) * noise.nextDouble();
                }
                measureV.put("Value", String.valueOf(measureVValue));
                try {
                    knowledge.addMeasure(startingMeasurementNumber++, measureV, network);
                } catch (IllegalArgumentException illegalArgumentException) {
                    continue;
                }

                // Pick a branch at random
                int nbOfBranches = adjacencyList.get(busID).size();
                if (nbOfBranches > 0) {
                    int randomIndex = random.nextInt(nbOfBranches);
                    String branchID = adjacencyList.get(busID).get(randomIndex);

                    // Special case "MeasuresP/Q" : add power net injection measures, not power flow measures
                    if (branchID.equals("MeasuresP/Q")) {

                        // Try to add measure P(b1) (do not forget "-" sign)
                        Map<String, String> measureP = new HashMap<>();
                        measureP.put("Type", "P");
                        measureP.put("BusID", busID);
                        double measurePValue = -1. * network.getBusView().getBus(busID).getP();
                        double measurePVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("P")
                                        * Math.max(Math.abs(measurePValue), MIN_ACTIVE_POWER_MW)
                                , 2);
                        measureP.put("Variance", String.valueOf(measurePVariance));
                        if (withNoise) {
                            measurePValue += -noiseCoef * Math.sqrt(measurePVariance) + 2 * noiseCoef * Math.sqrt(measurePVariance) * noise.nextDouble();
                        }
                        measureP.put("Value", String.valueOf(measurePValue));
                        try{
                            knowledge.addMeasure(startingMeasurementNumber++, measureP, network);
                        } catch (IllegalArgumentException illegalArgumentException) {
                            continue;
                        }

                        // Try to add measure Q(b1) (do not forget "-" sign)
                        Map<String, String> measureQ = new HashMap<>();
                        measureQ.put("Type", "Q");
                        measureQ.put("BusID", busID);
                        double measureQValue = -1. * network.getBusView().getBus(busID).getQ();
                        double measureQVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Q")
                                        * Math.max(Math.abs(measureQValue), MIN_REACTIVE_POWER_MVAR)
                                , 2);
                        measureQ.put("Variance", String.valueOf(measureQVariance));
                        if (withNoise) {
                            measureQValue += -noiseCoef * Math.sqrt(measureQVariance) + 2 * noiseCoef * Math.sqrt(measureQVariance) * noise.nextDouble();
                        }
                        measureQ.put("Value", String.valueOf(measureQValue));
                        try{
                            knowledge.addMeasure(startingMeasurementNumber++, measureQ, network);
                        } catch (IllegalArgumentException illegalArgumentException) {
                            continue;
                        }

                        // Remove the value "MeasuresP/Q" from the adjacency list of b1
                        List<String> linkedBranchesToBus = new ArrayList<>(List.copyOf(adjacencyList.get(busID)));
                        linkedBranchesToBus.remove(branchID);
                        adjacencyList.put(busID, linkedBranchesToBus);

                        // Update listOfBusesV, listOfBusesP and listOfBusesQ
                        listOfBusesV.removeIf(bus -> bus.getId().equals(busID));
                        listOfBusesP.removeIf(bus -> bus.getId().equals(busID));
                        listOfBusesQ.removeIf(bus -> bus.getId().equals(busID));
                    }
                    else { // Add power flow measures on branch picked
                        Branch branch = network.getBranch(branchID);
                        boolean isBusTerminal1 = branch.getTerminal1().getBusView().getConnectableBus().getId().equals(busID);
                        String otherBusID = isBusTerminal1 ?
                                branch.getTerminal2().getBusView().getConnectableBus().getId() :
                                branch.getTerminal1().getBusView().getConnectableBus().getId();

                        // Try to add measure Pf(b1->b2)
                        Map<String, String> measurePf = new HashMap<>();
                        measurePf.put("Type", "Pf");
                        measurePf.put("BranchID", branchID);
                        measurePf.put("FirstBusID", busID);
                        measurePf.put("SecondBusID", otherBusID);
                        double measurePfValue = isBusTerminal1 ? branch.getTerminal1().getP() : branch.getTerminal2().getP();
                        if (Double.isNaN(measurePfValue)) {
                            measurePfValue = 0;
                        }
                        double measurePfVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Pf")
                                        * Math.max(Math.abs(measurePfValue), MIN_ACTIVE_POWER_MW)
                                , 2);
                        measurePf.put("Variance", String.valueOf(measurePfVariance));
                        if (withNoise) {
                            measurePfValue += -noiseCoef * Math.sqrt(measurePfVariance) + 2 * noiseCoef * Math.sqrt(measurePfVariance) * noise.nextDouble();
                        }
                        measurePf.put("Value", String.valueOf(measurePfValue));
                        try {
                            knowledge.addMeasure(startingMeasurementNumber++, measurePf, network);
                        } catch (IllegalArgumentException illegalArgumentException) {
                            continue;
                        }

                        // Try to add measure Qf(b1->b2)
                        Map<String, String> measureQf = new HashMap<>();
                        measureQf.put("Type", "Qf");
                        measureQf.put("BranchID", branchID);
                        measureQf.put("FirstBusID", busID);
                        measureQf.put("SecondBusID", otherBusID);
                        double measureQfValue = isBusTerminal1 ? branch.getTerminal1().getQ() : branch.getTerminal2().getQ();
                        if (Double.isNaN(measureQfValue)) {
                            measureQfValue = 0;
                        }
                        double measureQfVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Qf")
                                        * Math.max(Math.abs(measureQfValue), MIN_REACTIVE_POWER_MVAR)
                                , 2);
                        measureQf.put("Variance", String.valueOf(measureQfVariance));
                        if (withNoise) {
                            measureQfValue += -noiseCoef * Math.sqrt(measureQfVariance) + 2 * noiseCoef * Math.sqrt(measureQfVariance) * noise.nextDouble();
                        }
                        measureQf.put("Value", String.valueOf(measureQfValue));
                        try {
                            knowledge.addMeasure(startingMeasurementNumber++, measureQf, network);
                        } catch (IllegalArgumentException illegalArgumentException) {
                            continue;
                        }

                        // Remove the branch b1-b2 from the lists of branches of b1 and b2
                        List<String> linkedBranchesToBus = new ArrayList<>(List.copyOf(adjacencyList.get(busID)));
                        List<String> linkedBranchesToOtherBus = new ArrayList<>(List.copyOf(adjacencyList.get(otherBusID)));
                        linkedBranchesToBus.remove(branchID);
                        linkedBranchesToOtherBus.remove(branchID);
                        adjacencyList.put(busID, linkedBranchesToBus);
                        adjacencyList.put(otherBusID, linkedBranchesToOtherBus);

                        // Update listOfBusesV, listOfBranchesPf and listOfBranchesQf
                        listOfBusesV.removeIf(bus -> bus.getId().equals(busID));
                        listOfBranchesPfSide1.removeIf(b -> b.getId().equals(branchID));
                        listOfBranchesPfSide2.removeIf(b -> b.getId().equals(branchID));
                        listOfBranchesQfSide1.removeIf(b -> b.getId().equals(branchID));
                        listOfBranchesQfSide2.removeIf(b -> b.getId().equals(branchID));
                    }
                }
            }
        }

        // Compute the number of measurements that remain to be generated randomly : find the number demanded by the Z/N ratio
        long nbMeasurements;
        if (ratioMeasuresToBuses.isPresent()) {
            double ratioZtoN = ratioMeasuresToBuses.get();
            if (ratioZtoN <= 0) {
                throw new IllegalArgumentException("Invalid value for the parameter ratioMeasuresToBuses : should be a positive float");
            }
            // Compute maximum ratio possible (note : if ensureObservability = true, power flows measures on both sides of a line are forbidden)
            double maxRatioMeasuresToBuses = ensureObservability ?
                    (2.0 * network.getBranchCount() + 3.0 * network.getBusView().getBusStream().count()
                    - 2.0 * knowledge.getZeroInjectionBuses().size())
                     / network.getBusView().getBusStream().count()
                    :
                    (4.0 * network.getBranchCount() + 3.0 * network.getBusView().getBusStream().count()
                            - 2.0 * knowledge.getZeroInjectionBuses().size())
                            / network.getBusView().getBusStream().count();
            if (ratioZtoN > maxRatioMeasuresToBuses) {
                throw new IllegalArgumentException(String.format("Provided value for ratioMeasuresToBuses is too large. Should be smaller than %f", maxRatioMeasuresToBuses));
            }
            if (ratioZtoN < 3 && ensureObservability) {
                System.out.println("[WARNING] Parameter ratioMeasuresToBuses has a lower value than what is needed to ensure observability (should be > 3) :" +
                        " it will likely not be respected." );
            }
            nbMeasurements = Math.round(ratioZtoN * network.getBusView().getBusStream().count());
        }
        else {
            nbMeasurements = Math.round(DEFAULT_RATIO_MEASURES_TO_BUSES * network.getBusView().getBusStream().count());
        }
        // Remove from it the number of measurements that already exist
        long nbAlreadyExistingMeasurements = knowledge.getMeasuresCount();
        nbMeasurements = Math.max(nbMeasurements - nbAlreadyExistingMeasurements, 0);

        // Initialize random variables used in the random generation loop
        int randomType;
        Branch randomBranch;
        int randomSide;
        Bus randomBus;

        // Initialize measurement value and variance
        double measurementValue;
        double measurementVariance;

        // For each measurement to be generated, pick a measurement type at random
        for (int i = startingMeasurementNumber; i < nbMeasurements + startingMeasurementNumber; i++) {

            Map<String, String> randomMeasure = new HashMap<>();

            randomType = random.nextInt(ALL_MEASUREMENT_TYPES.size());

            if (randomType == 0 || randomType == 1) {
                // Add a "Pf" measure
                randomMeasure.put("Type", "Pf");
                // Pick at random which branch side will be measured
                randomSide = random.nextInt(2);
                if (randomSide == 0) { // Side 1
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesPfSide1.isEmpty()) {
                        randomBranch = listOfBranchesPfSide1.remove(random.nextInt(listOfBranchesPfSide1.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal1().getP();
                        // If line is disconnected, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Pf")
                                        * Math.max(Math.abs(measurementValue), MIN_ACTIVE_POWER_MW)
                                , 2);
                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                } else { // Side 2
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesPfSide2.isEmpty()) {
                        randomBranch = listOfBranchesPfSide2.remove(random.nextInt(listOfBranchesPfSide2.size()));
                        // Get location IDs and the corresponding measurement value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal2().getP();
                        // If line is disconnected, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Pf")
                                        * Math.max(Math.abs(measurementValue), MIN_ACTIVE_POWER_MW)
                                , 2);
                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                }
            }

            else if (randomType == 2 || randomType == 3) {
                // Add a "Qf" measure
                randomMeasure.put("Type", "Qf");
                // Pick at random which branch side will be measured
                randomSide = random.nextInt(2);
                if (randomSide == 0) { // Side 1
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesQfSide1.isEmpty()) {
                        randomBranch = listOfBranchesQfSide1.remove(random.nextInt(listOfBranchesQfSide1.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal1().getQ();
                        // If line is disconnected but belongs to main connected component, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Qf")
                                        * Math.max(Math.abs(measurementValue), MIN_REACTIVE_POWER_MVAR)
                                , 2);
                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                } else { // Side 2
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesQfSide2.isEmpty()) {
                        randomBranch = listOfBranchesQfSide2.remove(random.nextInt(listOfBranchesQfSide2.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal2().getQ();
                        // If line is disconnected but belongs to main connected component, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Qf")
                                        * Math.max(Math.abs(measurementValue), MIN_REACTIVE_POWER_MVAR)
                                , 2);
                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                }
            }

            else if (randomType == 4) {
                // Add a "P" measure
                randomMeasure.put("Type", "P");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesP.isEmpty()) {
                    randomBus = listOfBusesP.remove(random.nextInt(listOfBusesP.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    measurementValue = -1 * randomBus.getP();
                    // Get and add measurement variance (in SI^2)
                    measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("P")
                                    * Math.max(Math.abs(measurementValue), MIN_ACTIVE_POWER_MW)
                            , 2);
                    randomMeasure.put("Variance", String.valueOf(measurementVariance));
                    // Add measurement value (possibly with noise)
                    if (withNoise) {
                        measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                    }
                    randomMeasure.put("Value", String.valueOf(measurementValue));
                } else {
                    randomMeasure = null;
                }

            } else if (randomType == 5) {
                // Add a "Q" measure
                randomMeasure.put("Type", "Q");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesQ.isEmpty()) {
                    randomBus = listOfBusesQ.remove(random.nextInt(listOfBusesQ.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    measurementValue = -1 * randomBus.getQ();
                    // Get and add measurement variance (in SI^2)
                    measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Q")
                                    * Math.max(Math.abs(measurementValue), MIN_REACTIVE_POWER_MVAR)
                            , 2);
                    randomMeasure.put("Variance", String.valueOf(measurementVariance));

                    // Add measurement value (possibly with noise)
                    if (withNoise) {
                        measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                    }
                    randomMeasure.put("Value", String.valueOf(measurementValue));
                } else {
                    randomMeasure = null;
                }
            }

            else if (randomType == 6) {
                // Add a "V" measure
                randomMeasure.put("Type", "V");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesV.isEmpty()) {
                    randomBus = listOfBusesV.remove(random.nextInt(listOfBusesV.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    measurementValue = randomBus.getV();
                    // Get and add measurement variance (in SI^2)
                    measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("V")
                                    * Math.max(Math.abs(measurementValue), MIN_VOLTAGE_KV)
                            , 2);
                    randomMeasure.put("Variance", String.valueOf(measurementVariance));
                    // Add measurement value (possibly with noise)
                    if (withNoise) {
                        measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                    }
                    randomMeasure.put("Value", String.valueOf(measurementValue));
                } else {
                    randomMeasure = null;
                }
            } else {
                throw new IllegalArgumentException("More measurement types given than what the generator can handle. Check ALL_MEASUREMENT_TYPES");
            }

            // Try to add the measure if not null (note that the measure could be redundant)
            if (randomMeasure != null) {
                try {
                    knowledge.addMeasure(i, randomMeasure, network);
                } catch (IllegalArgumentException illegalArgumentException) {
                    if (illegalArgumentException.getMessage().equals("A measurement already exists for the location and type of the measurement provided. It can not be added.")) {
                        i = i - 1;
                    }
                    else {
                        System.out.printf("%nMeasurement n° %d could not be added. Reason :%n", i);
                        throw illegalArgumentException;
                    }
                }
            } else { // If measure is null, it is because the list to choose measurement location has become empty (ex : all the buses are already assigned a voltage measure)
                // In this case, decrease i to get the proper quantity of measurements at the end of the process
                i = i - 1;
            }
        }
    }


    /**
     * This method generates random measurements out of the Load Flow results obtained on a network.
     * The measurements generated are added to the "knowledge" instance.
     * This variant of the random measurement generator involves the possibility of controlling the number of measurements
     * for one specified measurement type.
     * The number of measurement generated is large by default (4 times the number of buses) and distributed enough to ensure at best the network's observability.
     * It is possible to skew the distribution to pick more often buses with higher voltages ("double roll, pick better" method),
     * to emulate the fact that it is more likely to have measurement devices on the biggest nodes than on the smallest ones.
     * <p>
     * Note 1 : this method should be used for testing purposes only.
     * Note 2 : the sign of injected powers (P, Q) is inverted.
     * Note 3 : variance for measurements is given by (std_MeasType_% x |measurementValue|)^2
     * </p>
     *
     * @param knowledge The knowledge object that will store the random measurements generated
     * @param network The network (LF run previously) for which random measurements must be generated
     * @param ratioForCtrlMeasType The ratio "nb of measurements for the specified measurement type" / "total number of measurements"
     * @param ctrlMeasType The measurement type whose ratio is being controlled.
     * @param seed (optional) The seed used by the random generator
     * @param ratioMeasuresToBuses (optional) The ratio "number of measures"/"number of buses in the network" used to compute the number of measures generated
     * @param addNoise (optional) If "true", add gaussian noise (based on measurement type's variance) to the measurement value
     * @param noiseAmplitude (optional) A number k (default 1) defining noise amplitude : noise will be pick in [-K x sigma + measValue; K x sigma + measValue]
     * @param noPickBranchID (optional) The ID of a branch for which no Pf nor Qf measurements will be picked
     */
    public static void generateRandomMeasurementsWithCtrlMeasureRatio(StateEstimatorKnowledge knowledge, Network network,
                                                  Double ratioForCtrlMeasType, String ctrlMeasType,
                                                  Optional<Integer> seed, Optional<Double> ratioMeasuresToBuses,
                                                  Optional<Boolean> addNoise, Optional<Double> noiseAmplitude, Optional<String> noPickBranchID)
            throws IllegalArgumentException {

        // Compute the number of measurements that must be generated
        long nbMeasurements;
        if (ratioMeasuresToBuses.isPresent()) {
            if (ratioMeasuresToBuses.get() <= 0) {
                throw new IllegalArgumentException("Invalid value for the parameter ratioMeasuresToBuses : should be a positive float");
            }
            Double maxRatioMeasuresToBuses = (2.0 * network.getBranchCount() + 3.0 * network.getBusView().getBusStream().count())
                    / network.getBusView().getBusStream().count();
            if (ratioMeasuresToBuses.get() > maxRatioMeasuresToBuses) {
                throw new IllegalArgumentException(String.format("Provided value for ratioMeasuresToBuses is too large. Should be smaller than %f", maxRatioMeasuresToBuses));
            }
            nbMeasurements = Math.round(ratioMeasuresToBuses.get() * network.getBusView().getBusStream().count());
        }
        else {
            nbMeasurements = Math.round(DEFAULT_RATIO_MEASURES_TO_BUSES * network.getBusView().getBusStream().count());
        }
        // Remove from it the number of measurements that already exist
        long nbAlreadyExistingMeasurements = knowledge.getMeasuresCount();
        nbMeasurements = Math.max(nbMeasurements - nbAlreadyExistingMeasurements, 0);

        // Compute the number of measurements needed for the controlled measurement type
        long nbMeasurementsCtrlType = Math.round(nbMeasurements * ratioForCtrlMeasType);

        // Initialize lists in which to pick measurement locations (bus or branch) (one list per type of measurement)
        List<Branch> listOfBranchesPfSide1 = new ArrayList<>();
        List<Bus> listOfBusesV = new ArrayList<>();
        // Consider only buses and branches from the main connected component (make sure each element is unique)
        for (Branch branch : network.getBranches()) {
            Bus b1 = branch.getTerminal1().getBusView().getConnectableBus();
            Bus b2 = branch.getTerminal2().getBusView().getConnectableBus();
            if (b1.isInMainConnectedComponent()
                    && b2.isInMainConnectedComponent()) {
                listOfBranchesPfSide1.add(branch);
                if (!listOfBusesV.contains(b1)) {
                    listOfBusesV.add(b1);
                }
                if (!listOfBusesV.contains(b2)) {
                    listOfBusesV.add(b2);
                }
            }
        }
        // If a "no-pick" branch is given, remove it from these lists
        if (noPickBranchID.isPresent() && network.getBranchStream().map(Identifiable::getId).toList().contains(noPickBranchID.get())) {
            listOfBranchesPfSide1.removeIf(branch -> branch.getId().equals(noPickBranchID.get()));
        }
        List<Branch> listOfBranchesPfSide2 = new ArrayList<>(listOfBranchesPfSide1);
        List<Branch> listOfBranchesQfSide1 = new ArrayList<>(listOfBranchesPfSide1);
        List<Branch> listOfBranchesQfSide2 = new ArrayList<>(listOfBranchesPfSide1);
        List<Bus> listOfBusesP = new ArrayList<>();
        List<Bus> listOfBusesQ = new ArrayList<>();
        // For active and reactive power injections, remove from the list zero-injection buses (this information is known for sure : it is not a measure associated with uncertainty)
        for (Bus bus : listOfBusesV) {
            if (!knowledge.getZeroInjectionBuses().containsValue(bus.getId())) {
                listOfBusesP.add(bus);
                listOfBusesQ.add(bus);
            }
        }

        // Initialize random variables used in the random generation loop
        int randomType;
        Branch randomBranch;
        int randomSide;
        Bus randomBus;

        // Initialize a boolean stating if noise must be added to measurements,
        // and a Random for gaussian noise (use constant seed for repeatability)
        boolean withNoise = addNoise.isPresent() && addNoise.get().equals(true);
        Random noise = new Random(0);
        // If given, control the size of the interval [-k sigma; +k sigma] in which noise will be picked at random (uniform distribution, sigma : std of measurement)
        double noiseCoef = 1;
        if (noiseAmplitude.isPresent() && noiseAmplitude.get() > 0) {
            noiseCoef = noiseAmplitude.get();
        }

        // Initialize new Random (use the seed if provided) to pick measurements
        Random random = seed.map(Random::new).orElseGet(() -> new Random(System.currentTimeMillis()));

        // Initialize measurement value and variance
        double measurementValue;
        double measurementVariance;

        // Find the starting measurement number, such that no new measure will be given the number of an already existing measure
        ArrayList<Integer> allExistingMeasNumbers = new ArrayList<>();
        allExistingMeasNumbers.add(0);
        allExistingMeasNumbers.addAll(knowledge.getActivePowerInjectedMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getReactivePowerInjectedMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getActivePowerFlowMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getReactivePowerFlowMeasures().keySet());
        allExistingMeasNumbers.addAll(knowledge.getVoltageMagnitudeMeasures().keySet());
        int startingMeasurementNumber = Collections.max(allExistingMeasNumbers) + 1;

        // For each measurement to be generated, pick a measurement type at random
        for (int i = startingMeasurementNumber; i < nbMeasurements + startingMeasurementNumber; i++) {

            Map<String, String> randomMeasure = new HashMap<>();

            randomType = random.nextInt(ALL_MEASUREMENT_TYPES.size());

            // Change randomType to get (approximately) the number of measurements specified by nbMeasurementsCtrlType
            if (i < nbMeasurementsCtrlType + 1) {
                if (ctrlMeasType.equals("Pf")) {
                    randomType = 0;
                }
                if (ctrlMeasType.equals("Qf")) {
                    randomType = 2;
                }
                if (ctrlMeasType.equals("P")) {
                    randomType = 4;
                }
                if (ctrlMeasType.equals("Q")) {
                    randomType = 5;
                }
                if (ctrlMeasType.equals("V")) {
                    randomType = 6;
                }
            }
            else {
                if (ctrlMeasType.equals("Pf") && (randomType == 0 | randomType == 1 )) {
                    randomType = random.nextInt(5) + 2;
                }
                if (ctrlMeasType.equals("Qf") && (randomType == 2 | randomType == 3 )) {
                    randomType = random.nextInt(5);
                    randomType = randomType > 1 ? randomType + 2 : randomType;
                }
                if (ctrlMeasType.equals("P") && randomType == 4) {
                    randomType = random.nextInt(6);
                    randomType = randomType > 3 ? randomType + 1 : randomType;
                }
                if (ctrlMeasType.equals("Q") && randomType == 5) {
                    randomType = random.nextInt(6);
                    randomType = randomType > 4 ? randomType + 1 : randomType;
                }
                if (ctrlMeasType.equals("V") && randomType == 6) {
                    randomType = random.nextInt(6);
                }
            }

            if (randomType == 0 || randomType == 1) {
                // Add a "Pf" measure
                randomMeasure.put("Type", "Pf");
                // Pick at random which branch side will be measured
                randomSide = random.nextInt(2);
                if (randomSide == 0) { // Side 1
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesPfSide1.isEmpty()) {
                        randomBranch = listOfBranchesPfSide1.remove(random.nextInt(listOfBranchesPfSide1.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal1().getP();
                        // If line is disconnected, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Pf")
                                        * Math.max(Math.abs(measurementValue), MIN_ACTIVE_POWER_MW)
                                , 2);

                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                } else { // Side 2
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesPfSide2.isEmpty()) {
                        randomBranch = listOfBranchesPfSide2.remove(random.nextInt(listOfBranchesPfSide2.size()));
                        // Get location IDs and the corresponding measurement value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal2().getP();
                        // If line is disconnected, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Pf")
                                        * Math.max(Math.abs(measurementValue), MIN_ACTIVE_POWER_MW)
                                , 2);

                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                }
            }

            else if (randomType == 2 || randomType == 3) {
                // Add a "Qf" measure
                randomMeasure.put("Type", "Qf");
                // Pick at random which branch side will be measured
                randomSide = random.nextInt(2);
                if (randomSide == 0) { // Side 1
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesQfSide1.isEmpty()) {
                        randomBranch = listOfBranchesQfSide1.remove(random.nextInt(listOfBranchesQfSide1.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal1().getQ();
                        // If line is disconnected, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Qf")
                                        * Math.max(Math.abs(measurementValue), MIN_REACTIVE_POWER_MVAR)
                                , 2);

                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                } else { // Side 2
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesQfSide2.isEmpty()) {
                        randomBranch = listOfBranchesQfSide2.remove(random.nextInt(listOfBranchesQfSide2.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal2().getBusView().getConnectableBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal1().getBusView().getConnectableBus().getId());
                        measurementValue = randomBranch.getTerminal2().getQ();
                        // If line is disconnected, returned value will be Double.NaN : make it 0
                        if (Double.isNaN(measurementValue)) {
                            measurementValue = 0;
                        }
                        // Get and add measurement variance (in SI^2)
                        measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Qf")
                                        * Math.max(Math.abs(measurementValue), MIN_REACTIVE_POWER_MVAR)
                                , 2);

                        randomMeasure.put("Variance", String.valueOf(measurementVariance));
                        // Add measurement value (possibly with noise)
                        if (withNoise) {
                            measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                        }
                        randomMeasure.put("Value", String.valueOf(measurementValue));
                    } else {
                        randomMeasure = null;
                    }
                }
            }

            else if (randomType == 4) {
                // Add a "P" measure
                randomMeasure.put("Type", "P");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesP.isEmpty()) {
                    randomBus = listOfBusesP.remove(random.nextInt(listOfBusesP.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    measurementValue = -1 * randomBus.getP();
                    // Get and add measurement variance (in SI^2)
                    measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("P")
                                    * Math.max(Math.abs(measurementValue), MIN_ACTIVE_POWER_MW)
                            , 2);

                    randomMeasure.put("Variance", String.valueOf(measurementVariance));
                    // Add measurement value (possibly with noise)
                    if (withNoise) {
                        measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                    }
                    randomMeasure.put("Value", String.valueOf(measurementValue));
                } else {
                    randomMeasure = null;
                }

            } else if (randomType == 5) {
                // Add a "Q" measure
                randomMeasure.put("Type", "Q");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesQ.isEmpty()) {
                    randomBus = listOfBusesQ.remove(random.nextInt(listOfBusesQ.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    measurementValue = -1 * randomBus.getQ();

                    // TODO : modifier le scaling de la variance
                    // Get and add measurement variance (in SI^2)
                    measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("Q")
                                    * Math.max(Math.abs(measurementValue), MIN_REACTIVE_POWER_MVAR)
                            , 2);

                    randomMeasure.put("Variance", String.valueOf(measurementVariance));

                    // Add measurement value (possibly with noise)
                    if (withNoise) {
                        measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                    }
                    randomMeasure.put("Value", String.valueOf(measurementValue));
                } else {
                    randomMeasure = null;
                }
            }

            else if (randomType == 6) {
                // Add a "V" measure
                randomMeasure.put("Type", "V");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesV.isEmpty()) {
                    randomBus = listOfBusesV.remove(random.nextInt(listOfBusesV.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    measurementValue = randomBus.getV();
                    // TODO
                    // Get and add measurement variance (in SI^2)
                    measurementVariance = Math.pow(RELATIVE_STD_BY_MEAS_TYPE.get("V")
                                    * Math.max(Math.abs(measurementValue), MIN_VOLTAGE_KV)
                            , 2);
                    randomMeasure.put("Variance", String.valueOf(measurementVariance));
                    // Add measurement value (possibly with noise)
                    if (withNoise) {
                        measurementValue += - noiseCoef * Math.sqrt(measurementVariance) + 2 * noiseCoef * Math.sqrt(measurementVariance) * noise.nextDouble();
                    }
                    randomMeasure.put("Value", String.valueOf(measurementValue));
                } else {
                    randomMeasure = null;
                }
            } else {
                throw new IllegalArgumentException("More measurement types given than what the generator can handle. Check ALL_MEASUREMENT_TYPES");
            }

            // Try to add the measure if not null (note that the measure could be redundant)
            if (randomMeasure != null) {
                try {
                    knowledge.addMeasure(i, randomMeasure, network);
                } catch (IllegalArgumentException illegalArgumentException) {
                    if (illegalArgumentException.getMessage().equals("A measurement already exists for the location and type of the measurement provided. It can not be added.")) {
                        i = i - 1;
                    }
                    else {
                        System.out.printf("%nMeasurement n° %d could not be added. Reason :%n", i);
                        throw illegalArgumentException;
                    }
                }
            } else { // If measure is null, it is because the list to choose measurement location has become empty (ex : all the buses are already assigned a voltage measure)
                // In this case, decrease i to get the proper quantity of measurements at the end of the process
                i = i - 1;
            }
        }
    }
}
