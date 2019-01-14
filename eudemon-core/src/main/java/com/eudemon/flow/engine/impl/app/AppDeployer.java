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

package com.eudemon.flow.engine.impl.app;

import java.util.Map;

import com.eudemon.flow.common.engine.api.repository.EngineDeployment;
import com.eudemon.flow.common.engine.api.repository.EngineResource;
import com.eudemon.flow.common.engine.impl.EngineDeployer;
import com.eudemon.flow.engine.impl.cfg.ProcessEngineConfigurationImpl;
import com.eudemon.flow.engine.impl.persistence.deploy.DeploymentManager;
import com.eudemon.flow.engine.entity.DeploymentEntity;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class AppDeployer implements EngineDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDeployer.class);

    @Override
    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        LOGGER.debug("Processing app deployment {}", deployment.getName());

        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        DeploymentManager deploymentManager = processEngineConfiguration.getDeploymentManager();

        Object appResourceObject = null;
        DeploymentEntity deploymentEntity = (DeploymentEntity) deployment;
        Map<String, EngineResource> resources = deploymentEntity.getResources();
        for (String resourceName : resources.keySet()) {
            if (resourceName.endsWith(".app")) {
                LOGGER.info("Processing app resource {}", resourceName);

                EngineResource resourceEntity = resources.get(resourceName);
                byte[] resourceBytes = resourceEntity.getBytes();
                appResourceObject = processEngineConfiguration.getAppResourceConverter().convertAppResourceToModel(resourceBytes);
            }
        }

        if (appResourceObject != null) {
            deploymentManager.getAppResourceCache().put(deployment.getId(), appResourceObject);
        }
    }
}
