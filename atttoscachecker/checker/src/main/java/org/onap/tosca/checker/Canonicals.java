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
package org.onap.tosca.checker;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import kwalify.Validator;
import kwalify.Rule;
import kwalify.Types;

import com.google.common.collect.Table;
import com.google.common.collect.HashBasedTable;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;

import org.onap.tosca.checker.annotations.Validates;

/**
 * Constructs and collects the canonical form during the validation step (syntax check) based on the short form
 * indicator from the grammar specification.
 * The TOSCA spec indicates a 'short' form for most of the constructs but we want checking to be able to work on
 * the canonical form and not to have to handle the short form explicitly. Additionally tis makes for a simpler 
 * grammar specification. 
 */
public class Canonicals {

	private Logger    					log = Logger.getLogger("com.att.research.asc.chcker.Canonicals");
	
	private Table<Target, String, Object> canonicals = null; //HashBasedTable.create();

	@Validates(rule="", timing=Validates.Timing.pre)
  public boolean pre_validate_short_form(
			Object theValue, Rule theRule, Validator.ValidationContext theContext) {

		String hint = theRule.getShort();
		if (theValue != null &&
				hint != null) {

			log.finer("Attempting canonical at " + theContext.getPath() + ", rule " + theRule.getName());

		  Object canonical = null;
			//if the canonical form requires a collection
			if (Types.isCollectionType(theRule.getType())) {
				//and the actual value isn't one
				if( !(theValue instanceof Map || theValue instanceof List)) {
					//used to use singleton map/list here (was good for catching errors)
					//but there is the possibility of short forms within short forms so
					//the created canonicals need to accomodate other values.
					if (Types.isMapType(theRule.getType())) {
						canonical = new HashMap();
						((Map)canonical).put(hint, theValue);
					}
					else {
						//the hint is irrelevant here but we should impose a value when the target is a list
						canonical = new LinkedList();
						((List)canonical).add(theValue);
					}
       	}
				else {
					//we can accomodate:
					// map to list of map transformation
					if (!Types.isMapType(theRule.getType()) /* a seq */ &&
							theValue instanceof Map) {
						canonical = new LinkedList();
						((List)canonical).add(theValue);	
					}
					else {
     				log.fine("Grammar for rule " + theRule.getName() + " (at " + theContext.getPath() + ") would require unsupported short form transformation: " + theValue.getClass() + " to " + theRule.getType());
						return false;		
					}
				}
 
				int errc = theContext.errorCount();
				//validateRule(canonical, rule, context);
				if (errc != theContext.errorCount()) {
					//this would indicate that the grammar is not well specified
      		log.warning("Short notation for " + theRule.getName() + " through " + hint + " at " + theContext.getPath() + " failed validation");
				}
				else {
       		log.finer("Short notation for " + theRule.getName() + " through " + hint + " at " + theContext.getPath() + " passed validation. Canonical form is " + canonical);

					if (this.canonicals != null)
						this.canonicals.put(
										((Checker.TOSCAValidator)theContext.getValidator()).getTarget(),
										theContext.getPath(), canonical);
					else {
						applyCanonical(
										((Checker.TOSCAValidator)theContext.getValidator()).getTarget().getTarget(),
										theContext.getPath(), canonical);
					}
					return true;
				}
			}
			else {
      	log.info("Grammar for rule " + theRule.getName() + " (at " + theContext.getPath() + ") would require unsupported short form transformation: " + theValue.getClass() + " to " + theRule.getType());
				return false;
			}
		}
		return false;
	}


	protected Object applyCanonical(Object theTarget,
																	String thePath,
															  	Object theCanonical) {

		//Object canonical = theCanonicals.get(thePath);
		//if (canonical != null) {
			String path = patchWhitespaces(
											patchIndexes(thePath));
	    try {
		  	JXPathContext.newContext(theTarget).setValue(path, theCanonical);
				log.finer("Applied canonical form at: " + path);

		//		if (doRemove)
		//			theCanonicals.remove(thePath);
			}
			catch (JXPathException jxpx) {
				log.log(Level.WARNING, "Failed to apply canonical to " + theTarget, jxpx);
			}
		//}
		return theCanonical;
	}

	public void applyCanonicals(Target theTarget) {
    
		if (this.canonicals == null) {
			return;
	  }

		Map<String, Object> targetCanonicals = this.canonicals.row(theTarget);
		if (targetCanonicals != null) {
	    log.finest("applying canonicals: " + targetCanonicals);
			for(Map.Entry<String, Object> canonicalEntry: targetCanonicals.entrySet()) {
				applyCanonical(theTarget.getTarget(), canonicalEntry.getKey(), canonicalEntry.getValue());
			}
		}
	}

  /** Given that we remembered the canonical forms that were needed during
	 * validation to replace the short forms we can apply them to the target
	 * yaml.
	 * We take advantage here of the fact that the context path maintained 
	 * during validation is compatible with (j)xpath, with the exception of
	 * sequence/array indentation ..
	 */

  private Pattern indexPattern = Pattern.compile("/\\p{Digit}+"),
									spacePattern = Pattern.compile("\\s");

  private String patchIndexes(CharSequence thePath) {
  	Matcher m = indexPattern.matcher(thePath);
		StringBuffer path = new StringBuffer();
		while (m.find()) {
			String index = m.group();
      index = "[" + (Integer.valueOf(index.substring(1)).intValue() + 1) + "]";
			m.appendReplacement(path, m.quoteReplacement(index));
		}
		m.appendTail(path);
		return path.toString();
	}
 
 
	private String patchWhitespaces(String thePath) {
		String[] elems = thePath.split("/");
		StringBuffer path = new StringBuffer();
		for (int i = 0; i < elems.length; i++) {
			if (spacePattern.matcher(elems[i]).find())
				path.append("[@name='")
						.append(elems[i])
						.append("']");
			else
				path.append("/")
						.append(elems[i]);
		}
		return path.toString();
	}

}
