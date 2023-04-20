/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.tools.ConversionToolUtils;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.powsybl.iidm.network.tools.ConversionToolUtils.*;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
@AutoService(Tool.class)
public class OpenReacTool implements Tool {
    private static final String CASE_FILE = "case-file";
    private static final String SHUNTS_LIST = "variable-shunts-list";
    private static final String GENERATORS_LIST = "fixed-generators-list";
    private static final String TRANSFORMER_LIST = "variable-transformers-list";
    private static final String OPEN_REAC_PARAMS = "open-reac-params";
    public static final String EXECUTION_OPENREAC = "execution/openreac";

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "open-reac";
            }

            @Override
            public String getTheme() {
                return "Optimal Power Flow";
            }

            @Override
            public String getDescription() {
                return "An optimal powerflow on reactive components";
            }

            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder()
                        .longOpt(CASE_FILE)
                        .desc("the case path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(createImportParametersFileOption());
                options.addOption(createImportParameterOption());
                options.addOption(createExportParametersFileOption());
                options.addOption(createExportParameterOption());
                options.addOption(Option.builder()
                        .longOpt(OPEN_REAC_PARAMS)
                        .desc("the OpenReac configuation file")
                        .hasArg()
                        .argName("OPEN_REAC_PARAM_FILE")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine commandLine, ToolRunningContext context) throws Exception {
        // getting parameters
        Path inputCaseFile = context.getFileSystem().getPath(commandLine.getOptionValue(CASE_FILE));
        Files.createDirectories(context.getFileSystem().getPath(EXECUTION_OPENREAC));
        context.getOutputStream().println("Parsing properties...");
        Properties inputParams = readProperties(commandLine, ConversionToolUtils.OptionType.IMPORT, context);
        OpenReacParameters openReacParameters = createOpenReacParameters(commandLine, context);

        context.getOutputStream().println("Loading network '" + inputCaseFile + "'...");
        Network network = loadingNetwork(context, inputCaseFile, inputParams);

        itoolsOpenReac(context, network, openReacParameters);

        context.getOutputStream().println("Running a loadflow...");
        LoadFlowResult result = LoadFlow.run(network, context.getShortTimeExecutionComputationManager(), LoadFlowParameters.load());
        exportLoadFlowMetrics(context, result.getMetrics());
        context.getOutputStream().println("Loadflow done. Is ok ? " + result.isOk());

        itoolsOpenReac(context, network, openReacParameters);

        context.getOutputStream().println("All good. Exiting...");
    }

    public void exportLoadFlowMetrics(ToolRunningContext context, Map<String, String> metrics) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> metric : metrics.entrySet()) {
            builder.append(metric.getKey()).append(" ").append(metric.getValue()).append(System.lineSeparator());
        }
        Files.writeString(context.getFileSystem().getPath("./loadflow_metrics.txt"), builder.toString(), StandardOpenOption.CREATE);
    }

    public OpenReacParameters createOpenReacParameters(CommandLine line,
                                                       ToolRunningContext context) throws IOException {

        Properties inputParams = new Properties();
        String filename = line.getOptionValue(OPEN_REAC_PARAMS, null);
        if (filename != null) {
            try (InputStream inputStream = Files.newInputStream(context.getFileSystem().getPath(filename))) {
                if (filename.endsWith(".xml")) {
                    inputParams.loadFromXML(inputStream);
                } else {
                    inputParams.load(inputStream);
                }
            }
        }

        OpenReacParameters openReacParameters = new OpenReacParameters();
        String inputFileListSeparator = ";";
        if (inputParams.getProperty(SHUNTS_LIST) != null) {
            String[] shuntsList = inputParams.getProperty(SHUNTS_LIST).split(inputFileListSeparator);
            openReacParameters.addVariableShuntCompensators(List.of(shuntsList));
        }
        if (inputParams.getProperty(GENERATORS_LIST) != null) {
            String[] generatorsList = inputParams.getProperty(GENERATORS_LIST).split(inputFileListSeparator);
            openReacParameters.addConstantQGenerators(List.of(generatorsList));
        }
        if (inputParams.getProperty(TRANSFORMER_LIST) != null) {
            String[] transformerList = inputParams.getProperty(TRANSFORMER_LIST).split(inputFileListSeparator);
            openReacParameters.addVariableTwoWindingsTransformers(List.of(transformerList));
        }

        for (String key : inputParams.stringPropertyNames()) {
            if (!key.equals(SHUNTS_LIST) && !key.equals(GENERATORS_LIST) && !key.equals(TRANSFORMER_LIST)) {
                switch (key) {
                    case "obj_min_gen":
                        openReacParameters.setObjective(OpenReacOptimisationObjective.MIN_GENERATION);
                        break;
                    case "obj_target_ratio":
                        String ratioStr = inputParams.getProperty(key, null);
                        if (ratioStr == null) {
                            throw new InvalidParametersException("obj_target_ratio must have a value indicating the ratio to nominal voltage level to target.");
                        }
                        openReacParameters.setObjectiveDistance(Double.parseDouble(ratioStr));
                        openReacParameters.setObjective(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT);
                        break;
                    case "obj_provided_target_v":
                        openReacParameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
                        break;
                    default:
                        openReacParameters.addAlgorithmParam(key, inputParams.getProperty(key, ""));
                }
            }
        }
        return openReacParameters;
    }

    public void itoolsOpenReac(ToolRunningContext context, Network network,
                               OpenReacParameters openReacParameters) throws IOException {
        context.getOutputStream().println("Running OpenReac on the network...");
        OpenReacConfig config = new OpenReacConfig(true);
        OpenReacResult results;
        try (LocalComputationManager computationManager = new LocalComputationManager(context.getFileSystem().getPath(EXECUTION_OPENREAC))) {
            results = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), openReacParameters, config, computationManager);
            // Finding the last folder modified in the directory of the LocalComputationManager to tell the user which folder correspond.
            String[] amplRunningFolderList = context.getFileSystem().getPath(EXECUTION_OPENREAC).toFile().list();
            Objects.requireNonNull(amplRunningFolderList, "OpenReac did not run in the specified folder. Unexpected error.");
            Optional<Path> workingDir = Arrays.stream(amplRunningFolderList)
                    .map(filename -> context.getFileSystem().getPath(EXECUTION_OPENREAC, filename))
                    .max(Comparator.comparing(path -> path.toFile().lastModified()));
            context.getOutputStream().println("OpenReac optimisation done in " + workingDir.map(p -> p.toAbsolutePath().toString()).orElse("NOT_FOUND"));
        }

        context.getOutputStream().println("OpenReac status : " + results.getStatus().name());

    }

    public Network loadingNetwork(ToolRunningContext context, Path inputCaseFile, Properties inputParams) {
        Network network = Network.read(inputCaseFile, context.getShortTimeExecutionComputationManager(),
                ImportConfig.load(), inputParams);
        if (network == null) {
            throw new PowsyblException("Case '" + inputCaseFile + "' not found.");
        }
        return network;
    }
}
