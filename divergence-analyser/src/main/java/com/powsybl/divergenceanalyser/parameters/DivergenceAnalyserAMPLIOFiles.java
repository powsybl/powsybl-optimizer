package com.powsybl.divergenceanalyser.parameters;

import com.powsybl.divergenceanalyser.parameters.input.DivergenceAnalyserParameters;
import com.powsybl.divergenceanalyser.parameters.input.SolvingOptions;
import com.powsybl.divergenceanalyser.parameters.input.PenalizationControl;
import com.powsybl.divergenceanalyser.parameters.output.BranchPenalisationOutput;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.ampl.executor.AmplParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DivergenceAnalyserAMPLIOFiles implements AmplParameters {

    // Input files
    PenalizationControl penalizationControl;
    SolvingOptions solvingOptions;

    // Output files
    BranchPenalisationOutput branchPenalisationOutput;

    boolean debug;

    public DivergenceAnalyserAMPLIOFiles(DivergenceAnalyserParameters params, boolean debug){
        // Input file for activation of variables in ampl minlp
        this.penalizationControl = new PenalizationControl(params.getPenalization());
        this.solvingOptions = new SolvingOptions(params.getSolvingOptions());

        // Output file for network modifications
        this.branchPenalisationOutput = new BranchPenalisationOutput();

        this.debug = debug;
    }

    @Override
    public Collection<AmplInputFile> getInputParameters() {
        return List.of(penalizationControl, solvingOptions);
    }

    @Override
    public Collection<AmplOutputFile> getOutputParameters(boolean hasConverged) {
        if (hasConverged){
            List<AmplOutputFile> list = new ArrayList<>();
            list.add(branchPenalisationOutput);
            return list;
        } else {
            return List.of();
        }
    }

    @Override
    public boolean isDebug() {
        return debug;
    }

    public BranchPenalisationOutput getBranchModificationsOutput() {
        return branchPenalisationOutput;
    }
}
