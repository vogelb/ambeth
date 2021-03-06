package com.koch.ambeth.util;

/*-
 * #%L
 * jambeth-util
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

public interface IDedicatedConverter {
	/**
	 * Converts a defined set of types.
	 *
	 * @param expectedType Type to convert to
	 * @param sourceType Type to convert from
	 * @param value Value of class sourceType
	 * @param additionalInformation Optional information if neede for this conversion
	 * @return Value converted to expectedType
	 */
	Object convertValueToType(Class<?> expectedType, Class<?> sourceType, Object value,
			Object additionalInformation) throws Exception;
}
