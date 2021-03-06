package com.koch.ambeth.example.conversionHelper;

/*-
 * #%L
 * jambeth-examples
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

import java.util.Calendar;

import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.log.ILogger;
import com.koch.ambeth.log.LogInstance;
import com.koch.ambeth.util.IConversionHelper;

public class ConversionHelperExample {
	@LogInstance
	private ILogger log;

	@Autowired
	protected IConversionHelper conversionHelper;

	public void logCalendar(Calendar calendar) {
		if (log.isInfoEnabled()) {
			String stringValue = conversionHelper.convertValueToType(String.class, calendar);
			log.info(stringValue); // log ISO8601-formatted calendar value
		}
	}
}
