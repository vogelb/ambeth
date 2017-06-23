package com.koch.ambeth.merge.changecontroller;

/*-
 * #%L
 * jambeth-merge
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.collections.ArrayList;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;
import com.koch.ambeth.util.threading.IBackgroundWorkerDelegate;
import com.koch.ambeth.util.threading.IBackgroundWorkerParamDelegate;

public class CacheView implements ICacheView {
	// objects contains all new objects
	protected final List<Object> newObjects, oldObjects;

	// views contains a map from an interface to all objects whose class implements it. Entries are
	// created lazily below.
	protected final HashMap<Class<?>, Collection<?>> newViews =
			new HashMap<>();
	// The entries in the oldViews lists correlate to those in the newViews list
	protected final HashMap<Class<?>, Collection<?>> oldViews =
			new HashMap<>();

	protected HashMap<Object, Object> customStateMap;

	protected ArrayList<IBackgroundWorkerDelegate> customRunnables;

	public CacheView(List<Object> newObjects, List<Object> oldObjects) {
		this.newObjects = newObjects;
		this.oldObjects = oldObjects;
	}

	/**
	 * Returns a list of new objects that have the given interface as type.
	 *
	 * @param clazz The interface that the object should implemented by the objects
	 * @return a list of new objects that implement the interface, never <code>null</code>
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getNewObjectsOfClass(Class<T> clazz) {
		assureObjectsOfClass(clazz);
		return (Collection<T>) newViews.get(clazz);
	}

	/**
	 * Returns a list of new objects that have the given interface as type.
	 *
	 * @param clazz The interface that the object should implemented by the objects
	 * @return a list of new objects that implement the interface, never <code>null</code>
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Collection<T> getOldObjectsOfClass(Class<T> clazz) {
		assureObjectsOfClass(clazz);
		return (Collection<T>) oldViews.get(clazz);
	}

	/**
	 * Make sure that all elements of the given class are computed
	 *
	 * @param clazz
	 */
	protected void assureObjectsOfClass(Class<?> clazz) {
		// We just check for the entry in newViews because newViews and oldViews are updated
		// simultaneously.
		if (!newViews.containsKey(clazz)) {
			createObjectsOfClass(clazz);
		}
	}

	protected void createObjectsOfClass(Class<?> clazz) {
		ParamChecker.assertTrue(clazz.isInterface(), clazz.getName() + " is not an interface");

		ArrayList<Object> newResult = new ArrayList<>();
		ArrayList<Object> oldResult = new ArrayList<>();
		int size = newObjects.size();
		// Filter all objects that are an instance of the given class
		for (int index = 0; index < size; index += 1) {
			Object newEntity = newObjects.get(index);
			Object oldEntity = oldObjects.get(index);
			// just get one of both that is not null (there must be at least one)
			Object anyEntity = newEntity != null ? newEntity : oldEntity;
			if (clazz.isInstance(anyEntity)) {
				newResult.add(newEntity);
				oldResult.add(oldEntity);
			}
		}
		newViews.put(clazz, newResult);
		oldViews.put(clazz, oldResult);
	}

	@Override
	public Object getCustomState(Object key) {
		if (customStateMap == null) {
			return null;
		}
		return customStateMap.get(key);
	}

	@Override
	public void setCustomState(Object key, Object value) {
		if (customStateMap == null) {
			customStateMap = new HashMap<>();
		}
		customStateMap.put(key, value);
	}

	@Override
	public void queueRunnable(IBackgroundWorkerDelegate runnable) {
		if (customRunnables == null) {
			customRunnables = new ArrayList<>();
		}
		customRunnables.add(runnable);
	}

	@SuppressWarnings("unchecked")
	public void processRunnables() {
		if (customRunnables == null) {
			return;
		}
		// while loop because a runnable could queue cascading runnables
		while (!customRunnables.isEmpty()) {
			IBackgroundWorkerParamDelegate<ICacheView>[] runnables =
					customRunnables.toArray(IBackgroundWorkerParamDelegate.class);
			customRunnables.clear();
			try {
				for (IBackgroundWorkerParamDelegate<ICacheView> runnable : runnables) {
					runnable.invoke(this);
				}
			}
			catch (Throwable e) {
				throw RuntimeExceptionUtil.mask(e);
			}
		}
	}
}
