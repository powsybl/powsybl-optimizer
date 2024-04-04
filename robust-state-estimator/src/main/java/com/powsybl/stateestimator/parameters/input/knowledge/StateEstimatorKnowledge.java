/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.knowledge;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.ObjectCodec; // Keep this, even if marked as unused!
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.jgrapht.alg.util.Pair;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorKnowledge {

    public static final String DEFAULT_SLACK_SELECTION_MODE = "MOST_MESHED";
    public static final ArrayList<String> ALL_MEASUREMENT_TYPES = new ArrayList<>() {{
        add("PfSide1");
        add("PfSide2");
        add("QfSide1");
        add("QfSide2");
        add("P");
        add("Q");
        add("V");
    }};
    public static final Map<String, Double> DEFAULT_STD_IN_PU_BY_MEAS_TYPE = new HashMap<>() {{
        put("Pf", 0.02);
        put("Qf", 0.04);
        put("P", 0.02);
        put("Q", 0.04);
        put("V", 0.0001);
    }};
    // Standard deviation values (p.u) above as chosen in doi:10.3390/en11030570
    // TODO : check consistency of these values
    public static final double BASE_POWER_MVA = 100;


    Map<Integer, ArrayList<String>> activePowerFlowMeasures = new HashMap<>();
    Map<Integer, ArrayList<String>> reactivePowerFlowMeasures = new HashMap<>();

    Map<Integer, ArrayList<String>> activePowerInjectedMeasures = new HashMap<>();

    Map<Integer, ArrayList<String>> reactivePowerInjectedMeasures = new HashMap<>();

    Map<Integer, ArrayList<String>> voltageMagnitudeMeasures = new HashMap<>();

    Map<Integer, String> suspectBranches = new HashMap<>();

    String slackBus;

    public StateEstimatorKnowledge(Network network) {
        setSlack(DEFAULT_SLACK_SELECTION_MODE, network);
    }

    public StateEstimatorKnowledge(Network network, String slackBusId) {
        setSlack(slackBusId, network);
    }

    public int getMeasuresCount() {
        return getActivePowerFlowMeasures().size() + getActivePowerInjectedMeasures().size()
                + getReactivePowerFlowMeasures().size() + getReactivePowerInjectedMeasures().size()
                + getVoltageMagnitudeMeasures().size();
    }

    public Map<Integer, ArrayList<String>> getActivePowerFlowMeasures() {
        return activePowerFlowMeasures;
    }

    public Map<Integer, ArrayList<String>> getReactivePowerFlowMeasures() {
        return reactivePowerFlowMeasures;
    }

    public Map<Integer, ArrayList<String>> getActivePowerInjectedMeasures() {
        return activePowerInjectedMeasures;
    }

    public Map<Integer, ArrayList<String>> getReactivePowerInjectedMeasures() {
        return reactivePowerInjectedMeasures;
    }

    public Map<Integer, ArrayList<String>> getVoltageMagnitudeMeasures() {
        return voltageMagnitudeMeasures;
    }

    public Map<Integer, String> getSuspectBranches() {
        return suspectBranches;
    }

    public String getSlackBus() {
        return slackBus;
    }

    /**
     * @param measurementNumber The number of the measurement
     * @param measure           The measure to be added (contains type, location ID(s), value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied
     */
    public StateEstimatorKnowledge addMeasure(Integer measurementNumber, Map<String, String> measure, Network network) throws IllegalArgumentException {
        if (measure.get("Type").equals("Pf")) {
            return this.addActivePowerFlowMeasure(measurementNumber, measure, network);
        } else if (measure.get("Type").equals("Qf")) {
            return this.addReactivePowerFlowMeasure(measurementNumber, measure, network);
        } else if (measure.get("Type").equals("P")) {
            return this.addActivePowerInjectedMeasure(measurementNumber, measure, network);
        } else if (measure.get("Type").equals("Q")) {
            return this.addReactivePowerInjectedMeasure(measurementNumber, measure, network);
        } else if (measure.get("Type").equals("V")) {
            return this.addVoltageMagnitudeMeasure(measurementNumber, measure, network);
        } else {
            throw new IllegalArgumentException(("The measurement type provided is not accepted. Only types accepted : \"Pf\", \"Qf\", \"P\", \"Q\", \"V\"."));
        }
    }

    /**
     * @param measurementNumber The number of the measurement for this type
     * @param measure           The active power flow measure to be added (contains location, value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied
     */
    public StateEstimatorKnowledge addActivePowerFlowMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
        // Consistency checks on the measurement provided
        if (measurementNumber < 1) {
            throw new IllegalArgumentException("The measurement number must be a nonzero integer.");
        }
        if (activePowerFlowMeasures.containsKey(measurementNumber)) {
            throw new IllegalArgumentException("The measurement number must be unique within the set of active power flow measurements.");
        }
        if (!measure.containsKey("Value")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement value (recall : in MW).");
        }
        if (!measure.containsKey("Variance")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement variance (recall : in MW^2).");
        }
        if (!measure.containsKey("BranchID")) {
            throw new IllegalArgumentException("No entry corresponding to the branch ID related to the measurement.");
        }
        if (!measure.containsKey("FirstBusID")) {
            throw new IllegalArgumentException("No entry corresponding to the ID of the first bus related to the measurement.");
        }
        if (!measure.containsKey("SecondBusID")) {
            throw new IllegalArgumentException("No entry corresponding to the ID of the second bus related to the measurement.");
        }
        if (!measure.containsKey("Type")) {
            throw new IllegalArgumentException("No entry corresponding to the type of the measurement.");
        }
        if (measure.size() != 6) {
            throw new IllegalArgumentException("Unexpected number of indications (expected 6) given relative to the measurement.");
        }
        // Check that IDs related to the measurement exist in the network
        if (!network.getBranchStream().map(Identifiable::getId).toList().contains(measure.get("BranchID"))) {
            throw new IllegalArgumentException("The branch ID related to the measurement does not exist in the network.");
        }
        if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(measure.get("FirstBusID"))) {
            throw new IllegalArgumentException("The ID of the first bus related to the measurement does not exist in the network.");
        }
        if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(measure.get("SecondBusID"))) {
            throw new IllegalArgumentException("The ID of the second bus related to the measurement does not exist in the network.");
        }
        // Check that FirstBusID and SecondBusID are indeed the IDs of the terminals of the branch provided
        List<String> terminalsId = List.of(new String[]{network.getBranch(measure.get("BranchID")).getTerminal1().getBusView().getBus().getId(),
                network.getBranch(measure.get("BranchID")).getTerminal2().getBusView().getBus().getId()});
        if (!terminalsId.contains(measure.get("FirstBusID"))) {
            throw new IllegalArgumentException("FirstBusID is not the ID of any of the two terminals of the branch provided.");
        }
        if (!terminalsId.contains(measure.get("SecondBusID"))) {
            throw new IllegalArgumentException("SecondBusID is not the ID of any of the two terminals of the branch provided.");
        }
        if (measure.get("FirstBusID").equals(measure.get("SecondBusID"))) {
            throw new IllegalArgumentException("FirstBusID and SecondBusID can not be the same string.");
        }
        // Check that the measurement is not already contained in activePowerFlowMeasures (it is sufficient to check that the pair (BranchID, FirstBusID) does not already exist)
        if (activePowerFlowMeasures.values().stream().map(ArrayList -> new Pair(ArrayList.get(1), ArrayList.get(2))).toList().contains(
                new Pair(measure.get("BranchID"), measure.get("FirstBusID")))) {
            throw new IllegalArgumentException("A measurement already exists for the location and type of the measurement provided. It can not be added.");
        }
        // Add the measurement to the list
        ArrayList<String> arrayMeasure = new ArrayList<>();
        arrayMeasure.add(measure.get("Type"));
        arrayMeasure.add(measure.get("BranchID"));
        arrayMeasure.add(measure.get("FirstBusID"));
        arrayMeasure.add(measure.get("SecondBusID"));
        arrayMeasure.add(measure.get("Value"));
        arrayMeasure.add(measure.get("Variance"));
        activePowerFlowMeasures.put(measurementNumber, arrayMeasure);
        return this;
    }

    /**
     * @param measurementNumber The number of the measurement for this type
     * @param measure           The reactive power flow measure to be added (contains location, value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge addReactivePowerFlowMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
        // Consistency checks on the measurement provided
        if (measurementNumber < 1) {
            throw new IllegalArgumentException("The measurement number must be a nonzero integer.");
        }
        if (reactivePowerFlowMeasures.containsKey(measurementNumber)) {
            throw new IllegalArgumentException("The measurement number must be unique within the set of reactive power flow measurements.");
        }
        if (!measure.containsKey("Value")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement value (recall : in MVar).");
        }
        if (!measure.containsKey("Variance")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement variance (recall : in MVar^2).");
        }
        if (!measure.containsKey("BranchID")) {
            throw new IllegalArgumentException("No entry corresponding to the branch ID related to the measurement.");
        }
        if (!measure.containsKey("FirstBusID")) {
            throw new IllegalArgumentException("No entry corresponding to the ID of the first bus related to the measurement.");
        }
        if (!measure.containsKey("SecondBusID")) {
            throw new IllegalArgumentException("No entry corresponding to the ID of the second bus related to the measurement.");
        }
        if (!measure.containsKey("Type")) {
            throw new IllegalArgumentException("No entry corresponding to the type of the measurement.");
        }
        if (measure.size() != 6) {
            throw new IllegalArgumentException("Unexpected number of indications (expected 6) given relative to the measurement.");
        }
        // Check that IDs related to the measurement exist in the network
        if (!network.getBranchStream().map(Identifiable::getId).toList().contains(measure.get("BranchID"))) {
            throw new IllegalArgumentException("The branch ID related to the measurement does not exist in the network.");
        }
        if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(measure.get("FirstBusID"))) {
            throw new IllegalArgumentException("The ID of the first bus related to the measurement does not exist in the network.");
        }
        if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(measure.get("SecondBusID"))) {
            throw new IllegalArgumentException("The ID of the second bus related to the measurement does not exist in the network.");
        }
        // Check that FirstBusID and SecondBusID are indeed the IDs of the terminals of the branch provided
        List<String> terminalsId = List.of(new String[]{network.getBranch(measure.get("BranchID")).getTerminal1().getBusView().getBus().getId(),
                network.getBranch(measure.get("BranchID")).getTerminal2().getBusView().getBus().getId()});
        if (!terminalsId.contains(measure.get("FirstBusID"))) {
            throw new IllegalArgumentException("FirstBusID is not the ID of any of the two terminals of the branch provided.");
        }
        if (!terminalsId.contains(measure.get("SecondBusID"))) {
            throw new IllegalArgumentException("SecondBusID is not the ID of any of the two terminals of the branch provided.");
        }
        if (measure.get("FirstBusID").equals(measure.get("SecondBusID"))) {
            throw new IllegalArgumentException("FirstBusID and SecondBusID can not be the same string.");
        }
        // Check that the measurement is not already contained in activePowerFlowMeasures (it is sufficient to check that the pair (BranchID, FirstBusID) does not already exist)
        if (reactivePowerFlowMeasures.values().stream().map(ArrayList -> new Pair(ArrayList.get(1), ArrayList.get(2))).toList().contains(
                new Pair(measure.get("BranchID"), measure.get("FirstBusID")))) {
            throw new IllegalArgumentException("A measurement already exists for the location and type of the measurement provided. It can not be added.");
        }
        // Add the measurement to the list
        ArrayList<String> arrayMeasure = new ArrayList<>();
        arrayMeasure.add(measure.get("Type"));
        arrayMeasure.add(measure.get("BranchID"));
        arrayMeasure.add(measure.get("FirstBusID"));
        arrayMeasure.add(measure.get("SecondBusID"));
        arrayMeasure.add(measure.get("Value"));
        arrayMeasure.add(measure.get("Variance"));
        reactivePowerFlowMeasures.put(measurementNumber, arrayMeasure);
        return this;
    }

    /**
     * @param measurementNumber The number of the measurement for this type
     * @param measure           The active injected power measure to be added (contains location, value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge addActivePowerInjectedMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
        // Consistency checks on the measurement provided
        if (measurementNumber < 1) {
            throw new IllegalArgumentException("The measurement number must be a nonzero integer.");
        }
        if (activePowerInjectedMeasures.containsKey(measurementNumber)) {
            throw new IllegalArgumentException("The measurement number must be unique within the set of active injected power measurements.");
        }
        if (!measure.containsKey("Value")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement value (recall : in MW).");
        }
        if (!measure.containsKey("Variance")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement variance (recall : in MW^2).");
        }
        if (!measure.containsKey("BusID")) {
            throw new IllegalArgumentException("No entry corresponding to the ID of the bus related to the measurement.");
        }
        if (!measure.containsKey("Type")) {
            throw new IllegalArgumentException("No entry corresponding to the type of the measurement.");
        }
        if (measure.size() != 4) {
            throw new IllegalArgumentException("Unexpected number of indications (expected 4) given relative to the measurement.");
        }
        // Check that ID related to the measurement exist in the network
        if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(measure.get("BusID"))) {
            throw new IllegalArgumentException("The ID of the bus related to the measurement does not exist in the network.");
        }
        // Check that the measurement is not already contained in activePowerInjectedMeasures
        if (activePowerInjectedMeasures.values().stream().map(ArrayList -> ArrayList.get(1)).toList().contains((measure.get("BusID")))) {
            throw new IllegalArgumentException("A measurement already exists for the location and type of the measurement provided. It can not be added.");
        }
        // Add the measurement to the list
        ArrayList<String> arrayMeasure = new ArrayList<>();
        arrayMeasure.add(measure.get("Type"));
        arrayMeasure.add(measure.get("BusID"));
        arrayMeasure.add(measure.get("Value"));
        arrayMeasure.add(measure.get("Variance"));
        activePowerInjectedMeasures.put(measurementNumber, arrayMeasure);
        return this;
    }

    /**
     * @param measurementNumber The number of the measurement for this type
     * @param measure           The reactive injected power measure to be added (contains location, value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge addReactivePowerInjectedMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
        // Consistency checks on the measurement provided
        if (measurementNumber < 1) {
            throw new IllegalArgumentException("The measurement number must be a nonzero integer.");
        }
        if (reactivePowerInjectedMeasures.containsKey(measurementNumber)) {
            throw new IllegalArgumentException("The measurement number must be unique within the set of reactive injected power measurements.");
        }
        if (!measure.containsKey("Value")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement value (recall : in MVar).");
        }
        if (!measure.containsKey("Variance")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement variance (recall : in MVar^2).");
        }
        if (!measure.containsKey("BusID")) {
            throw new IllegalArgumentException("No entry corresponding to the ID of the bus related to the measurement.");
        }
        if (!measure.containsKey("Type")) {
            throw new IllegalArgumentException("No entry corresponding to the type of the measurement.");
        }
        if (measure.size() != 4) {
            throw new IllegalArgumentException("Unexpected number of indications (expected 4) given relative to the measurement.");
        }
        // Check that ID related to the measurement exist in the network
        if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(measure.get("BusID"))) {
            throw new IllegalArgumentException("The ID of the bus related to the measurement does not exist in the network.");
        }
        // Check that the measurement is not already contained in reactivePowerInjectedMeasures
        if (reactivePowerInjectedMeasures.values().stream().map(ArrayList -> ArrayList.get(1)).toList().contains((measure.get("BusID")))) {
            throw new IllegalArgumentException("A measurement already exists for the location and type of the measurement provided. It can not be added.");
        }
        // Add the measurement to the list
        ArrayList<String> arrayMeasure = new ArrayList<>();
        arrayMeasure.add(measure.get("Type"));
        arrayMeasure.add(measure.get("BusID"));
        arrayMeasure.add(measure.get("Value"));
        arrayMeasure.add(measure.get("Variance"));
        reactivePowerInjectedMeasures.put(measurementNumber, arrayMeasure);
        return this;
    }

    /**
     * @param measurementNumber The number of the measurement for this type
     * @param measure           The voltage magnitude measure to be added (contains location, value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge addVoltageMagnitudeMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
        // Consistency checks on the measurement provided
        if (measurementNumber < 1) {
            throw new IllegalArgumentException("The measurement number must be a nonzero integer.");
        }
        if (voltageMagnitudeMeasures.containsKey(measurementNumber)) {
            throw new IllegalArgumentException("The measurement number must be unique within the set of voltage magnitude measurements.");
        }
        if (!measure.containsKey("Value")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement value (recall : in kV).");
        }
        if (Float.parseFloat(measure.get("Value")) < 0) {
            throw new IllegalArgumentException(("The value of a measurement on voltage magnitude must be non-negative."));
        }
        if (!measure.containsKey("Variance")) {
            throw new IllegalArgumentException("No entry corresponding to the measurement variance (recall : in kV^2).");
        }
        if (!measure.containsKey("BusID")) {
            throw new IllegalArgumentException("No entry corresponding to the ID of the bus related to the measurement.");
        }
        if (!measure.containsKey("Type")) {
            throw new IllegalArgumentException("No entry corresponding to the type of the measurement.");
        }
        if (measure.size() != 4) {
            throw new IllegalArgumentException("Unexpected number of indications (expected 4) given relative to the measurement.");
        }
        // Check that ID related to the measurement exist in the network
        if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(measure.get("BusID"))) {
            throw new IllegalArgumentException("The ID of the bus related to the measurement does not exist in the network.");
        }
        // Check that the measurement is not already contained in voltageMagnitudeMeasures
        if (voltageMagnitudeMeasures.values().stream().map(ArrayList -> ArrayList.get(1)).toList().contains((measure.get("BusID")))) {
            throw new IllegalArgumentException("A measurement already exists for the location and type of the measurement provided. It can not be added.");
        }
        // Add the measurement to the list
        ArrayList<String> arrayMeasure = new ArrayList<>();
        arrayMeasure.add(measure.get("Type"));
        arrayMeasure.add(measure.get("BusID"));
        arrayMeasure.add(measure.get("Value"));
        arrayMeasure.add(measure.get("Variance"));
        voltageMagnitudeMeasures.put(measurementNumber, arrayMeasure);
        return this;
    }

    /**
     * @param suspectBranchNumber The number corresponding to the addition of new suspect branch
     * @param suspectBranchId     The ID of the suspect branch added, within the network
     * @param network             The network to which the measurement is related
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge addSuspectBranch(Integer suspectBranchNumber, String suspectBranchId, Network network) {
        // Consistency checks
        if (suspectBranchNumber < 1) {
            throw new IllegalArgumentException("The suspect branch number must be a nonzero integer.");
        }
        if (suspectBranches.containsKey(suspectBranchNumber)) {
            throw new IllegalArgumentException("The suspect branch number must be unique within the set of suspect branches.");
        }
        if (suspectBranches.containsValue(suspectBranchId)) {
            throw new IllegalArgumentException("This suspect branch has already been added.");
        }
        // Check that the ID of the suspect branch exists in the network
        if (!network.getBranchStream().map(Identifiable::getId).toList().contains(suspectBranchId)) {
            throw new IllegalArgumentException("The ID of the suspect branch does not exist in the network.");
        }
        // Check that the suspect branch is not already contained in suspectBranches
        if (suspectBranches.values().stream().toList().contains(suspectBranchId)) {
            throw new IllegalArgumentException("The suspect branch provided as already been added.");
        }
        suspectBranches.put(suspectBranchNumber, suspectBranchId);
        return this;
    }

    /**
     * @param slackBusId The ID of the bus chosen as angle reference ("slack"). Can be equal to "MOST_MESHED" if such selection method is chosen.
     * @param network    The network to which the slack selection is related.
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge setSlack(String slackBusId, Network network) {
        if (slackBusId.equals(DEFAULT_SLACK_SELECTION_MODE)) {
            slackBus = selectMostMeshedBus(network);
        }
        // If slack bus ID is directly given, check that its ID does exist in the network
        else if (!network.getBusView().getBusStream().map(Identifiable::getId).toList().contains(slackBusId)) {
            throw new IllegalArgumentException("The ID of the bus indicated as slack does not exist in the network.");
        } else {
            slackBus = slackBusId;
        }
        return this;
    }

    /**
     * @param network The network to which the slack selection is related.
     * @return The object on which the method is applied.
     */
    public static String selectMostMeshedBus(Network network) {
        // Map each bus of the network to its nominal voltage
        Map<String, Double> nominalVoltages = network.getBusView().getBusStream().collect(Collectors.toMap(Identifiable::getId, bus -> bus.getVoltageLevel().getNominalV()));
        // Find the maximum nominal voltage in the network
        Double maxNominalV = nominalVoltages.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
        String slackBusId = network.getBusView().getBuses().iterator().next().getId();
        int largestNbBranchesConnected = 0;
        // Keep in memory the most connected bus with nominal voltage at least larger than 90% of the maximum nominal voltage
        for (Map.Entry<String, Double> entry : nominalVoltages.entrySet()) {
            String busId = entry.getKey();
            Double busNominalV = entry.getValue();
            if (busNominalV > 0.9 * maxNominalV) {
                if (network.getBusView().getBus(busId).getConnectedTerminalCount() > largestNbBranchesConnected) {
                    slackBusId = busId;
                }
            }
        }
        return slackBusId;
    }

    public StateEstimatorKnowledge generateRandomMeasurements(Network network) {
        long seed = System.currentTimeMillis();
        return generateRandomMeasurements(network, seed);
    }

    /**
     * This method generates random measurements out of the Load Flow results obtained on a network.
     * The measurements generated are added to the "knowledge" instance.
     * The number of measurement generated is large (4 times the number of buses) and distributed enough to ensure network observability.
     * We might want to skew the distribution to pick more often buses with higher voltages ("double roll, pick better"),
     * to emulate the fact that it is more likely to have measurement devices on the biggest nodes than on the smallest ones.
     * <p>
     * Note 1 : this method should be used for testing purposes only.
     * Note 2 : the sign of injected powers (P, Q) is inverted.
     * </p>
     *
     * @param network The network (LF run previously) for which random measurements must be generated
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge generateRandomMeasurements(Network network, long seed) throws IllegalArgumentException {

        // Compute the number of measurements needed (desired : 4 times the number of buses)
        long nbMeasurements = 4 * network.getBusView().getBusStream().count();
        // Remove from it the number of measurements that already exist
        long nbAlreadyExistingMeasurements = this.getMeasuresCount();
        nbMeasurements = Math.max(nbMeasurements - nbAlreadyExistingMeasurements, 0);

        // Initialize lists in which to pick measurement locations (bus or branch) (one list per type of measurement)
        List<Branch> listOfBranchesPfSide1 = new ArrayList<>(network.getBranchStream().toList());
        List<Branch> listOfBranchesPfSide2 = new ArrayList<>(listOfBranchesPfSide1);
        List<Branch> listOfBranchesQfSide1 = new ArrayList<>(listOfBranchesPfSide1);
        List<Branch> listOfBranchesQfSide2 = new ArrayList<>(listOfBranchesPfSide1);
        List<Bus> listOfBusesP = new ArrayList<>(network.getBusView().getBusStream().toList());
        List<Bus> listOfBusesQ = new ArrayList<>(listOfBusesP);
        List<Bus> listOfBusesV = new ArrayList<>(listOfBusesP);
        // Initialize random variables used in the random generation loop
        int randomType;
        Branch randomBranch;
        int randomSide;
        Bus randomBus;
        // Initialize new Random with seed given
        Random random = new Random(seed);

        // For each measurement to be generated, pick a measurement type at random
        for (int i = 1; i < nbMeasurements + 1; i++) {
            Map<String, String> randomMeasure = new HashMap<>();
            randomType = random.nextInt(ALL_MEASUREMENT_TYPES.size());

            if (randomType == 0 || randomType == 1) {
                // Add a "Pf" measure
                randomMeasure.put("Type", "Pf");
                // Pick at random which branch side will be measured
                randomSide = random.nextInt(2);
                if (randomSide == 0) {
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesPfSide1.isEmpty()) {
                        randomBranch = listOfBranchesPfSide1.remove(random.nextInt(listOfBranchesPfSide1.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getBus().getId());
                        randomMeasure.put("Value", String.valueOf(randomBranch.getTerminal1().getP()));
                    } else {
                        randomMeasure = null;
                    }
                } else {
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesPfSide2.isEmpty()) {
                        randomBranch = listOfBranchesPfSide2.remove(random.nextInt(listOfBranchesPfSide2.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal2().getBusView().getBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal1().getBusView().getBus().getId());
                        randomMeasure.put("Value", String.valueOf(randomBranch.getTerminal2().getP()));
                    } else {
                        randomMeasure = null;
                    }
                }
                // Get variance (in SI^2, not p.u.^2)
                randomMeasure.put("Variance", String.valueOf(
                        Math.pow(DEFAULT_STD_IN_PU_BY_MEAS_TYPE.get("Pf") * BASE_POWER_MVA, 2)));
            } else if (randomType == 2 || randomType == 3) {
                // Add a "Qf" measure
                randomMeasure.put("Type", "Qf");
                // Pick at random which branch side will be measured
                randomSide = random.nextInt(2);
                if (randomSide == 0) {
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesQfSide1.isEmpty()) {
                        randomBranch = listOfBranchesQfSide1.remove(random.nextInt(listOfBranchesQfSide1.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getBus().getId());
                        randomMeasure.put("Value", String.valueOf(randomBranch.getTerminal1().getQ()));
                    } else {
                        randomMeasure = null;
                    }
                } else {
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesQfSide2.isEmpty()) {
                        randomBranch = listOfBranchesQfSide2.remove(random.nextInt(listOfBranchesQfSide2.size()));
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal2().getBusView().getBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal1().getBusView().getBus().getId());
                        randomMeasure.put("Value", String.valueOf(randomBranch.getTerminal2().getQ()));
                    } else {
                        randomMeasure = null;
                    }
                }
                // Get variance (in SI^2, not p.u.^2)
                randomMeasure.put("Variance", String.valueOf(
                        Math.pow(DEFAULT_STD_IN_PU_BY_MEAS_TYPE.get("Qf") * BASE_POWER_MVA, 2)));
            } else if (randomType == 4) {
                // Add a "P" measure
                randomMeasure.put("Type", "P");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesP.isEmpty()) {
                    randomBus = listOfBusesP.remove(random.nextInt(listOfBusesP.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    randomMeasure.put("Value", String.valueOf(-1 * randomBus.getP()));
                    // Get variance (in SI^2, not p.u.^2)
                    randomMeasure.put("Variance", String.valueOf(
                            Math.pow(DEFAULT_STD_IN_PU_BY_MEAS_TYPE.get("P") * BASE_POWER_MVA, 2)));
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
                    randomMeasure.put("Value", String.valueOf(-1 * randomBus.getQ()));
                    // Get variance (in SI^2, not p.u.^2)
                    randomMeasure.put("Variance", String.valueOf(
                            Math.pow(DEFAULT_STD_IN_PU_BY_MEAS_TYPE.get("Q") * BASE_POWER_MVA, 2)));
                } else {
                    randomMeasure = null;
                }
            } else if (randomType == 6) {
                // Add a "V" measure
                randomMeasure.put("Type", "V");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesV.isEmpty()) {
                    randomBus = listOfBusesV.remove(random.nextInt(listOfBusesV.size()));
                    // Get bus ID
                    randomMeasure.put("BusID", randomBus.getId());
                    // Get measurement value (in SI), as given by the Load Flow solution
                    randomMeasure.put("Value", String.valueOf(randomBus.getV()));
                    // Get variance (in SI^2, not p.u.^2)
                    randomMeasure.put("Variance", String.valueOf(
                            Math.pow(DEFAULT_STD_IN_PU_BY_MEAS_TYPE.get("V") * randomBus.getVoltageLevel().getNominalV(), 2)));
                } else {
                    randomMeasure = null;
                }
            } else {
                throw new IllegalArgumentException("More measurements types given than what the generator can handle. Check ALL_MEASUREMENT_TYPES");
            }
            // Try to add the measure if not null (could be redundant)
            if (randomMeasure != null) {
                try {
                    this.addMeasure(i, randomMeasure, network);
                } catch (IllegalArgumentException illegalArgumentException) {
                    System.out.printf("%nMeasurement n° %d could not be added. Reason :%n", i);
                    throw illegalArgumentException;
                }
            } else { // If measure is null, it is because the list to choose measurement location has become empty (ex : all the buses are already assigned a voltage measure)
                // In this case, decrease i to get the proper quantity of measurements at the end of the process
                i = i - 1;
            }
        }
        return this;
    }

    public void printAllMeasures() {
        this.printActivePowerFlowMeasures();
        this.printReactivePowerFlowMeasures();
        this.printActivePowerInjectedMeasures();
        this.printReactivePowerInjectedMeasures();
        this.printVoltageMagnitudeMeasures();
    }

    public void printActivePowerFlowMeasures() {
        new ActivePowerFlowMeasures(this.getActivePowerFlowMeasures()).print();
    }

    public void printReactivePowerFlowMeasures() {
        new ReactivePowerFlowMeasures(this.getReactivePowerFlowMeasures()).print();
    }

    public void printActivePowerInjectedMeasures() {
        new ActivePowerInjectedMeasures(this.getActivePowerInjectedMeasures()).print();
    }

    public void printReactivePowerInjectedMeasures() {
        new ReactivePowerInjectedMeasures(this.getReactivePowerInjectedMeasures()).print();
    }

    public void printVoltageMagnitudeMeasures() {
        new VoltageMagnitudeMeasures(this.getVoltageMagnitudeMeasures()).print();
    }

    // To save and load an instance of StateEstimatorKnowledge
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Empty constructor, for deserialization
    public StateEstimatorKnowledge() {
    }

    // Save StateEstimatorKnowledge instance as JSON file
    public void write(FileOutputStream fileOutputStream) {
        try {
            OBJECT_MAPPER.writeValue(fileOutputStream, this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // Build a StateEstimatorKnowledge instance from a JSON file
    public static StateEstimatorKnowledge read(String pathToFile) throws IOException {
        File file = new File(pathToFile);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return OBJECT_MAPPER.readValue(file, StateEstimatorKnowledge.class);
    }
}



