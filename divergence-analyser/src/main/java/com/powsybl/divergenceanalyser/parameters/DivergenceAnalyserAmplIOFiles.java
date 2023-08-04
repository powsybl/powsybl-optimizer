package com.powsybl.divergenceanalyser.parameters;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.divergenceanalyser.parameters.input.SolvingOptions;
import com.powsybl.divergenceanalyser.parameters.input.PenalizationControl;
import com.powsybl.divergenceanalyser.parameters.output.BranchPenalizationOutput;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.ampl.executor.AmplParameters;
import com.powsybl.divergenceanalyser.parameters.output.BusPenalizationOutput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DivergenceAnalyserAmplIOFiles implements AmplParameters {

    // Input files
    PenalizationControl penalizationControl;
    SolvingOptions solvingOptions;

    // Output files
    BusPenalizationOutput busPenalizationOutput;
    BranchPenalizationOutput branchPenalizationOutput;

    boolean debug;

    public DivergenceAnalyserAmplIOFiles(DivergenceAnalyserParameters params, boolean debug) {
        // Input file for activation of variables in ampl minlp
        this.penalizationControl = new PenalizationControl(params.getPenalization());
        this.solvingOptions = new SolvingOptions(params.getSolvingOptions());

        // Output file for network modifications
        this.busPenalizationOutput = new BusPenalizationOutput();
        this.branchPenalizationOutput = new BranchPenalizationOutput();

        this.debug = debug;
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(penalizationControl, solvingOptions);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters(boolean hasConverged) {
        if (hasConverged) {
            List<AmplOutputFile> list = new ArrayList<>();
            list.add(busPenalizationOutput);
            list.add(branchPenalizationOutput);
            return list;
        } else {
            return List.of();
        }
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    public BusPenalizationOutput getBusPenalizationOutput() {
        return busPenalizationOutput;
    }

    public BranchPenalizationOutput getBranchModificationsOutput() {
        return branchPenalizationOutput;
    }
}
