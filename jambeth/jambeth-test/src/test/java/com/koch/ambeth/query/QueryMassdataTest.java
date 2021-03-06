package com.koch.ambeth.query;

/*-
 * #%L
 * jambeth-test
 * %%
 * Copyright (C) 2017 Koch Softwaredevelopment
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 * #L%
 */

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.koch.ambeth.cache.IRootCache;
import com.koch.ambeth.cache.config.CacheConfigurationConstants;
import com.koch.ambeth.cache.config.CacheNamedBeans;
import com.koch.ambeth.filter.FilterDescriptor;
import com.koch.ambeth.filter.IPagingResponse;
import com.koch.ambeth.filter.ISortDescriptor;
import com.koch.ambeth.filter.PagingRequest;
import com.koch.ambeth.filter.SortDescriptor;
import com.koch.ambeth.filter.SortDirection;
import com.koch.ambeth.informationbus.persistence.setup.SQLStructure;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.IocConfigurationConstants;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.ioc.threadlocal.IThreadLocalCleanupController;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.merge.cache.ICacheContext;
import com.koch.ambeth.merge.cache.ICacheProvider;
import com.koch.ambeth.persistence.api.IDatabase;
import com.koch.ambeth.persistence.api.database.DatabaseCallback;
import com.koch.ambeth.persistence.config.PersistenceConfigurationConstants;
import com.koch.ambeth.persistence.jdbc.IConnectionExtension;
import com.koch.ambeth.persistence.jdbc.JdbcUtil;
import com.koch.ambeth.persistence.jdbc.config.PersistenceJdbcConfigurationConstants;
import com.koch.ambeth.persistence.xml.TestServicesModule;
import com.koch.ambeth.query.config.QueryConfigurationConstants;
import com.koch.ambeth.query.filter.IFilterToQueryBuilder;
import com.koch.ambeth.query.filter.IPagingQuery;
import com.koch.ambeth.service.config.ServiceConfigurationConstants;
import com.koch.ambeth.testutil.AbstractInformationBusWithPersistenceTest;
import com.koch.ambeth.testutil.TestFrameworkModule;
import com.koch.ambeth.testutil.TestModule;
import com.koch.ambeth.testutil.TestProperties;
import com.koch.ambeth.testutil.TestPropertiesList;
import com.koch.ambeth.testutil.category.PerformanceTests;
import com.koch.ambeth.util.MeasurementUtil;
import com.koch.ambeth.util.ParamHolder;
import com.koch.ambeth.util.collections.ILinkedMap;
import com.koch.ambeth.util.collections.IList;
import com.koch.ambeth.util.config.IProperties;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.state.IStateRollback;
import com.koch.ambeth.util.threading.ProcessIdHelper;

@Category(PerformanceTests.class)
@TestFrameworkModule({ QueryMassDataModule.class })
@TestModule({ TestServicesModule.class })
@SQLStructure("QueryMassdata_structure.sql")
@TestPropertiesList({
		@TestProperties(name = IocConfigurationConstants.TrackDeclarationTrace, value = "false"),
		@TestProperties(name = IocConfigurationConstants.MonitorBeansActive, value = "false"),
		@TestProperties(name = QueryMassdataTest.DURATION_PER_TEST, value = "300"),
		@TestProperties(name = QueryMassdataTest.QUERY_PAGE_SIZE, value = "500"),
		@TestProperties(name = QueryMassdataTest.THREAD_COUNT, value = "10"),
		@TestProperties(name = PersistenceConfigurationConstants.DatabasePoolMaxUnused, value = "${"
				+ QueryMassdataTest.THREAD_COUNT + "}"),
		@TestProperties(name = PersistenceConfigurationConstants.DatabasePoolMaxUsed, value = "${"
				+ QueryMassdataTest.THREAD_COUNT + "}"),
		@TestProperties(name = QueryMassDataModule.ROW_COUNT, value = "2000000"),
		@TestProperties(name = CacheConfigurationConstants.CacheLruThreshold, value = "${"
				+ QueryMassDataModule.ROW_COUNT + "}"),
		@TestProperties(name = ServiceConfigurationConstants.mappingFile, value = "com/koch/ambeth/query/QueryMassdata_orm.xml"),
		@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "false"),
		@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "false"),
		@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "false"),
		@TestProperties(name = QueryConfigurationConstants.PagingPrefetchBehavior, value = "true"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.cache.DefaultPersistenceCacheRetriever", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.cache.FirstLevelCacheManager", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.filter.PagingQuery", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.orm.XmlDatabaseMapper", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.persistence.EntityLoader", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.persistence.jdbc.JdbcTable", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.persistence.jdbc.JDBCDatabaseWrapper", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.persistence.jdbc.connection.LogPreparedStatementInterceptor", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.persistence.jdbc.connection.LogStatementInterceptor", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.persistence.jdbc.database.JdbcTransaction", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.proxy.AbstractCascadePostProcessor", value = "INFO"),
		@TestProperties(name = "ambeth.log.level.com.koch.ambeth.service.PersistenceMergeServiceExtension", value = "INFO") })
public class QueryMassdataTest extends AbstractInformationBusWithPersistenceTest {
	public static final String THREAD_COUNT = "QueryMassdataTest.threads";

	public static final String DURATION_PER_TEST = "QueryMassdataTest.duration.pertest";

	public static final String QUERY_PAGE_SIZE = "QueryMassdataTest.query.pagesize";

	protected static final String paramName1 = "param.1";
	protected static final String paramName2 = "param.2";
	protected static final String columnName1 = "ID";
	protected static final String columnName2 = "VERSION";
	protected static final String columnName3 = "FK";

	@LogInstance
	private ILogger log;

	@Autowired
	protected Connection connection;

	@Autowired
	protected IConnectionExtension connectionExtension;

	@Property(name = PersistenceJdbcConfigurationConstants.DatabaseSchemaName)
	protected String[] schemaNames;

	@Property(name = DURATION_PER_TEST)
	protected int duration;

	@Property(name = THREAD_COUNT)
	protected int threads;

	@Property(name = QUERY_PAGE_SIZE)
	protected int size;

	@Property(name = QueryMassDataModule.ROW_COUNT)
	protected int dataCount;

	protected HashMap<Object, Object> nameToValueMap = new HashMap<>();

	@Test
	public void massDataReadFalseFalseFalse() throws Exception {
		massDataReadIntern();
	}

	@Test
	@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true")
	public void massDataReadFalseFalseTrue() throws Exception {
		massDataReadIntern();
	}

	@Test
	@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true")
	public void massDataReadFalseTrueFalse() throws Exception {
		massDataReadIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true"),
			@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true") })
	public void massDataReadFalseTrueTrue() throws Exception {
		massDataReadIntern();
	}

	@Test
	@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true")
	public void massDataReadTrueFalseFalse() throws Exception {
		massDataReadIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true"),
			@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true") })
	public void massDataReadTrueFalseTrue() throws Exception {
		massDataReadIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true"),
			@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true") })
	public void massDataReadTrueTrueFalse() throws Exception {
		massDataReadIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true"),
			@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true"),
			@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true") })
	public void massDataReadTrueTrueTrue() throws Exception {
		massDataReadIntern();
	}

	@Test
	public void massDataReadAll() {
		IQueryBuilder<QueryEntity> qb = queryBuilderFactory.create(QueryEntity.class);
		IQuery<QueryEntity> query = qb.build();
		IList<QueryEntity> all = query.retrieve();
		System.out.println(all.size());
	}

	protected void flushSharedPool() {
		transaction.processAndCommit(new DatabaseCallback() {
			@Override
			public void callback(ILinkedMap<Object, IDatabase> persistenceUnitToDatabaseMap)
					throws Exception {
				Connection connection = beanContext.getService(Connection.class);
				Statement stm = connection.createStatement();
				try {
					stm.execute("alter system flush shared_pool");
				}
				catch (PersistenceException e) {
					if (e.getCause() instanceof SQLException
							&& "42000".equals(((SQLException) e.getCause()).getSQLState())) {
						return;
					}
					throw e;
				}
				finally {
					JdbcUtil.close(stm);
				}
			}
		});
	}

	protected void massDataReadIntern() throws Exception {
		flushSharedPool();

		final boolean useSecondLevelCache = Boolean
				.parseBoolean(beanContext.getService(IProperties.class)
						.getString(CacheConfigurationConstants.SecondLevelCacheActive));

		final IFilterToQueryBuilder ftqb = beanContext.getService(IFilterToQueryBuilder.class);

		final ICacheContext cacheContext = beanContext.getService(ICacheContext.class);

		final ICacheProvider cacheProvider = beanContext
				.getService(CacheNamedBeans.CacheProviderThreadLocal, ICacheProvider.class);

		final FilterDescriptor<QueryEntity> fd = new FilterDescriptor<>(QueryEntity.class);

		final SortDescriptor sd1 = new SortDescriptor();
		sd1.setMember("Id");
		sd1.setSortDirection(SortDirection.DESCENDING);

		final SortDescriptor sd2 = new SortDescriptor();
		sd2.setMember("Version");
		sd2.setSortDirection(SortDirection.ASCENDING);

		final int lastPageNumber = dataCount / size,
				lastPageNumberSize = dataCount - lastPageNumber * size;

		final ParamHolder<Integer> overallQueryCountIndex = new ParamHolder<>();
		overallQueryCountIndex.setValue(new Integer(0));

		final ReentrantLock oqciLock = new ReentrantLock();

		long start = System.currentTimeMillis();
		long startCpuUsage = ProcessIdHelper.getCumulatedCpuUsage();

		final long finishTime = start + duration * 1000; // Duration measured in seconds
		long lastPrint = start;

		final CountDownLatch latch = new CountDownLatch(threads);

		final ParamHolder<Throwable> throwableHolder = new ParamHolder<>();

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					IRootCache rootCache = beanContext.getService("rootCache", IRootCache.class);
					IThreadLocalCleanupController threadLocalCleanupController = beanContext
							.getService(IThreadLocalCleanupController.class);

					final IPagingQuery<QueryEntity> randomPagingQuery = ftqb.buildQuery(fd,
							new ISortDescriptor[] { sd1, sd2 });

					while (System.currentTimeMillis() <= finishTime && throwableHolder.getValue() == null) {
						IStateRollback rollback = cacheContext.pushCache(cacheProvider);
						try {
							final PagingRequest randomPReq = new PagingRequest();
							randomPReq.setNumber((int) (Math.random() * dataCount / size));
							randomPReq.setSize(size);

							IPagingResponse<QueryEntity> response = randomPagingQuery.retrieve(randomPReq);
							List<QueryEntity> result = response.getResult();

							Assert.assertEquals(randomPReq.getNumber(), response.getNumber());
							if (response.getNumber() == lastPageNumber) {
								Assert.assertEquals(lastPageNumberSize, result.size());
							}
							else {
								Assert.assertEquals(size, result.size());
							}
							QueryEntity objectBefore = result.get(0);
							for (int a = 1, resultSize = result.size(); a < resultSize; a++) {
								QueryEntity objectCurrent = result.get(a);
								// IDs descending
								// Version ascending
								Assert.assertFalse(objectBefore.equals(objectCurrent));
								Assert.assertTrue(objectBefore.getId() >= objectCurrent.getId());
								if (objectBefore.getId() == objectCurrent.getId()) {
									Assert.assertTrue(objectBefore.getVersion() <= objectCurrent.getVersion());
								}
								objectBefore = objectCurrent;
							}
							oqciLock.lock();
							try {
								overallQueryCountIndex
										.setValue(new Integer(overallQueryCountIndex.getValue().intValue() + 1));
							}
							finally {
								oqciLock.unlock();
							}
						}
						finally {
							rollback.rollback();
							if (!useSecondLevelCache) {
								rootCache.clear();
							}
							threadLocalCleanupController.cleanupThreadLocal();
						}
					}
					randomPagingQuery.dispose();
				}
				catch (Throwable e) {
					throwableHolder.setValue(e);
					throw RuntimeExceptionUtil.mask(e);
				}
				finally {
					latch.countDown();
				}
			}
		};

		for (int threadIndex = threads; threadIndex-- > 0;) {
			Thread thread = new Thread(runnable);
			thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
			thread.start();
		}

		double lastOverallCount = overallQueryCountIndex.getValue().intValue();
		while (!latch.await(5000, TimeUnit.MILLISECONDS)) {
			Throwable e = throwableHolder.getValue();
			if (e != null) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
			if (System.currentTimeMillis() - lastPrint >= 5000) {
				long beforeLastPrint = lastPrint;
				double overallCount = overallQueryCountIndex.getValue().intValue();
				lastPrint = System.currentTimeMillis();

				double intervalCount = overallCount - lastOverallCount;

				ValueWithTimeUnit timeSpent = new ValueWithTimeUnit(lastPrint - start);
				ValueWithTimeUnit timeSpentLastInterval = new ValueWithTimeUnit(
						lastPrint - beforeLastPrint);
				ValueWithTimeUnit timeSpentPerExecution = new ValueWithTimeUnit(
						timeSpentLastInterval.getValue() / intervalCount);
				ValueWithTimeUnit timeSpentPerLoadedEntity = new ValueWithTimeUnit(
						timeSpentLastInterval.getValue() / (intervalCount * size));

				log.info(lastPrint - start + " ms for " + overallCount + " queries ("
						+ new ValueWithTimeUnit(
								timeSpent.withTimeUnit(TimeUnit.MILLISECONDS).getValue() / overallCount)
										.toNonZeroValue()
						+ " per query, last interval overall: " + timeSpentPerExecution.toNonZeroValue()
						+ " per query, " + timeSpentPerLoadedEntity.toNonZeroValue() + " per entity");
				lastOverallCount = overallCount;
			}
		}
		long end = System.currentTimeMillis();
		ValueWithTimeUnit timeSpent = new ValueWithTimeUnit(end - start);
		int overallCount = overallQueryCountIndex.getValue().intValue();
		ValueWithTimeUnit timeSpentPerExecution = new ValueWithTimeUnit(
				timeSpent.getValue() / overallCount);
		ValueWithTimeUnit timeSpentPerLoadedEntity = new ValueWithTimeUnit(
				timeSpent.getValue() / (overallCount * size));

		log.info(timeSpent.toNonZeroValue() + " for " + overallCount + " queries distributed among "
				+ threads + " threads in parallel (" + timeSpentPerExecution.toNonZeroValue()
				+ " per query, " + timeSpentPerLoadedEntity.toNonZeroValue() + " per entity)");
		ValueWithTimeUnit cpuUsage = new ValueWithTimeUnit(
				ProcessIdHelper.getCumulatedCpuUsage() - startCpuUsage);
		toMeasurementString("Read Data", timeSpent, overallCount, timeSpentPerExecution, cpuUsage);
	}

	private static class ValueWithTimeUnit {
		private static final HashMap<TimeUnit, String> timeUnitToStringMap = new HashMap<>();

		private static final HashMap<TimeUnit, Double> timeUnitToNanoFactorMap = new HashMap<>();

		static {
			timeUnitToStringMap.put(TimeUnit.SECONDS, "s");
			timeUnitToStringMap.put(TimeUnit.MILLISECONDS, "ms");
			timeUnitToStringMap.put(TimeUnit.MICROSECONDS, "µs");
			timeUnitToStringMap.put(TimeUnit.NANOSECONDS, "ns");

			timeUnitToNanoFactorMap.put(TimeUnit.SECONDS, new Double(1000L * 1000L * 1000L));
			timeUnitToNanoFactorMap.put(TimeUnit.MILLISECONDS, new Double(1000L * 1000L));
			timeUnitToNanoFactorMap.put(TimeUnit.MICROSECONDS, new Double(1000L));
			timeUnitToNanoFactorMap.put(TimeUnit.NANOSECONDS, new Double(1));
		}

		private final double value;

		private final TimeUnit timeUnit;

		public ValueWithTimeUnit(double value) {
			this(value, TimeUnit.MILLISECONDS);
		}

		public ValueWithTimeUnit(double value, TimeUnit timeUnit) {
			super();
			this.value = value;
			this.timeUnit = timeUnit;
		}

		public TimeUnit getTimeUnit() {
			return timeUnit;
		}

		public double getValue() {
			return value;
		}

		@Override
		public String toString() {
			return new DecimalFormat("#.#########").format(getValue()) + ' '
					+ timeUnitToStringMap.get(getTimeUnit());
		}

		public ValueWithTimeUnit toNonZeroValue() {
			if (getValue() >= 1) {
				return this;
			}
			Double currNanoFactor = timeUnitToNanoFactorMap.get(getTimeUnit());
			double newValue = (long) (currNanoFactor.doubleValue() * getValue());
			TimeUnit newTimeUnit = TimeUnit.NANOSECONDS;
			if (newValue > 100) {
				newValue /= 1000.0;
				newTimeUnit = TimeUnit.MICROSECONDS;
			}
			if (newValue > 100) {
				newValue /= 1000.0;
				newTimeUnit = TimeUnit.MILLISECONDS;
			}
			if (newValue > 100) {
				newValue /= 1000.0;
				newTimeUnit = TimeUnit.SECONDS;
			}
			return new ValueWithTimeUnit(newValue, newTimeUnit);
		}

		public ValueWithTimeUnit withTimeUnit(TimeUnit timeUnit) {
			if (getTimeUnit() == timeUnit) {
				return this;
			}
			Double currNanoFactor = timeUnitToNanoFactorMap.get(getTimeUnit());
			Double givenNanoFactor = timeUnitToNanoFactorMap.get(timeUnit);
			return new ValueWithTimeUnit(
					getValue() * currNanoFactor.doubleValue() / givenNanoFactor.doubleValue(), timeUnit);
		}
	}

	protected void toMeasurementString(String name, ValueWithTimeUnit timeSpent, int overallCount,
			ValueWithTimeUnit timeSpentPerExecution, ValueWithTimeUnit cpuUsage) {
		String queryCacheActive = beanContext.getService(IProperties.class)
				.getString(PersistenceConfigurationConstants.QueryCacheActive);
		String secondLevelCacheActive = beanContext.getService(IProperties.class)
				.getString(CacheConfigurationConstants.SecondLevelCacheActive);
		String serviceResultCacheActive = beanContext.getService(IProperties.class)
				.getString(CacheConfigurationConstants.ServiceResultCacheActive);
		final String prefix = name + " 2ndLevelCache(" + secondLevelCacheActive + ") QueryCache("
				+ queryCacheActive + ") ServiceCache(" + serviceResultCacheActive + ")";
		MeasurementUtil.logMeasurement(prefix + " Time spent for scenario (ms)", timeSpent);
		MeasurementUtil.logMeasurement(prefix + " Sum of executions", overallCount);
		MeasurementUtil.logMeasurement(prefix + " Time spent per execution (ms)",
				timeSpentPerExecution);
		MeasurementUtil.logMeasurement(prefix + " CPU usage for scenario (%)",
				(int) (100 * cpuUsage.withTimeUnit(TimeUnit.MILLISECONDS).getValue()
						/ timeSpent.withTimeUnit(TimeUnit.MILLISECONDS).getValue()));

		measurement.log(prefix + " Time spent for scenario (ms)", timeSpent);
		measurement.log(prefix + " Sum of executions", overallCount);
		measurement.log(prefix + " Time spent per execution (ms)", timeSpentPerExecution);
		measurement.log(prefix + " CPU usage for scenario (%)",
				(int) (100 * cpuUsage.withTimeUnit(TimeUnit.MILLISECONDS).getValue()
						/ timeSpent.withTimeUnit(TimeUnit.MILLISECONDS).getValue()));

		transaction.processAndCommit(new DatabaseCallback() {
			@Override
			public void callback(ILinkedMap<Object, IDatabase> persistenceUnitToDatabaseMap)
					throws Exception {
				PreparedStatement pstm = null;
				ResultSet rs = null;
				Array array = null;
				try {
					pstm = connection.prepareStatement(
							"SELECT sql_text,cpu_time/1000000 cpu_time,elapsed_time/1000000 elapsed_time,executions,parse_calls,disk_reads,buffer_gets,rows_processed FROM v$sqlarea WHERE PARSING_SCHEMA_NAME IN (SELECT COLUMN_VALUE FROM TABLE(?)) AND MODULE='JDBC Thin Client' ORDER BY executions DESC");
					String[] uppercaseSchemaNames = new String[schemaNames.length];
					for (int a = schemaNames.length; a-- > 0;) {
						uppercaseSchemaNames[a] = schemaNames[a].toUpperCase();
					}
					array = connectionExtension.createJDBCArray(String.class, uppercaseSchemaNames);
					pstm.setArray(1, array);
					rs = pstm.executeQuery();
					final int columnCount = rs.getMetaData().getColumnCount();
					while (rs.next()) {
						final ResultSet fRs = rs;
						measurement.log(prefix + " database", new Object() {
							@Override
							public String toString() {
								for (int a = 0; a < columnCount; a++) {
									try {
										String name = fRs.getMetaData().getColumnName(a + 1);
										Object value = fRs.getObject(a + 1);
										MeasurementUtil.logMeasurement(name, value);
										measurement.log(name, value);
									}
									catch (SQLException e) {
										throw RuntimeExceptionUtil.mask(e);
									}
								}
								return "";
							}
						});
					}
				}
				finally {
					JdbcUtil.close(array);
					JdbcUtil.close(pstm, rs);
				}
			}
		});
	}

	@Test
	public void massDataWriteFalseFalseFalse() throws Exception {
		massDataWriteIntern();
	}

	@Test
	@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true")
	public void massDataWriteFalseFalseTrue() throws Exception {
		massDataWriteIntern();
	}

	@Test
	@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true")
	public void massDataWriteFalseTrueFalse() throws Exception {
		massDataWriteIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true"),
			@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true") })
	public void massDataWriteFalseTrueTrue() throws Exception {
		massDataWriteIntern();
	}

	@Test
	@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true")
	public void massDataWriteTrueFalseFalse() throws Exception {
		massDataWriteIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true"),
			@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true") })
	public void massDataWriteTrueFalseTrue() throws Exception {
		massDataWriteIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true"),
			@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true") })
	public void massDataWriteTrueTrueFalse() throws Exception {
		massDataWriteIntern();
	}

	@Test
	@TestPropertiesList({
			@TestProperties(name = CacheConfigurationConstants.SecondLevelCacheActive, value = "true"),
			@TestProperties(name = CacheConfigurationConstants.ServiceResultCacheActive, value = "true"),
			@TestProperties(name = PersistenceConfigurationConstants.QueryCacheActive, value = "true") })
	public void massDataWriteTrueTrueTrue() throws Exception {
		massDataWriteIntern();
	}

	@Test
	public void testForToManyPrepStmtParams() {
		IQueryBuilder<QueryEntity> queryBuilder = queryBuilderFactory.create(QueryEntity.class);
		IQuery<QueryEntity> query = queryBuilder.build();
		query.retrieve();
	}

	protected void massDataWriteIntern() throws Exception {
		flushSharedPool();

		final boolean useSecondLevelCache = Boolean
				.parseBoolean(beanContext.getService(IProperties.class)
						.getString(CacheConfigurationConstants.SecondLevelCacheActive));

		final IFilterToQueryBuilder ftqb = beanContext.getService(IFilterToQueryBuilder.class);

		final IQueryEntityCRUD queryEntityCRUD = beanContext.getService(IQueryEntityCRUD.class);

		final ICacheContext cacheContext = beanContext.getService(ICacheContext.class);

		final ICacheProvider cacheProvider = beanContext
				.getService(CacheNamedBeans.CacheProviderThreadLocal, ICacheProvider.class);

		final FilterDescriptor<QueryEntity> fd = new FilterDescriptor<>(QueryEntity.class);

		final SortDescriptor sd1 = new SortDescriptor();
		sd1.setMember("Id");
		sd1.setSortDirection(SortDirection.DESCENDING);

		final SortDescriptor sd2 = new SortDescriptor();
		sd2.setMember("Version");
		sd2.setSortDirection(SortDirection.ASCENDING);

		final int lastPageNumber = dataCount / size,
				lastPageNumberSize = dataCount - lastPageNumber * size;

		final ParamHolder<Integer> overallQueryCountIndex = new ParamHolder<>();
		overallQueryCountIndex.setValue(new Integer(0));

		final ReentrantLock oqciLock = new ReentrantLock();

		long start = System.currentTimeMillis();
		long startCpuUsage = ProcessIdHelper.getCumulatedCpuUsage();
		final long finishTime = start + duration * 1000; // Duration measured in seconds
		long lastPrint = start;

		final CountDownLatch latch = new CountDownLatch(threads);

		final ParamHolder<Throwable> throwableHolder = new ParamHolder<>();

		final boolean[] usedPages = new boolean[dataCount / size];
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					IRootCache rootCache = beanContext.getService("rootCache", IRootCache.class);
					IThreadLocalCleanupController threadLocalCleanupController = beanContext
							.getService(IThreadLocalCleanupController.class);
					while (System.currentTimeMillis() <= finishTime && throwableHolder.getValue() == null) {
						try {
							int usedPage;
							while (true) {
								oqciLock.lock();
								try {
									usedPage = (int) (Math.random() * usedPages.length);
									if (!usedPages[usedPage]) {
										usedPages[usedPage] = true;
										break;
									}
								}
								finally {
									oqciLock.unlock();
								}
							}
							try {
								final PagingRequest randomPReq = new PagingRequest();
								randomPReq.setNumber(usedPage);
								randomPReq.setSize(size);
								final IPagingQuery<QueryEntity> randomPagingQuery = ftqb.buildQuery(fd,
										new ISortDescriptor[] { sd1, sd2 });

								IPagingResponse<QueryEntity> response;
								IStateRollback rollback = cacheContext.pushCache(cacheProvider);
								try {
									response = randomPagingQuery.retrieve(randomPReq);
								}
								finally {
									rollback.rollback();
								}
								randomPagingQuery.dispose();

								List<QueryEntity> result = response.getResult();

								Assert.assertEquals(randomPReq.getNumber(), response.getNumber());
								if (response.getNumber() == lastPageNumber) {
									Assert.assertEquals(lastPageNumberSize, result.size());
								}
								else {
									Assert.assertEquals(size, result.size());
								}
								QueryEntity objectBefore = result.get(0);
								for (int a = 1, resultSize = result.size(); a < resultSize; a++) {
									QueryEntity objectCurrent = result.get(a);
									// IDs descending
									// Version ascending
									Assert.assertFalse(objectBefore.equals(objectCurrent));
									Assert.assertTrue(objectBefore.getId() >= objectCurrent.getId());
									if (objectBefore.getId() == objectCurrent.getId()) {
										Assert.assertTrue(objectBefore.getVersion() <= objectCurrent.getVersion());
									}
									objectCurrent.setName1("1_" + Math.random());
									objectCurrent.setName2("2_" + Math.random() + "");
									objectBefore = objectCurrent;
								}
								try {
									queryEntityCRUD.IwantAFunnySaveMethod(result);
								}
								catch (OptimisticLockException e) {
									// Intended blank
									continue;
								}
								catch (PessimisticLockException e) {
									// Intended blank
									continue;
								}
								oqciLock.lock();
								try {
									overallQueryCountIndex
											.setValue(new Integer(overallQueryCountIndex.getValue().intValue() + 1));
								}
								finally {
									oqciLock.unlock();
								}
								// if (Math.random() < 0.5)
								// {
								// for (int a = result.size(); a-- > 0;)
								// {
								// QueryEntity queryEntity = (QueryEntity) result.get(a);
								// queryEntity.setName1("1_" + Math.random());
								// queryEntity.setName2("2_" + Math.random());
								// }
								// queryEntityCRUD.IwantAFunnySaveMethod(result);
								// }
							}
							finally {
								oqciLock.lock();
								try {
									usedPages[usedPage] = false;
								}
								finally {
									oqciLock.unlock();
								}
							}
						}
						finally {
							if (!useSecondLevelCache) {
								rootCache.clear();
							}
							threadLocalCleanupController.cleanupThreadLocal();
						}
					}
				}
				catch (Throwable e) {
					throwableHolder.setValue(e);
					throw RuntimeExceptionUtil.mask(e);
				}
				finally {
					latch.countDown();
				}
			}
		};

		for (int threadIndex = threads; threadIndex-- > 0;) {
			Thread thread = new Thread(runnable);
			thread.start();
		}

		double lastOverallCount = overallQueryCountIndex.getValue().intValue();
		while (!latch.await(1000, TimeUnit.MILLISECONDS)) {
			Throwable e = throwableHolder.getValue();
			if (e != null) {
				throw new RuntimeException(e);
			}
			if (System.currentTimeMillis() - lastPrint >= 5000) {
				long beforeLastPrint = lastPrint;
				double overallCount = overallQueryCountIndex.getValue().intValue();
				lastPrint = System.currentTimeMillis();

				double intervalCount = overallCount - lastOverallCount;

				ValueWithTimeUnit timeSpent = new ValueWithTimeUnit(lastPrint - start);
				ValueWithTimeUnit timeSpentLastInterval = new ValueWithTimeUnit(
						lastPrint - beforeLastPrint);
				ValueWithTimeUnit timeSpentPerExecution = new ValueWithTimeUnit(
						timeSpentLastInterval.getValue() / intervalCount);
				ValueWithTimeUnit timeSpentPerLoadedEntity = new ValueWithTimeUnit(
						timeSpentLastInterval.getValue() / (intervalCount * size));

				log.info(lastPrint - start + " ms for " + overallCount + " queries ("
						+ new ValueWithTimeUnit(
								timeSpent.withTimeUnit(TimeUnit.MILLISECONDS).getValue() / overallCount)
										.toNonZeroValue()
						+ " per query, last interval overall: " + timeSpentPerExecution.toNonZeroValue()
						+ " per query, " + timeSpentPerLoadedEntity.toNonZeroValue() + " per entity");
				lastOverallCount = overallCount;
			}
		}
		Throwable e = throwableHolder.getValue();
		if (e != null) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException(e);
		}
		long end = System.currentTimeMillis();
		ValueWithTimeUnit timeSpent = new ValueWithTimeUnit(end - start);
		int overallCount = overallQueryCountIndex.getValue().intValue();
		ValueWithTimeUnit timeSpentPerExecution = new ValueWithTimeUnit(
				timeSpent.getValue() / overallCount);
		ValueWithTimeUnit timeSpentPerLoadedEntity = new ValueWithTimeUnit(
				timeSpent.getValue() / (overallCount * size));

		log.info(timeSpent.toNonZeroValue() + " for " + overallCount + " queries distributed among "
				+ threads + " threads in parallel (" + timeSpentPerExecution.toNonZeroValue()
				+ " per query, " + timeSpentPerLoadedEntity.toNonZeroValue() + " per entity)");
		ValueWithTimeUnit cpuUsage = new ValueWithTimeUnit(
				ProcessIdHelper.getCumulatedCpuUsage() - startCpuUsage);
		toMeasurementString("Write Data", timeSpent, overallCount, timeSpentPerExecution, cpuUsage);
	}
}
