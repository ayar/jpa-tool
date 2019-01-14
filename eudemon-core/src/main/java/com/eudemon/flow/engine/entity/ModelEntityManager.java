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

import com.eudemon.flow.common.engine.impl.persistence.entity.EntityManager;
import com.eudemon.flow.engine.impl.ModelQueryImpl;
import com.eudemon.flow.engine.repository.Model;

/**
 * @author Joram Barrez
 */
public interface ModelEntityManager extends EntityManager<ModelEntity> {

    void insertEditorSourceForModel(String modelId, byte[] modelSource);

    void insertEditorSourceExtraForModel(String modelId, byte[] modelSource);

    List<Model> findModelsByQueryCriteria(ModelQueryImpl query);

    long findModelCountByQueryCriteria(ModelQueryImpl query);

    byte[] findEditorSourceByModelId(String modelId);

    byte[] findEditorSourceExtraByModelId(String modelId);

    List<Model> findModelsByNativeQuery(Map<String, Object> parameterMap);

    long findModelCountByNativeQuery(Map<String, Object> parameterMap);

    void updateModel(ModelEntity updatedModel);

    void deleteEditorSource(ModelEntity model);

    void deleteEditorSourceExtra(ModelEntity model);

}
