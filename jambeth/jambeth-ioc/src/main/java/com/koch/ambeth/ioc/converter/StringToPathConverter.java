package com.koch.ambeth.ioc.converter;

/*-
 * #%L
 * jambeth-ioc
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

import java.nio.file.Path;
import java.nio.file.Paths;

import com.koch.ambeth.util.IDedicatedConverter;

public class StringToPathConverter implements IDedicatedConverter {
	@Override
	public Object convertValueToType(Class<?> expectedType, Class<?> sourceType, Object value,
			Object additionalInformation) throws Exception {
		if (Path[].class.equals(expectedType)) {
			String[] split = StringToFileConverter.fileDelimiterPattern.split((String) value);
			Path[] files = new Path[split.length];
			for (int a = split.length; a-- > 0;) {
				files[a] = Paths.get(split[a]);
			}
			return files;
		}
		return Paths.get((String) value);
	}
}
