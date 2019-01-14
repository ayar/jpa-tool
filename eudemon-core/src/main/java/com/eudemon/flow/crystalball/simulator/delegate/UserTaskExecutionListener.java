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
package com.eudemon.flow.crystalball.simulator.delegate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.eudemon.flow.crystalball.simulator.SimulationEvent;
import com.eudemon.flow.crystalball.simulator.SimulationRunContext;
import com.eudemon.flow.crystalball.simulator.delegate.event.impl.UserTaskCompleteTransformer;
import com.eudemon.flow.crystalball.simulator.impl.StartReplayProcessEventHandler;
import com.eudemon.flow.engine.delegate.TaskListener;
import com.eudemon.flow.task.service.DelegateTask;

/**
 * in the case of task event create simulation event in the event calendar
 *
 * @author martin.grofcik
 */
public class UserTaskExecutionListener implements TaskListener {

    private final String typeToFind;
    protected final String typeToCreate;
    private final Collection<SimulationEvent> events;

    public UserTaskExecutionListener(String typeToFind, String typeToCreate, Collection<SimulationEvent> events) {
        this.typeToFind = typeToFind;
        this.typeToCreate = typeToCreate;
        this.events = events;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        SimulationEvent eventToSimulate = findUserTaskCompleteEvent(delegateTask);
        if (eventToSimulate != null) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("taskId", delegateTask.getId());
            properties.put("variables", eventToSimulate.getProperty(UserTaskCompleteTransformer.TASK_VARIABLES));
            // we were able to resolve event to simulate automatically
            SimulationEvent e = new SimulationEvent.Builder(typeToCreate).properties(properties).build();
            SimulationRunContext.getEventCalendar().addEvent(e);
        }
    }

    private SimulationEvent findUserTaskCompleteEvent(DelegateTask delegateTask) {
        if (delegateTask.hasVariable(StartReplayProcessEventHandler.PROCESS_INSTANCE_ID)) {
            String toSimulateProcessInstanceId = (String) delegateTask.getVariable(StartReplayProcessEventHandler.PROCESS_INSTANCE_ID);
            String toSimulateTaskDefinitionKey = delegateTask.getTaskDefinitionKey();
            for (SimulationEvent e : events) {
                if (typeToFind.equals(e.getType()) && toSimulateProcessInstanceId.equals(e.getProperty(UserTaskCompleteTransformer.PROCESS_INSTANCE_ID))
                        && toSimulateTaskDefinitionKey.equals(e.getProperty(UserTaskCompleteTransformer.TASK_DEFINITION_KEY)))
                    return e;
            }
        }
        return null;
    }
}
