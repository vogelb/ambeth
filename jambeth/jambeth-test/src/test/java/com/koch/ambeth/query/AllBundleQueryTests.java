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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({FilterDescriptorTest.class, //
		FilterToQueryBuilderTest.class, //
		PropertyQueryTest.class, //
		Query10000Test.class, //
		// QueryMassdataTest.class, //
		QueryTest.class, //
		StoredFunctionTest.class, //
		com.koch.ambeth.query.alternateid.MultiAlternateIdQueryTest.class, //
		com.koch.ambeth.query.backwards.BackwardsQueryTest.class, //
		com.koch.ambeth.query.behavior.QueryBehaviorTest.class, //
		// com.koch.ambeth.query.isin.QueryIsInMassdataTest.class, //
		com.koch.ambeth.query.subquery.SubQueryTest.class})
public class AllBundleQueryTests {
	// Intended blank
}
