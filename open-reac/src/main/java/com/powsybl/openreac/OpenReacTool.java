/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Exporter;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.tools.ConversionToolUtils;
import com.powsybl.openreac.parameters.input.AlgorithmInput;
import com.powsybl.openreac.parameters.input.OpenReacParameters;
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
import java.util.Properties;

import static com.powsybl.iidm.network.tools.ConversionToolUtils.*;

/**
 * @author Nicolas Pierre <nicolas.pierre at artelys.com>
 */
@AutoService(Tool.class)
public class OpenReacTool implements Tool {
    private static final String CASE_FILE = "case-file";
    private static final String OUTPUT_CASE_FORMAT = "output-case-format";
    private static final String OUTPUT_CASE_FILE = "output-case-file";
    private static final String SHUNTS_LIST = "tunable-reactance-shunts-list";
    private static final String GENERATORS_LIST = "fixed-reactance-generators-list";
    private static final String TRANSFORMER_LIST = "ratio-tunable-transformers-list";
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

        context.getOutputStream().println("Parsing properties...");
        Properties inputParams = readProperties(commandLine, ConversionToolUtils.OptionType.IMPORT, context);
        OpenReacParameters openReacParameters = createOpenReacParameters(commandLine, context);

        context.getOutputStream().println("Loading network '" + inputCaseFile + "'...");
        Network network = loadingNetwork(context, inputCaseFile, inputParams);

        itoolsOpenReac(context, network, openReacParameters);

        context.getOutputStream().println("Exporting network '" + outputCaseFile + "' with the results...");
        exportNetwork(commandLine, context, outputCaseFile, network);
        context.getOutputStream().println("OpenReac ran successfully.");
    }

    private OpenReacParameters createOpenReacParameters(CommandLine line,
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
        String[] shuntsList = inputParams.getProperty(SHUNTS_LIST, "").split(inputFileListSeparator);
        openReacParameters.addVariableShuntCompensators(shuntsList);
        String[] generatorsList = inputParams.getProperty(SHUNTS_LIST, "").split(inputFileListSeparator);
        openReacParameters.addTargetQGenerators(generatorsList);
        String[] transformerList = inputParams.getProperty(SHUNTS_LIST, "").split(inputFileListSeparator);
        openReacParameters.addVariableTwoWindingsTransformers(transformerList);

        for (String key : inputParams.stringPropertyNames()) {
            if (!key.equals(SHUNTS_LIST) && !key.equals(GENERATORS_LIST) && !key.equals(TRANSFORMER_LIST)) {
                AlgorithmInput.OpenReacAlgoParam algoParam = null;
                switch (key) {
                    case "foo":
                        algoParam = AlgorithmInput.OpenReacAlgoParam.TEST_PARAM;
                        break;
                    default:
                        context.getOutputStream()
                               .println(
                                       "Algorithm parameter " + key + " does not match any OpenReacParameter. Skipping...");
                        continue;
                }
                openReacParameters.addAlgorithmParam(algoParam, inputParams.getProperty(key));
            }
        }
        return openReacParameters;
    }

    private static void itoolsOpenReac(ToolRunningContext context, Network network,
                                       OpenReacParameters openReacParameters) {
        context.getOutputStream().println("Running OpenReac on the network...");
        OpenReacRunner.run(network, network.getVariantManager().getWorkingVariantId(), openReacParameters);
        context.getOutputStream().println("OpenReac optimisation done.");
    }

    private static void exportNetwork(CommandLine commandLine, ToolRunningContext context, Path outputCaseFile,
                                      Network network) throws IOException {
        String outputCaseFormat = commandLine.getOptionValue(OUTPUT_CASE_FORMAT);
        Properties outputParams = readProperties(commandLine, OptionType.EXPORT, context);
        network.write(outputCaseFormat, outputParams, outputCaseFile);
    }

    private static Network loadingNetwork(ToolRunningContext context, Path inputCaseFile, Properties inputParams) {
        Network network = Network.read(inputCaseFile, context.getShortTimeExecutionComputationManager(),
                ImportConfig.load(), inputParams);
        if (network == null) {
            throw new PowsyblException("Case '" + inputCaseFile + "' not found.");
        }
        return network;
    }
}
