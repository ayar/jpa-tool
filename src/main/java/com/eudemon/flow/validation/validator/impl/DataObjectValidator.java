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
package com.eudemon.flow.validation.validator.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.eudemon.flow.bpmn.model.BpmnModel;
import com.eudemon.flow.bpmn.model.Process;
import com.eudemon.flow.bpmn.model.SubProcess;
import com.eudemon.flow.bpmn.model.ValuedDataObject;
import com.eudemon.flow.validation.ValidationError;
import com.eudemon.flow.validation.validator.Problems;
import com.eudemon.flow.validation.validator.ProcessLevelValidator;

/**
 * @author jbarrez
 */
public class DataObjectValidator extends ProcessLevelValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {

        // Gather data objects
        List<ValuedDataObject> allDataObjects = new ArrayList<>();
        allDataObjects.addAll(process.getDataObjects());
        List<SubProcess> subProcesses = process.findFlowElementsOfType(SubProcess.class, true);
        for (SubProcess subProcess : subProcesses) {
            allDataObjects.addAll(subProcess.getDataObjects());
        }

        // Validate
        for (ValuedDataObject dataObject : allDataObjects) {
            if (StringUtils.isEmpty(dataObject.getName())) {
                addError(errors, Problems.DATA_OBJECT_MISSING_NAME, process, dataObject, "Name is mandatory for a data object");
            }
        }

    }

}
