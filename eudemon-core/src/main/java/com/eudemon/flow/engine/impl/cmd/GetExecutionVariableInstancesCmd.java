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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.eudemon.flow.common.engine.api.FlowableIllegalArgumentException;
import com.eudemon.flow.common.engine.api.FlowableObjectNotFoundException;
import com.eudemon.flow.common.engine.impl.interceptor.Command;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.compatibility.Flowable5CompatibilityHandler;
import com.eudemon.flow.engine.entity.ExecutionEntity;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.impl.util.Flowable5Util;
import com.eudemon.flow.engine.runtime.Execution;
import com.eudemon.flow.variable.api.persistence.entity.VariableInstance;

public class GetExecutionVariableInstancesCmd implements Command<Map<String, VariableInstance>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String executionId;
    protected Collection<String> variableNames;
    protected boolean isLocal;

    public GetExecutionVariableInstancesCmd(String executionId, Collection<String> variableNames, boolean isLocal) {
        this.executionId = executionId;
        this.variableNames = variableNames;
        this.isLocal = isLocal;
    }

    @Override
    public Map<String, VariableInstance> execute(CommandContext commandContext) {

        // Verify existence of execution
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("executionId is null");
        }

        ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager(commandContext).findById(executionId);

        if (execution == null) {
            throw new FlowableObjectNotFoundException("execution " + executionId + " doesn't exist", Execution.class);
        }

        Map<String, VariableInstance> variables = null;

        if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, execution.getProcessDefinitionId())) {
            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            variables = compatibilityHandler.getExecutionVariableInstances(executionId, variableNames, isLocal);

        } else {

            if (variableNames == null || variableNames.isEmpty()) {
                // Fetch all
                if (isLocal) {
                    variables = execution.getVariableInstancesLocal();
                } else {
                    variables = execution.getVariableInstances();
                }

            } else {
                // Fetch specific collection of variables
                if (isLocal) {
                    variables = execution.getVariableInstancesLocal(variableNames, false);
                } else {
                    variables = execution.getVariableInstances(variableNames, false);
                }
            }
        }

        return variables;
    }
}
