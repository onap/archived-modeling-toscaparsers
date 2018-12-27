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

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.concurrent.Callable;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest; 

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.Api;

import org.onap.tosca.checker.Checker;
import org.onap.tosca.checker.CheckerException;
import org.onap.tosca.checker.Report;
import org.onap.tosca.checker.Catalog;
import org.onap.tosca.checker.Target;
import org.onap.tosca.checker.TargetLocator;
import org.onap.tosca.checker.CommonLocator;

@Api(description = "Tosca document validation operations")
@RestController
public class CheckerController implements ApplicationContextAware {

	private static Logger log = Logger.getLogger(CheckerController.class.getName());

	private ApplicationContext			appCtx;
	@Autowired
	private Catalogs					catalogs;
	private Checker						checker;
	
	public void setApplicationContext(ApplicationContext theCtx) throws BeansException {
    this.appCtx = theCtx;
	}

	/**
	 * standalone checking, everything will be forgotten
	 */
	@ApiOperation(value = "Validate given tosca yaml document. All errors reported as part of the returned report.", response = Report.class)
	@RequestMapping(value={"/check_template/"}, method={RequestMethod.POST}, produces={"application/json"})
	public Report validate(@RequestBody String theTemplate,
												 HttpServletRequest theRequest) {

		CachedTarget target = new CachedTarget("", requestURI(theRequest));
		target.setContent(theTemplate);
		((InCatalogLocator)this.checker.getTargetLocator()).setCatalog(null);

		try {
			this.checker.check(target);
		}
		catch (CheckerException cx) {
			log.log(Level.WARNING, "Failed to check tosca template", cx);
			target.getReport().add(cx);
		}

		return target.getReport();
	}

	/**
	 * checking with respect to a namespace/catalog but the outcome is forgotten (not added to the catalog)
	 */
	@ApiOperation(value = "Validate given tosca yaml document with repect to the information currently available in the indicated catalog. All errors reported as part of the returned report.", response = Report.class)
	@ApiResponses(value = { @ApiResponse(code = 412, message = "No such catalog exists in the service") }) 
	@RequestMapping(value={"/check_template/{catalog}"}, method={RequestMethod.POST}, produces={"application/json"})
	public Report validate(@RequestBody String theTemplate,
												 @PathVariable(value="catalog") String theCatalog,
												 HttpServletRequest theRequest) 
																										throws NoSuchCatalogException {

		Catalog catalog = this.catalogs.getCatalog(theCatalog);
		if (catalog == null)
			throw new NoSuchCatalogException(theCatalog);
		else
			((InCatalogLocator)this.checker.getTargetLocator()).setCatalog(catalog);

		CachedTarget target = new CachedTarget("", requestURI(theRequest));
		target.setContent(theTemplate);

		try {
			this.checker.check(target, new Catalog(catalog));
		}
		catch (CheckerException cx) {
			log.log(Level.WARNING, "Failed to check tosca template", cx);
			target.getReport().add(cx);
		}

		return target.getReport();
	}

	/**
	 * checking with respect to a namespace/catalog, the outcome is registered within the catalog 
	 */	
	@ApiOperation(value = "Validate given tosca yaml document with repect to the information currently available in the indicated catalog. Template/target tosca information set is added to the catalog and remains available under the specified name which can be used in the imports of other templetes submitted for validation. All errors reported as part of the returned report.", notes = "If a catalog with the given name does not exist one will be created", response = Report.class)
	@ApiResponses(value = { @ApiResponse(code=412, message="Same target exists in given catalog") }) 
	@RequestMapping(value={"/check_template/{catalog}/{name}"}, method={RequestMethod.POST}, produces={"application/json"})
	public Report validate(@RequestBody String theTemplate,
												 @PathVariable(value="catalog") String theCatalog,
												 @PathVariable(value="name") String theName,
												 HttpServletRequest theRequest)
																								throws TargetConflictException {
		
		Catalog catalog = this.catalogs.getCatalog(theCatalog);
		((InCatalogLocator)this.checker.getTargetLocator()).setCatalog(catalog);

		URI targetURI = requestURI(theRequest);
		if (catalog != null) {
			if (catalog.getTarget(targetURI) != null)
				throw new TargetConflictException(theName, theCatalog);
		}

		CachedTarget target = new CachedTarget("", targetURI);
		target.setContent(theTemplate);

		try {
			if (catalog == null)
				this.checker.check(target);
			else
				this.checker.check(target, catalog);
		}
		catch (CheckerException cx) {
			log.log(Level.WARNING, "Failed to check tosca template", cx);
			target.getReport().add(cx);
		}

		if (target.getReport().isEmpty() && catalog == null)
			this.catalogs.setCatalog(theCatalog, checker.catalog());

		return target.getReport();
	}
	
	@ApiOperation(value = "Check the existence of a catalog with the given name.")
	@ApiResponses(value = { @ApiResponse(code=404, message="") }) 
	@RequestMapping(value={"/check_template/{catalog}"}, method={RequestMethod.GET})
	public ResponseEntity<Void> validate(@PathVariable(value="catalog") String theCatalog) {

		Catalog cat = catalogs.getCatalog(theCatalog);
		if (cat == null) {
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity(HttpStatus.OK);
	}
	
	@ApiOperation(value = "Remove the catalog with the given name and the associated tosca information set.")
	@ApiResponses(value = { @ApiResponse(code=404, message="") }) 
	@RequestMapping(value={"/check_template/{catalog}"}, method={RequestMethod.DELETE})
	public ResponseEntity<Void> deleteCatalog(@PathVariable(value="catalog") String theCatalog) {

		Catalog cat = catalogs.removeCatalog(theCatalog);
		if (cat == null) {
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity(HttpStatus.OK);
	}
	
	@ApiOperation(value = "Check the existence of the specified template/target within the given catalog.")
	@ApiResponses(value = { @ApiResponse(code=404, message="No such catalog"), @ApiResponse(code=404, message="No such template")}) 
	@RequestMapping(value={"/check_template/{catalog}/{name}"}, method={RequestMethod.GET})
	public ResponseEntity<String> retrieve(@PathVariable(value="catalog") String theCatalog,
												 								 @PathVariable(value="name") String theTemplateName,
																		 		 HttpServletRequest theRequest) {

		Catalog cat = catalogs.getCatalog(theCatalog);
		if (cat == null) {
			return new ResponseEntity("No such catalog", HttpStatus.NOT_FOUND);
		}

		Target t = cat.getTarget(requestURI(theRequest));
		if (t == null) {
			return new ResponseEntity("No such template", HttpStatus.NOT_FOUND);
		}

		return new ResponseEntity("{}", HttpStatus.OK);
	}
	

	@PostConstruct
	public void initController() {
		log.entering(getClass().getName(), "initCheckerController");

		try {
			this.checker = new Checker();
			this.checker.setTargetLocator(new InCatalogLocator());
		}
		catch (CheckerException cx) {
			log.log(Level.WARNING, "CheckerController setup failed", cx);
			throw new BeanInitializationException("Failed to create a checker", cx);
		}

		log.log(Level.INFO, "CheckerController started");
	}

	@PreDestroy
	public void cleanupController() {
		log.entering(getClass().getName(), "cleanupCheckerController");
	}

	private URI requestURI(HttpServletRequest theRequest) {
		try {
			return new URI(String.format("%s://%s:%d%s", theRequest.getScheme(),
																									 theRequest.getServerName(),
																									 theRequest.getServerPort(),
																									 theRequest.getRequestURI().toString()));
		}
		catch(URISyntaxException urisx) {
			throw new RuntimeException(urisx);
		}		
	}

	/**
	 */
	public static class InCatalogLocator extends CommonLocator {
		
		private ThreadLocal<Catalog> catalog = new ThreadLocal();

		public InCatalogLocator() {
		}

		protected void setCatalog(Catalog theCatalog) {
			this.catalog.set(theCatalog);
		}

		/** */
		@Override
		public Target resolve(String theName) {
			Target target = null;
			if (this.catalog.get() != null)
				target = this.catalog.get()
															.targets()
															.stream()
															.filter(t -> t.getName().equals(theName))
															.findFirst()
															.orElse(null);
			return target == null ?	super.resolve(theName) :	target;
		}
	}

}
