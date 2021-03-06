package com.koch.ambeth.persistence.sql;

import java.util.Iterator;

/*-
 * #%L
 * jambeth-persistence
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

import com.koch.ambeth.ioc.IInitializingBean;
import com.koch.ambeth.query.persistence.IDataCursor;
import com.koch.ambeth.query.persistence.IDataItem;
import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.collections.IMap;

public class ResultSetDataCursor
		implements IDataCursor, IDataItem, IInitializingBean, Iterator<IDataItem> {
	protected IResultSet resultSet;

	protected Iterator<Object[]> resultSetIter;

	protected IMap<String, Integer> propertyToColIndexMap;

	protected Object[] data;

	@Override
	public void afterPropertiesSet() {
		ParamChecker.assertNotNull(resultSet, "ResultSet");
		ParamChecker.assertNotNull(propertyToColIndexMap, "PropertyToColIndexMap");
		data = new Object[propertyToColIndexMap.size()];
	}

	public IResultSet getResultSet() {
		return resultSet;
	}

	public void setResultSet(IResultSet resultSet) {
		this.resultSet = resultSet;
	}

	public void setPropertyToColIndexMap(IMap<String, Integer> propertyToColIndexMap) {
		this.propertyToColIndexMap = propertyToColIndexMap;
	}

	@Override
	public int getFieldCount() {
		return propertyToColIndexMap.size();
	}

	@Override
	public Object getValue(String propertyName) {
		Integer index = propertyToColIndexMap.get(propertyName);
		return data[index];
	}

	@Override
	public Object getValue(int index) {
		return data[index];
	}

	@Override
	public boolean hasNext() {
		if (resultSetIter == null) {
			resultSetIter = resultSet.iterator();
		}
		return resultSetIter.hasNext();
	}

	@Override
	public IDataItem next() {
		if (resultSetIter == null) {
			resultSetIter = resultSet.iterator();
		}
		data = resultSetIter.next();
		if (data == null) {
			return null;
		}
		return this;
	}

	@Override
	public void dispose() {
		if (resultSet != null) {
			resultSet.dispose();
			resultSet = null;
		}
		propertyToColIndexMap = null;
	}

	@Override
	public Iterator<IDataItem> iterator() {
		return this;
	}
}
