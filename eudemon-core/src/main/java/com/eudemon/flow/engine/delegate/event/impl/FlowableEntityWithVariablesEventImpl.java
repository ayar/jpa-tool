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
package com.eudemon.flow.engine.delegate.event.impl;

import java.util.Map;

import com.eudemon.flow.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import com.eudemon.flow.common.engine.api.delegate.event.FlowableEngineEventType;
import com.eudemon.flow.engine.delegate.event.FlowableEntityWithVariablesEvent;

/**
 * Base class for all {@link FlowableEngineEntityEvent} implementations, related to entities with variables.
 *
 * @author Tijs Rademakers
 */
@SuppressWarnings("rawtypes")
public class FlowableEntityWithVariablesEventImpl extends FlowableEntityEventImpl implements FlowableEntityWithVariablesEvent {

    protected Map variables;
    protected boolean localScope;

    public FlowableEntityWithVariablesEventImpl(Object entity, Map variables, boolean localScope, FlowableEngineEventType type) {
        super(entity, type);

        this.variables = variables;
        this.localScope = localScope;
    }

    @Override
    public Map getVariables() {
        return variables;
    }

    @Override
    public boolean isLocalScope() {
        return localScope;
    }
}
