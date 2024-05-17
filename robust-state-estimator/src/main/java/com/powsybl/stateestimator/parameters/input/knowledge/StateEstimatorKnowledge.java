/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.knowledge;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.stateestimator.parameters.input.knowledge.RandomMeasuresGenerator;
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

    private Map<Integer, ArrayList<String>> activePowerFlowMeasures = new HashMap<>();
    private Map<Integer, ArrayList<String>> reactivePowerFlowMeasures = new HashMap<>();
    private Map<Integer, ArrayList<String>> activePowerInjectedMeasures = new HashMap<>();
    private Map<Integer, ArrayList<String>> reactivePowerInjectedMeasures = new HashMap<>();
    private Map<Integer, ArrayList<String>> voltageMagnitudeMeasures = new HashMap<>();
    private Map<Integer, ArrayList<String>> suspectBranches = new HashMap<>();
    private String slackBus;
    private Map<Integer, String> zeroInjectionBuses = new HashMap<>();

    public StateEstimatorKnowledge(Network network) {
        setSlack(DEFAULT_SLACK_SELECTION_MODE, network)
                .setZeroInjectionBuses(network)
                .setSuspectBranchesByDefault(network);
    }

    public StateEstimatorKnowledge(Network network, String slackBusId) {
        setSlack(slackBusId, network)
                .setZeroInjectionBuses(network)
                .setSuspectBranchesByDefault(network);
    }

    // Constructor used to deep copy a StateEstimatorKnowledge instance
    public StateEstimatorKnowledge(StateEstimatorKnowledge knowledge) {
        this.activePowerFlowMeasures = deepCopy(knowledge.activePowerFlowMeasures);
        this.reactivePowerFlowMeasures = deepCopy(knowledge.reactivePowerFlowMeasures);
        this.activePowerInjectedMeasures = deepCopy(knowledge.activePowerInjectedMeasures);
        this.reactivePowerInjectedMeasures = deepCopy(knowledge.reactivePowerInjectedMeasures);
        this.voltageMagnitudeMeasures = deepCopy(knowledge.voltageMagnitudeMeasures);
        this.suspectBranches = deepCopy(knowledge.suspectBranches);
        this.zeroInjectionBuses =  new HashMap<>(knowledge.zeroInjectionBuses);
        this.slackBus = knowledge.slackBus;
    }

    // Auxiliary function used for deep copying
    public Map<Integer, ArrayList<String>> deepCopy(Map<Integer, ArrayList<String>> originalMap) {
        Map<Integer, ArrayList<String>> deepCopyMap = new HashMap<>();
        for (Map.Entry<Integer, ArrayList<String>> entry : originalMap.entrySet()) {
            Integer key = entry.getKey();
            ArrayList<String> originalList = entry.getValue();
            ArrayList<String> deepCopyList = new ArrayList<>(originalList);
            deepCopyMap.put(key, deepCopyList);
        }
        return deepCopyMap;
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

    public Map<Integer, ArrayList<String>> getSuspectBranches() {
        return suspectBranches;
    }

    public String getSlackBus() {
        return slackBus;
    }

    public Map<Integer, String> getZeroInjectionBuses() {
        return zeroInjectionBuses;
    }

    /**
     * @param measurementNumber The number of the measurement, which must be unique within the set of all measurements (all types considered)
     * @return The measure corresponding to the measurement number, or "null" if no such measure exists
     */
    public ArrayList<String> getMeasure(Integer measurementNumber) {
        if (activePowerFlowMeasures.containsKey(measurementNumber)) {
            return activePowerFlowMeasures.get(measurementNumber);
        }
        if (reactivePowerFlowMeasures.containsKey(measurementNumber)) {
            return reactivePowerFlowMeasures.get(measurementNumber);
        }
        if (activePowerInjectedMeasures.containsKey(measurementNumber)) {
            return activePowerInjectedMeasures.get(measurementNumber);
        }
        if (reactivePowerInjectedMeasures.containsKey(measurementNumber)) {
            return reactivePowerInjectedMeasures.get(measurementNumber);
        }
        if (voltageMagnitudeMeasures.containsKey(measurementNumber)) {
            return voltageMagnitudeMeasures.get(measurementNumber);
        }
        return null;
    }

    /**
     * @param measurementNumber The number of the measurement, which must be unique within the set of all measurements (all types considered)
     * @param measure           The measure to be added (contains type, location ID(s), value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied
     */
    public StateEstimatorKnowledge addMeasure(Integer measurementNumber, Map<String, String> measure, Network network) throws IllegalArgumentException {
        // Check that measurementNumber is unique within the set of all measurements
        if (this.activePowerFlowMeasures.containsKey(measurementNumber) | this.reactivePowerFlowMeasures.containsKey(measurementNumber)
        | this.activePowerInjectedMeasures.containsKey(measurementNumber) | this.reactivePowerInjectedMeasures.containsKey(measurementNumber)
        | this.voltageMagnitudeMeasures.containsKey(measurementNumber)) {
            throw new IllegalArgumentException("The measurement number provided is not unique within the set of all measurements.");
        }
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
     * @param measurementNumberToBeRemoved The number of the measurement, which must be unique within the set of all measurements (all types considered).
     *                                     If it does not exist, no exception is thrown.
     * @return The object on which the method is applied
     */
    public StateEstimatorKnowledge removeMeasure(Integer measurementNumberToBeRemoved) {
        this.activePowerFlowMeasures.remove(measurementNumberToBeRemoved);
        this.reactivePowerFlowMeasures.remove(measurementNumberToBeRemoved);
        this.activePowerInjectedMeasures.remove(measurementNumberToBeRemoved);
        this.reactivePowerInjectedMeasures.remove(measurementNumberToBeRemoved);
        this.voltageMagnitudeMeasures.remove(measurementNumberToBeRemoved);
        return this;
    }

    /**
     * @param measurementNumber The number of the measurement for this type
     * @param measure           The active power flow measure to be added (contains location, value and variance)
     * @param network           The network to which the measurement is related
     * @return The object on which the method is applied
     */
    private StateEstimatorKnowledge addActivePowerFlowMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
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
        // Check that the variance provided is positive
        if (Double.parseDouble(measure.get("Variance")) < 0) {
            throw new IllegalArgumentException("The variance provided must be non-negative.");
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
        List<String> terminalsId = List.of(new String[]{network.getBranch(measure.get("BranchID")).getTerminal1().getBusView().getConnectableBus().getId(),
                network.getBranch(measure.get("BranchID")).getTerminal2().getBusView().getConnectableBus().getId()});
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
    private StateEstimatorKnowledge addReactivePowerFlowMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
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
        // Check that the variance provided is positive
        if (Double.parseDouble(measure.get("Variance")) < 0) {
            throw new IllegalArgumentException("The variance provided must be non-negative.");
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
        List<String> terminalsId = List.of(new String[]{network.getBranch(measure.get("BranchID")).getTerminal1().getBusView().getConnectableBus().getId(),
                network.getBranch(measure.get("BranchID")).getTerminal2().getBusView().getConnectableBus().getId()});
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
    private StateEstimatorKnowledge addActivePowerInjectedMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
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
        // Check that the variance provided is positive
        if (Double.parseDouble(measure.get("Variance")) < 0) {
            throw new IllegalArgumentException("The variance provided must be non-negative.");
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
    private StateEstimatorKnowledge addReactivePowerInjectedMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
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
        // Check that the variance provided is positive
        if (Double.parseDouble(measure.get("Variance")) < 0) {
            throw new IllegalArgumentException("The variance provided must be non-negative.");
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
    private StateEstimatorKnowledge addVoltageMagnitudeMeasure(Integer measurementNumber, Map<String, String> measure, Network network) {
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
        // Check that the variance provided is positive
        if (Double.parseDouble(measure.get("Variance")) < 0) {
            throw new IllegalArgumentException("The variance provided must be non-negative.");
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
     * @param network The network for which the object suspectBranches is created by default
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge setSuspectBranchesByDefault(Network network) {
        int i = 1;
        suspectBranches.clear();
        for (Branch branch : network.getBranches()) {
            ArrayList<String> suspectBranch = new ArrayList<>();
            suspectBranch.add(branch.getId());
            suspectBranch.add("0"); // By default, branch is not suspected
            suspectBranch.add("1"); // By default, branch is presumed to be closed
            suspectBranches.put(i, suspectBranch);
            i++;
        }
        return this;
    }

    /**
     * @param suspectBranchID The ID of the branch to be set
     * @param isSuspected     Boolean : if true, branch status is suspected; else, it is not
     * @param presumedStatus  Initial assumption on the status of the branch : can only equal "ASSUMED CLOSED" or "ASSUMED OPENED"
     * @return The object on which the method is applied.
     */
    public StateEstimatorKnowledge setSuspectBranch(String suspectBranchID, boolean isSuspected, String presumedStatus) {
        // Check that the ID of the suspect branch exists in the network (or equivalently, in suspectBranches, as suspectBranches contains all the branches by default)
        if (!suspectBranches.values().stream().map(ArrayList -> ArrayList.get(0)).toList().contains(suspectBranchID)) {
            String messageError = "The branch ID (" + suspectBranchID + ") does not exist in the network.";
            throw new IllegalArgumentException(messageError);
        }
        if (!presumedStatus.equals("PRESUMED CLOSED") && !presumedStatus.equals("PRESUMED OPENED")) {
            throw new IllegalArgumentException("The assumed status can only be one of the two following values : "+
                    "PRESUMED CLOSED or PRESUMED OPENED");
        }
        // Get the suspect branch number corresponding to suspectBranchID
        Integer suspectBranchNumber = suspectBranches.entrySet().stream().filter(entry -> entry.getValue()
                        .get(0).equals(suspectBranchID)).map(Map.Entry::getKey)
                        .findFirst().get();
        ArrayList<String> newAssumption = new ArrayList<>();
        newAssumption.add(suspectBranchID);
        newAssumption.add(isSuspected ? "1" : "0");
        newAssumption.add(presumedStatus.equals("PRESUMED CLOSED") ? "1" : "0");
        suspectBranches.put(suspectBranchNumber, newAssumption);
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
    private static String selectMostMeshedBus(Network network) {
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

    /**
     * @param network    The network the zero-injection buses belong to.
     * @return The object on which the method is applied.
     */
    private StateEstimatorKnowledge setZeroInjectionBuses(Network network) {
        int i = 1;
        zeroInjectionBuses.clear();
        for (Bus bus: network.getBusView().getBuses()) {
            if (bus.getGeneratorStream().findAny().isEmpty() && bus.getLoadStream().findAny().isEmpty()
                    && bus.getShuntCompensatorStream().findAny().isEmpty() && bus.getStaticVarCompensatorStream().findAny().isEmpty()
                    && bus.getBatteryStream().findAny().isEmpty() && bus.getLccConverterStationStream().findAny().isEmpty()
                    && bus.getVscConverterStationStream().findAny().isEmpty()) {
                zeroInjectionBuses.put(i++, bus.getId());
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

    // TODO : fix deserialization
    // Empty constructor, used only for deserialization
    @JsonCreator
    public StateEstimatorKnowledge() {
    }

    // To save and load an instance of StateEstimatorKnowledge
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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



