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

package com.eudemon.flow.engine.impl.history;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.eudemon.flow.common.engine.api.delegate.event.FlowableEngineEventType;
import com.eudemon.flow.common.engine.api.delegate.event.FlowableEventDispatcher;
import com.eudemon.flow.common.engine.api.scope.ScopeTypes;
import com.eudemon.flow.common.engine.impl.history.HistoryLevel;
import com.eudemon.flow.engine.delegate.event.impl.FlowableEventBuilder;
import com.eudemon.flow.engine.history.HistoricActivityInstance;
import com.eudemon.flow.engine.history.HistoricProcessInstance;
import com.eudemon.flow.engine.impl.HistoricActivityInstanceQueryImpl;
import com.eudemon.flow.engine.impl.cfg.ProcessEngineConfigurationImpl;
import com.eudemon.flow.engine.entity.ExecutionEntity;
import com.eudemon.flow.engine.entity.HistoricActivityInstanceEntity;
import com.eudemon.flow.engine.entity.HistoricDetailVariableInstanceUpdateEntity;
import com.eudemon.flow.engine.entity.HistoricProcessInstanceEntity;
import com.eudemon.flow.engine.entity.ProcessDefinitionEntity;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.impl.util.TaskHelper;
import com.eudemon.flow.entitylink.api.history.HistoricEntityLinkService;
import com.eudemon.flow.entitylink.api.EntityLinkEntity;
import com.eudemon.flow.entitylink.api.HistoricEntityLinkEntity;
import com.eudemon.flow.identitylink.service.HistoricIdentityLinkService;
import com.eudemon.flow.identitylink.api.HistoricIdentityLinkEntity;
import com.eudemon.flow.identitylink.api.IdentityLinkEntity;
import com.eudemon.flow.task.api.history.HistoricTaskInstance;
import com.eudemon.flow.task.repository.HistoricTaskService;
import com.eudemon.flow.task.service.impl.HistoricTaskInstanceQueryImpl;
import com.eudemon.flow.task.api.HistoricTaskInstanceEntity;
import com.eudemon.flow.task.api.TaskEntity;
import com.eudemon.flow.variable.api.persistence.entity.VariableInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class that centralises recording of all history-related operations that are originated from inside the engine.
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class DefaultHistoryManager extends AbstractHistoryManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHistoryManager.class.getName());

    public DefaultHistoryManager(ProcessEngineConfigurationImpl processEngineConfiguration, HistoryLevel historyLevel, boolean usePrefixId) {
        super(processEngineConfiguration, historyLevel, usePrefixId);
    }

    // Process related history

    @Override
    public void recordProcessInstanceEnd(ExecutionEntity processInstance, String deleteReason, String activityId) {

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());

            if (historicProcessInstance != null) {
                historicProcessInstance.markEnded(deleteReason);
                historicProcessInstance.setEndActivityId(activityId);

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(
                            FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED, historicProcessInstance));
                }

            }
        }
    }

    @Override
    public void recordProcessInstanceNameChange(ExecutionEntity processInstanceExecution, String newName) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstanceExecution.getProcessDefinitionId())) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceExecution.getId());

            if (historicProcessInstance != null) {
                historicProcessInstance.setName(newName);
            }
        }
    }

    @Override
    public void recordProcessInstanceStart(ExecutionEntity processInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processInstance.getProcessDefinitionId())) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().create(processInstance);

            // Insert historic process-instance
            getHistoricProcessInstanceEntityManager().insert(historicProcessInstance, false);

            // Fire event
            FlowableEventDispatcher eventDispatcher = getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(
                        FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, historicProcessInstance));
            }

        }
    }

    @Override
    public void recordSubProcessInstanceStart(ExecutionEntity parentExecution, ExecutionEntity subProcessInstance) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, subProcessInstance.getProcessDefinitionId())) {

            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().create(subProcessInstance);
            getHistoricProcessInstanceEntityManager().insert(historicProcessInstance, false);

            // Fire event
            FlowableEventDispatcher eventDispatcher = getEventDispatcher();
            if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                eventDispatcher.dispatchEvent(
                                FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED, historicProcessInstance));
            }

            HistoricActivityInstanceEntity activityInstance = findActivityInstance(parentExecution, false, true);
            if (activityInstance != null) {
                activityInstance.setCalledProcessInstanceId(subProcessInstance.getProcessInstanceId());
            }

        }
    }

    @Override
    public void recordProcessInstanceDeleted(String processInstanceId, String processDefinitionId) {
        if (getHistoryManager().isHistoryEnabled(processDefinitionId)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);

            getHistoricDetailEntityManager().deleteHistoricDetailsByProcessInstanceId(processInstanceId);
            CommandContextUtil.getHistoricVariableService().deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
            getHistoricActivityInstanceEntityManager().deleteHistoricActivityInstancesByProcessInstanceId(processInstanceId);
            TaskHelper.deleteHistoricTaskInstancesByProcessInstanceId(processInstanceId);
            CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLinksByProcessInstanceId(processInstanceId);

            if (processEngineConfiguration.isEnableEntityLinks()) {
                CommandContextUtil.getHistoricEntityLinkService().deleteHistoricEntityLinksByScopeIdAndScopeType(processInstanceId, ScopeTypes.BPMN);
            }

            getCommentEntityManager().deleteCommentsByProcessInstanceId(processInstanceId);

            if (historicProcessInstance != null) {
                getHistoricProcessInstanceEntityManager().delete(historicProcessInstance, false);
            }

            // Also delete any sub-processes that may be active (ACT-821)

            List<HistoricProcessInstance> selectList = getHistoricProcessInstanceEntityManager().findHistoricProcessInstancesBySuperProcessInstanceId(processInstanceId);
            for (HistoricProcessInstance child : selectList) {
                recordProcessInstanceDeleted(child.getId(), processDefinitionId);
            }
        }
    }

    @Override
    public void recordDeleteHistoricProcessInstancesByProcessDefinitionId(String processDefinitionId) {
        if (getHistoryManager().isHistoryEnabled(processDefinitionId)) {
            List<String> historicProcessInstanceIds = getHistoricProcessInstanceEntityManager().findHistoricProcessInstanceIdsByProcessDefinitionId(processDefinitionId);
            for (String historicProcessInstanceId : historicProcessInstanceIds) {
                recordProcessInstanceDeleted(historicProcessInstanceId, processDefinitionId);
            }
        }
    }

    // Activity related history

    @Override
    public void recordActivityStart(ExecutionEntity executionEntity) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
            if (executionEntity.getActivityId() != null && executionEntity.getCurrentFlowElement() != null) {

                HistoricActivityInstanceEntity historicActivityInstanceEntity = null;

                // Historic activity instance could have been created (but only in cache, never persisted)
                // for example when submitting form properties
                HistoricActivityInstanceEntity historicActivityInstanceEntityFromCache = getHistoricActivityInstanceFromCache(executionEntity.getId(), executionEntity.getActivityId(), true);
                if (historicActivityInstanceEntityFromCache != null) {
                    historicActivityInstanceEntity = historicActivityInstanceEntityFromCache;
                } else {
                    historicActivityInstanceEntity = createHistoricActivityInstanceEntity(executionEntity);
                }

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(
                                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_CREATED, historicActivityInstanceEntity));
                }

            }
        }
    }

    @Override
    public void recordActivityEnd(ExecutionEntity executionEntity, String deleteReason) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, executionEntity.getProcessDefinitionId())) {
            HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(executionEntity, false, true);
            if (historicActivityInstance != null) {
                historicActivityInstance.markEnded(deleteReason);

                // Fire event
                FlowableEventDispatcher eventDispatcher = getEventDispatcher();
                if (eventDispatcher != null && eventDispatcher.isEnabled()) {
                    eventDispatcher.dispatchEvent(
                                    FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED, historicActivityInstance));
                }
            }
        }
    }

    @Override
    public void recordProcessDefinitionChange(String processInstanceId, String processDefinitionId) {
        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstanceId);
            if (historicProcessInstance != null) {
                historicProcessInstance.setProcessDefinitionId(processDefinitionId);
            }
        }
    }

    // Task related history

    @Override
    public void recordTaskCreated(TaskEntity task, ExecutionEntity execution) {
        String processDefinitionId = null;
        if (execution != null) {
            processDefinitionId = execution.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            if (execution != null) {
                task.setExecutionId(execution.getId());
                task.setProcessInstanceId(execution.getProcessInstanceId());
                task.setProcessDefinitionId(execution.getProcessDefinitionId());

                if (execution.getTenantId() != null) {
                    task.setTenantId(execution.getTenantId());
                }
            }
            HistoricTaskInstanceEntity historicTaskInstance = CommandContextUtil.getHistoricTaskService().recordTaskCreated(task);
            historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());

            if (execution != null) {
                historicTaskInstance.setExecutionId(execution.getId());
            }
        }

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            if (execution != null) {
                HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(execution, false, true);
                if (historicActivityInstance != null) {
                    historicActivityInstance.setTaskId(task.getId());
                }
            }
        }
    }

    @Override
    public void recordTaskEnd(TaskEntity task, ExecutionEntity execution, String deleteReason) {
        String processDefinitionId = null;
        if (execution != null) {
            processDefinitionId = execution.getProcessDefinitionId();
        } else if (task != null) {
            processDefinitionId = task.getProcessDefinitionId();
        }
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            HistoricTaskInstanceEntity historicTaskInstance = CommandContextUtil.getHistoricTaskService().recordTaskEnd(task, deleteReason);
            if (historicTaskInstance != null) {
                historicTaskInstance.setLastUpdateTime(processEngineConfiguration.getClock().getCurrentTime());
            }
        }
    }

    @Override
    public void recordTaskInfoChange(TaskEntity taskEntity) {

        boolean assigneeChanged = false;
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, taskEntity.getProcessDefinitionId())) {
            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            HistoricTaskInstanceEntity originalHistoricTaskInstanceEntity = historicTaskService.getHistoricTask(taskEntity.getId());
            String originalAssignee = null;
            if (originalHistoricTaskInstanceEntity != null) {
                originalAssignee = originalHistoricTaskInstanceEntity.getAssignee();
            }

            HistoricTaskInstanceEntity historicTaskInstance = historicTaskService.recordTaskInfoChange(taskEntity);
            if (historicTaskInstance != null) {
                if (!Objects.equals(originalAssignee, taskEntity.getAssignee())) {
                    assigneeChanged = true;
                }
            }
        }

        if (assigneeChanged && isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, taskEntity.getProcessDefinitionId())) {
            if (taskEntity.getExecutionId() != null) {
                ExecutionEntity executionEntity = getExecutionEntityManager().findById(taskEntity.getExecutionId());
                HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(executionEntity, false, true);
                if (historicActivityInstance != null) {
                    historicActivityInstance.setAssignee(taskEntity.getAssignee());
                }
            }
        }
    }

    // Variables related history

    @Override
    public void recordVariableCreate(VariableInstanceEntity variable) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            CommandContextUtil.getHistoricVariableService().createAndInsert(variable);
        }
    }

    @Override
    public void recordHistoricDetailVariableCreate(VariableInstanceEntity variable, ExecutionEntity sourceActivityExecution, boolean useActivityId) {
        String processDefinitionId = null;
        if (sourceActivityExecution != null) {
            processDefinitionId = sourceActivityExecution.getProcessDefinitionId();
        } else if (variable.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variable.getProcessInstanceId());
            if (processInstanceExecution != null) {
                processDefinitionId = processInstanceExecution.getProcessDefinitionId();
            }
        } else if (variable.getTaskId() != null) {
            TaskEntity taskEntity = CommandContextUtil.getTaskService().getTask(variable.getTaskId());
            if (taskEntity != null) {
                processDefinitionId = taskEntity.getProcessDefinitionId();
            }
        }

        if (isHistoryLevelAtLeast(HistoryLevel.FULL, processDefinitionId)) {

            HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = getHistoricDetailEntityManager().copyAndInsertHistoricDetailVariableInstanceUpdateEntity(variable);

            if (useActivityId && sourceActivityExecution != null) {
                HistoricActivityInstanceEntity historicActivityInstance = findActivityInstance(sourceActivityExecution, false, false);
                if (historicActivityInstance != null) {
                    historicVariableUpdate.setActivityInstanceId(historicActivityInstance.getId());
                }
            }
        }
    }

    @Override
    public void recordVariableUpdate(VariableInstanceEntity variableInstanceEntity) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variableInstanceEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variableInstanceEntity.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            CommandContextUtil.getHistoricVariableService().recordVariableUpdate(variableInstanceEntity);
        }
    }

    @Override
    public void recordVariableRemoved(VariableInstanceEntity variableInstanceEntity) {
        String processDefinitionId = null;
        if (enableProcessDefinitionHistoryLevel && variableInstanceEntity.getProcessInstanceId() != null) {
            ExecutionEntity processInstanceExecution = CommandContextUtil.getExecutionEntityManager().findById(variableInstanceEntity.getProcessInstanceId());
            processDefinitionId = processInstanceExecution.getProcessDefinitionId();
        }

        if (isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processDefinitionId)) {
            CommandContextUtil.getHistoricVariableService().recordVariableRemoved(variableInstanceEntity);
        }
    }

    @Override
    public void recordFormPropertiesSubmitted(ExecutionEntity processInstance, Map<String, String> properties, String taskId) {
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processInstance.getProcessDefinitionId())) {
            for (String propertyId : properties.keySet()) {
                String propertyValue = properties.get(propertyId);
                getHistoricDetailEntityManager().insertHistoricFormPropertyEntity(processInstance, propertyId, propertyValue, taskId);
            }
        }
    }

    // Identity link related history
    @Override
    public void recordIdentityLinkCreated(IdentityLinkEntity identityLink) {
        String processDefinitionId = null;
        if (identityLink.getProcessInstanceId() != null) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(identityLink.getProcessInstanceId());
            if (execution != null) {
                processDefinitionId = execution.getProcessDefinitionId();
            }
        } else if (identityLink.getTaskId() != null) {
            TaskEntity task = CommandContextUtil.getTaskService().getTask(identityLink.getTaskId());
            if (task != null) {
                processDefinitionId = task.getProcessDefinitionId();
            }
        }

        // It makes no sense storing historic counterpart for an identity link that is related
        // to a process definition only as this is never kept in history
        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId) && (identityLink.getProcessInstanceId() != null || identityLink.getTaskId() != null)) {
            HistoricIdentityLinkService historicIdentityLinkService = CommandContextUtil.getHistoricIdentityLinkService();
            HistoricIdentityLinkEntity historicIdentityLinkEntity = historicIdentityLinkService.createHistoricIdentityLink();
            historicIdentityLinkEntity.setId(identityLink.getId());
            historicIdentityLinkEntity.setGroupId(identityLink.getGroupId());
            historicIdentityLinkEntity.setProcessInstanceId(identityLink.getProcessInstanceId());
            historicIdentityLinkEntity.setTaskId(identityLink.getTaskId());
            historicIdentityLinkEntity.setType(identityLink.getType());
            historicIdentityLinkEntity.setUserId(identityLink.getUserId());
            historicIdentityLinkService.insertHistoricIdentityLink(historicIdentityLinkEntity, false);
        }
    }

    @Override
    public void recordIdentityLinkDeleted(IdentityLinkEntity identityLink) {
        String processDefinitionId = null;
        if (identityLink.getProcessInstanceId() != null) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(identityLink.getProcessInstanceId());
            if (execution != null) {
                processDefinitionId = execution.getProcessDefinitionId();
            }
        } else if (identityLink.getTaskId() != null) {
            TaskEntity task = CommandContextUtil.getTaskService().getTask(identityLink.getTaskId());
            if (task != null) {
                processDefinitionId = task.getProcessDefinitionId();
            }
        }

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            CommandContextUtil.getHistoricIdentityLinkService().deleteHistoricIdentityLink(identityLink.getId());
        }
    }

    // Entity link related history
    @Override
    public void recordEntityLinkCreated(EntityLinkEntity entityLink) {
        String processDefinitionId = null;
        if (ScopeTypes.BPMN.equals(entityLink.getScopeType()) && entityLink.getScopeId() != null) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(entityLink.getScopeId());
            if (execution != null) {
                processDefinitionId = execution.getProcessDefinitionId();
            }

        } else if (ScopeTypes.TASK.equals(entityLink.getScopeType()) && entityLink.getScopeId() != null) {
            TaskEntity task = CommandContextUtil.getTaskService().getTask(entityLink.getScopeId());
            if (task != null) {
                processDefinitionId = task.getProcessDefinitionId();
            }
        }

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            HistoricEntityLinkService historicEntityLinkService = CommandContextUtil.getHistoricEntityLinkService();
            HistoricEntityLinkEntity historicEntityLinkEntity = (HistoricEntityLinkEntity) historicEntityLinkService.createHistoricEntityLink();
            historicEntityLinkEntity.setId(entityLink.getId());
            historicEntityLinkEntity.setLinkType(entityLink.getLinkType());
            historicEntityLinkEntity.setCreateTime(entityLink.getCreateTime());
            historicEntityLinkEntity.setScopeId(entityLink.getScopeId());
            historicEntityLinkEntity.setScopeType(entityLink.getScopeType());
            historicEntityLinkEntity.setScopeDefinitionId(entityLink.getScopeDefinitionId());
            historicEntityLinkEntity.setReferenceScopeId(entityLink.getReferenceScopeId());
            historicEntityLinkEntity.setReferenceScopeType(entityLink.getReferenceScopeType());
            historicEntityLinkEntity.setReferenceScopeDefinitionId(entityLink.getReferenceScopeDefinitionId());
            historicEntityLinkService.insertHistoricEntityLink(historicEntityLinkEntity, false);
        }
    }

    @Override
    public void recordEntityLinkDeleted(EntityLinkEntity entityLink) {
        String processDefinitionId = null;
        if (ScopeTypes.BPMN.equals(entityLink.getScopeType()) && entityLink.getScopeId() != null) {
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(entityLink.getScopeId());
            if (execution != null) {
                processDefinitionId = execution.getProcessDefinitionId();
            }

        } else if (ScopeTypes.TASK.equals(entityLink.getScopeType()) && entityLink.getScopeId() != null) {
            TaskEntity task = CommandContextUtil.getTaskService().getTask(entityLink.getScopeId());
            if (task != null) {
                processDefinitionId = task.getProcessDefinitionId();
            }
        }

        if (isHistoryLevelAtLeast(HistoryLevel.AUDIT, processDefinitionId)) {
            CommandContextUtil.getHistoricEntityLinkService().deleteHistoricEntityLink(entityLink.getId());
        }
    }

    @Override
    public void updateProcessBusinessKeyInHistory(ExecutionEntity processInstance) {
        if (processInstance != null) {
            if (isHistoryEnabled(processInstance.getProcessDefinitionId())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("updateProcessBusinessKeyInHistory : {}", processInstance.getId());
                }

                HistoricProcessInstanceEntity historicProcessInstance = getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
                if (historicProcessInstance != null) {
                    historicProcessInstance.setBusinessKey(processInstance.getProcessInstanceBusinessKey());
                    getHistoricProcessInstanceEntityManager().update(historicProcessInstance, false);
                }
            }
        }
    }

    @Override
    public void updateProcessDefinitionIdInHistory(ProcessDefinitionEntity processDefinitionEntity, ExecutionEntity processInstance) {
        if (isHistoryEnabled(processDefinitionEntity.getId())) {
            HistoricProcessInstanceEntity historicProcessInstance = (HistoricProcessInstanceEntity) getHistoricProcessInstanceEntityManager().findById(processInstance.getId());
            historicProcessInstance.setProcessDefinitionId(processDefinitionEntity.getId());
            getHistoricProcessInstanceEntityManager().update(historicProcessInstance);

            HistoricTaskService historicTaskService = CommandContextUtil.getHistoricTaskService();
            HistoricTaskInstanceQueryImpl taskQuery = new HistoricTaskInstanceQueryImpl();
            taskQuery.processInstanceId(processInstance.getId());
            List<HistoricTaskInstance> historicTasks = historicTaskService.findHistoricTaskInstancesByQueryCriteria(taskQuery);
            if (historicTasks != null) {
                for (HistoricTaskInstance historicTaskInstance : historicTasks) {
                    HistoricTaskInstanceEntity taskEntity = (HistoricTaskInstanceEntity) historicTaskInstance;
                    taskEntity.setProcessDefinitionId(processDefinitionEntity.getId());
                    historicTaskService.updateHistoricTask(taskEntity, true);
                }
            }

            HistoricActivityInstanceQueryImpl activityQuery = new HistoricActivityInstanceQueryImpl();
            activityQuery.processInstanceId(processInstance.getId());
            List<HistoricActivityInstance> historicActivities = getHistoricActivityInstanceEntityManager().findHistoricActivityInstancesByQueryCriteria(activityQuery);
            if (historicActivities != null) {
                for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                    HistoricActivityInstanceEntity activityEntity = (HistoricActivityInstanceEntity) historicActivityInstance;
                    activityEntity.setProcessDefinitionId(processDefinitionEntity.getId());
                    getHistoricActivityInstanceEntityManager().update(activityEntity);
                }
            }
        }
    }

}