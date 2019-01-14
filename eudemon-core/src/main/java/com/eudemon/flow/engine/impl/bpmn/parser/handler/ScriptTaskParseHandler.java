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
package com.eudemon.flow.engine.impl.bpmn.parser.handler;

import org.apache.commons.lang3.StringUtils;
import com.eudemon.flow.bpmn.model.BaseElement;
import com.eudemon.flow.bpmn.model.ScriptTask;
import com.eudemon.flow.engine.impl.bpmn.parser.BpmnParse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class ScriptTaskParseHandler extends AbstractActivityBpmnParseHandler<ScriptTask> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptTaskParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return ScriptTask.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, ScriptTask scriptTask) {

        if (StringUtils.isEmpty(scriptTask.getScript())) {
            LOGGER.warn("No script provided for scriptTask {}", scriptTask.getId());
        }

        scriptTask.setBehavior(bpmnParse.getActivityBehaviorFactory().createScriptTaskActivityBehavior(scriptTask));

    }

}
