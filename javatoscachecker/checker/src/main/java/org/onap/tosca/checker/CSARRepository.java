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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Reader;
import java.io.StringReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Collections;

import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import java.util.function.BiFunction;

import org.apache.commons.io.IOUtils;

/**
 * Handles targets located within the same CSAR file.
 * This is where the knowledge about the structure of a CSAR file should be placed.
 * TOSCA 1.0 CSAR archive compliant.
 */
public class CSARRepository extends Repository {

    private static Logger logger =
            Logger.getLogger("com.att.research.is.tosca.yaml.CSARRepository");
    private static final String CHARSET = "UTF-8";
    private String metaEntryName = "TOSCA-Metadata/TOSCA.meta";
    private byte[] data;
    private Properties meta = new Properties();
    private Map<URI, Target> entries = null;
	
	public CSARRepository(String theName, URI theRoot) throws IOException {
		super(theName, theRoot);
		load();
	}

	private void load() throws IOException {
		InputStream is = null;
        try {
            is = this.getRoot().toURL().openStream();
            this.data = IOUtils.toByteArray(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException iox) {
                    logger.log(Level.WARNING, "Failed to close input stream", iox);
                }
            }
        }
	}

	//one should read the meta-inf/MANIFEST.MF file before deciding that a file is text
	private Object processData(BiFunction<ZipEntry,InputStream,Object> theProcessor) {

		ZipInputStream archiveInputStream = new ZipInputStream(new ByteArrayInputStream(this.data));
		Object result = null;
		try {
			ZipEntry archiveEntry = null;
			while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
				result = theProcessor.apply(archiveEntry, archiveInputStream);
				if (null != result)
					return result;
				archiveInputStream.closeEntry();	
			}	
		}
		catch (IOException iox) {
			logger.log(Level.WARNING, "Failed to read archive", iox);
		}
		finally {
			try {
				archiveInputStream.close();
			}
			catch (IOException iox) {
			    logger.log(Level.WARNING, "Failed to close input stream", iox);
			}
		}
		return result;
	}

	/* this will be useful when processing new style meta information .. */
	private Object processMeta() {
		return
			processData( (entry,stream) -> {
				if (!entry.getName().equals(this.metaEntryName))
					return null;

				return readMeta(stream);
			});
	}
	
	private Boolean readMeta(InputStream theStream) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(theStream, CHARSET));
			this.meta.load(reader);
			return Boolean.TRUE;
		}
		catch(IOException iox) {
			logger.log(Level.WARNING, "Failed to read archive meta entry", iox);
			return Boolean.FALSE;
		}	
		//!!Do not close reader as it is used with processData which does the entry close itself
	}			

	private Map<URI,Target> entries() {
		if (this.entries == null) {
			this.entries = new HashMap<>();
			processData( (entry,stream) -> { 
				URI entryURI = this.rootURI.resolve(entry.getName());
				this.entries.put(entryURI, new CsarTarget(entry.getName(), entryURI)); 
				
				if (entry.getName().equals(this.metaEntryName))
					readMeta(stream);

				return null;
			});
		}
		return this.entries;
	}

	public Target mainTarget() {
		return entries().get(rootURI.resolve(this.meta.getProperty("Entry-Definitions")));
	}
	
	public Iterable<Target> targets() {
		return entries().values();
	}

	/** */
	public Target resolve(URI theURI) {
		return entries().get(theURI);
	}

	public TargetLocator getTargetLocator() {
		return new CSARTargetLocator();
	}

	/**
	 */
	private class CSARTargetLocator implements TargetLocator {

		/** */
		public boolean addSearchPath(URI theURI) {
			return false;
		}

		/** */
		public boolean addSearchPath(String thePath) {
			return false;
		}

		/** */
		public Iterable<URI> searchPaths() {
			return Collections.singleton(CSARRepository.this.rootURI);
		}
	
		/** */
		public Target resolve(String theName) {
			Target t = entries().get(CSARRepository.this.rootURI.resolve(theName));
			if (t == null) {
				//fallback: check relative to the main target folder
				t = entries().get(CSARRepository.this.mainTarget().getLocation().resolve(theName));
			}
			return t;
		}
	}

	/** */
	private class CsarTarget extends Target {
					
		private String content;

		private CsarTarget(String theName, URI theUri) {
			super(theName, theUri);
		}

		private String content() {
			if (this.content == null) {
				this.content = (String)processData( (entry,stream) -> {
					//go over the entries and find the one with a matching name
					ByteArrayOutputStream out = null;
					if (entry.getName().equals(getName())) {
						byte[] buffer = new byte[4096];
						int len = 0;
						out = new ByteArrayOutputStream();
						try {
							while ((len = stream.read(buffer)) > 0) {
   							out.write(buffer, 0, len);
							}
							logger.info(entry.getName() + ": " + out.toString(CHARSET));
						}
						catch (IOException iox) {
							logger.warning("Failed to read entry data: " + iox);
							out = null;
							return out;
						}
					}
					//!!Do not close as it is used with processData which does the entry close itself

					try {
						return (out != null) ? out.toString(CHARSET) : null;
					}
					catch (UnsupportedEncodingException uex) {
						logger.warning("Failed to process entry data as string: " + uex);
						return "";
					}
				});
			}
			return this.content;
		}

		@Override
		public Reader open() throws IOException {
			return new StringReader(content());
		}
	}

}

