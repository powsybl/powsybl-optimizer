package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.IntersectionStatus;
import com.powsybl.openreac.network.ParallelTwoWindingsTransformersDetector.ParallelGroup;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes parallel transformer groups whose rho intersection is LARGE — those
 * that can be tied under a shared decision variable in the optimization.
 *
 * <p>POINT and EMPTY groups are written by
 * {@link FixedRatioTwoWindingsTransformers} instead, since their transformers
 * must be fixed rather than tied.
 *
 * <p>Format:
 * <pre>
 * #num_group num_branch
 * 1 142
 * 1 287
 * 2 95
 * 2 96
 * </pre>
 *
 * @author Oscar Lamolet {@literal <lamoletoscar at proton.me>}
 */
public class ParallelTwoWindingsTransformersGroups implements AmplInputFile {

    public static final String PARAM_PARALLEL_TRANSFORMERS_FILE_NAME = "param_parallel_transformers.txt";

    private final List<ParallelGroup> largeGroups;

    public ParallelTwoWindingsTransformersGroups(List<ParallelGroup> allGroups) {
        this.largeGroups = new ArrayList<>();
        for (ParallelGroup group : allGroups) {
            if (group.status() == IntersectionStatus.LARGE) {
                largeGroups.add(group);
            }
        }
    }

    @Override
    public String getFileName() {
        return PARAM_PARALLEL_TRANSFORMERS_FILE_NAME;
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        writer.write("#num_group num_branch\n");
        int groupIndex = 1;
        for (ParallelGroup group : largeGroups) {
            for (String transformerId : group.transformerIds().stream().sorted().toList()) {
                int amplBranchId = stringToIntMapper.getInt(AmplSubset.BRANCH, transformerId);
                writer.write(groupIndex + " " + amplBranchId);
                writer.newLine();
            }
            groupIndex++;
        }
        writer.newLine();
        writer.flush();
    }
}
