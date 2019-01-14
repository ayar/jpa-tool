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
package com.eudemon.flow.validation;

import java.util.ArrayList;
import java.util.List;

import com.eudemon.flow.bpmn.model.BpmnModel;
import com.eudemon.flow.validation.validator.Validator;
import com.eudemon.flow.validation.validator.ValidatorSet;

/**
 * @author jbarrez
 */
public class ProcessValidatorImpl implements ProcessValidator {

    protected List<ValidatorSet> validatorSets;

    @Override
    public List<ValidationError> validate(BpmnModel bpmnModel) {

        List<ValidationError> allErrors = new ArrayList<>();

        for (ValidatorSet validatorSet : validatorSets) {
            for (Validator validator : validatorSet.getValidators()) {
                List<ValidationError> validatorErrors = new ArrayList<>();
                validator.validate(bpmnModel, validatorErrors);
                if (!validatorErrors.isEmpty()) {
                    for (ValidationError error : validatorErrors) {
                        error.setValidatorSetName(validatorSet.getName());
                    }
                    allErrors.addAll(validatorErrors);
                }
            }
        }
        return allErrors;
    }

    @Override
    public List<ValidatorSet> getValidatorSets() {
        return validatorSets;
    }

    public void setValidatorSets(List<ValidatorSet> validatorSets) {
        this.validatorSets = validatorSets;
    }

    public void addValidatorSet(ValidatorSet validatorSet) {
        if (validatorSets == null) {
            validatorSets = new ArrayList<>();
        }
        validatorSets.add(validatorSet);
    }

}
