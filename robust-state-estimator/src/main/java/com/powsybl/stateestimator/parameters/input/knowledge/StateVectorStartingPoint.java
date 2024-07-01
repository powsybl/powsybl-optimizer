/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator.parameters.input.knowledge;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;
import com.powsybl.iidm.network.Network;
import org.jgrapht.alg.util.Pair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateVectorStartingPoint implements AmplInputFile {

    Map<Integer, ArrayList<String>> stateVectorStartingPoint;


    public StateVectorStartingPoint(Map<Integer, ArrayList<String>> stateVectorStartingPoint) {
        this.stateVectorStartingPoint = stateVectorStartingPoint;
    }


    @Override
    public String getFileName() {
        return "ampl_starting_point.txt";
    }

    @Override
    public void write(BufferedWriter writer, StringToIntMapper<AmplSubset> stringToIntMapper) throws IOException {
        // Expected format : "num" "bus_id" "V (pu)" "theta (rad)"
        writer.write("#\"num\" \"bus_id\" \"V (pu)\" \"theta (rad)\"");
        writer.newLine();
        // For each bus
        for (var busStartingPoint : stateVectorStartingPoint.entrySet()) {
            StringBuilder line = new StringBuilder(busStartingPoint.getKey().toString());
            for (String var : busStartingPoint.getValue()) {
                line.append(" ").append(var);
            }
            writer.write(line.toString());
            writer.newLine();
        }
        //add new line at the end of the file !
        writer.newLine();
        writer.flush();
    }

}
