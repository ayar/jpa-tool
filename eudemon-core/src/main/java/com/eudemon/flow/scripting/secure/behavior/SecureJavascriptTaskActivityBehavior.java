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
package com.eudemon.flow.scripting.secure.behavior;

import org.apache.commons.lang3.exception.ExceptionUtils;
import com.eudemon.flow.common.engine.api.FlowableException;
import com.eudemon.flow.engine.delegate.BpmnError;
import com.eudemon.flow.engine.delegate.DelegateExecution;
import com.eudemon.flow.engine.impl.bpmn.behavior.ScriptTaskActivityBehavior;
import com.eudemon.flow.engine.impl.bpmn.helper.ErrorPropagation;
import com.eudemon.flow.scripting.secure.impl.SecureJavascriptUtil;

/**
 * @author Joram Barrez
 */
public class SecureJavascriptTaskActivityBehavior extends ScriptTaskActivityBehavior {

    public SecureJavascriptTaskActivityBehavior(String scriptTaskId, String script,
            String language, String resultVariable, boolean storeScriptVariables) {
        super(scriptTaskId, script, language, resultVariable, storeScriptVariables);
    }

    @Override
    public void execute(DelegateExecution execution) {
        boolean noErrors = true;
        try {
            Object result = SecureJavascriptUtil.evaluateScript(execution, script);

            if (resultVariable != null) {
                execution.setVariable(resultVariable, result);
            }

        } catch (FlowableException e) {
            noErrors = false;
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof BpmnError) {
                ErrorPropagation.propagateError((BpmnError) rootCause, execution);
            } else {
                throw e;
            }
        }
        if (noErrors) {
            leave(execution);
        }
    }

}
