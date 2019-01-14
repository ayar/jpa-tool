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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.eudemon.flow.bpmn.model.BoundaryEvent;
import com.eudemon.flow.bpmn.model.Signal;
import com.eudemon.flow.bpmn.model.SignalEventDefinition;
import com.eudemon.flow.common.engine.api.delegate.Expression;
import com.eudemon.flow.common.engine.api.delegate.event.FlowableEngineEventType;
import com.eudemon.flow.common.engine.impl.context.Context;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.delegate.DelegateExecution;
import com.eudemon.flow.engine.delegate.event.impl.FlowableEventBuilder;
import com.eudemon.flow.engine.entity.EventSubscriptionEntity;
import com.eudemon.flow.engine.entity.EventSubscriptionEntityManager;
import com.eudemon.flow.engine.entity.ExecutionEntity;
import com.eudemon.flow.engine.entity.SignalEventSubscriptionEntity;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class BoundarySignalEventActivityBehavior extends BoundaryEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected SignalEventDefinition signalEventDefinition;
    protected Signal signal;

    public BoundarySignalEventActivityBehavior(SignalEventDefinition signalEventDefinition, Signal signal, boolean interrupting) {
        super(interrupting);
        this.signalEventDefinition = signalEventDefinition;
        this.signal = signal;
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        String signalName = null;
        if (StringUtils.isNotEmpty(signalEventDefinition.getSignalRef())) {
            signalName = signalEventDefinition.getSignalRef();
        } else {
            Expression signalExpression = CommandContextUtil.getProcessEngineConfiguration(commandContext).getExpressionManager()
                    .createExpression(signalEventDefinition.getSignalExpression());
            signalName = signalExpression.getValue(execution).toString();
        }

        CommandContextUtil.getEventSubscriptionEntityManager(commandContext).insertSignalEvent(signalName, signal, executionEntity);

        if (CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher().isEnabled()) {
            CommandContextUtil.getProcessEngineConfiguration(commandContext).getEventDispatcher()
                    .dispatchEvent(FlowableEventBuilder.createSignalEvent(FlowableEngineEventType.ACTIVITY_SIGNAL_WAITING, executionEntity.getActivityId(), signalName,
                            null, executionEntity.getId(), executionEntity.getProcessInstanceId(), executionEntity.getProcessDefinitionId()));
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();

        if (boundaryEvent.isCancelActivity()) {
            String eventName = null;
            if (signal != null) {
                eventName = signal.getName();
            } else {
                eventName = signalEventDefinition.getSignalRef();
            }

            EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager();
            List<EventSubscriptionEntity> eventSubscriptions = executionEntity.getEventSubscriptions();
            for (EventSubscriptionEntity eventSubscription : eventSubscriptions) {
                if (eventSubscription instanceof SignalEventSubscriptionEntity && eventSubscription.getEventName().equals(eventName)) {

                    eventSubscriptionEntityManager.delete(eventSubscription);
                }
            }
        }

        super.trigger(executionEntity, triggerName, triggerData);
    }
}
