/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.converter.AmplUtil;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Geoffroy Jamgotchian {@literal <geoffroy.jamgotchian at rte-france.com>}
 */
class VoltageLevelLimitsOverrideInputTest {

    @Test
    void testValidRelativeVoltageOverride() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        // verify relative override can be applied on both defined low/high limits
        VoltageLevel vlGen = network.getVoltageLevel("VLGEN");
        vlGen.setLowVoltageLimit(20)
                .setHighVoltageLimit(26);

        List<VoltageLimitOverride> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new VoltageLimitOverride("VLGEN", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, -1));
        voltageLimitsOverride.add(new VoltageLimitOverride("VLGEN", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, 2));

        VoltageLevelLimitsOverrideInput input = new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network, ReportNode.NO_OP);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);
        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            String ref = String.join(System.lineSeparator(), "#num minV (pu) maxV (pu) id",
                    "1 0.7916666666666667 1.1666666666666665 \"VLGEN\"") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void testValidAbsoluteVoltageOverride() throws IOException {
        Network network = EurostagTutorialExample1Factory.create();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        // change high voltage limits to undefined values
        VoltageLevel vlgen = network.getVoltageLevel("VLGEN");
        vlgen.setLowVoltageLimit(Double.NaN)
                .setHighVoltageLimit(Double.NaN);

        List<VoltageLimitOverride> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new VoltageLimitOverride("VLGEN", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 20));
        voltageLimitsOverride.add(new VoltageLimitOverride("VLGEN", VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, 26));

        VoltageLevelLimitsOverrideInput input = new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network, ReportNode.NO_OP);
        StringToIntMapper<AmplSubset> mapper = AmplUtil.createMapper(network);

        try (Writer w = new StringWriter();
             BufferedWriter writer = new BufferedWriter(w)) {
            input.write(writer, mapper);
            String data = w.toString();
            String ref = String.join(System.lineSeparator(), "#num minV (pu) maxV (pu) id",
                    "1 0.8333333333333334 1.0833333333333333 \"VLGEN\"") + System.lineSeparator() + System.lineSeparator();
            assertEquals(ref, data);
        }
    }

    @Test
    void testZeroVoltageLimit() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        OpenReacParameters params = new OpenReacParameters();

        // if one low voltage limit is < 0 and there is no voltage limit override, invalid OpenReacParameters
        vl.setLowVoltageLimit(0);
        assertDoesNotThrow(() -> params.checkIntegrity(network));
    }

    @Test
    void testUndefinedVoltageLimitsWithoutOverride() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        OpenReacParameters params = new OpenReacParameters();

        // if one low voltage limit is undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(Double.NaN);
        vl.setHighVoltageLimit(480);
        PowsyblException e = assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));
        assertEquals("At least one voltage level has an undefined or incorrect voltage limit.", e.getMessage());

        // if one high voltage limit is undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(480);
        vl.setHighVoltageLimit(Double.NaN);
        PowsyblException e2 = assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));
        assertEquals("At least one voltage level has an undefined or incorrect voltage limit.", e2.getMessage());

        // if both low/high voltage limit are undefined, invalid OpenReacParameters
        vl.setLowVoltageLimit(Double.NaN);
        vl.setHighVoltageLimit(Double.NaN);
        PowsyblException e3 = assertThrows(PowsyblException.class, () -> params.checkIntegrity(network));
        assertEquals("At least one voltage level has an undefined or incorrect voltage limit.", e3.getMessage());
    }

    @Test
    void testVoltageOverrideWithNegativeVoltageLimit() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        vl.setHighVoltageLimit(480);
        vl.setLowVoltageLimit(400);

        // if low relative voltage override leads to negative voltage limit, throws exception
        OpenReacParameters params = new OpenReacParameters();
        List<VoltageLimitOverride> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, -410));
        params.addSpecificVoltageLimits(voltageLimitsOverride);
        InvalidParametersException e = assertThrows(InvalidParametersException.class, () -> params.checkIntegrity(network));
        assertEquals("At least one voltage limit override is inconsistent.", e.getMessage());

        params.getSpecificVoltageLimits().clear();
        voltageLimitsOverride.clear();
        voltageLimitsOverride.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, -400));
        params.addSpecificVoltageLimits(voltageLimitsOverride);
        assertDoesNotThrow(() -> params.checkIntegrity(network)); // zero value

        // if high relative voltage override leads to negative voltage limit, throws exception
        OpenReacParameters params2 = new OpenReacParameters();
        List<VoltageLimitOverride> voltageLimitsOverride2 = new ArrayList<>();
        voltageLimitsOverride2.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, -490));
        params2.addSpecificVoltageLimits(voltageLimitsOverride2);
        InvalidParametersException e2 = assertThrows(InvalidParametersException.class, () -> params2.checkIntegrity(network));
        assertEquals("At least one voltage limit override is inconsistent.", e2.getMessage());

        params2.getSpecificVoltageLimits().clear();
        voltageLimitsOverride2.clear();
        vl.setLowVoltageLimit(0.);
        voltageLimitsOverride2.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, -480));
        params2.addSpecificVoltageLimits(voltageLimitsOverride2);
        assertDoesNotThrow(() -> params2.checkIntegrity(network)); // zero value
    }

    @Test
    void testVoltageOverrideWithLowLimitGreaterHighLimit() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        vl.setHighVoltageLimit(480);
        vl.setLowVoltageLimit(400);

        List<VoltageLimitOverride> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, true, -90));

        List<VoltageLimitOverride> voltageLimitsOverride2 = new ArrayList<>();
        voltageLimitsOverride2.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, 90));

        List<VoltageLimitOverride> voltageLimitsOverride3 = new ArrayList<>();
        voltageLimitsOverride3.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 395));
        voltageLimitsOverride3.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, 390));

        // if after relative override, low limit > high limit, wrong parameters
        InvalidParametersException e = assertThrows(InvalidParametersException.class, () -> new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network, ReportNode.NO_OP));
        assertEquals("Override on voltage level " + vl.getId() + " leads to low voltage limit >= high voltage limit.", e.getMessage());
        InvalidParametersException e2 = assertThrows(InvalidParametersException.class, () -> new VoltageLevelLimitsOverrideInput(voltageLimitsOverride2, network, ReportNode.NO_OP));
        assertEquals("Override on voltage level " + vl.getId() + " leads to low voltage limit >= high voltage limit.", e2.getMessage());
        InvalidParametersException e3 = assertThrows(InvalidParametersException.class, () -> new VoltageLevelLimitsOverrideInput(voltageLimitsOverride3, network, ReportNode.NO_OP));
        assertEquals("Override on voltage level " + vl.getId() + " leads to low voltage limit >= high voltage limit.", e3.getMessage());
    }

    private static boolean checkReportWithKey(String key, ReportNode reportNode) {
        if (reportNode.getMessageKey() != null && reportNode.getMessageKey().equals(key)) {
            return true;
        }
        boolean found = false;
        Iterator<ReportNode> reportersIterator = reportNode.getChildren().iterator();
        while (!found && reportersIterator.hasNext()) {
            found = checkReportWithKey(key, reportersIterator.next());
        }
        return found;
    }

    @Test
    void testVoltageOverrideWithLowLimitOutOfNominalVoltageRange() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        vl.setHighVoltageLimit(400);
        vl.setLowVoltageLimit(350);
        vl.setNominalV(380);

        List<VoltageLimitOverride> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 390.));

        // if after override, low limit is in the nominal voltage range, no specific report has been created
        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("openReac", "openReac").build();
        new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network, reportNode);
        assertFalse(checkReportWithKey("nbVoltageLevelsWithLimitsOutOfNominalVRange", reportNode));

        // if after override, low limit is out of nominal voltage range, a specific report has been created
        reportNode = ReportNode.newRootReportNode().withMessageTemplate("openReac", "openReac").build();
        voltageLimitsOverride.clear();
        voltageLimitsOverride.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, false, 317.));
        new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network, reportNode);
        assertTrue(checkReportWithKey("nbVoltageLevelsWithLimitsOutOfNominalVRange", reportNode));
    }

    @Test
    void testVoltageOverrideWithHighLimitOutOfNominalVoltageRange() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        VoltageLevel vl = network.getVoltageLevels().iterator().next();
        vl.setHighVoltageLimit(400);
        vl.setLowVoltageLimit(350);
        vl.setNominalV(380);

        List<VoltageLimitOverride> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, 420.));

        // if after override, high limit is in the nominal voltage range, no specific report has been created
        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("openReac", "openReac").build();
        new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network, reportNode);
        assertFalse(checkReportWithKey("nbVoltageLevelsWithLimitsOutOfNominalVRange", reportNode));

        // if after override, high limit is out of nominal voltage range, a specific report has been created
        reportNode = ReportNode.newRootReportNode().withMessageTemplate("openReac", "openReac").build();
        voltageLimitsOverride.clear();
        voltageLimitsOverride.add(new VoltageLimitOverride(vl.getId(), VoltageLimitOverride.VoltageLimitType.HIGH_VOLTAGE_LIMIT, false, 445.));
        new VoltageLevelLimitsOverrideInput(voltageLimitsOverride, network, reportNode);
        assertTrue(checkReportWithKey("nbVoltageLevelsWithLimitsOutOfNominalVRange", reportNode));
    }

    @Test
    void testVoltageOverrideOnInvalidVoltageLevel() {
        Network network = IeeeCdfNetworkFactory.create57();
        setDefaultVoltageLimits(network); // set default voltage limits to every voltage levels of the network

        // if voltage level (on which is applied override) is not in the network, throws exception
        OpenReacParameters params3 = new OpenReacParameters();
        List<VoltageLimitOverride> voltageLimitsOverride = new ArrayList<>();
        voltageLimitsOverride.add(new VoltageLimitOverride("UNKNOWN_ID", VoltageLimitOverride.VoltageLimitType.LOW_VOLTAGE_LIMIT, true, 1));
        params3.addSpecificVoltageLimits(voltageLimitsOverride);
        InvalidParametersException e = assertThrows(InvalidParametersException.class, () -> params3.checkIntegrity(network));
        assertEquals("At least one voltage limit override is inconsistent.", e.getMessage());
    }

    void setDefaultVoltageLimits(Network network) {
        for (VoltageLevel vl : network.getVoltageLevels()) {
            if (vl.getLowVoltageLimit() <= 0 || Double.isNaN(vl.getLowVoltageLimit())) {
                vl.setLowVoltageLimit(0.8 * vl.getNominalV());
            }
            if (Double.isNaN(vl.getHighVoltageLimit())) {
                vl.setHighVoltageLimit(1.2 * vl.getNominalV());
            }
        }
    }
}
