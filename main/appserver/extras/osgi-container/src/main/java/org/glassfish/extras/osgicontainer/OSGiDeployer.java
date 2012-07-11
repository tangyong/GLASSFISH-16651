/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.deployment.Deployer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.MetaData;
import org.glassfish.api.deployment.OpsParams;
import org.jvnet.hk2.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.service.packageadmin.PackageAdmin;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes.Name;

/**
 * OSGi deployer, takes care of loading and cleaning modules from the OSGi runtime.
 *
 * @author Jerome Dochez
 * @author Sanjeeb Sahoo
 */
@Service
public class OSGiDeployer implements Deployer<OSGiContainer, OSGiDeployedBundle> {

    private static final String BUNDLE_ID = "bundle.id";

    public OSGiDeployedBundle load(OSGiContainer container, DeploymentContext context) {
        return new OSGiDeployedBundle(getApplicationBundle(context));
    }

    public void unload(OSGiDeployedBundle appContainer, DeploymentContext context) {
    }

    public void clean(DeploymentContext context) {
        try {
            OpsParams params = context.getCommandParameters(OpsParams.class);
            // we should clean for both undeployment and the failed deployment
            if (params.origin.isUndeploy() || params.origin.isDeploy()) {
                Bundle bundle = getApplicationBundle(context);
                bundle.uninstall();
                getPA().refreshPackages(new Bundle[]{bundle});
                System.out.println("Uninstalled " + bundle);
            }
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
    }

    private PackageAdmin getPA() {
        final BundleContext context = getBundleContext();
        return (PackageAdmin) context.getService(context.getServiceReference(PackageAdmin.class.getName()));
    }

    public MetaData getMetaData() {
        return null;
    }

    public <V> V loadMetaData(Class<V> type, DeploymentContext context) {
        return null;
    }

    public boolean prepare(DeploymentContext context) {
        File file = context.getSourceDir();
        try {
            OpsParams params = context.getCommandParameters(OpsParams.class);
            if (params.origin.isDeploy()) {
                assert(file.isDirectory());
                Bundle bundle = getBundleContext().installBundle(makeBundleLocation(context,file));
                
                System.out.println("Installed " + bundle + " from " + bundle.getLocation());
            }
        } catch (BundleException e) {
            throw new RuntimeException(e);
        }
        return true; 
    }

    private BundleContext getBundleContext() {
        return BundleReference.class.cast(getClass().getClassLoader()).getBundle().getBundleContext();
    }

    private Bundle getApplicationBundle(DeploymentContext context) {
        String location = makeBundleLocation(context,context.getSourceDir());
        for(Bundle b : getBundleContext().getBundles()) {
            if (location.equals(b.getLocation())) {
                return b;
            }
        }
        throw new RuntimeException("Unable to determine bundle corresponding to application location " + context.getSourceDir());
    }
    
    //TangYong Added
    private String makeBundleLocation(DeploymentContext context,File file) {
    	Properties props = context.getAppProps();
    	
        if (props != null){
        	String uriScheme = props.getProperty("uriScheme");
        	if ((uriScheme != null) && ("wab").equalsIgnoreCase(uriScheme)){
        		StringBuilder wabpath = new StringBuilder();
        		try {
					wabpath.append(context.getOriginalSource().getURI().toURL().toExternalForm());
					
					//Because the current deploy options do not suport liking queryParams="Web-ContextPath=/test_sample1"
					//In the future, maybe improve this.
					String wcp = props.getProperty("Web-ContextPath"); 
					if (wcp != null){
						wabpath.append("?" + "Web-ContextPath=" + wcp);
					}
					
					String bundleName = props.getProperty("Bundle-SymbolicName");
					if (bundleName != null){
						wabpath.append("&" + "Bundle-SymbolicName=" + bundleName);
					}
					
					String bundleVersion = props.getProperty("Bundle-Version");
					if (bundleVersion != null){
						wabpath.append("&" + "Bundle-Version=" + bundleVersion);
					}
					
					String bundleMV = props.getProperty("Bundle-ManifestVersion");
					if (bundleMV != null){
						wabpath.append("&" + "Bundle-ManifestVersion=" + bundleMV);
					}
					
					String importPKG = props.getProperty("Import-Package");
					if (importPKG != null){
						wabpath.append("&" + "Import-Package=" + importPKG);
					}	
					
					URL waburl = new URL("webbundle",null, wabpath.toString());
					
					 return waburl.toExternalForm();
					
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
        		
        		/*
        		String queryParams = props.getProperty("queryParams");
        		if (queryParams != null){
        			StringTokenizer st = new StringTokenizer(queryParams, ",");
                    while (st.hasMoreTokens())
                    {
                        String next = st.nextToken();
                        wabpath.append("?" + next);
                    }
                    
                    URL waburl = null;
					try {
						waburl = new URL("webbundle",null, wabpath.toString());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					}*/					                  
        		}
        	}
                   	
        return makeBundleLocation(file);
    }

    private String makeBundleLocation(File file) {
        return "reference:" + file.toURI();
    }
}
