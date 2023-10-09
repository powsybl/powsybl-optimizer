/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import com.powsybl.openreac.parameters.input.VoltageLimitOverride;
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
    private static final String VOLTAGE_OVERRIDE_LIST = "voltage-level-override";
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

        String filename = line.getOptionValue(OPEN_REAC_PARAMS, null);
        JsonNode jsonNode = new ObjectMapper().readTree("{}");
        if (filename != null) {
            try (InputStream inputStream = Files.newInputStream(context.getFileSystem().getPath(filename))) {
                if (filename.endsWith(".json")) {
                    jsonNode = new ObjectMapper().readTree(inputStream);
                } else {
                    throw new InvalidParametersException("Format of properties must be json.");
                }
            }
        }

        OpenReacParameters openReacParameters = new OpenReacParameters();
        if (jsonNode.get(SHUNTS_LIST) != null && jsonNode.get(SHUNTS_LIST).isArray()) {
            ArrayNode array = (ArrayNode) jsonNode.get(SHUNTS_LIST);
            array.forEach(node -> openReacParameters.addVariableShuntCompensators(List.of(node.asText())));
        }
        if (jsonNode.get(GENERATORS_LIST) != null && jsonNode.get(GENERATORS_LIST).isArray()) {
            ArrayNode array = (ArrayNode) jsonNode.get(GENERATORS_LIST);
            array.forEach(node -> openReacParameters.addConstantQGenerators(List.of(node.asText())));
        }
        if (jsonNode.get(TRANSFORMER_LIST) != null && jsonNode.get(TRANSFORMER_LIST).isArray()) {
            ArrayNode array = (ArrayNode) jsonNode.get(TRANSFORMER_LIST);
            array.forEach(node -> openReacParameters.addVariableTwoWindingsTransformers(List.of(node.asText())));
        }
        if (jsonNode.get(VOLTAGE_OVERRIDE_LIST) != null && jsonNode.get(VOLTAGE_OVERRIDE_LIST).isArray()) {
            ArrayNode array = (ArrayNode) jsonNode.get(VOLTAGE_OVERRIDE_LIST);
            array.forEach(node -> {
                String voltageId = node.get("id").asText();
                double lowerPercent = node.get("lower").asDouble();
                double upperPercent = node.get("upper").asDouble();
                openReacParameters.addSpecificVoltageLimits(Map.of(voltageId,
                        new VoltageLimitOverride(VoltageLimitOverride.OverrideKind.RELATIVE, VoltageLimitOverride.OverrideKind.RELATIVE,
                                lowerPercent, upperPercent)));
            });
        }
        boolean objectiveSet = false;
        for (Iterator<String> it = jsonNode.fieldNames(); it.hasNext(); ) {
            String key = it.next();
            if (!key.equals(SHUNTS_LIST) && !key.equals(GENERATORS_LIST) && !key.equals(TRANSFORMER_LIST)) {
                switch (key) {
                    case "obj_min_gen":
                        if (objectiveSet) {
                            throw new InvalidParametersException("Objective is set twice in JSON. Please put only one.");
                        }
                        openReacParameters.setObjective(OpenReacOptimisationObjective.MIN_GENERATION);
                        objectiveSet = true;
                        break;
                    case "obj_provided_target_v":
                        if (objectiveSet) {
                            throw new InvalidParametersException("Objective is set twice in JSON. Please put only one.");
                        }
                        openReacParameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
                        objectiveSet = true;
                        break;
                    case "obj_target_ratio":
                        if (objectiveSet) {
                            throw new InvalidParametersException("Objective is set twice in JSON. Please put only one.");

                        }
                        if (jsonNode.get(key).isNull()) {
                            throw new InvalidParametersException("obj_target_ratio must have a value indicating the ratio to nominal voltage level to target.");
                        }
                        double ratio = jsonNode.get(key).asDouble();
                        openReacParameters.setObjectiveDistance(ratio);
                        openReacParameters.setObjective(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_LIMIT);
                        objectiveSet = true;
                        break;
                    default:
                        openReacParameters.addAlgorithmParam(key, jsonNode.get(key).asText());
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
