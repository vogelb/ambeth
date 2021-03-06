package com.koch.ambeth.cache.bytecode.visitor;

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

import com.koch.ambeth.bytecode.ClassGenerator;
import com.koch.ambeth.bytecode.MethodInstance;
import com.koch.ambeth.service.merge.model.IEntityMetaData;
import com.koch.ambeth.util.ListUtil;

public class EntityFactoryVisitor extends ClassGenerator {
	public static final MethodInstance m_createObservableCollection = new MethodInstance(null,
			ListUtil.class, List.class, "createObservableCollectionOfType", Class.class);

	protected final IEntityMetaData metaData;

	public EntityFactoryVisitor(ClassVisitor cv, IEntityMetaData metaData) {
		super(cv);
		this.metaData = metaData;
	}

	@Override
	public void visitEnd() {
		// ITypeInfoItem[] primitiveMembers = metaData.getPrimitiveMembers();
		// ArrayList<ITypeInfoItem> listMembers = new ArrayList<ITypeInfoItem>();
		// for (ITypeInfoItem primitiveMember : primitiveMembers)
		// {
		// Class<?> realType = primitiveMember.getRealType();
		// if (!Collection.class.isAssignableFrom(realType))
		// {
		// continue;
		// }
		// primitiveMember = RelationsGetterVisitor.getApplicableMember(primitiveMember);
		// if (primitiveMember == null)
		// {
		// // member is handled in another type
		// continue;
		// }
		// listMembers.add(primitiveMember);
		// }
		// if (!listMembers.isEmpty())
		// {
		// IBytecodeBehaviorState state = BytecodeBehaviorState.getState();
		// Constructor<?>[] constructors = state.getCurrentType().getDeclaredConstructors();
		//
		// if (constructors.length == 0)
		// {
		// // must never happen because the originalType which is the base of the current type
		// enhancement must already be a valid enhanced entity type
		// // this implies at least 1 generated constructor
		// throw new IllegalStateException("Must never happen");
		// }
		// for (Constructor<?> constructor : constructors)
		// {
		// ConstructorInstance c_method = new ConstructorInstance(constructor);
		//
		// MethodGenerator mg = visitMethod(c_method);
		// mg.loadThis();
		// mg.loadArgs();
		// mg.invokeConstructor(c_method);
		// for (ITypeInfoItem member : listMembers)
		// {
		// PropertyInstance p_member = getState().getProperty(member.getName(), member.getRealType());
		// if (p_member == null)
		// {
		// // same reason as before
		// throw new IllegalStateException("Must never happen");
		// }
		// mg.callThisSetter(p_member, new Script()
		// {
		// @Override
		// public void execute(MethodGenerator mg)
		// {
		// Object primitive = primitiveMember.getValue(entity);
		// if (primitive == null)
		// {
		// primitive = ListUtil.createObservableCollectionOfType(realType);
		// primitiveMember.setValue(entity, primitive);
		// }
		// }
		// });
		// }
		// mg.returnValue();
		// mg.endMethod();
		// }
		// }
		super.visitEnd();
	}
}
