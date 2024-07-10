/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.commons.datasource.FileDataSource;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.ieeecdf.converter.IeeeCdfNetworkFactory;
import com.powsybl.iidm.network.*;
import com.powsybl.matpower.converter.MatpowerImporter;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;


/**
 * @author Pierre ARVY <pierre.arvy@artelys.com>
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class MatpowerToIIDMConverter {

    @Test
    void convert() throws IOException {

        // Load network from .mat file
        Properties properties = new Properties();
        // We want base voltages to be taken into account
        properties.put("matpower.import.ignore-base-voltage", false);
        Network network = new MatpowerImporter().importData(
                new FileDataSource(Path.of("D:", "Projet", "Réseaux_tests", "Pegase2869"), "pglib_opf_case2869_pegase"),
                NetworkFactory.findDefault(), properties);

        network.write("XIIDM", new Properties(), Path.of("D:", "Projet", "Réseaux_tests", "Pegase2869", "pglib_opf_case2869_pegase"));
    }
}
