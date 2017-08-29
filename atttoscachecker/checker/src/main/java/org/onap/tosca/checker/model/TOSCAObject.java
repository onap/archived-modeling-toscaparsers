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

import java.util.Map;
import java.util.List;
import java.util.Collections;

import java.lang.reflect.Proxy;

/*
 * Some choice to make here: do we explcitly extend Map or not
 * Note that this makes assumptions about the POJO representation of the yaml document.
 */
public interface TOSCAObject<T extends TOSCAObject<T>> extends Map<String, Object> {

	/** */
	public abstract Map info();

	/* if the key is absent this produces a null value, rather normal I'd say
	 */
	public default <A> A proxy(String theKey, final Class<A> theType) {
		return (A)/*info().*/computeIfPresent(theKey,
																			 (key, val)-> {
																					/*
																					if (val instanceof TOSCAObject ||
																							val instanceof TOSCAMap ||
																							val instanceof TOSCASeq)
																						return val;
																					*/
																					//this makes the assumption that no other proxies are at play
																					if (Proxy.isProxyClass(val.getClass()))
																						return val;

																					if (val instanceof Map && TOSCAMap.class.isAssignableFrom(theType))
																						return TOSCAProxy.buildMap((String)key, (Map<String,Map>)val, (Class<TOSCAMap>)theType);
																					if (val instanceof List && TOSCASeq.class.isAssignableFrom(theType))
																						return TOSCAProxy.buildSeq((List)val, (Class<TOSCASeq>)theType);
																					
																					return TOSCAProxy.buildObject((String)key, (Map<String,Map>)val, theType);
																				});
	}

	public default <A extends List> A proxyList(String theKey, final Class<A> theType) {
		return (A)computeIfPresent(theKey,
															 (key, val)-> {
																					if (Proxy.isProxyClass(val.getClass()))
																						return val;
																					return TOSCAProxy.buildList((List)val, theType);
																				});
	}

}
