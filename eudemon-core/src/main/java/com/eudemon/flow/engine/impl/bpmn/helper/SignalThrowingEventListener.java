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
package com.eudemon.flow.engine.impl.bpmn.helper;

import java.util.List;

import com.eudemon.flow.common.engine.api.FlowableIllegalArgumentException;
import com.eudemon.flow.common.engine.api.delegate.event.FlowableEngineEvent;
import com.eudemon.flow.common.engine.api.delegate.event.FlowableEvent;
import com.eudemon.flow.common.engine.api.delegate.event.FlowableEventListener;
import com.eudemon.flow.common.engine.impl.context.Context;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.entity.EventSubscriptionEntityManager;
import com.eudemon.flow.engine.entity.SignalEventSubscriptionEntity;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.repository.ProcessDefinition;

/**
 * An {@link FlowableEventListener} that throws a signal event when an event is dispatched to it.
 *
 * @author Frederik Heremans
 *
 */
public class SignalThrowingEventListener extends BaseDelegateEventListener {

    protected String signalName;
    protected boolean processInstanceScope = true;

    @Override
    public void onEvent(FlowableEvent event) {
        if (isValidEvent(event) && event instanceof FlowableEngineEvent) {

            FlowableEngineEvent engineEvent = (FlowableEngineEvent) event;

            if (engineEvent.getProcessInstanceId() == null && processInstanceScope) {
                throw new FlowableIllegalArgumentException("Cannot throw process-instance scoped signal, since the dispatched event is not part of an ongoing process instance");
            }

            CommandContext commandContext = Context.getCommandContext();
            EventSubscriptionEntityManager eventSubscriptionEntityManager = CommandContextUtil.getEventSubscriptionEntityManager(commandContext);
            List<SignalEventSubscriptionEntity> subscriptionEntities = null;
            if (processInstanceScope) {
                subscriptionEntities = eventSubscriptionEntityManager.findSignalEventSubscriptionsByProcessInstanceAndEventName(engineEvent.getProcessInstanceId(), signalName);
            } else {
                String tenantId = null;
                if (engineEvent.getProcessDefinitionId() != null) {
                    ProcessDefinition processDefinition = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                            .getDeploymentManager()
                            .findDeployedProcessDefinitionById(engineEvent.getProcessDefinitionId());
                    tenantId = processDefinition.getTenantId();
                }
                subscriptionEntities = eventSubscriptionEntityManager.findSignalEventSubscriptionsByEventName(signalName, tenantId);
            }

            for (SignalEventSubscriptionEntity signalEventSubscriptionEntity : subscriptionEntities) {
                eventSubscriptionEntityManager.eventReceived(signalEventSubscriptionEntity, null, false);
            }
        }
    }

    public void setSignalName(String signalName) {
        this.signalName = signalName;
    }

    public void setProcessInstanceScope(boolean processInstanceScope) {
        this.processInstanceScope = processInstanceScope;
    }

    @Override
    public boolean isFailOnException() {
        return true;
    }
}
