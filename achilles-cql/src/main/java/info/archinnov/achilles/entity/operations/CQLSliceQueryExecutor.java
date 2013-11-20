/**
 *
 * Copyright (C) 2012-2013 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.archinnov.achilles.entity.operations;

import info.archinnov.achilles.context.CQLDaoContext;
import info.archinnov.achilles.context.CQLPersistenceContext;
import info.archinnov.achilles.context.CQLPersistenceContextFactory;
import info.archinnov.achilles.context.ConfigurationContext;
import info.archinnov.achilles.entity.CQLEntityMapper;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.iterator.CQLSliceQueryIterator;
import info.archinnov.achilles.query.SliceQuery;
import info.archinnov.achilles.query.slice.CQLSliceQuery;
import info.archinnov.achilles.statement.CQLStatementGenerator;
import info.archinnov.achilles.type.ConsistencyLevel;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CQLSliceQueryExecutor {

	private CQLStatementGenerator generator = new CQLStatementGenerator();
	private CQLEntityMapper mapper = new CQLEntityMapper();
	private CQLEntityProxifier proxifier = new CQLEntityProxifier();
	private CQLPersistenceContextFactory contextFactory;
	private CQLDaoContext daoContext;
	private ConsistencyLevel defaultReadLevel;

	public CQLSliceQueryExecutor(CQLPersistenceContextFactory contextFactory, ConfigurationContext configContext,
			CQLDaoContext daoContext) {
		this.contextFactory = contextFactory;
		this.daoContext = daoContext;
		this.defaultReadLevel = configContext.getDefaultReadConsistencyLevel();
	}

	public <T> List<T> get(SliceQuery<T> sliceQuery) {

		EntityMeta meta = sliceQuery.getMeta();

		List<T> clusteredEntities = new ArrayList<T>();

		CQLSliceQuery<T> cqlSliceQuery = new CQLSliceQuery<T>(sliceQuery, defaultReadLevel);
		Query query = generator.generateSelectSliceQuery(cqlSliceQuery, cqlSliceQuery.getLimit());
		List<Row> rows = daoContext.execute(query).all();

		for (Row row : rows) {
			T clusteredEntity = meta.<T> instanciate();
			mapper.setEagerPropertiesToEntity(row, meta, clusteredEntity);
			clusteredEntities.add(clusteredEntity);
		}

		return Lists.transform(clusteredEntities, getProxyTransformer(sliceQuery, meta.getEagerGetters()));
	}

	public <T> Iterator<T> iterator(SliceQuery<T> sliceQuery) {

		CQLSliceQuery<T> cqlSliceQuery = new CQLSliceQuery<T>(sliceQuery, defaultReadLevel);
		Query query = generator.generateSelectSliceQuery(cqlSliceQuery, cqlSliceQuery.getBatchSize());
		Iterator<Row> iterator = daoContext.execute(query).iterator();
		PreparedStatement ps = generator.generateIteratorSliceQuery(cqlSliceQuery, daoContext);
		CQLPersistenceContext context = buildContextForQuery(sliceQuery);
		return new CQLSliceQueryIterator<T>(cqlSliceQuery, context, iterator, ps);
	}

	public <T> void remove(SliceQuery<T> sliceQuery) {
		CQLSliceQuery<T> cqlSliceQuery = new CQLSliceQuery<T>(sliceQuery, defaultReadLevel);
		cqlSliceQuery.validateSliceQueryForRemove();
		Query query = generator.generateRemoveSliceQuery(cqlSliceQuery);
		daoContext.execute(query);
	}

	protected <T> CQLPersistenceContext buildContextForQuery(SliceQuery<T> sliceQuery) {

		ConsistencyLevel cl = sliceQuery.getConsistencyLevel() == null ? defaultReadLevel : sliceQuery
				.getConsistencyLevel();
		return contextFactory.newContextForSliceQuery(sliceQuery.getEntityClass(), sliceQuery.getPartitionComponents(),
				cl);
	}

	private <T> Function<T, T> getProxyTransformer(final SliceQuery<T> sliceQuery, final List<Method> getters) {
		return new Function<T, T>() {
			@Override
			public T apply(T clusteredEntity) {
				CQLPersistenceContext context = contextFactory.newContext(clusteredEntity);
				return proxifier.buildProxy(clusteredEntity, context, Sets.newHashSet(getters));
			}
		};
	}
}