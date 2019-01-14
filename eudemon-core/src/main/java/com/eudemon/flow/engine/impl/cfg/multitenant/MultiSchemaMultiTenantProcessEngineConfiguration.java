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
package com.eudemon.flow.engine.impl.cfg.multitenant;

import java.util.concurrent.ExecutorService;

import javax.sql.DataSource;

import com.eudemon.flow.common.engine.impl.cfg.multitenant.TenantAwareDataSource;
import com.eudemon.flow.common.engine.impl.cfg.multitenant.TenantInfoHolder;
import com.eudemon.flow.common.engine.impl.interceptor.Command;
import com.eudemon.flow.common.engine.impl.interceptor.CommandContext;
import com.eudemon.flow.common.engine.impl.interceptor.CommandInterceptor;
import com.eudemon.flow.common.engine.impl.persistence.StrongUuidGenerator;
import com.eudemon.flow.engine.ProcessEngine;
import com.eudemon.flow.engine.ProcessEngineConfiguration;
import com.eudemon.flow.engine.impl.SchemaOperationProcessEngineClose;
import com.eudemon.flow.engine.impl.cfg.ProcessEngineConfigurationImpl;
import com.eudemon.flow.engine.impl.db.DbIdGenerator;
import com.eudemon.flow.engine.impl.util.CommandContextUtil;
import com.eudemon.flow.engine.repository.DeploymentBuilder;
import com.eudemon.flow.job.service.impl.asyncexecutor.AsyncExecutor;
import com.eudemon.flow.job.service.impl.asyncexecutor.multitenant.ExecutorPerTenantAsyncExecutor;
import com.eudemon.flow.job.service.impl.asyncexecutor.multitenant.SharedExecutorServiceAsyncExecutor;
import com.eudemon.flow.job.service.impl.asyncexecutor.multitenant.TenantAwareAsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ProcessEngineConfiguration} that builds a multi tenant {@link ProcessEngine} where each tenant has its own database schema.
 *
 * If multitenancy is needed and no data isolation is needed: the default {@link ProcessEngineConfigurationImpl} of Flowable is multitenant enabled out of the box by setting a tenant identifier on a
 * {@link DeploymentBuilder}.
 *
 * This configuration has following characteristics:
 *
 * - It needs a {@link TenantInfoHolder} to determine which tenant is currently 'active'. Ie for which tenant a certain API call is executed.
 *
 * - The {@link StrongUuidGenerator} is used by default. The 'regular' {@link DbIdGenerator} cannot be used with this config.
 *
 * - Adding tenants (also after boot!) is done using the {@link #registerTenant(String, DataSource)} operations.
 *
 * - Currently, this config does not work with the 'old' {@link JobExecutor}, but only with the newer {@link AsyncExecutor}. There are two different implementations: - The
 * {@link ExecutorPerTenantAsyncExecutor}: creates one full {@link AsyncExecutor} for each tenant. - The {@link SharedExecutorServiceAsyncExecutor}: created acquisition threads for each tenant, but
 * the job execution is done using a process engine shared {@link ExecutorService}. The {@link AsyncExecutor} needs to be injected using the {@link #setAsyncExecutor(AsyncExecutor)} method on this
 * class.
 *
 * databasetype
 *
 * @author Joram Barrez
 */
public class MultiSchemaMultiTenantProcessEngineConfiguration extends ProcessEngineConfigurationImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiSchemaMultiTenantProcessEngineConfiguration.class);

    protected TenantInfoHolder tenantInfoHolder;
    protected boolean booted;

    public MultiSchemaMultiTenantProcessEngineConfiguration(TenantInfoHolder tenantInfoHolder) {

        this.tenantInfoHolder = tenantInfoHolder;

        // Using the UUID generator, as otherwise the ids are pulled from a global pool of ids, backed by
        // a database table. Which is impossible with a multi-database-schema setup.

        // Also: it avoids the need for having a process definition cache for each tenant

        this.idGenerator = new StrongUuidGenerator();

        this.dataSource = new TenantAwareDataSource(tenantInfoHolder);
    }

    /**
     * Add a new {@link DataSource} for a tenant, identified by the provided tenantId, to the engine. This can be done after the engine has booted up.
     *
     * Note that the tenant identifier must have been added to the {@link TenantInfoHolder} *prior* to calling this method.
     */
    public void registerTenant(String tenantId, DataSource dataSource) {
        ((TenantAwareDataSource) super.getDataSource()).addDataSource(tenantId, dataSource);

        if (booted) {
            createTenantSchema(tenantId);

            createTenantAsyncJobExecutor(tenantId);

            tenantInfoHolder.setCurrentTenantId(tenantId);
            super.postProcessEngineInitialisation();
            tenantInfoHolder.clearCurrentTenantId();
        }
    }

    @Override
    public void initAsyncExecutor() {

        if (asyncExecutor == null) {
            asyncExecutor = new ExecutorPerTenantAsyncExecutor(tenantInfoHolder);
        }

        super.initAsyncExecutor();

        if (asyncExecutor instanceof TenantAwareAsyncExecutor) {
            for (String tenantId : tenantInfoHolder.getAllTenants()) {
                ((TenantAwareAsyncExecutor) asyncExecutor).addTenantAsyncExecutor(tenantId, false); // false -> will be started later with all the other executors
            }
        }
    }

    @Override
    public ProcessEngine buildProcessEngine() {

        // Disable schema creation/validation by setting it to null.
        // We'll do it manually, see buildProcessEngine() method (hence why it's copied first)
        String originalDatabaseSchemaUpdate = this.databaseSchemaUpdate;
        this.databaseSchemaUpdate = null;

        // Also, we shouldn't start the async executor until *after* the schema's have been created
        boolean originalIsAutoActivateAsyncExecutor = this.asyncExecutorActivate;
        this.asyncExecutorActivate = false;

        ProcessEngine processEngine = super.buildProcessEngine();

        // Reset to original values
        this.databaseSchemaUpdate = originalDatabaseSchemaUpdate;
        this.asyncExecutorActivate = originalIsAutoActivateAsyncExecutor;

        // Create tenant schema
        for (String tenantId : tenantInfoHolder.getAllTenants()) {
            createTenantSchema(tenantId);
        }

        // Start async executor
        if (asyncExecutor != null && originalIsAutoActivateAsyncExecutor) {
            asyncExecutor.start();
        }

        booted = true;
        return processEngine;
    }

    protected void createTenantSchema(String tenantId) {
        LOGGER.info("creating/validating database schema for tenant {}", tenantId);
        tenantInfoHolder.setCurrentTenantId(tenantId);
        getCommandExecutor().execute(getSchemaCommandConfig(), new ExecuteSchemaOperationCommand(databaseSchemaUpdate));
        tenantInfoHolder.clearCurrentTenantId();
    }

    protected void createTenantAsyncJobExecutor(String tenantId) {
        ((TenantAwareAsyncExecutor) asyncExecutor).addTenantAsyncExecutor(tenantId, isAsyncExecutorActivate() && booted);
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        return null;
    }

    @Override
    protected void postProcessEngineInitialisation() {
        // empty here. will be done in registerTenant
    }

    @Override
    public Runnable getProcessEngineCloseRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                for (String tenantId : tenantInfoHolder.getAllTenants()) {
                    tenantInfoHolder.setCurrentTenantId(tenantId);
                    commandExecutor.execute(getProcessEngineCloseCommand());
                    tenantInfoHolder.clearCurrentTenantId();
                }
            }
        };
    }

    public Command<Void> getProcessEngineCloseCommand() {
        return new Command<Void>() {
            @Override
            public Void execute(CommandContext commandContext) {
                CommandContextUtil.getProcessEngineConfiguration(commandContext).getCommandExecutor().execute(new SchemaOperationProcessEngineClose());
                return null;
            }
        };
    }

    public TenantInfoHolder getTenantInfoHolder() {
        return tenantInfoHolder;
    }
}
