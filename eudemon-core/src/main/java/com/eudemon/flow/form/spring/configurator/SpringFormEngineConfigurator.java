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
package com.eudemon.flow.form.spring.configurator;

import com.eudemon.flow.common.engine.api.FlowableException;
import com.eudemon.flow.common.engine.impl.AbstractEngineConfiguration;
import com.eudemon.flow.common.spring.SpringEngineConfiguration;
import com.eudemon.flow.form.engine.FormEngine;
import com.eudemon.flow.form.engine.configurator.FormEngineConfigurator;
import com.eudemon.flow.form.spring.SpringFormEngineConfiguration;
import com.eudemon.flow.form.spring.SpringFormExpressionManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringFormEngineConfigurator extends FormEngineConfigurator {

    @Override
    public void configure(AbstractEngineConfiguration engineConfiguration) {
        if (formEngineConfiguration == null) {
            formEngineConfiguration = new SpringFormEngineConfiguration();
        } else if (!(formEngineConfiguration instanceof SpringFormEngineConfiguration)) {
            throw new IllegalArgumentException("Expected formEngine configuration to be of type"
                + SpringFormEngineConfiguration.class + " but was " + formEngineConfiguration.getClass());
        }
        initialiseCommonProperties(engineConfiguration, formEngineConfiguration);
        SpringEngineConfiguration springEngineConfiguration = (SpringEngineConfiguration) engineConfiguration;
        ((SpringFormEngineConfiguration) formEngineConfiguration).setTransactionManager(springEngineConfiguration.getTransactionManager());
        formEngineConfiguration.setExpressionManager(new SpringFormExpressionManager(
                        springEngineConfiguration.getApplicationContext(), springEngineConfiguration.getBeans()));

        initFormEngine();

        initServiceConfigurations(engineConfiguration, formEngineConfiguration);
    }

    @Override
    protected synchronized FormEngine initFormEngine() {
        if (formEngineConfiguration == null) {
            throw new FlowableException("FormEngineConfiguration is required");
        }

        return formEngineConfiguration.buildFormEngine();
    }
}
