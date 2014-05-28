/*
 * Copyright (C) 2012-2014 DuyHai DOAN
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package info.archinnov.achilles.integration.spring;

import static info.archinnov.achilles.configuration.ConfigurationParameters.BEAN_VALIDATION_ENABLE;
import static info.archinnov.achilles.configuration.ConfigurationParameters.BEAN_VALIDATION_VALIDATOR;
import static info.archinnov.achilles.configuration.ConfigurationParameters.CONSISTENCY_LEVEL_READ_DEFAULT;
import static info.archinnov.achilles.configuration.ConfigurationParameters.CONSISTENCY_LEVEL_READ_MAP;
import static info.archinnov.achilles.configuration.ConfigurationParameters.CONSISTENCY_LEVEL_WRITE_DEFAULT;
import static info.archinnov.achilles.configuration.ConfigurationParameters.CONSISTENCY_LEVEL_WRITE_MAP;
import static info.archinnov.achilles.configuration.ConfigurationParameters.ENTITIES_LIST;
import static info.archinnov.achilles.configuration.ConfigurationParameters.ENTITY_PACKAGES;
import static info.archinnov.achilles.configuration.ConfigurationParameters.FORCE_BATCH_STATEMENTS_ORDERING;
import static info.archinnov.achilles.configuration.ConfigurationParameters.FORCE_TABLE_CREATION;
import static info.archinnov.achilles.configuration.ConfigurationParameters.INSERT_STRATEGY;
import static info.archinnov.achilles.configuration.ConfigurationParameters.KEYSPACE_NAME;
import static info.archinnov.achilles.configuration.ConfigurationParameters.NATIVE_SESSION;
import static info.archinnov.achilles.configuration.ConfigurationParameters.OBJECT_MAPPER;
import static info.archinnov.achilles.configuration.ConfigurationParameters.OBJECT_MAPPER_FACTORY;
import static info.archinnov.achilles.configuration.ConfigurationParameters.PREPARED_STATEMENTS_CACHE_SIZE;
import static info.archinnov.achilles.configuration.ConfigurationParameters.PROXIES_WARM_UP_DISABLED;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Validator;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.archinnov.achilles.configuration.ConfigurationParameters;
import info.archinnov.achilles.json.ObjectMapperFactory;
import info.archinnov.achilles.persistence.PersistenceManager;
import info.archinnov.achilles.persistence.PersistenceManagerFactory;
import info.archinnov.achilles.persistence.PersistenceManagerFactory.PersistenceManagerFactoryBuilder;
import info.archinnov.achilles.type.InsertStrategy;

public class PersistenceManagerFactoryBean extends AbstractFactoryBean<PersistenceManager> {
    private static PersistenceManager manager;

    private Cluster cluster;
    private String entityPackages;
    private List<Class<?>> entityList;

    private Session session;
    private String keyspaceName;

    private ObjectMapperFactory objectMapperFactory;
    private ObjectMapper objectMapper;

    private String consistencyLevelReadDefault;
    private String consistencyLevelWriteDefault;
    private Map<String, String> consistencyLevelReadMap;
    private Map<String, String> consistencyLevelWriteMap;

    private boolean forceTableCreation = false;

    private boolean enableBeanValidation = false;

    private Validator beanValidator;

    private Integer preparedStatementCacheSize;

    private boolean disableProxiesWarmUp = true;

    private boolean forceBatchStatementOrdering = false;

    private InsertStrategy insertStrategy = InsertStrategy.ALL_FIELDS;


    protected void initialize() {
        Map<ConfigurationParameters, Object> configMap = new HashMap<>();

        fillEntityPackages(configMap);

        fillEntityList(configMap);

        if (session != null) {
            configMap.put(NATIVE_SESSION, session);
        }

        fillKeyspaceName(configMap);

        fillObjectMapper(configMap);

        fillConsistencyLevels(configMap);

        configMap.put(FORCE_TABLE_CREATION, forceTableCreation);

        fillBeanValidation(configMap);

        if (preparedStatementCacheSize != null) {
            configMap.put(PREPARED_STATEMENTS_CACHE_SIZE, preparedStatementCacheSize);
        }
        configMap.put(PROXIES_WARM_UP_DISABLED, disableProxiesWarmUp);
        configMap.put(FORCE_BATCH_STATEMENTS_ORDERING, forceBatchStatementOrdering);
        configMap.put(INSERT_STRATEGY, insertStrategy);

        PersistenceManagerFactory pmf = PersistenceManagerFactoryBuilder.build(cluster, configMap);
        manager = pmf.createPersistenceManager();
    }

    private void fillBeanValidation(Map<ConfigurationParameters, Object> configMap) {
        configMap.put(BEAN_VALIDATION_ENABLE, enableBeanValidation);
        if (beanValidator != null) {
            configMap.put(BEAN_VALIDATION_VALIDATOR, beanValidator);
        }
    }

    private void fillEntityPackages(Map<ConfigurationParameters, Object> configMap) {
        if (isNotBlank(entityPackages)) {
            configMap.put(ENTITY_PACKAGES, entityPackages);
        }
    }

    private void fillEntityList(Map<ConfigurationParameters, Object> configMap) {
        if (CollectionUtils.isNotEmpty(entityList)) {
            configMap.put(ENTITIES_LIST, entityList);
        }
    }

    private void fillKeyspaceName(Map<ConfigurationParameters, Object> configMap) {
        if (isBlank(keyspaceName)) {
            throw new IllegalArgumentException("Keyspace name should be provided");
        }
        configMap.put(KEYSPACE_NAME, keyspaceName);
    }


    private void fillObjectMapper(Map<ConfigurationParameters, Object> configMap) {
        if (objectMapperFactory != null) {
            configMap.put(OBJECT_MAPPER_FACTORY, objectMapperFactory);
        }
        if (objectMapper != null) {
            configMap.put(OBJECT_MAPPER, objectMapper);
        }
    }

    private void fillConsistencyLevels(Map<ConfigurationParameters, Object> configMap) {
        if (consistencyLevelReadDefault != null) {
            configMap.put(CONSISTENCY_LEVEL_READ_DEFAULT, consistencyLevelReadDefault);
        }
        if (consistencyLevelWriteDefault != null) {
            configMap.put(CONSISTENCY_LEVEL_WRITE_DEFAULT, consistencyLevelWriteDefault);
        }

        if (consistencyLevelReadMap != null) {
            configMap.put(CONSISTENCY_LEVEL_READ_MAP, consistencyLevelReadMap);
        }
        if (consistencyLevelWriteMap != null) {
            configMap.put(CONSISTENCY_LEVEL_WRITE_MAP, consistencyLevelWriteMap);
        }
    }


    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }


    public void setEntityPackages(String entityPackages) {
        this.entityPackages = entityPackages;
    }

    public void setEntityList(List<Class<?>> entityList) {
        this.entityList = entityList;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setEnableBeanValidation(boolean enableBeanValidation) {
        this.enableBeanValidation = enableBeanValidation;
    }

    public void setBeanValidator(Validator beanValidator) {
        this.beanValidator = beanValidator;
    }

    public void setPreparedStatementCacheSize(Integer preparedStatementCacheSize) {
        this.preparedStatementCacheSize = preparedStatementCacheSize;
    }

    public void setDisableProxiesWarmUp(boolean disableProxiesWarmUp) {
        this.disableProxiesWarmUp = disableProxiesWarmUp;
    }

    public void setForceBatchStatementOrdering(boolean forceBatchStatementOrdering) {
        this.forceBatchStatementOrdering = forceBatchStatementOrdering;
    }

    public void setInsertStrategy(InsertStrategy insertStrategy) {
        this.insertStrategy = insertStrategy;
    }

    public void setForceTableCreation(boolean forceTableCreation) {
        this.forceTableCreation = forceTableCreation;
    }

    public void setObjectMapperFactory(ObjectMapperFactory objectMapperFactory) {
        this.objectMapperFactory = objectMapperFactory;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setConsistencyLevelReadDefault(String consistencyLevelReadDefault) {
        this.consistencyLevelReadDefault = consistencyLevelReadDefault;
    }

    public void setConsistencyLevelWriteDefault(String consistencyLevelWriteDefault) {
        this.consistencyLevelWriteDefault = consistencyLevelWriteDefault;
    }

    public void setConsistencyLevelReadMap(Map<String, String> consistencyLevelReadMap) {
        this.consistencyLevelReadMap = consistencyLevelReadMap;
    }

    public void setConsistencyLevelWriteMap(Map<String, String> consistencyLevelWriteMap) {
        this.consistencyLevelWriteMap = consistencyLevelWriteMap;
    }

    @Override
    public Class<?> getObjectType() {
        return PersistenceManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    protected PersistenceManager createInstance() throws Exception {
        synchronized (this) {
            if (manager == null) {
                initialize();
            }
        }
        return manager;
    }

}
