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

package com.eudemon.flow.engine.delegate;

import com.eudemon.flow.common.engine.api.FlowableException;
import com.eudemon.flow.common.engine.api.FlowableIllegalArgumentException;
import com.eudemon.flow.engine.impl.bpmn.parser.Error;

/**
 * Special exception that can be used to throw a BPMN Error from {@link JavaDelegate}s and expressions.
 *
 * This should only be used for business faults, which shall be handled by a Boundary Error Event or Error Event Sub-Process modeled in the process definition. Technical errors should be represented
 * by other exception types.
 *
 * This class represents an actual instance of a BPMN Error, whereas {@link Error} represents an Error definition.
 *
 * @author Tijs Rademakers
 */
public class BpmnError extends FlowableException {

    private static final long serialVersionUID = 1L;

    private String errorCode;

    public BpmnError(String errorCode) {
        super("");
        setErrorCode(errorCode);
    }

    public BpmnError(String errorCode, String message) {
        super(message);
        setErrorCode(errorCode);
    }

    protected void setErrorCode(String errorCode) {
        if (errorCode == null) {
            throw new FlowableIllegalArgumentException("Error Code must not be null.");
        }
        if (errorCode.length() < 1) {
            throw new FlowableIllegalArgumentException("Error Code must not be empty.");
        }
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
