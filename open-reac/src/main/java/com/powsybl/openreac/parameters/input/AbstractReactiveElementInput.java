package com.powsybl.openreac.parameters.input;

import com.powsybl.ampl.converter.AmplConstants;
import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplInputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * List of shunts that can be modified by OpenReac
 * timestep num bus id
 */
public abstract class AbstractReactiveElementInput implements AmplInputFile {
    private final List<String> elementIds;
    private static final String QUOTE = "'";

    public String addQuotes(String str) {
        return QUOTE + str + QUOTE;
    }

    public AbstractReactiveElementInput(List<String> elementIds) {
        this.elementIds = elementIds;
    }

    @Override
    public InputStream getParameterFileAsStream(StringToIntMapper<AmplSubset> stringToIntMapper) {
        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("#NetworkId amplId powsyblId\n");
        for (String elementID : elementIds) {
            int amplId = stringToIntMapper.getInt(getElementAmplSubset(), elementID);
            String[] tokens = {Integer.toString(AmplConstants.DEFAULT_VARIANT_INDEX), Integer.toString(
                    amplId), addQuotes(elementID)};
            dataBuilder.append(String.join(" ", tokens));
            dataBuilder.append("\n");
        }
        //add new line at the end of the file !
        dataBuilder.append("\n");
        return new ByteArrayInputStream(dataBuilder.toString().getBytes(StandardCharsets.UTF_8));
    }

    abstract AmplSubset getElementAmplSubset();

}
