package com.koch.ambeth.informationbus.persistence.datagenerator;

/*-
 * #%L
 * jambeth-information-bus-with-persistence-test
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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.koch.ambeth.ioc.DefaultExtendableContainer;
import com.koch.ambeth.ioc.IInitializingBean;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.ioc.extendable.IExtendableContainer;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.merge.IEntityFactory;
import com.koch.ambeth.merge.proxy.IEntityMetaDataHolder;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.service.metadata.Member;
import com.koch.ambeth.util.ParamChecker;
import com.koch.ambeth.util.collections.HashMap;

public class TestDataGenerator
		implements IInitializingBean, ITestSetterExtendable, ITestDataGenerator {
	@Autowired
	protected IEntityFactory entityFactory;

	@LogInstance
	private ILogger log;

	protected final Map<Class<?>, Class<?>> wrapperPrimitiveMap = new HashMap<>();
	{
		wrapperPrimitiveMap.put(Boolean.class, Boolean.TYPE);
		wrapperPrimitiveMap.put(Byte.class, Byte.TYPE);
		wrapperPrimitiveMap.put(Character.class, Character.TYPE);
		wrapperPrimitiveMap.put(Short.class, Short.TYPE);
		wrapperPrimitiveMap.put(Integer.class, Integer.TYPE);
		wrapperPrimitiveMap.put(Long.class, Long.TYPE);
		wrapperPrimitiveMap.put(Double.class, Double.TYPE);
		wrapperPrimitiveMap.put(Float.class, Float.TYPE);
		wrapperPrimitiveMap.put(Void.TYPE, Void.TYPE);
	}

	protected final IExtendableContainer<ITestSetter> testSetter =
			new DefaultExtendableContainer<>(ITestSetter.class, "testSetter");

	protected Class<?> wrapperToPrimitive(final Class<?> cls) {
		return wrapperPrimitiveMap.get(cls);
	}

	@Override
	public void registerTestSetter(ITestSetter extension) {
		testSetter.register(extension);
	}

	@Override
	public void unregisterTestSetter(ITestSetter extension) {
		testSetter.unregister(extension);
	}

	@Override
	public void afterPropertiesSet() throws Throwable {
		ParamChecker.assertNotNull(testSetter, "testSetter");
	}

	/*
	 * (non-Javadoc)
	 * @see com.basf.ap.soa.services.rnd.logistic.delivery.bservice.testdata.ITestDataGenerator#
	 * generateTestClass(java.lang .Class)
	 */
	@Override
	public <T> T generateTestClass(Class<T> type, String... toIgnore) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return generateTestClass(type, null, toIgnore);
	}

	/*
	 * (non-Javadoc)
	 * @see com.basf.ap.soa.services.rnd.logistic.delivery.bservice.testdata.ITestDataGenerator#
	 * generateTestClass(java.lang .Class, java.util.Map)
	 */
	@Override
	public <T> T generateTestClass(Class<T> type, Map<Object, Object> arguments, String... toIgnore)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {

		Set<String> toIgnoreSet = getToIgnoreSet(toIgnore);

		T instance = entityFactory.createEntity(type);

		if (instance != null && instance instanceof IEntityMetaDataHolder) {
			Set<Member> members = getMembers((IEntityMetaDataHolder) instance);

			for (Member member : members) {
				String name = member.getName();
				if (toIgnoreSet.contains(name)) {
					continue;
				}

				Class<?> parameterType = member.getRealType();

				for (ITestSetter setter : testSetter.getExtensions()) {
					if (setter.isApplicable(parameterType)) {
						Object parameter = setter.createParameter(name, arguments);
						member.setValue(instance, parameter);
						continue;
					}
				}
				log.warn("Unable to find matching 'setter' for type <<" + parameterType != null
						? parameterType.getName()
						: "null" + ">>");
			}
			return instance;
		}
		else {
			throw new IllegalArgumentException("Entity creation for given type failed.");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.basf.ap.soa.services.rnd.logistic.delivery.bservice.testdata.ITestDataGenerator#
	 * checkTestInstance(java.lang .Object, java.lang.String)
	 */
	@Override
	public void checkTestInstance(Object instance, String... toIgnore) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		checkTestInstance(instance, null, toIgnore);
	}

	/*
	 * (non-Javadoc)
	 * @see com.basf.ap.soa.services.rnd.logistic.delivery.bservice.testdata.ITestDataGenerator#
	 * checkTestInstance(java.lang .Object, java.util.Map, java.lang.String)
	 */
	@Override
	public void checkTestInstance(Object instance, Map<Object, Object> arguments, String... toIgnore)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		ParamChecker.assertNotNull(instance, "instance");
		ParamChecker.assertTrue(instance instanceof IEntityMetaDataHolder, "instance");

		Set<String> toIgnoreSet = getToIgnoreSet(toIgnore);

		Set<Member> members = getMembers((IEntityMetaDataHolder) instance);

		for (Member member : members) {
			String name = member.getName();
			if (toIgnoreSet.contains(name)) {
				continue;
			}

			Class<?> parameterType = member.getRealType();

			for (ITestSetter setter : testSetter.getExtensions()) {

				if (setter.isApplicable(parameterType)) {
					Object content = member.getValue(instance);
					setter.compareResult(name, arguments, content);
				}
			}
		}
	}

	/**
	 * Get all properties from an entity (excluding Ambeth enhancements)
	 *
	 * @param instance "enhanced" entity containing entity meta data
	 * @return a {@link Set} of all publicly accessible properties ({@link Member}s)
	 */
	private Set<Member> getMembers(IEntityMetaDataHolder instance) {
		Set<Member> members = new HashSet<>();
		IEntityMetaData entityMetaData = instance.get__EntityMetaData();
		members.addAll(Arrays.asList(entityMetaData.getPrimitiveMembers()));
		members.addAll(Arrays.asList(entityMetaData.getRelationMembers()));
		return members;
	}

	/**
	 * Extends the set of attributes to ignore with the technical attributes.
	 *
	 * @param toIgnore
	 * @return
	 */
	protected Set<String> getToIgnoreSet(String... toIgnore) {
		Set<String> toIgnoreSet = new HashSet<>();
		toIgnoreSet.addAll(Arrays.asList(toIgnore));
		// Technical Attributes are always ignored!
		toIgnoreSet.addAll(Arrays.asList(EntityData.TECHNICAL_ATTRIBUTES));
		return toIgnoreSet;
	}

	@Override
	public void compareTestInstance(Object expected, Object instance, boolean recursive,
			String... toIgnore)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		compareTestInstance(expected, instance, recursive, new HashSet<>(), getToIgnoreSet(toIgnore));
	}

	protected void compareTestInstance(Object expected, Object instance, boolean recursive,
			Set<Object> alreadyCompared, Set<String> toIgnoreSet)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

		if (expected == instance) {
			return;
		}

		toIgnoreSet.add("AssignableFrom");
		toIgnoreSet.add("Class");

		ParamChecker.assertNotNull(instance, "instace");
		ParamChecker.assertNotNull(expected, "expected");

		Class<? extends Object> type = expected.getClass();
		if (Collection.class.isAssignableFrom(type)) {
			// Special behavior for Collections:
			Collection<?> expectedCol = (Collection<?>) expected;
			Collection<?> instanceCol = (Collection<?>) instance;

			ParamChecker.assertTrue(expectedCol.size() == instanceCol.size(), "size");
			for (Iterator<?> instanceIt = instanceCol.iterator(), expectedIt =
					expectedCol.iterator(); instanceIt.hasNext() && expectedIt.hasNext();) {
				Object expectedItem = expectedIt.next();
				Object instanceItem = instanceIt.next();
				if (expectedItem == null && instanceItem == null) {
					continue;
				}
				ParamChecker.assertNotNull(expectedItem, instance.getClass().getName());
				ParamChecker.assertNotNull(instanceItem, instance.getClass().getName());
				Class<?> clazz = expectedItem.getClass();
				if (wrapperToPrimitive(clazz) != null || clazz.isPrimitive()
						|| String.class.isAssignableFrom(clazz)) {
					ParamChecker.assertTrue(Objects.equals(expectedItem, instanceItem),
							instance.getClass().getName());
				}
				else if (recursive && !alreadyCompared.contains(expectedItem)) {
					alreadyCompared.add(expectedItem);
					compareTestInstance(expectedItem, instanceItem, recursive, alreadyCompared, toIgnoreSet);
				}
			}
		}
		else {
			Set<Member> members = getMembers((IEntityMetaDataHolder) instance);

			for (Member member : members) {
				String name = member.getName();
				if (toIgnoreSet.contains(name)) {
					continue;
				}

				Object expectedContent = member.getValue(expected);
				Object content = member.getValue(instance);
				Class<?> parameterType = member.getRealType();
				if (wrapperToPrimitive(parameterType) != null || parameterType.isPrimitive()
						|| String.class.isAssignableFrom(parameterType)) {
					ParamChecker.assertTrue(Objects.equals(expectedContent, content), name);
				}
				else if (recursive && !alreadyCompared.contains(expectedContent)) {
					alreadyCompared.add(expectedContent);
					compareTestInstance(expectedContent, content, recursive, alreadyCompared, toIgnoreSet);
				}
			}
		}
	}
}
