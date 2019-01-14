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

package com.eudemon.flow.engine.impl.dynamic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.eudemon.flow.bpmn.model.FlowElement;
import com.eudemon.flow.bpmn.model.SubProcess;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.dynamic.DynamicStateManager;
import com.eudemon.flow.engine.entity.ExecutionEntity;
import com.eudemon.flow.engine.entity.ExecutionEntityManager;
import com.eudemon.flow.engine.impl.runtime.ChangeActivityStateBuilderImpl;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class DefaultDynamicStateManager extends AbstractDynamicStateManager implements DynamicStateManager {

    @Override
    public void moveExecutionState(ChangeActivityStateBuilderImpl changeActivityStateBuilder, CommandContext commandContext) {
        List<MoveExecutionEntityContainer> moveExecutionEntityContainerList = resolveMoveExecutionEntityContainers(changeActivityStateBuilder, Optional.empty(), commandContext);
        List<ExecutionEntity> executions = moveExecutionEntityContainerList.iterator().next().getExecutions();
        String processInstanceId = executions.iterator().next().getProcessInstanceId();

        ProcessInstanceChangeState processInstanceChangeState = new ProcessInstanceChangeState()
            .setProcessInstanceId(processInstanceId)
            .setMoveExecutionEntityContainers(moveExecutionEntityContainerList)
            .setLocalVariables(changeActivityStateBuilder.getLocalVariables())
            .setProcessInstanceVariables(changeActivityStateBuilder.getProcessInstanceVariables());

        doMoveExecutionState(processInstanceChangeState, commandContext);
    }

    @Override
    protected Map<String, List<ExecutionEntity>> resolveActiveEmbeddedSubProcesses(String processInstanceId, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        List<ExecutionEntity> childExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(processInstanceId);

        Map<String, List<ExecutionEntity>> activeSubProcessesByActivityId = childExecutions.stream()
            .filter(ExecutionEntity::isActive)
            .filter(executionEntity -> executionEntity.getCurrentFlowElement() instanceof SubProcess)
            .collect(Collectors.groupingBy(ExecutionEntity::getActivityId));
        return activeSubProcessesByActivityId;
    }

    @Override
    protected boolean isDirectFlowElementExecutionMigration(FlowElement currentFlowElement, FlowElement newFlowElement) {
        return false;
    }

}
