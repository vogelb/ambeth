package com.koch.ambeth.cache.stream.bytebuffer;

/*-
 * #%L
 * jambeth-cache-stream
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

/**
 * Ensures that {@link FileKey}-related IO will be executed always with an identical instance of
 * RandomAccessFile which is then forwarded to the <code>IFileReadDelegate</code>.
 */
public interface IFileHandleCache {
	<T> T readOnFile(FileKey fileKey, IFileReadDelegate<T> fileReadDelegate);
}
