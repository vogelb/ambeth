package com.koch.ambeth.bytecode.visitor;

/*-
 * #%L
 * jambeth-bytecode
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

import java.lang.reflect.Constructor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import com.koch.ambeth.bytecode.ClassGenerator;
import com.koch.ambeth.bytecode.ConstructorInstance;
import com.koch.ambeth.bytecode.MethodGenerator;
import com.koch.ambeth.bytecode.TypeUtil;
import com.koch.ambeth.bytecode.behavior.BytecodeBehaviorState;
import com.koch.ambeth.bytecode.behavior.IBytecodeBehaviorState;
import com.koch.ambeth.util.exception.RuntimeExceptionUtil;

/**
 * PublicConstructorVisitor declares constructors derived from {@link #superClass}.
 *
 * {@link #superClass} is defined either by {@link BytecodeBehaviorState#getCurrentType()} or by the
 * parameter extendedType of the
 * {@link PublicConstructorVisitor#PublicConstructorVisitor(ClassVisitor, Class)} constructor. In
 * the latter case extendedType can be an abstract type used as template to implement the interface
 * defined by {@link BytecodeBehaviorState#getOriginalType()}.
 *
 * If {@link #superClass} does not declare constructors or {@link #superClass} is an interface a
 * default constructor is created.
 */
public class PublicConstructorVisitor extends ClassGenerator {
	public static boolean hasValidConstructor() {
		IBytecodeBehaviorState state = BytecodeBehaviorState.getState();

		Constructor<?>[] constructors = state.getCurrentType().getDeclaredConstructors();

		for (Constructor<?> constructor : constructors) {
			if (state.isMethodAlreadyImplementedOnNewType(new ConstructorInstance(constructor))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Derives constructors from {@link BytecodeBehaviorState#getState()#getCurrentType()}
	 *
	 * @param cv ClassVisitor
	 */
	public PublicConstructorVisitor(ClassVisitor cv) {
		super(cv);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visitEnd() {
		if (!hasValidConstructor()) {
			IBytecodeBehaviorState state = BytecodeBehaviorState.getState();
			Constructor<?>[] constructors = state.getCurrentType().getDeclaredConstructors();

			for (Constructor<?> constructor : constructors) {
				int access = TypeUtil.getModifiersToAccess(constructor.getModifiers());
				access &= ~Opcodes.ACC_PROTECTED;
				access &= ~Opcodes.ACC_PRIVATE;
				access |= Opcodes.ACC_PUBLIC;
				ConstructorInstance c_method = new ConstructorInstance(constructor.getDeclaringClass(),
						access, ConstructorInstance.getSignature(constructor), constructor.getParameterTypes());

				MethodGenerator mg = visitMethod(c_method);
				mg.loadThis();
				mg.loadArgs();
				mg.invokeConstructor(c_method);

				mg.returnValue();
				mg.endMethod();
			}
			if (constructors.length == 0) {
				// Implement "first" default constructor
				ConstructorInstance c_method;
				try {
					c_method = new ConstructorInstance(Object.class.getConstructor());
				}
				catch (Exception e) {
					throw RuntimeExceptionUtil.mask(e);
				}
				MethodGenerator ga = visitMethod(c_method);
				ga.loadThis();
				ga.invokeConstructor(c_method);
				ga.returnValue();
				ga.endMethod();
			}
		}
		super.visitEnd();
	}
}
