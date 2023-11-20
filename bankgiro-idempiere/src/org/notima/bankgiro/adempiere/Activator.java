package org.notima.bankgiro.adempiere;

import org.osgi.framework.BundleContext;


public class Activator extends org.adempiere.plugin.utils.AdempiereActivator {
	
	private static BundleContext ctx;
	
	public static BundleContext getCtx() {
		return ctx;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		// super.start(bundleContext);
		
//		setContext(bundleContext);
//		logger.info(getName() + " " + getVersion() + " starting...");
//		start();
//		logger.info(getName() + " " + getVersion() + " ready.");
		ctx = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		// super.stop(bundleContext);
		ctx = null;
	}


}
