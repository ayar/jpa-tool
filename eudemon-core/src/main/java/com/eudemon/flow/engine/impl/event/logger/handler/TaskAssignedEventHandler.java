/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eudemon.flow.engine.impl.event.logger.handler;

import java.util.Map;

import com.eudemon.flow.common.engine.api.delegate.event.FlowableEntityEvent;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.entity.EventLogEntryEntity;
import com.eudemon.flow.task.api.TaskEntity;

/**
 * @author Joram Barrez
 */
public class TaskAssignedEventHandler extends AbstractTaskEventHandler {

    @Override
    public EventLogEntryEntity generateEventLogEntry(CommandContext commandContext) {
        TaskEntity task = (TaskEntity) ((FlowableEntityEvent) event).getEntity();
        Map<String, Object> data = handleCommonTaskFields(task);
        return createEventLogEntry(task.getProcessDefinitionId(), task.getProcessInstanceId(), task.getExecutionId(), task.getId(), data);
    }

}