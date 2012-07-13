/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.extras.osgicontainer;

import org.glassfish.api.deployment.archive.*;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.internal.deployment.GenericHandler;
import org.glassfish.internal.api.DelegatingClassLoader;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PreDestroy;
import org.jvnet.hk2.component.Singleton;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.*;
import java.net.URL;
import java.net.URI;
import java.lang.ref.WeakReference;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleDefinition;
import com.sun.enterprise.module.common_impl.DefaultModuleDefinition;
import com.sun.enterprise.util.io.FileUtils;

import javax.inject.Inject;

/**
 * Archive Handler for OSGi modules.
 * 
 * @author Jerome Dochez tangyong@cn.fujitsu.com
 */
@Service(name = OSGiArchiveDetector.OSGI_ARCHIVE_TYPE)
@Scoped(Singleton.class)
public class OSGiArchiveHandler extends GenericHandler implements
		CompositeHandler {

	@Inject
	private OSGiArchiveDetector detector;

	public String getArchiveType() {
		return OSGiArchiveDetector.OSGI_ARCHIVE_TYPE;
	}

	public boolean accept(ReadableArchive source, String entryName) {
		// we hide everything so far.
		return false;
	}

	public boolean handles(ReadableArchive archive) throws IOException {
		return detector.handles(archive);
	}

	public ClassLoader getClassLoader(ClassLoader parent,
			DeploymentContext context) {
		return parent;
	}

	public String getDefaultApplicationName(ReadableArchive archive,
			DeploymentContext context) {
		return getDefaultApplicationNameFromArchiveName(archive);
	}

	/**
	 * Prepares the jar file to a format the ApplicationContainer is expecting.
	 * This could be just a pure unzipping of the jar or nothing at all.
	 * 
	 * @param source
	 *            of the expanding
	 * @param target
	 *            of the expanding
	 * @param context
	 *            deployment context
	 * @throws IOException
	 *             when the archive is corrupted
	 */
	@Override
	public void expand(ReadableArchive source, WritableArchive target,
			DeploymentContext context) throws IOException {

		Enumeration<String> e = source.entries();
		while (e.hasMoreElements()) {
			String entryName = e.nextElement();
			InputStream is = new BufferedInputStream(source.getEntry(entryName));
			OutputStream os = null;
			try {
				os = target.putNextEntry(entryName);
				FileUtils.copy(is, os, source.getEntrySize(entryName));
			} finally {
				if (os != null) {
					target.closeEntry();
				}
				is.close();
			}
		}

		// last is manifest is existing.
		Manifest m = null;

		// see [GLASSFISH-16651]
		// if uriScheme is webbundle, we need to construct a new URL based on
		// user's input and souce parameter and call openStream() on it.
		// user's input can be the following:
		// asadmin deploy --properties uriScheme=webBundle:Bundle-SymbolicName=foo:
		//                  Import-Package=javax.servlet:Web-ContextPath=/foo /tmp/foo.war
		Properties props = context
				.getCommandParameters(DeployCommandParameters.class).properties;
		if ((props != null) && (props.containsKey("uriScheme"))) {
			Enumeration p = props.propertyNames();
			StringBuilder sb = new StringBuilder();

			sb.append(props.getProperty("uriScheme"));
			sb.append(":");
			sb.append(context.getOriginalSource().getURI().toURL()
					.toExternalForm()
					+ "?");

			while (p.hasMoreElements()) {
				String key = (String) p.nextElement();
				if ("uriScheme".equalsIgnoreCase(key)) {
					continue;
				}
				sb.append(key);
				sb.append("=");
				sb.append(props.getProperty(key));
				
				if (p.hasMoreElements()){
					sb.append("&");
				}
			}

			URL url = new URL(sb.toString());
			JarInputStream jis = new JarInputStream(url.openStream());
			m = jis.getManifest();
			
			jis.close();
		}else{
			m = source.getManifest();
		}
		
		if (m != null) {
			OutputStream os = target.putNextEntry(JarFile.MANIFEST_NAME);
			m.write(os);
			target.closeEntry();
		}
	}
}