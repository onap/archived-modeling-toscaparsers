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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.Callable;

import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.CommonLocator;
import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.Catalog;
import org.onap.tosca.checker.Report;
import org.onap.tosca.checker.CheckerException;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.annotation.Autowired;

/**
 */
@Component("templateChecker")
@Scope("prototype")
public class TemplateChecker implements Callable<Report> {

	private static Logger log = Logger.getLogger(TemplateChecker.class.getName());

	private	String						template,
														name,
														catalog;
	private URI								uri;

	@Autowired
	private	Catalogs					catalogs;

	/* */
	public TemplateChecker(String theTemplate, String theName, String theCatalog, URI theRef) {
		this.template = theTemplate;
		this.name = theName;
		this.catalog = theCatalog;
		this.uri = theRef;
	}

	/*
	 */
	@Override
	public Report call() throws Exception {

		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST, "Processing template " + this.template);

		Catalog cat = catalogs.getCatalog(this.catalog);
		
		log.log(Level.FINER, "validating template " + this.name + " at " + this.uri + ". Active catalog: " + cat);
	
		//because this is a new checker we do not care about the top target name ..	
		CachedTarget target = new CachedTarget(this.name == null ? "" : this.name, this.uri);
		target.setContent(this.template);
		Checker checker = new Checker();
		checker.setTargetLocator(new InCatalogLocator(cat));
		try {
			if (cat != null) {
				checker.check(target, cat);
			}
			else {
				checker.check(target);
			}
		}
		catch (CheckerException cx) {
			log.log(Level.WARNING, "Failed to check tosca template", cx);
			target.getReport().add(cx);
		}

		//if named template is succesfull and this is a new catalog, register it
		if (this.name != null && target.getReport().isEmpty() && cat == null) {
			this.catalogs.setCatalog(this.catalog, checker.catalog());
		}	
			
		return target.getReport().isEmpty() ? null : target.getReport();

	}


	public static class InCatalogLocator extends CommonLocator {
		
		private Catalog catalog;

		public InCatalogLocator(Catalog theCatalog) {
			this.catalog = theCatalog;
		}

		/** */
		@Override
		public Target resolve(String theName) {
			Target target = null;
			if (this.catalog != null)
				target = this.catalog.targets()
															.stream()
															.filter(t -> t.getName().equals(theName))
															.findFirst()
															.orElse(null);
			return target == null ?	super.resolve(theName) :	target;
		}
	}
}
