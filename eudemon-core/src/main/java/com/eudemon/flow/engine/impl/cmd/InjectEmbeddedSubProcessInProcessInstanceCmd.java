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
package com.eudemon.flow.engine.impl.cmd;

import java.util.List;

import com.eudemon.flow.bpmn.model.BpmnModel;
import com.eudemon.flow.bpmn.model.FlowElement;
import com.eudemon.flow.bpmn.model.Process;
import com.eudemon.flow.bpmn.model.StartEvent;
import com.eudemon.flow.bpmn.model.SubProcess;
import com.eudemon.flow.common.engine.api.FlowableException;
import com.eudemon.flow.common.engine.impl.interceptor.Command;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.impl.context.Context;
import com.eudemon.flow.engine.impl.dynamic.DynamicEmbeddedSubProcessBuilder;
import com.eudemon.flow.engine.impl.dynamic.DynamicSubProcessParallelInjectUtil;
import com.eudemon.flow.engine.entity.DeploymentEntity;
import com.eudemon.flow.engine.entity.ExecutionEntity;
import com.eudemon.flow.engine.entity.ExecutionEntityManager;
import com.eudemon.flow.engine.entity.ProcessDefinitionEntity;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.impl.util.ProcessDefinitionUtil;

public class InjectEmbeddedSubProcessInProcessInstanceCmd extends AbstractDynamicInjectionCmd implements Command<Void> {

    protected String processInstanceId;
    protected DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder;

    public InjectEmbeddedSubProcessInProcessInstanceCmd(String processInstanceId, DynamicEmbeddedSubProcessBuilder dynamicEmbeddedSubProcessBuilder) {
        this.processInstanceId = processInstanceId;
        this.dynamicEmbeddedSubProcessBuilder = dynamicEmbeddedSubProcessBuilder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        createDerivedProcessDefinitionForProcessInstance(commandContext, processInstanceId);
        return null;
    }

    @Override
    protected void updateBpmnProcess(CommandContext commandContext, Process process,
            BpmnModel bpmnModel, ProcessDefinitionEntity originalProcessDefinitionEntity, DeploymentEntity newDeploymentEntity) {

        DynamicSubProcessParallelInjectUtil.injectParallelSubProcess(process, bpmnModel, dynamicEmbeddedSubProcessBuilder,
                        originalProcessDefinitionEntity, newDeploymentEntity, commandContext);
    }

    @Override
    protected void updateExecutions(CommandContext commandContext, ProcessDefinitionEntity processDefinitionEntity,
                                    ExecutionEntity processInstance, List<ExecutionEntity> childExecutions) {

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionEntity.getId());
        SubProcess subProcess = (SubProcess) bpmnModel.getFlowElement(dynamicEmbeddedSubProcessBuilder.getDynamicSubProcessId());
        ExecutionEntity subProcessExecution = executionEntityManager.createChildExecution(processInstance);
        subProcessExecution.setScope(true);
        subProcessExecution.setCurrentFlowElement(subProcess);
        CommandContextUtil.getHistoryManager(commandContext).recordActivityStart(subProcessExecution);

        ExecutionEntity childExecution = executionEntityManager.createChildExecution(subProcessExecution);

        StartEvent initialEvent = null;
        for (FlowElement subElement : subProcess.getFlowElements()) {
            if (subElement instanceof StartEvent) {
                StartEvent startEvent = (StartEvent) subElement;
                if (startEvent.getEventDefinitions().size() == 0) {
                    initialEvent = startEvent;
                    break;
                }
            }
        }

        if (initialEvent == null) {
            throw new FlowableException("Could not find a none start event in dynamic sub process");
        }

        childExecution.setCurrentFlowElement(initialEvent);

        Context.getAgenda().planContinueProcessOperation(childExecution);
    }

}
