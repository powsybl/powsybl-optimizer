/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.knowledge;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
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
    public static final Map<String, Double> DEFAULT_STD_IN_PU_BY_MEAS_TYPE = new HashMap<>() {{
        put("Pf", 0.021);
        put("Qf", 0.043);
        put("P", 0.021);
        put("Q", 0.043);
        put("V", 0.0001);
    }};
    // Standard deviation values (p.u) for measurements, see Master Thesis' report

    public static final double BASE_POWER_MVA = 100;

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
     * </p>
     *
     * @param knowledge The knowledge object that will store the random measurements generated
     * @param network The network (LF run previously) for which random measurements must be generated
     * @param seed (optional) The seed used by the random generator
     * @param ratioMeasuresToBuses (optional) The ratio "number of measures"/"number of buses in the network" used to compute the number of measures generated
     * @param biasTowardsHVNodes (optional) If "true", a bias towards HV nodes, making them more likely to be picked as the locations of generated measurements
     */
    public static void generateRandomMeasurements(StateEstimatorKnowledge knowledge, Network network,
                                                  Optional<Integer> seed, Optional<Double> ratioMeasuresToBuses,
                                                  Optional<Boolean> biasTowardsHVNodes)
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

        // The following variables will be used only if bias towards HV nodes is activated
        boolean withBias = biasTowardsHVNodes.isPresent() && biasTowardsHVNodes.get().equals(true);
        Branch randomBranchSecondPick;
        Bus randomBusSecondPick;
        double tmpVNomi;

        // Initialize new Random (use the seed if provided)
        Random random = seed.map(Random::new).orElseGet(() -> new Random(System.currentTimeMillis()));

        // For each measurement to be generated, pick a measurement type at random
        for (int i = 1; i < nbMeasurements + 1; i++) {
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
                        // If bias towards HV nodes activated, pick 2 branches at random and keep the one connected to the highest voltage level
                        if (withBias) {
                            tmpVNomi = Math.max(randomBranch.getTerminal1().getVoltageLevel().getNominalV(), randomBranch.getTerminal2().getVoltageLevel().getNominalV());
                            randomBranchSecondPick = listOfBranchesPfSide1.remove(random.nextInt(listOfBranchesPfSide1.size()));
                            if (tmpVNomi < Math.max(randomBranchSecondPick.getTerminal1().getVoltageLevel().getNominalV(), randomBranchSecondPick.getTerminal2().getVoltageLevel().getNominalV())) {
                                // Put back the first pick in the list of branches and keep the second pick
                                listOfBranchesPfSide1.add(randomBranch);
                                randomBranch = randomBranchSecondPick;
                            } // Else, put back the second pick and keep the first pick
                            else {
                                listOfBranchesPfSide1.add(randomBranchSecondPick);
                            }
                        }
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getBus().getId());
                        randomMeasure.put("Value", String.valueOf(randomBranch.getTerminal1().getP()));
                    } else {
                        randomMeasure = null;
                    }
                } else { // Side 2
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesPfSide2.isEmpty()) {
                        randomBranch = listOfBranchesPfSide2.remove(random.nextInt(listOfBranchesPfSide2.size()));
                        // If bias towards HV nodes activated, pick 2 branches at random and keep the one connected to the highest voltage level
                        if (withBias) {
                            tmpVNomi = Math.max(randomBranch.getTerminal1().getVoltageLevel().getNominalV(), randomBranch.getTerminal2().getVoltageLevel().getNominalV());
                            randomBranchSecondPick = listOfBranchesPfSide2.remove(random.nextInt(listOfBranchesPfSide2.size()));
                            if (tmpVNomi < Math.max(randomBranchSecondPick.getTerminal1().getVoltageLevel().getNominalV(), randomBranchSecondPick.getTerminal2().getVoltageLevel().getNominalV())) {
                                // Put back the first pick in the list of branches and keep the second pick
                                listOfBranchesPfSide2.add(randomBranch);
                                randomBranch = randomBranchSecondPick;
                            } // Else, put back the second pick and keep the first pick
                            else {
                                listOfBranchesPfSide2.add(randomBranchSecondPick);
                            }
                        }
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
                        // If bias towards HV nodes activated, pick 2 branches at random and keep the one connected to the highest voltage level
                        if (withBias) {
                            tmpVNomi = Math.max(randomBranch.getTerminal1().getVoltageLevel().getNominalV(), randomBranch.getTerminal2().getVoltageLevel().getNominalV());
                            randomBranchSecondPick = listOfBranchesQfSide1.remove(random.nextInt(listOfBranchesQfSide1.size()));
                            if (tmpVNomi < Math.max(randomBranchSecondPick.getTerminal1().getVoltageLevel().getNominalV(), randomBranchSecondPick.getTerminal2().getVoltageLevel().getNominalV())) {
                                // Put back the first pick in the list of branches and keep the second pick
                                listOfBranchesQfSide1.add(randomBranch);
                                randomBranch = randomBranchSecondPick;
                            } // Else, put back the second pick and keep the first pick
                            else {
                                listOfBranchesQfSide1.add(randomBranchSecondPick);
                            }
                        }
                        // Get location IDs and the corresponding value (in SI), as given by the Load Flow solution
                        randomMeasure.put("BranchID", randomBranch.getId());
                        randomMeasure.put("FirstBusID", randomBranch.getTerminal1().getBusView().getBus().getId());
                        randomMeasure.put("SecondBusID", randomBranch.getTerminal2().getBusView().getBus().getId());
                        randomMeasure.put("Value", String.valueOf(randomBranch.getTerminal1().getQ()));
                    } else {
                        randomMeasure = null;
                    }
                } else { // Side 2
                    // Pick a branch at random and remove it from the list of potential choices for next measurement (if some branches are still to be picked)
                    if (!listOfBranchesQfSide2.isEmpty()) {
                        randomBranch = listOfBranchesQfSide2.remove(random.nextInt(listOfBranchesQfSide2.size()));
                        // If bias towards HV nodes activated, pick 2 branches at random and keep the one connected to the highest voltage level
                        if (withBias) {
                            tmpVNomi = Math.max(randomBranch.getTerminal1().getVoltageLevel().getNominalV(), randomBranch.getTerminal2().getVoltageLevel().getNominalV());
                            randomBranchSecondPick = listOfBranchesQfSide2.remove(random.nextInt(listOfBranchesQfSide2.size()));
                            if (tmpVNomi < Math.max(randomBranchSecondPick.getTerminal1().getVoltageLevel().getNominalV(), randomBranchSecondPick.getTerminal2().getVoltageLevel().getNominalV())) {
                                // Put back the first pick in the list of branches and keep the second pick
                                listOfBranchesQfSide2.add(randomBranch);
                                randomBranch = randomBranchSecondPick;
                            } // Else, put back the second pick and keep the first pick
                            else {
                                listOfBranchesQfSide2.add(randomBranchSecondPick);
                            }
                        }
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
            }

            else if (randomType == 4) {
                // Add a "P" measure
                randomMeasure.put("Type", "P");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesP.isEmpty()) {
                    randomBus = listOfBusesP.remove(random.nextInt(listOfBusesP.size()));
                    // If bias towards HV nodes activated, pick 2 buses at random and keep the one with the highest voltage level
                    if (withBias) {
                        tmpVNomi = randomBus.getVoltageLevel().getNominalV();
                        randomBusSecondPick = listOfBusesP.remove(random.nextInt(listOfBusesP.size()));
                        if (tmpVNomi < randomBusSecondPick.getVoltageLevel().getNominalV()) {
                            // Put back the first pick in the list of buses and keep the second pick
                            listOfBusesP.add(randomBus);
                            randomBus = randomBusSecondPick;
                        } // Else, put back the second pick in the list and keep the first pick
                        else {
                            listOfBusesP.add(randomBusSecondPick);
                        }
                    }
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
                    // If bias towards HV nodes activated, pick 2 buses at random and keep the one with the highest voltage level
                    if (withBias) {
                        tmpVNomi = randomBus.getVoltageLevel().getNominalV();
                        randomBusSecondPick = listOfBusesQ.remove(random.nextInt(listOfBusesQ.size()));
                        if (tmpVNomi < randomBusSecondPick.getVoltageLevel().getNominalV()) {
                            // Put back the first pick in the list of buses and keep the second pick
                            listOfBusesQ.add(randomBus);
                            randomBus = randomBusSecondPick;
                        } // Else, put back the second pick in the list and keep the first pick
                        else {
                            listOfBusesQ.add(randomBusSecondPick);
                        }
                    }
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
            }

            else if (randomType == 6) {
                // Add a "V" measure
                randomMeasure.put("Type", "V");
                // Pick a bus at random and remove it from the list (if some buses are still to be picked)
                if (!listOfBusesV.isEmpty()) {
                    randomBus = listOfBusesV.remove(random.nextInt(listOfBusesV.size()));
                    // If bias towards HV nodes activated, pick 2 buses at random and keep the one with the highest voltage level
                    if (withBias) {
                        tmpVNomi = randomBus.getVoltageLevel().getNominalV();
                        randomBusSecondPick = listOfBusesV.remove(random.nextInt(listOfBusesV.size()));
                        if (tmpVNomi < randomBusSecondPick.getVoltageLevel().getNominalV()) {
                            // Put back the first pick in the list of buses and keep the second pick
                            listOfBusesV.add(randomBus);
                            randomBus = randomBusSecondPick;
                        } // Else, put back the second pick in the list and keep the first pick
                        else {
                            listOfBusesV.add(randomBusSecondPick);
                        }
                    }
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

            // Try to add the measure if not null (note that the measure could be redundant)
            if (randomMeasure != null) {
                try {
                    knowledge.addMeasure(i, randomMeasure, network);
                } catch (IllegalArgumentException illegalArgumentException) {
                    System.out.printf("%nMeasurement n° %d could not be added. Reason :%n", i);
                    throw illegalArgumentException;
                }
            } else { // If measure is null, it is because the list to choose measurement location has become empty (ex : all the buses are already assigned a voltage measure)
                // In this case, decrease i to get the proper quantity of measurements at the end of the process
                i = i - 1;
            }
        }
    }


}



