package org.ourgrid.androidworker.services;

import org.apache.log4j.Logger;
import org.ourgrid.common.interfaces.management.RemoteWorkerManagement;
import org.ourgrid.common.interfaces.management.RemoteWorkerManagementClient;
import org.ourgrid.common.interfaces.to.WorkerStatus;
import org.ourgrid.worker.WorkerConstants;

import android.content.Context;
import br.edu.ufcg.lsd.commune.api.FailureNotification;
import br.edu.ufcg.lsd.commune.api.InvokeOnDeploy;
import br.edu.ufcg.lsd.commune.api.MonitoredBy;
import br.edu.ufcg.lsd.commune.api.RecoveryNotification;
import br.edu.ufcg.lsd.commune.container.servicemanager.ServiceManager;
import br.edu.ufcg.lsd.commune.identification.DeploymentID;
import br.edu.ufcg.lsd.commune.identification.ServiceID;

public class RemoteWorkerManagementImpl implements RemoteWorkerManagement {

	private static final Logger LOGGER = Logger.getLogger(RemoteWorkerManagementImpl.class);
	private ServiceManager serviceManager;
	private String remotePeerPubKey;
	private Context appContext;
	
	public RemoteWorkerManagementImpl(Context appContext, String peerPubKey) {
		this.appContext = appContext;
		this.remotePeerPubKey = peerPubKey;
	}

	@InvokeOnDeploy
	public void init(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}
	
	@Override
	public void workForBroker(@MonitoredBy (WorkerConstants.REMOTE_WORKER_MANAGEMENT) RemoteWorkerManagementClient remotePeer,
			String brokerPubKey) {
		if (!serviceManager.getSenderPublicKey().equals(remotePeerPubKey)) {
			LOGGER.warn("Not allocated for this peer.");
			return;
		}
		DAO.getInstance().setRwmClientId(
				serviceManager.getStubDeploymentID(remotePeer));
		ServiceID wId = Utils.deploy(serviceManager,
				WorkerConstants.WORKER, 
				new WorkerImpl(appContext, brokerPubKey));
		DAO.getInstance().setStatus(WorkerStatus.ALLOCATED_FOR_BROKER);
		remotePeer.statusChangedAllocatedForBroker(wId);
	}
	
	/**
	 * Informs that the master peer failed.
	 * @param monitorable 
	 * @param monitorableID
	 */
	@FailureNotification
	public void doNotifyFailure(RemoteWorkerManagementClient monitorable, DeploymentID monitorableID) {
	}

	@RecoveryNotification
	public void doNotifyRecovery(RemoteWorkerManagementClient monitorable, DeploymentID monitorableID) {
	}
}
