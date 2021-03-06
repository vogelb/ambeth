package com.koch.ambeth.util.io;

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

import java.io.IOException;
import java.io.OutputStream;

public class ConfigurableGZIPOutputStream extends java.util.zip.GZIPOutputStream {
	public ConfigurableGZIPOutputStream(OutputStream out, int compressionLevel) throws IOException {
		super(out);
		setCompressionLevel(compressionLevel);
	}

	public ConfigurableGZIPOutputStream(OutputStream out, int compressionLevel, int size)
			throws IOException {
		super(out, size);
		setCompressionLevel(compressionLevel);
	}

	public void setCompressionLevel(int compressionLevel) {
		def.setLevel(compressionLevel);
	}
}
