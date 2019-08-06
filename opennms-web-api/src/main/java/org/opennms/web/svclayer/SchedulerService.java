/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.svclayer;

import java.util.List;

import org.opennms.api.reporting.parameter.ReportParameters;
import org.opennms.reporting.core.DeliveryOptions;
import org.opennms.web.svclayer.model.TriggerDescription;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>SchedulerService interface.</p>
 *
 * @author ranger
 * @version $Id: $
 * @since 1.8.1
 */
@Transactional(readOnly = true)
public interface SchedulerService {
    
    List<TriggerDescription> getTriggerDescriptions();

    @Transactional(readOnly = false)
    void removeTrigger(String triggerName);
    
    @Transactional(readOnly = false)
    void removeTriggers(String[] triggerNames);
    
    Boolean exists(String triggerName);

    String updateCronTrigger(String cronTrigger, ReportParameters criteria, DeliveryOptions deliveryOptions, String cronExpression, SchedulerRequestContext context);

    @Transactional(readOnly = false)
    String addCronTrigger(String id,
            ReportParameters criteria, 
            DeliveryOptions deliveryOptions,
            String cronExpression, 
            SchedulerRequestContext context);

    @Transactional(readOnly = false)
    String execute(String id,
            ReportParameters criteria, 
            DeliveryOptions deliveryOptions,
                   SchedulerRequestContext context);

}
