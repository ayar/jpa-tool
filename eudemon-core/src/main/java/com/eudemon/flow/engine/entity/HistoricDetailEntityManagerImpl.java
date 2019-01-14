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

package com.eudemon.flow.engine.entity;

import java.util.List;
import java.util.Map;

import com.eudemon.flow.common.engine.impl.history.HistoryLevel;
import com.eudemon.flow.common.engine.impl.persistence.entity.data.DataManager;
import com.eudemon.flow.engine.history.HistoricDetail;
import com.eudemon.flow.engine.impl.HistoricDetailQueryImpl;
import com.eudemon.flow.engine.impl.cfg.ProcessEngineConfigurationImpl;
import com.eudemon.flow.engine.entity.data.HistoricDetailDataManager;
import com.eudemon.flow.variable.api.persistence.entity.VariableInstanceEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricDetailEntityManagerImpl extends AbstractEntityManager<HistoricDetailEntity> implements HistoricDetailEntityManager {

    protected HistoricDetailDataManager historicDetailDataManager;

    public HistoricDetailEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoricDetailDataManager historicDetailDataManager) {
        super(processEngineConfiguration);
        this.historicDetailDataManager = historicDetailDataManager;
    }

    @Override
    protected DataManager<HistoricDetailEntity> getDataManager() {
        return historicDetailDataManager;
    }

    @Override
    public HistoricFormPropertyEntity insertHistoricFormPropertyEntity(ExecutionEntity execution,
            String propertyId, String propertyValue, String taskId) {

        HistoricFormPropertyEntity historicFormPropertyEntity = historicDetailDataManager.createHistoricFormProperty();
        historicFormPropertyEntity.setProcessInstanceId(execution.getProcessInstanceId());
        historicFormPropertyEntity.setExecutionId(execution.getId());
        historicFormPropertyEntity.setTaskId(taskId);
        historicFormPropertyEntity.setPropertyId(propertyId);
        historicFormPropertyEntity.setPropertyValue(propertyValue);
        historicFormPropertyEntity.setTime(getClock().getCurrentTime());

        HistoricActivityInstanceEntity historicActivityInstance = getHistoryManager().findActivityInstance(execution, true, false);
        if (historicActivityInstance != null) {
            historicFormPropertyEntity.setActivityInstanceId(historicActivityInstance.getId());
        }

        insert(historicFormPropertyEntity);
        return historicFormPropertyEntity;
    }

    @Override
    public HistoricDetailVariableInstanceUpdateEntity copyAndInsertHistoricDetailVariableInstanceUpdateEntity(VariableInstanceEntity variableInstance) {
        HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = historicDetailDataManager.createHistoricDetailVariableInstanceUpdate();
        historicVariableUpdate.setProcessInstanceId(variableInstance.getProcessInstanceId());
        historicVariableUpdate.setExecutionId(variableInstance.getExecutionId());
        historicVariableUpdate.setTaskId(variableInstance.getTaskId());
        historicVariableUpdate.setTime(getClock().getCurrentTime());
        historicVariableUpdate.setRevision(variableInstance.getRevision());
        historicVariableUpdate.setName(variableInstance.getName());
        historicVariableUpdate.setVariableType(variableInstance.getType());
        historicVariableUpdate.setTextValue(variableInstance.getTextValue());
        historicVariableUpdate.setTextValue2(variableInstance.getTextValue2());
        historicVariableUpdate.setDoubleValue(variableInstance.getDoubleValue());
        historicVariableUpdate.setLongValue(variableInstance.getLongValue());

        if (variableInstance.getBytes() != null) {
            historicVariableUpdate.setBytes(variableInstance.getBytes());
        }

        insert(historicVariableUpdate);
        return historicVariableUpdate;
    }

    @Override
    public void delete(HistoricDetailEntity entity, boolean fireDeleteEvent) {
        super.delete(entity, fireDeleteEvent);

        if (entity instanceof HistoricDetailVariableInstanceUpdateEntity) {
            HistoricDetailVariableInstanceUpdateEntity historicDetailVariableInstanceUpdateEntity = ((HistoricDetailVariableInstanceUpdateEntity) entity);
            if (historicDetailVariableInstanceUpdateEntity.getByteArrayRef() != null) {
                historicDetailVariableInstanceUpdateEntity.getByteArrayRef().delete();
            }
        }
    }

    @Override
    public void deleteHistoricDetailsByProcessInstanceId(String historicProcessInstanceId) {
        if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
            List<HistoricDetailEntity> historicDetails = historicDetailDataManager.findHistoricDetailsByProcessInstanceId(historicProcessInstanceId);

            for (HistoricDetailEntity historicDetail : historicDetails) {
                delete(historicDetail);
            }
        }
    }

    @Override
    public long findHistoricDetailCountByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery) {
        return historicDetailDataManager.findHistoricDetailCountByQueryCriteria(historicVariableUpdateQuery);
    }

    @Override
    public List<HistoricDetail> findHistoricDetailsByQueryCriteria(HistoricDetailQueryImpl historicVariableUpdateQuery) {
        return historicDetailDataManager.findHistoricDetailsByQueryCriteria(historicVariableUpdateQuery);
    }

    @Override
    public void deleteHistoricDetailsByTaskId(String taskId) {
        if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.FULL)) {
            List<HistoricDetailEntity> details = historicDetailDataManager.findHistoricDetailsByTaskId(taskId);
            for (HistoricDetail detail : details) {
                delete((HistoricDetailEntity) detail);
            }
        }
    }

    @Override
    public List<HistoricDetail> findHistoricDetailsByNativeQuery(Map<String, Object> parameterMap) {
        return historicDetailDataManager.findHistoricDetailsByNativeQuery(parameterMap);
    }

    @Override
    public long findHistoricDetailCountByNativeQuery(Map<String, Object> parameterMap) {
        return historicDetailDataManager.findHistoricDetailCountByNativeQuery(parameterMap);
    }

    public HistoricDetailDataManager getHistoricDetailDataManager() {
        return historicDetailDataManager;
    }

    public void setHistoricDetailDataManager(HistoricDetailDataManager historicDetailDataManager) {
        this.historicDetailDataManager = historicDetailDataManager;
    }

}