package com.koch.ambeth.cache.bytecode.behavior;

/*-
 * #%L
 * jambeth-cache-bytecode
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

import java.util.List;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import com.koch.ambeth.bytecode.behavior.AbstractBehavior;
import com.koch.ambeth.bytecode.behavior.IBytecodeBehavior;
import com.koch.ambeth.bytecode.behavior.IBytecodeBehaviorState;
import com.koch.ambeth.bytecode.visitor.InterfaceAdder;
import com.koch.ambeth.cache.bytecode.visitor.EntityMetaDataHolderVisitor;
import com.koch.ambeth.cache.bytecode.visitor.GetBaseTypeMethodCreator;
import com.koch.ambeth.ioc.annotation.Autowired;
import com.koch.ambeth.merge.bytecode.EmbeddedEnhancementHint;
import com.koch.ambeth.merge.bytecode.EntityEnhancementHint;
import com.koch.ambeth.merge.propertychange.PropertyChangeEnhancementHint;
import com.koch.ambeth.merge.proxy.IEnhancedType;
import com.koch.ambeth.merge.proxy.IEntityMetaDataHolder;
import com.koch.ambeth.service.merge.IEntityMetaDataProvider;
import com.koch.ambeth.service.merge.model.IEntityMetaData;

public class EnhancedTypeBehavior extends AbstractBehavior {
	@Autowired
	protected IEntityMetaDataProvider entityMetaDataProvider;

	@Override
	public Class<?>[] getEnhancements() {
		return new Class<?>[] { IEnhancedType.class, IEntityMetaDataHolder.class };
	}

	@Override
	public ClassVisitor extend(ClassVisitor visitor, IBytecodeBehaviorState state,
			List<IBytecodeBehavior> remainingPendingBehaviors,
			List<IBytecodeBehavior> cascadePendingBehaviors) {
		if ((state.getContext(EntityEnhancementHint.class) == null
				&& state.getContext(EmbeddedEnhancementHint.class) == null)
				&& state.getContext(PropertyChangeEnhancementHint.class) == null) {
			return visitor;
		}
		if (state.getContext(EntityEnhancementHint.class) != null) {
			IEntityMetaData metaData = entityMetaDataProvider.getMetaData(state.getOriginalType());
			visitor = new InterfaceAdder(visitor, Type.getInternalName(IEntityMetaDataHolder.class));
			visitor = new EntityMetaDataHolderVisitor(visitor, metaData);
		}
		visitor = new InterfaceAdder(visitor, Type.getInternalName(IEnhancedType.class));
		visitor = new GetBaseTypeMethodCreator(visitor);
		return visitor;
	}
}
