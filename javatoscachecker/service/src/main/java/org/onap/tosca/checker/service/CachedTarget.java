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

import java.io.FilterReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;

import java.net.URI;

import org.springframework.core.io.Resource;

import org.apache.commons.io.input.CharSequenceReader;

import org.onap.tosca.checker.Target;


/**
 * Cache of the target content until invalidation ..
 */
public class CachedTarget extends Target {

	private StringBuilder	content;

	public CachedTarget(String theName, URI theResource) {
		super(theName, theResource);
	}

	public CachedTarget(Resource theResource) throws IOException {
		super(theResource.getFilename(), theResource.getURI());
	}

	protected CachedTarget(CachedTarget theSource) {
		super(theSource.getName(), theSource.getLocation());
		if (theSource.hasContent()) {
			setContent(theSource.getContent());
		}
	}

	protected boolean hasContent() {
		return this.content != null;
	}

	protected CharSequence getContent() {
		return this.content == null ? null : this.content;
	}

	protected void setContent(CharSequence theContent) {
		this.content = new StringBuilder(theContent);
	}

	/*
   */
	public void invalidate() {
		this.content = null;
		setTarget(null);
	}

	@Override
	public Reader open() throws IOException {
		return this.content != null ? 
						new CharSequenceReader(this.content) : 
						new FilterReader(super.open()) {
							{
								content = new StringBuilder();
							}

							public int read(char[] cbuf, int off, int len) throws IOException {
								int res = super.read(cbuf, off, len);
								if (res > 0)
									content.append(cbuf, off, res);
								return res;
							}

							public int read() throws IOException {
								int res = super.read();
								if (res > 0)
									/* the cast here is troublesome: the original stream had an encoding and this cast has to
									done with respect to that. */
									content.append((char)res);
								return res;
							}
	
						};
	}

}

