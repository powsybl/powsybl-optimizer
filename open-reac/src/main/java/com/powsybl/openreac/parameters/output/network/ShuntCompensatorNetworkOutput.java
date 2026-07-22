/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output.network;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.modification.ShuntCompensatorModification;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ShuntCompensator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public class ShuntCompensatorNetworkOutput extends AbstractNetworkOutput<ShuntCompensatorModification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShuntCompensatorNetworkOutput.class);
    private static final String ELEMENT = "shunts";
    public static final int EXPECTED_COLS = 6;
    private static final int ID_COLUMN_INDEX = 1;
    private static final int B_COLUMN_INDEX = 3;
    private static final int BUS_COLUMN_INDEX = 2;
    private final List<ShuntWithDeltaDiscreteOptimalOverThreshold> shuntWithDeltaDiscreteOptimalOverThresholds = new ArrayList<>();
    private final double shuntCompensatorActivationAlertThreshold;
    /**
     * Reactive power deviation, in MVar at nominal voltage, introduced by discretizing the
     * continuous susceptances returned by the optimizer onto the sections available on each
     * shunt, summed over every shunt read. NaN as soon as one shunt carries an invalid value.
     */
    public double totalReactiveDeviation = 0;
    /**
     * Reactive power deviation, in MVar at nominal voltage, introduced by the discretization,
     * per shunt compensator.
     */
    public Map<ShuntCompensator, Double> reactiveDeviationByShunt = new HashMap<>();

    public record ShuntWithDeltaDiscreteOptimalOverThreshold(String id, int maximumSectionCount, double discretizedReactiveValue, double optimalReactiveValue) { }

    public ShuntCompensatorNetworkOutput(Network network, double shuntCompensatorActivationAlertThreshold) {
        super(network);
        this.shuntCompensatorActivationAlertThreshold = shuntCompensatorActivationAlertThreshold;
    }

    @Override
    public String getElement() {
        return ELEMENT;
    }

    @Override
    public int getExpectedColumns() {
        return EXPECTED_COLS;
    }

    @Override
    protected void readLine(String[] tokens, StringToIntMapper<AmplSubset> stringToIntMapper) {
        String id = stringToIntMapper.getId(AmplSubset.SHUNT, Integer.parseInt(tokens[ID_COLUMN_INDEX]));
        ShuntCompensator shuntCompensator = network.getShuntCompensator(id);
        if (!Objects.isNull(shuntCompensator)) {
            double b = readDouble(tokens[B_COLUMN_INDEX]) * AmplConstants.SB / Math.pow(shuntCompensator.getTerminal().getVoltageLevel().getNominalV(), 2);
            String busId = stringToIntMapper.getId(AmplSubset.BUS, Integer.parseInt(tokens[BUS_COLUMN_INDEX]));
            Boolean reconnect = null;
            if (busId != null && busId.equals(shuntCompensator.getTerminal().getBusView().getConnectableBus().getId())) {
                reconnect = true;
            }
            modifications.add(new ShuntCompensatorModification(id, reconnect, findSectionCount(shuntCompensator, b)));
        } else {
            LOGGER.warn("Shunt compensator with id {} not found in the network", id);
        }
    }

    /**
     * As b is continuous in output files, we have to find the shunt closest section that matches with b.
     */
    private int findSectionCount(ShuntCompensator sc, double b) {
        double minDistance = Math.abs(b - sc.getB());
        double distance;
        int sectionCount = sc.getSectionCount();
        for (int i = 0; i <= sc.getMaximumSectionCount(); i++) {
            distance = Math.abs(b - sc.getB(i));
            if (distance < minDistance) {
                minDistance = distance;
                sectionCount = i;
            }
        }
        double squaredNominalV = Math.pow(sc.getTerminal().getVoltageLevel().getNominalV(), 2);
        double optimalReactiveValue = Math.abs(b * squaredNominalV);
        double discretizedReactiveValue = Math.abs(sc.getB(sectionCount) * squaredNominalV);
        if (Math.abs(discretizedReactiveValue - optimalReactiveValue) > shuntCompensatorActivationAlertThreshold) {
            shuntWithDeltaDiscreteOptimalOverThresholds.add(new ShuntWithDeltaDiscreteOptimalOverThreshold(sc.getId(), sc.getMaximumSectionCount(), discretizedReactiveValue, optimalReactiveValue));
        }
        double reactiveDeviation = minDistance * squaredNominalV;
        totalReactiveDeviation += reactiveDeviation;
        reactiveDeviationByShunt.put(sc, reactiveDeviation);
        return sectionCount;
    }

    public List<ShuntWithDeltaDiscreteOptimalOverThreshold> getShuntsWithDeltaDiscreteOptimalOverThresholds() {
        return shuntWithDeltaDiscreteOptimalOverThresholds;
    }
}
