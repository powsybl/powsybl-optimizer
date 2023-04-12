/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.google.auto.service.AutoService;
import com.powsybl.ampl.executor.AmplConfig;
import com.powsybl.ampl.executor.AmplModelExecutionHandler;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Exporter;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.tools.ConversionToolUtils;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openreac.exceptions.InvalidParametersException;
import com.powsybl.openreac.parameters.OpenReacAmplIOFiles;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
import com.powsybl.openreac.parameters.input.algo.OpenReacOptimisationObjective;
import com.powsybl.openreac.parameters.output.OpenReacResult;
import com.powsybl.openreac.parameters.output.ReactiveSlackOutput;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.powsybl.iidm.network.tools.ConversionToolUtils.*;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
@AutoService(Tool.class)
public class OpenReacTool implements Tool {
    private static final String CASE_FILE = "case-file";
    private static final String OUTPUT_CASE_FORMAT = "output-case-format";
    private static final String OUTPUT_CASE_FILE = "output-case-file";
    private static final String SHUNTS_LIST = "variable-shunts-list";
    private static final String GENERATORS_LIST = "fixed-generators-list";
    private static final String TRANSFORMER_LIST = "variable-transformers-list";
    private static final String OPEN_REAC_PARAMS = "open-reac-params";

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
                options.addOption(Option.builder()
                        .longOpt(OUTPUT_CASE_FORMAT)
                        .desc("modified network output format " + Exporter.getFormats())
                        .hasArg()
                        .argName("CASEFORMAT")
                        .required()
                        .build());
                options.addOption(Option.builder()
                        .longOpt(OUTPUT_CASE_FILE)
                        .desc("modified network base name")
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
        Path outputCaseFile = context.getFileSystem().getPath(commandLine.getOptionValue(OUTPUT_CASE_FILE));
        Files.createDirectories(context.getFileSystem().getPath("./export/before_open_reac"));
        Files.createDirectories(context.getFileSystem().getPath("./export/after_open_reac"));
        Files.createDirectories(context.getFileSystem().getPath("./export/after_loadflow"));
        context.getOutputStream().println("Parsing properties...");
        Properties inputParams = readProperties(commandLine, ConversionToolUtils.OptionType.IMPORT, context);
        OpenReacParameters openReacParameters = createOpenReacParameters(commandLine, context);

        context.getOutputStream().println("Loading network '" + inputCaseFile + "'...");
        Network network = loadingNetwork(context, inputCaseFile, inputParams);

        context.getOutputStream().println("Exporting network with ampl.");
        exportOpenReacRunningFolder(context, network, openReacParameters, "./export/before_open_reac");

        itoolsOpenReac(context, network, openReacParameters);

        context.getOutputStream().println("Exporting network with ampl.");
        exportOpenReacRunningFolder(context, network, openReacParameters, "./export/after_open_reac");

        context.getOutputStream().println("Exporting network '" + outputCaseFile + "' with the results...");
        exportNetwork(commandLine, context, outputCaseFile, network);

        context.getOutputStream().println("Running a loadflow...");
        LoadFlowResult result = LoadFlow.run(network, context.getShortTimeExecutionComputationManager(), LoadFlowParameters.load());
        exportLoadFlowMetrics(context, result.getMetrics());
        context.getOutputStream().println("Loadflow done. Is ok ? " + result.isOk());

        context.getOutputStream().println("Exporting network with ampl.");
        exportOpenReacRunningFolder(context, network, openReacParameters, "./export/after_loadflow");
        context.getOutputStream().println("All good. Exiting...");
    }

    public static void exportLoadFlowMetrics(ToolRunningContext context, Map<String, String> metrics) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> metric : metrics.entrySet()) {
            builder.append(metric.getKey()).append(" ").append(metric.getValue()).append(System.lineSeparator());
        }
        Files.writeString(context.getFileSystem().getPath("./loadflow_metrics.txt"), builder.toString(), StandardOpenOption.CREATE);
    }

    public static OpenReacParameters createOpenReacParameters(CommandLine line,
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
                        openReacParameters.setRatioVoltageObjective(Double.parseDouble(ratioStr));
                        openReacParameters.setObjective(OpenReacOptimisationObjective.BETWEEN_HIGH_AND_LOW_VOLTAGE_PROFILE);
                        break;
                    case "obj_provided_target_v":
                        openReacParameters.setObjective(OpenReacOptimisationObjective.SPECIFIC_VOLTAGE_PROFILE);
                        break;
                    default:
                        context.getOutputStream()
                                .println(
                                        "Algorithm parameter " + key + " does not match any OpenReacParameter. Skipping...");
                }
            }
        }
        return openReacParameters;
    }

    public static void itoolsOpenReac(ToolRunningContext context, Network network,
                                       OpenReacParameters openReacParameters) {
        context.getOutputStream().println("Running OpenReac on the network...");
        OpenReacResult results = OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), openReacParameters);
        List<Map.Entry<String, String>> sortedIndicators = results.getIndicators().entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
        context.getOutputStream().println("OpenReac optimisation done.");

        context.getOutputStream().println("OpenReac status:" + results.getStatus().name());
        context.getOutputStream().println("OpenReac indicators: ");
        for (Map.Entry<String, String> indicator : sortedIndicators) {
            context.getOutputStream().println(indicator.getKey() + " " + indicator.getValue());
        }
        context.getOutputStream().println("OpenReac reactive slacks: ");
        for (ReactiveSlackOutput.ReactiveSlack reactiveSlack : results.getReactiveSlacks()) {
            context.getOutputStream().println("reactiveSlack : " + reactiveSlack.busId + " " + reactiveSlack.voltageLevelId + " " + reactiveSlack.slack);
        }
    }

    /**
     * Export the folder that will run ampl with mainly OpenReac files and network export in ampl format.
     *
     * @implNote Quite a HACK, it should be exposed as a utility from powsybl-core.
     */
    public static void exportOpenReacRunningFolder(ToolRunningContext context, Network network,
                                                    OpenReacParameters openReacParameters, String contextRelativePath) throws IOException {
        AmplModelExecutionHandler amplModelExecutionHandler = new AmplModelExecutionHandler(
                OpenReacModel.buildModel(),
                network,
                network.getVariantManager().getWorkingVariantId(),
                AmplConfig.load(),
                new OpenReacAmplIOFiles(openReacParameters, network, false)
        );
        amplModelExecutionHandler.before(context.getFileSystem().getPath(contextRelativePath));
    }

    public static void exportNetwork(CommandLine commandLine, ToolRunningContext context, Path outputCaseFile,
                                      Network network) throws IOException {
        String outputCaseFormat = commandLine.getOptionValue(OUTPUT_CASE_FORMAT);
        Properties outputParams = readProperties(commandLine, OptionType.EXPORT, context);
        network.write(outputCaseFormat, outputParams, outputCaseFile);
    }

    public static Network loadingNetwork(ToolRunningContext context, Path inputCaseFile, Properties inputParams) {
        Network network = Network.read(inputCaseFile, context.getShortTimeExecutionComputationManager(),
                ImportConfig.load(), inputParams);
        if (network == null) {
            throw new PowsyblException("Case '" + inputCaseFile + "' not found.");
        }
        return network;
    }
}
