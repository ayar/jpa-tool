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
package com.eudemon.flow.engine.impl.history.async.json.transformer;

import java.util.List;

import com.eudemon.flow.common.engine.api.scope.ScopeTypes;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.history.HistoricProcessInstance;
import com.eudemon.flow.engine.entity.HistoricProcessInstanceEntity;
import com.eudemon.flow.engine.entity.HistoricProcessInstanceEntityManager;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.impl.util.TaskHelper;

public abstract class AbstractProcessInstanceDeleteHistoryTransformer extends AbstractHistoryJsonTransformer {

    protected void deleteProcessInstance(String processInstanceId, CommandContext commandContext) {
        HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager = CommandContextUtil.getHistoricProcessInstanceEntityManager(commandContext);
        HistoricProcessInstanceEntity historicProcessInstance = historicProcessInstanceEntityManager.findById(processInstanceId);

        CommandContextUtil.getHistoricDetailEntityManager(commandContext).deleteHistoricDetailsByProcessInstanceId(processInstanceId);
        CommandContextUtil.getHistoricVariableService().deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
        CommandContextUtil.getHistoricActivityInstanceEntityManager(commandContext).deleteHistoricActivityInstancesByProcessInstanceId(processInstanceId);
        TaskHelper.deleteHistoricTaskInstancesByProcessInstanceId(processInstanceId);
        CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLinksByProcessInstanceId(processInstanceId);
        CommandContextUtil.getHistoricEntityLinkService().deleteHistoricEntityLinksByScopeIdAndScopeType(processInstanceId, ScopeTypes.BPMN);
        CommandContextUtil.getCommentEntityManager(commandContext).deleteCommentsByProcessInstanceId(processInstanceId);

        historicProcessInstanceEntityManager.delete(historicProcessInstance, false);

        // Also delete any sub-processes that may be active (ACT-821)

        List<HistoricProcessInstance> selectList = historicProcessInstanceEntityManager.findHistoricProcessInstancesBySuperProcessInstanceId(processInstanceId);
        for (HistoricProcessInstance child : selectList) {
            deleteProcessInstance(child.getId(), commandContext);
        }
    }
}
