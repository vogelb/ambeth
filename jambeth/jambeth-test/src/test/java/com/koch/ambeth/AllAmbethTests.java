package com.koch.ambeth;

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

import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.koch.ambeth.testutil.category.SlowTests;

@RunWith(Suite.class)
@ExcludeCategory(SlowTests.class)
@SuiteClasses({com.koch.ambeth.audit.AllAuditTests.class, //
		com.koch.ambeth.cache.AllBundleCacheTests.class, //
		com.koch.ambeth.bytecode.AllBytecodeTests.class, //
		// com.koch.ambeth.AllDataChangePersistenceTests.class, //
		// com.koch.ambeth.AllIocTests.class,//
		com.koch.ambeth.AllMergeBytecodeTests.class, //
		// com.koch.ambeth.AllPersistenceTests.class, //
		// com.koch.ambeth.AllMergeTests.class, //
		// com.koch.ambeth.AllUtilTests.class, //
		com.koch.ambeth.ioc.AllIocTests.class, //
		com.koch.ambeth.bytecode.AllBundleBytecodeTests.class, //
		com.koch.ambeth.merge.mergecontroller.AllMergeControllerTests.class, //
		com.koch.ambeth.merge.orihelper.AllORIHelperTests.class, //
		com.koch.ambeth.orm20.AllOrm20Tests.class, //
		com.koch.ambeth.persistence.AllTestPersistenceTests.class, //
		com.koch.ambeth.persistence.jdbc.AllTests.class, //
		com.koch.ambeth.persistence.streaming.StreamingEntityTest.class, //
		com.koch.ambeth.persistence.xml.AllPersistenceXmlTests.class, //
		com.koch.ambeth.query.AllBundleQueryTests.class, //
		// com.koch.ambeth.query.AllQueryTests.class, //
		com.koch.ambeth.relations.AllRelationTests.class, //
		com.koch.ambeth.service.AllServiceTests.class, //
		// com.koch.ambeth.testutil.AllTestUtilTests.class, //
		// com.koch.ambeth.testutil.AllTestUtilPersistenceTests.class, //
		com.koch.ambeth.util.LongIdTest.class, //
		com.koch.ambeth.xml.AllXmlTests.class, //
		com.koch.ambeth.xml.oriwrapper.AllOriWrapperTests.class})
public class AllAmbethTests {
}
