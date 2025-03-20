package reports; /**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

import com.google.auto.service.AutoService;
import com.powsybl.commons.report.ReportResourceBundle;

/**
 * @author Sophie Frasnedo {@literal <sophie.frasnedo at rte-france.com>}
 */

@AutoService(ReportResourceBundle.class)
public class PowsyblOptimizerReportResourceBundle implements ReportResourceBundle {

    public static final String BASE_NAME = "com.powsybl.optimizer.commons";

    public String getBaseName() {
        return BASE_NAME;
    }
}
