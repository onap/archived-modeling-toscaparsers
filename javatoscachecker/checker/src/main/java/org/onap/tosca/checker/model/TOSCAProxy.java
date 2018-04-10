/*
 * Copyright (c) 2017 <AT&T>.  All rights reserved.
 * ===================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */
package org.onap.tosca.checker.model;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import java.util.stream.Collectors;

import java.util.function.Function;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;

import java.lang.invoke.MethodHandles;

import com.google.common.reflect.Invokable;
import com.google.common.reflect.AbstractInvocationHandler;


/**
 */
public class TOSCAProxy 
									extends AbstractInvocationHandler {


	public static Constructor<MethodHandles.Lookup> lookupHandleConstructor;

	static {
		try {
			lookupHandleConstructor =
					MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,
																														int.class);

			if (!lookupHandleConstructor.isAccessible()) {
   			lookupHandleConstructor.setAccessible(true);
			}
		}
		catch (Exception x) {
			throw new RuntimeException(x);
		}	
	}

	protected static <T> Class<T> typeArgument(Class theType) {
		return (Class<T>)	
						((ParameterizedType)theType.getGenericInterfaces()[0]).
							getActualTypeArguments()[0];
	}

	private static Map	proxyRecords = new HashMap();

	/** */
	public static Object record(Object theRef, Function theProxyBuilder) {
		return proxyRecords.computeIfAbsent(theRef, theProxyBuilder);
	}

	/* a version allowing for the 'decoration/wrapping' of a basic list
	 */
	public static <T extends List> T buildList(final List theInfo, Class<T> theType) {
		return (T)java.lang.reflect.Proxy.newProxyInstance(
							TOSCAProxy.class.getClassLoader(),
							new Class[] { theType },
							new AbstractInvocationHandler() {
								protected Object handleInvocation(Object theProxy,Method theMethod,Object[] theArgs)
																																												throws Throwable {
									return theMethod.invoke(theInfo, theArgs);
								}
							});
	}
	
	/*
 	 * This is targeted at lists of one entry maps seen in in the TOSCA spec 
   */
	public static <T extends TOSCASeq> T buildSeq(final List<Map> theInfo, Class<T> theType) {
		theInfo.replaceAll((value) -> {	Map.Entry<String,?> entry = (Map.Entry<String,?>)
																																			value.entrySet().iterator().next();
																		
																		return entry.getValue() instanceof Map ?
																							buildObject(entry.getKey(), (Map)entry.getValue(), typeArgument(theType)) :
																							buildObject(entry.getKey(), Collections.singletonMap("value", entry.getValue()), typeArgument(theType));
																	});
		return (T)java.lang.reflect.Proxy.newProxyInstance(
							TOSCAProxy.class.getClassLoader(),
							new Class[] { theType },
							new AbstractInvocationHandler() {
								protected Object handleInvocation(Object theProxy,Method theMethod,Object[] theArgs)
																																												throws Throwable {
									//A Seq is nothing but a List so just propagate the call ..
									return theMethod.invoke(theInfo, theArgs);
								}
							});
	}
	
	/*
 	 * All entries in the map will become TOSCAObject instances (used for collections of constructs)
   */
	public static <T extends TOSCAMap> T buildMap(String theName, Map<String, ?> theInfo, Class<T> theType) {
		theInfo.replaceAll((name, value) -> {
													return
														value instanceof Map ?
															buildObject(name, (Map)value, typeArgument(theType)) :
															buildObject(name, Collections.singletonMap("value", value), typeArgument(theType));
													});
		return buildObject(theName, theInfo, theType);
	}

	public static <T> T buildObject(String theName, Map theInfo, Class<T> theType) {
		return (T)java.lang.reflect.Proxy.newProxyInstance(
							TOSCAProxy.class.getClassLoader(),
							new Class[] { theType /*, Map.class*/ },
							new TOSCAProxy(theName, theInfo));
	}
/*
	public static <T> T build(String theName, Map theInfo, Class<T> theType) {
		if (TOSCAMap.class.isAssignableFrom(theType))
			return buildMap(theName, theInfo, (Class<TOSCAMap>)theType);
		else
			return buildObject(theName, theInfo, theType);
	}
*/
	private String name;
	private Map 	 info;

	protected TOSCAProxy(String theName, Map theInfo) {
		this.name = theName;
		this.info = theInfo;
	}

	protected Object handleInvocation(
											Object theProxy,Method theMethod,Object[] theArgs)
																												throws Throwable {

//System.out.println(" ** proxy looking for " + theMethod + " in " + name + "&" + info);

		//if the method was declared in Map.class, just default to 'info' (we should make this read only)
		if (Map.class.equals(theMethod.getDeclaringClass())) {
			return theMethod.invoke(this.info, theArgs);
		}

		if (theMethod.isDefault()) {
			final Class<?> declaringClass = theMethod.getDeclaringClass();
			return lookupHandleConstructor
							.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
							.unreflectSpecial(theMethod, declaringClass)
							.bindTo(theProxy)
							.invokeWithArguments(theArgs);
		}

		if ("info".equals(theMethod.getName())) {
			return this.info;
		}

		if ("name".equals(theMethod.getName())) {
			return this.name;
		}

		return this.info.get(theMethod.getName());
	}
}
