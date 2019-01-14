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
package com.eudemon.flow.idm.engine.configurator;

import java.util.List;

import com.eudemon.flow.common.engine.impl.AbstractEngineConfiguration;
import com.eudemon.flow.common.engine.impl.AbstractEngineConfigurator;
import com.eudemon.flow.common.engine.impl.EngineDeployer;
import com.eudemon.flow.common.engine.impl.interceptor.EngineConfigurationConstants;
import com.eudemon.flow.common.engine.impl.persistence.entity.Entity;
import com.eudemon.flow.idm.engine.IdmEngineConfiguration;
import com.eudemon.flow.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import com.eudemon.flow.idm.engine.impl.db.EntityDependencyOrder;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class IdmEngineConfigurator extends AbstractEngineConfigurator {

    protected IdmEngineConfiguration idmEngineConfiguration;

    @Override
    public int getPriority() {
        return EngineConfigurationConstants.PRIORITY_ENGINE_IDM;
    }

    @Override
    protected List<EngineDeployer> getCustomDeployers() {
        return null;
    }

    @Override
    protected String getMybatisCfgPath() {
        return IdmEngineConfiguration.DEFAULT_MYBATIS_MAPPING_FILE;
    }

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (idmEngineConfiguration == null) {
            idmEngineConfiguration = new StandaloneIdmEngineConfiguration();
        }

        initialiseCommonProperties(engineConfiguration, idmEngineConfiguration);

        idmEngineConfiguration.buildIdmEngine();

        initServiceConfigurations(engineConfiguration, idmEngineConfiguration);
    }

    @Override
    protected List<Class<? extends Entity>> getEntityInsertionOrder() {
        return EntityDependencyOrder.INSERT_ORDER;
    }

    @Override
    protected List<Class<? extends Entity>> getEntityDeletionOrder() {
        return EntityDependencyOrder.DELETE_ORDER;
    }

    public IdmEngineConfiguration getIdmEngineConfiguration() {
        return idmEngineConfiguration;
    }

    public IdmEngineConfigurator setIdmEngineConfiguration(IdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
        return this;
    }

}
