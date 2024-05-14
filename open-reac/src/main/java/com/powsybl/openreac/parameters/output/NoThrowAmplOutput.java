/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.openreac.parameters.output;

import com.powsybl.ampl.converter.AmplSubset;
import com.powsybl.ampl.executor.AmplOutputFile;
import com.powsybl.commons.util.StringToIntMapper;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Interface for output ampl files, which changes the behavior of errors during file reading.
 * <p>
 * Removes the contract of {@link AmplOutputFile} to throw IOException.
 * @author Nicolas Pierre {@literal <nicolas.pierre at artelys.com>}
 */
public interface NoThrowAmplOutput extends AmplOutputFile {
    /**
     * On IO errors during this function:
     * <ul>
     *     <li>
     *         {@link NoThrowAmplOutput#isErrorState()} must return <code>true</code>.
     *     </li>
     *     <li>
     *         All other methods can have an undefined behavior.
     *     </li>
     * </ul>
     */
    @Override
    void read(BufferedReader var1, StringToIntMapper<AmplSubset> var2) throws IOException;

    /**
     * @return True if the output reading went bad. WARNING, object might have undefined behavior.
     */
    boolean isErrorState();
}
