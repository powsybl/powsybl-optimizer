/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.divergenceanalyser.parameters;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.divergenceanalyser.parameters.input.SolvingOptions;
import com.powsybl.divergenceanalyser.parameters.input.PenalizationControl;
import com.powsybl.divergenceanalyser.parameters.output.BranchPenalizationOutput;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.ampl.executor.AmplParameters;
import com.powsybl.divergenceanalyser.parameters.output.BusPenalizationOutput;
import com.powsybl.divergenceanalyser.parameters.output.NetworkIndicatorsOutput;
import com.powsybl.divergenceanalyser.parameters.output.PenalizationIndicatorsOutput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 */
public class DivergenceAnalyserAmplIOFiles implements AmplParameters {

    // Input files
    PenalizationControl penalizationControl;
    SolvingOptions solvingOptions;

    // Output files
    BusPenalizationOutput busPenalizationOutput;
    BranchPenalizationOutput branchPenalizationOutput;
    NetworkIndicatorsOutput networkIndicatorsOutput;
    PenalizationIndicatorsOutput penalizationIndicatorsOutput;

    boolean debug;

    public DivergenceAnalyserAmplIOFiles(DivergenceAnalyserParameters params, boolean debug) {
        // Input file for activation of variables in ampl minlp
        this.penalizationControl = new PenalizationControl(params.getPenalizationOptions());
        this.solvingOptions = new SolvingOptions(params.getSolvingOptions());

        // Output file (indicators, network modifications...)
        this.busPenalizationOutput = new BusPenalizationOutput();
        this.branchPenalizationOutput = new BranchPenalizationOutput();
        this.networkIndicatorsOutput = new NetworkIndicatorsOutput();
        this.penalizationIndicatorsOutput = new PenalizationIndicatorsOutput();

        this.debug = debug;
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(penalizationControl, solvingOptions);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters(boolean hasConverged) {
        List<AmplOutputFile> list = new ArrayList<>();
        list.add(networkIndicatorsOutput);
        if (hasConverged) {
            list.add(busPenalizationOutput);
            list.add(branchPenalizationOutput);
            list.add(penalizationIndicatorsOutput);
        }
        return list;
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    // Getters for output files
    public BusPenalizationOutput getBusPenalizationOutput() {
        return busPenalizationOutput;
    }

    public BranchPenalizationOutput getBranchModificationsOutput() {
        return branchPenalizationOutput;
    }

    public NetworkIndicatorsOutput getNetworkIndicatorsOutput() {
        return networkIndicatorsOutput;
    }

    public PenalizationIndicatorsOutput getPenalizationIndicatorsOutput() {
        return penalizationIndicatorsOutput;
    }
}
