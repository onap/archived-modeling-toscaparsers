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
package org.onap.tosca.checker.service;

import java.net.URI;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;

import java.util.stream.Collectors;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.Catalog;
import org.onap.tosca.checker.Target;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Component("catalogs")
@Scope("singleton")
@ConfigurationProperties(prefix="catalogs")
public class Catalogs implements ApplicationContextAware {

	private static Logger logger = Logger.getLogger(Catalogs.class.getName());

	private ApplicationContext			appCtx;
	private ResourcePatternResolver	resolver;
	private Map<String, Catalog> 		catalogs = new HashMap<String, Catalog>();

	public Catalogs() {
		resolver = new PathMatchingResourcePatternResolver();
	}

	public void setApplicationContext(ApplicationContext theCtx) throws BeansException {
    this.appCtx = theCtx;
	}

	@PostConstruct
	public void initCatalogs() {
		logger.entering(getClass().getName(), "initCatalogs");
		
		// Done
		logger.log(Level.INFO, "Catalogs available");
	}

	@PreDestroy
	public void cleanupCatalogs() {
		logger.entering(getClass().getName(), "destroyCatalogs");
	}

	public Catalog getCatalog(String theName) {

System.out.println("getCatalog: " + theName + ". Known catalogs: " + this.catalogs.keySet());

		return this.catalogs.get(theName);
	}

	public void setCatalog(String theName, Catalog theCatalog) {
		this.catalogs.put(theName, theCatalog);
	}

	public Catalog removeCatalog(String theName) {
		return this.catalogs.remove(theName);
	}

	/* configuration interface */
	public void setCatalogs(Map<String, ?> theCatalogs) {
			//just look here at the pain of creating a checker ever time ..
		for (Map.Entry<String, ?> catalogEntry: theCatalogs.entrySet()) {
			try {
				Checker checker = new Checker();
				checker.check((String)catalogEntry.getValue()); 
				setCatalog(catalogEntry.getKey(), checker.catalog());
			}
			catch (Exception x) {
				throw new FatalBeanException("Failed to add catalog " + catalogEntry.getKey() + " from " + catalogEntry.getValue(), x);
			}
		}
	}

	public Map<String, ?> getCatalogs() {
		return this.catalogs;
	}

}
