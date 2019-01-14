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
package com.eudemon.flow.engine.impl;

import java.util.List;
import java.util.Map;

import com.eudemon.flow.common.engine.impl.AbstractNativeQuery;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.common.engine.impl.interceptor.CommandExecutor;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.repository.Model;
import com.eudemon.flow.engine.repository.NativeModelQuery;

public class NativeModelQueryImpl extends AbstractNativeQuery<NativeModelQuery, Model> implements NativeModelQuery {

    private static final long serialVersionUID = 1L;

    public NativeModelQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public NativeModelQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    // results ////////////////////////////////////////////////////////////////

    @Override
    public List<Model> executeList(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getModelEntityManager(commandContext).findModelsByNativeQuery(parameterMap);
    }

    @Override
    public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
        return CommandContextUtil.getModelEntityManager(commandContext).findModelCountByNativeQuery(parameterMap);
    }

}
