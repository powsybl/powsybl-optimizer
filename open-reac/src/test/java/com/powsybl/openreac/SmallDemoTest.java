package com.powsybl.openreac;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowParameters;
import com.powsybl.openloadflow.network.SlackBusSelectionMode;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.OpenReacStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Pierre Arvy {@literal <pierre.arvy at artelys.com>}
 */
public class SmallDemoTest {

    @Test
    void testOpenReac() throws IOException {
        Network network = IeeeCdfNetworkFactory.create14();

        // add bounds to generators B1-G and B2-G, to avoid using default ones
        network.getGenerator("B1-G").setMinP(0).setMaxP(100);
        network.getGenerator("B2-G").setMinP(0).setMaxP(100);

        // add voltage limits to voltage levels of the network
        setDefaultVoltageLimits(network);

        // create open reac parameters and add transformers as variable
        OpenReacParameters parameters = new OpenReacParameters();
        parameters.addVariableShuntCompensators(List.of("B9-SH"));

//        AmplExportConfig config = new AmplExportConfig(AmplExportConfig.ExportScope.ALL, false, AmplExportConfig.ExportActionType.CURATIVE, false, true);
        OpenReacResult openReacResult = OpenReacRunner.run(
                // network on which optimization is conducted
                network,
                // variant of the network on which optimization is conducted
                network.getVariantManager().getWorkingVariantId(),
                // opf parameters
                parameters,
                // debug mode to export optimization directory
                new OpenReacConfig(true),
                // computation manager
                new LocalComputationManager(),
                // no report node
                ReportNode.NO_OP,
                // Ampl export configuration
                null);

        assertEquals(OpenReacStatus.OK, openReacResult.getStatus());

        // update the network with computed voltage plan and profile
        openReacResult.setUpdateNetworkWithVoltages(true);
        openReacResult.applyAllModifications(network);

        // parameters specific to open reac run
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setVoltageInitMode(LoadFlowParameters.VoltageInitMode.PREVIOUS_VALUES)
                .setDistributedSlack(false)
                .setUseReactiveLimits(true)
                .setReadSlackBus(false);
        OpenLoadFlowParameters.create(loadFlowParameters)
                .setLowImpedanceBranchMode(OpenLoadFlowParameters.LowImpedanceBranchMode.REPLACE_BY_MIN_IMPEDANCE_LINE)
                .setSlackBusSelectionMode(SlackBusSelectionMode.MOST_MESHED);

        // run ac load flow to check opf results
        LoadFlowResult result = LoadFlow.run(network, loadFlowParameters);
        assertTrue(result.isFullyConverged());
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
