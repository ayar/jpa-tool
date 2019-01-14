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

package com.eudemon.flow.engine.impl.bpmn.behavior;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.eudemon.flow.bpmn.model.EventSubProcess;
import com.eudemon.flow.bpmn.model.MessageEventDefinition;
import com.eudemon.flow.bpmn.model.StartEvent;
import com.eudemon.flow.bpmn.model.SubProcess;
import com.eudemon.flow.bpmn.model.ValuedDataObject;
import com.eudemon.flow.common.engine.impl.context.Context;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.delegate.DelegateExecution;
import com.eudemon.flow.engine.history.DeleteReason;
import com.eudemon.flow.engine.entity.EventSubscriptionEntity;
import com.eudemon.flow.engine.entity.EventSubscriptionEntityManager;
import com.eudemon.flow.engine.entity.ExecutionEntity;
import com.eudemon.flow.engine.entity.ExecutionEntityManager;
import com.eudemon.flow.engine.entity.MessageEventSubscriptionEntity;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;

/**
 * Implementation of the BPMN 2.0 event subprocess message start event.
 *
 * @author Tijs Rademakers
 */
public class EventSubProcessMessageStartEventActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected MessageEventDefinition messageEventDefinition;

    public EventSubProcessMessageStartEventActivityBehavior(MessageEventDefinition messageEventDefinition) {
        this.messageEventDefinition = messageEventDefinition;
    }

    @Override
    public void execute(DelegateExecution execution) {
        StartEvent startEvent = (StartEvent) execution.getCurrentFlowElement();
        EventSubProcess eventSubProcess = (EventSubProcess) startEvent.getSubProcess();

        execution.setScope(true);

        // initialize the template-defined data objects as variables
        Map<String, Object> dataObjectVars = processDataObjects(eventSubProcess.getDataObjects());
        if (dataObjectVars != null) {
            execution.setVariablesLocal(dataObjectVars);
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        StartEvent startEvent = (StartEvent) execution.getCurrentFlowElement();
        if (startEvent.isInterrupting()) {
            List<ExecutionEntity> childExecutions = executionEntityManager.collectChildren(executionEntity.getParent());
            for (int i = childExecutions.size() - 1; i >= 0; i--) {
                ExecutionEntity childExecutionEntity = childExecutions.get(i);
                if (!childExecutionEntity.isEnded() && !childExecutionEntity.getId().equals(executionEntity.getId())) {
                    executionEntityManager.deleteExecutionAndRelatedData(childExecutionEntity,
                            DeleteReason.EVENT_SUBPROCESS_INTERRUPTING + "(" + startEvent.getId() + ")");
                }
            }

            EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();

            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (eventSubscription instanceof MessageEventSubscriptionEntity && eventSubscription.getEventName().equals(messageEventDefinition.getMessageRef())) {

                    eventSubscriptionEntityManager.delete(eventSubscription);
                }
            }
        }

        ExecutionEntity newSubProcessExecution = executionEntityManager.createChildExecution(executionEntity.getParent());
        newSubProcessExecution.setCurrentFlowElement((SubProcess) executionEntity.getCurrentFlowElement().getParentContainer());
        newSubProcessExecution.setEventScope(false);
        newSubProcessExecution.setScope(true);

        ExecutionEntity outgoingFlowExecution = executionEntityManager.createChildExecution(newSubProcessExecution);
        outgoingFlowExecution.setCurrentFlowElement(startEvent);

        CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(outgoingFlowExecution);

        leave(outgoingFlowExecution);
    }

    protected Map<String, Object> processDataObjects(Collection<ValuedDataObject> dataObjects) {
        Map<String, Object> variablesMap = new HashMap<>();
        // convert data objects to process variables
        if (dataObjects != null) {
            for (ValuedDataObject dataObject : dataObjects) {
                variablesMap.put(dataObject.getName(), dataObject.getValue());
            }
        }
        return variablesMap;
    }
}
