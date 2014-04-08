package org.ourgrid.androidworker.services;

import br.edu.ufcg.lsd.commune.container.servicemanager.ServiceManager;
import br.edu.ufcg.lsd.commune.identification.ServiceID;

public class Utils {

	public static ServiceID deploy(ServiceManager serviceManager, 
			String serviceName, Object impl) {
		undeploy(serviceManager, serviceName);
		serviceManager.deploy(serviceName, impl);
		return new ServiceID(serviceManager.getMyDeploymentID().getContainerID(), 
				serviceName);
	}

	public static void undeploy(ServiceManager serviceManager,
			String serviceName) {
		try {
			serviceManager.undeploy(serviceName);
		} catch (Throwable t) {
		}
	}
}
