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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
//import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * This is the entry point of the validation engine. 
 * The maven build script packages the application as a 'all-in-one' jar. As such, there are 2 ways to start
 * the application:
 *	java -jar ASC-Validator-some_version.jar
 * or
 *	java -cp some_path/ASC-Validator-some_version.jar org.springframework.boot.loader.JarLauncher
 *
 * The second version is important because it gets around the known java issue/bug of not being able to specify
 * additional classpath elements when using the '-jar' option (additional '-cp' options are being ignored). This 
 * version allows to add to the classpath the location of additional configuration.
 *
 * The approach in packaging the application was to include in the (all-in-one) package the known configurations
 * (such that it is as easy as possible to install/setup/start the applications) while still having the possibility
 * of re-specifying (some of) the configuration.
 * 
 * All out beans are specified within 'checker.xml'; use spring profiles to distinguish between the
 * configurations for each environment.
 *  <beans profile="dev"> .. </beans>
 *  <beans profile="ist"> .. </beans>
 *
 * The default configuration is for the development environment. In order to run a particular configuration we use the
 * spring.profiles.active environment variable, as in: 
 *	java -Dspring.profiles.active=ist -jar some_path/ASC-Validator-Service-some_version.jar
 * or
 *	java -Dspring.profiles.active=ist -cp some_path/ASC-Validator-some_version.jar org.springframework.boot.loader.JarLauncher
 *
 * Note: we can have a 'common' profile to be always activated that includes those bean specifications least likely to
 * change.
 *
 * Resource reference syntax:
 *	classpath*:validator.xml
 * vs
 *  classpath:validator.xml
 *
 * In the first case we direct the bean loader to find *ALL* validator.xml files in the classpath and merge them
 * (merge the parts for the active profile(s))
 * In the second case we instruct the bean loader to locate the *FIRST* validator.xml file and process it. All others
 * (validator.xml) files will be ignored.
 *
 * Overwriting pre-packaged bean configuration:
 * 	The first version would theoretically allow us to specify beans configuration deltas (with respect to the 
 * pre-packaged version). In practice I did not manage to have this approach working reliably: it relies on a
 * deterministic order of classpath processing and on overriding some of the spring framework defaults (in not so
 * obvious ways, bean indexing for example).
 * 	Currently we enable the second option, i.e. one has to provide a FULL alternative bean configuration. This
 * alternative configuration can be specified as overwriting an existing profile or as an entirely new profile.
 * (when using alternative profiles the resourcec reference syntax is irrelevant ..)  
 */

@SpringBootApplication(scanBasePackages={"org.onap.tosca.checker.service"})
@ImportResource({"classpath:${beans.config?:checker}.xml"})
@EnableScheduling
public class CheckerEngine
{

    public CheckerEngine() {
    }
	
    public static void main(String theArgs[]) {
			SpringApplication sapp = new SpringApplication(CheckerEngine.class);
			ConfigurableApplicationContext ctx = sapp.run(theArgs);
    }

}
