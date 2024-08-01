/**
 * Copyright (c) 2022,2023 RTE (http://www.rte-france.com), Coreso and TSCNet Services
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.stateestimator;

import com.powsybl.ampl.converter.AmplNetworkUpdaterFactory;
import com.powsybl.ampl.converter.AmplReadableElement;
import com.powsybl.ampl.converter.DefaultAmplNetworkUpdater;
import com.powsybl.ampl.converter.OutputFileFormat;
import com.powsybl.ampl.executor.AmplModel;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.ampl.converter.AmplConstants.DEFAULT_VARIANT_INDEX;

/**
 * @author Lucas RIOU <lucas.riou@artelys.com>
 */
public class StateEstimatorModel implements AmplModel {

    public static final String OUTPUT_FILE_PREFIX = "se";

    public static final OutputFileFormat OUTPUT_FILE_FORMAT = new OutputFileFormat() {

            @Override
            public String getTokenSeparator() {
                return ";";
            }

            @Override
            public String getFileExtension() {
                return "csv";
            }

            @Override
            public Charset getFileEncoding() {
                return StandardCharsets.UTF_8;
            }
        };

    public static StateEstimatorModel buildModel(String estimatorType) {
        String resourcesFolder = "stateestimatorWLS";
        if (estimatorType.equals("WLAV")) {
            resourcesFolder = "stateestimatorWLAV";
        }
        if (estimatorType.equals("FILTER")) {
            resourcesFolder = "stateestimatorFILTER";
        }

        return new StateEstimatorModel(OUTPUT_FILE_PREFIX, resourcesFolder,
                    List.of("state_estimator.run"),
                    List.of("data_network.dat",
                            "data_importer.mod",
                            "state_estimator.mod",
                            "MINLP_formulation.mod",
                            "MINLP_variables.mod",
                            "results_exit.run",
                            "results_exporter.run"));
    }

    private static final String NETWORK_DATA_PREFIX = "ampl";
    private static final String INDICATOR_STATUS_KEY = "final_status";
    private static final String INDICATOR_STATUS_SUCCESS = "OK";

    /**
     * A list containing the name of the files and their path in the resources
     */
    private final List<Pair<String, String>> modelNameAndPath;
    private final List<String> runFiles;
    private final String outputFilePrefix;

        /**
         * Create a ampl Model to be executed
         *
         * @param outputFilePrefix The prefix used for the output files, they must be
         *                         compatible with AmplNetworkReader
         * @param resourcesFolder  The resources folder name containing all the files
         * @param runFiles         The names of the file that must be run in AMPL (.run
         *                         files). The order of the list gives the order of
         *                         execution.
         * @param resourcesFiles   All others files needed by the model (.dat and .mod
         *                         files)
         */
    StateEstimatorModel(String outputFilePrefix, String resourcesFolder, List<String> runFiles, List<String> resourcesFiles) {
        this.runFiles = runFiles;
        this.outputFilePrefix = outputFilePrefix;
        List<String> modelFiles = new ArrayList<>();
        modelFiles.addAll(resourcesFiles);
        modelFiles.addAll(runFiles);
        this.modelNameAndPath = modelFiles.stream()
                .map(file -> Pair.of(file, resourcesFolder + "/" + file))
                .collect(Collectors.toList());
    }

    /**
     * @return each pair contains the name, and the InputStream of the file
     */
    @Override
    public List<Pair<String, InputStream>> getModelAsStream() {

        return modelNameAndPath.stream()
                .map(nameAndPath -> {
                    InputStream resourceAsStream = this.getClass()
                            .getClassLoader()
                            .getResourceAsStream(nameAndPath.getRight());
                    if (resourceAsStream == null) {
                        throw new MissingResourceException(
                                "Missing state estimator ampl files : " + nameAndPath.getLeft() + " at " + nameAndPath.getRight(),
                                this.getClass().getName(), nameAndPath.getLeft());
                    }
                    return Pair.of(nameAndPath.getLeft(), resourceAsStream);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAmplRunFiles() {
        return this.runFiles;
    }

    @Override
    public String getOutputFilePrefix() {
        return this.outputFilePrefix;
    }

    @Override
    public AmplNetworkUpdaterFactory getNetworkUpdaterFactory() {
        return (mapper, network) -> new DefaultAmplNetworkUpdater(mapper);
    }

    @Override
    public int getVariant() {
        return DEFAULT_VARIANT_INDEX;
    }

    @Override
    public String getNetworkDataPrefix() {
        return NETWORK_DATA_PREFIX;
    }

    @Override
    public Collection<AmplReadableElement> getAmplReadableElement() {
        return List.of();
    }

    @Override
    public boolean checkModelConvergence(Map<String, String> map) {
        return map.getOrDefault(INDICATOR_STATUS_KEY, "").equals(INDICATOR_STATUS_SUCCESS);
    }

    @Override
    public OutputFileFormat getOutputFormat() {
        return OUTPUT_FILE_FORMAT;
    }

}
