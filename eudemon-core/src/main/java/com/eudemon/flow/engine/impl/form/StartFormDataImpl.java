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

package com.eudemon.flow.engine.impl.form;

import com.eudemon.flow.engine.form.StartFormData;
import com.eudemon.flow.engine.repository.ProcessDefinition;

/**
 * @author Tom Baeyens
 */
public class StartFormDataImpl extends FormDataImpl implements StartFormData {

    private static final long serialVersionUID = 1L;

    protected ProcessDefinition processDefinition;

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }
}