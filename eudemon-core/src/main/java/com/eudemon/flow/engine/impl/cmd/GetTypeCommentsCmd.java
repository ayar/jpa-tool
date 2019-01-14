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

package com.eudemon.flow.engine.impl.cmd;

import java.io.Serializable;
import java.util.List;

import com.eudemon.flow.common.engine.impl.interceptor.Command;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.task.Comment;

/**
 * @author Sam Kim
 */
public class GetTypeCommentsCmd implements Command<List<Comment>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String type;

    public GetTypeCommentsCmd(String type) {
        this.type = type;
    }

    @Override
    public List<Comment> execute(CommandContext commandContext) {
        return CommandContextUtil.getCommentEntityManager(commandContext).findCommentsByType(type);
    }
}
