package com.koch.ambeth.query.inmemory.builder;

/*-
 * #%L
 * jambeth-query-inmemory
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

import javax.persistence.criteria.JoinType;

import com.koch.ambeth.ioc.IBeanRuntime;
import com.koch.ambeth.ioc.IServiceContext;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.config.Property;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.query.IOperand;
import com.koch.ambeth.query.IOperator;
import com.koch.ambeth.query.IQuery;
import com.koch.ambeth.query.IQueryBuilder;
import com.koch.ambeth.query.ISqlJoin;
import com.koch.ambeth.query.ISubQuery;
import com.koch.ambeth.query.OrderByType;
import com.koch.ambeth.query.filter.IPagingQuery;
import com.koch.ambeth.query.inmemory.bool.AndOperator;
import com.koch.ambeth.query.inmemory.bool.FalseOperator;
import com.koch.ambeth.query.inmemory.bool.IsNotNullOperator;
import com.koch.ambeth.query.inmemory.bool.IsNullOperator;
import com.koch.ambeth.query.inmemory.bool.OrOperator;
import com.koch.ambeth.query.inmemory.bool.TrueOperator;
import com.koch.ambeth.query.inmemory.ordinal.IsGreaterThanOperator;
import com.koch.ambeth.query.inmemory.ordinal.IsGreaterThanOrEqualToOperator;
import com.koch.ambeth.query.inmemory.ordinal.IsLessThanOperator;
import com.koch.ambeth.query.inmemory.ordinal.IsLessThanOrEqualToOperator;
import com.koch.ambeth.query.inmemory.text.EndsWithOperator;
import com.koch.ambeth.query.inmemory.text.IsEqualToOperator;
import com.koch.ambeth.query.inmemory.text.IsNotEqualToOperator;
import com.koch.ambeth.query.inmemory.text.StartsWithOperator;
import com.koch.ambeth.util.IParamHolder;
import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;

public class InMemoryQueryBuilder<T> implements IQueryBuilder<T> {
	@SuppressWarnings("unused")
	@LogInstance
	private ILogger log;

	@Autowired
	protected IServiceContext beanContext;

	@Property
	protected Class<?> entityType;

	@Override
	public void dispose() {
		// Intended blank
	}

	@Override
	public Class<?> getEntityType() {
		return entityType;
	}

	protected IOperator createUnaryOperator(Class<? extends IOperator> operatorType, Object operand,
			Boolean caseSensitive) {
		ParamChecker.assertParamNotNull(operatorType, "operatorType");
		ParamChecker.assertParamNotNull(operand, "operand");
		try {
			IBeanRuntime<? extends IOperator> operatorBC = beanContext.registerBean(operatorType)
					.propertyValue("Operand", operand);
			if (caseSensitive != null) {
				operatorBC.propertyValue("CaseSensitive", caseSensitive);
			}
			return operatorBC.finish();
		}
		catch (Throwable e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	protected IOperator createBinaryOperator(Class<? extends IOperator> operatorType,
			Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		ParamChecker.assertParamNotNull(operatorType, "operatorType");
		ParamChecker.assertParamNotNull(leftOperand, "leftOperand");
		ParamChecker.assertParamNotNull(rightOperand, "rightOperand");
		try {
			IBeanRuntime<? extends IOperator> operatorBC = beanContext.registerBean(operatorType)
					.propertyValue("LeftOperand", leftOperand).propertyValue("RightOperand", rightOperand);
			if (caseSensitive != null) {
				operatorBC.propertyValue("CaseSensitive", caseSensitive);
			}
			return operatorBC.finish();
		}
		catch (Throwable e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	protected IOperator createManyPlaceOperator(Class<? extends IOperator> operatorType,
			IOperand... operands) {
		ParamChecker.assertParamNotNull(operatorType, "operatorType");
		ParamChecker.assertParamNotNull(operands, "operands");
		try {
			IBeanRuntime<? extends IOperator> operatorBC = beanContext.registerBean(operatorType)
					.propertyValue("Operands", operands);
			return operatorBC.finish();
		}
		catch (Throwable e) {
			throw RuntimeExceptionUtil.mask(e);
		}
	}

	@Override
	public IOperator and(IOperand leftOperand, IOperand rightOperand) {
		return createManyPlaceOperator(AndOperator.class, leftOperand, rightOperand);
	}

	@Override
	public IOperator and(IOperand... operands) {
		return createManyPlaceOperator(AndOperator.class, operands);
	}

	@Override
	public IOperator or(IOperand leftOperand, IOperand rightOperand) {
		return createManyPlaceOperator(OrOperator.class, leftOperand, rightOperand);
	}

	@Override
	public IOperator or(IOperand... operands) {
		return createManyPlaceOperator(OrOperator.class, operands);
	}

	@Override
	public IOperand overlaps(Object leftOperand, Object rightOperand) {
		return null;
	}

	@Override
	public IOperand timeUnitMultipliedInterval(IOperand timeUnit, IOperand multiplicatedInterval) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator trueOperator() {
		return beanContext.registerBean(TrueOperator.class).finish();
	}

	@Override
	public IOperator falseOperator() {
		return beanContext.registerBean(FalseOperator.class).finish();
	}

	@Override
	public IOperand property(String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand property(String propertyName, JoinType joinType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand property(String propertyName, JoinType joinType,
			IParamHolder<Class<?>> fieldType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand column(String columnName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand column(String columnName, ISqlJoin joinClause) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator contains(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator contains(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand difference(IOperand... operands) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator endsWith(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(EndsWithOperator.class, leftOperand, rightOperand, null);
	}

	@Override
	public IOperator endsWith(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		return createBinaryOperator(EndsWithOperator.class, leftOperand, rightOperand, caseSensitive);
	}

	@Override
	public IOperator fulltext(IOperand queryOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator fulltext(Class<?> entityType, IOperand queryOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isContainedIn(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isContainedIn(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isIn(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isIn(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isEqualTo(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(IsEqualToOperator.class, leftOperand, rightOperand, null);
	}

	@Override
	public IOperator isEqualTo(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		return createBinaryOperator(IsEqualToOperator.class, leftOperand, rightOperand, caseSensitive);
	}

	@Override
	public IOperator isGreaterThan(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(IsGreaterThanOperator.class, leftOperand, rightOperand, null);
	}

	@Override
	public IOperator isGreaterThanOrEqualTo(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(IsGreaterThanOrEqualToOperator.class, leftOperand, rightOperand,
				null);
	}

	@Override
	public IOperator isLessThan(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(IsLessThanOperator.class, leftOperand, rightOperand, null);
	}

	@Override
	public IOperator isLessThanOrEqualTo(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(IsLessThanOrEqualToOperator.class, leftOperand, rightOperand, null);
	}

	@Override
	public IOperator isNotContainedIn(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isNotContainedIn(Object leftOperand, Object rightOperand,
			Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isNotIn(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isNotIn(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isNotEqualTo(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(IsNotEqualToOperator.class, leftOperand, rightOperand, null);
	}

	@Override
	public IOperator isNotEqualTo(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		return createBinaryOperator(IsNotEqualToOperator.class, leftOperand, rightOperand,
				caseSensitive);
	}

	@Override
	public IOperator notContains(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator notContains(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator notLike(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator notLike(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator isNull(IOperand operand) {
		return createUnaryOperator(IsNullOperator.class, operand, null);
	}

	@Override
	public IOperator isNotNull(IOperand operand) {
		return createUnaryOperator(IsNotNullOperator.class, operand, null);
	}

	@Override
	public IOperator like(Object leftOperand, Object rightOperand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator like(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand regexpLike(IOperand sourceString, IOperand pattern) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand regexpLike(IOperand sourceString, IOperand pattern, IOperand matchParameter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IQueryBuilder<T> limit(IOperand operand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperator startsWith(Object leftOperand, Object rightOperand) {
		return createBinaryOperator(StartsWithOperator.class, leftOperand, rightOperand, null);
	}

	@Override
	public IOperator startsWith(Object leftOperand, Object rightOperand, Boolean caseSensitive) {
		return createBinaryOperator(StartsWithOperator.class, leftOperand, rightOperand, caseSensitive);
	}

	@Override
	public IOperand value(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand valueName(String paramName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand all() {
		return trueOperator();
	}

	@Override
	public IOperand function(String functionName, IOperand... parameters) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IQueryBuilder<T> groupBy(IOperand... operand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand interval(IOperand lowerBoundary, IOperand upperBoundary) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IQueryBuilder<T> orderBy(IOperand column, OrderByType orderByType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int selectColumn(String columnName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int selectColumn(String columnName, ISqlJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int selectProperty(String propertyName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int select(IOperand operand) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISqlJoin join(Class<?> entityType, IOperand columnBase, IOperand columnJoined,
			JoinType joinType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISqlJoin join(Class<?> entityType, IOperator clause, JoinType joinType) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISqlJoin join(Class<?> entityType, IOperand columnBase, IOperand columnJoined) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISqlJoin join(Class<?> entityType, IOperator clause) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S> IOperand subQuery(ISubQuery<S> subQuery, IOperand... selectedColumns) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand sum(IOperand... summands) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IQuery<T> build() {
		return build(all());
	}

	@Override
	public IQuery<T> build(IOperand whereClause) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IQuery<T> build(IOperand whereClause, ISqlJoin... joinClauses) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPagingQuery<T> buildPaging() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPagingQuery<T> buildPaging(IOperand whereClause) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IPagingQuery<T> buildPaging(IOperand whereClause, ISqlJoin... joinClauses) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISubQuery<T> buildSubQuery() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISubQuery<T> buildSubQuery(IOperand whereClause) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ISubQuery<T> buildSubQuery(IOperand whereClause, ISqlJoin... joinClauses) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T plan() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IOperand property(Object propertyProxy) {
		throw new UnsupportedOperationException();
	}
}
