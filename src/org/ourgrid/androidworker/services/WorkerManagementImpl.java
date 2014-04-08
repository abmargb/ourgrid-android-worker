package org.ourgrid.androidworker.services;

import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.log4j.Logger;
import org.ourgrid.common.WorkerLoginResult;
import org.ourgrid.common.interfaces.management.WorkerManagement;
import org.ourgrid.common.interfaces.management.WorkerManagementClient;
import org.ourgrid.common.interfaces.to.WorkerStatus;
import org.ourgrid.common.specification.OurGridSpecificationConstants;
import org.ourgrid.common.specification.worker.WorkerSpecification;
import org.ourgrid.common.specification.worker.WorkerSpecificationConstants;
import org.ourgrid.worker.WorkerConstants;

import android.content.Context;
import br.edu.ufcg.lsd.commune.api.FailureNotification;
import br.edu.ufcg.lsd.commune.api.InvokeOnDeploy;
import br.edu.ufcg.lsd.commune.api.MonitoredBy;
import br.edu.ufcg.lsd.commune.api.RecoveryNotification;
import br.edu.ufcg.lsd.commune.container.ObjectDeployment;
import br.edu.ufcg.lsd.commune.container.servicemanager.ServiceManager;
import br.edu.ufcg.lsd.commune.identification.DeploymentID;
import br.edu.ufcg.lsd.commune.identification.ServiceID;

public class WorkerManagementImpl implements WorkerManagement {

	private static final Logger LOGGER = Logger.getLogger(WorkerManagementImpl.class);
	private ServiceManager serviceManager;
	
	private Context appContext;
	
	public WorkerManagementImpl(Context appContext) {
		this.appContext = appContext;
	}

	@InvokeOnDeploy
	public void init(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}
	
	public void loginSucceeded(@MonitoredBy (WorkerConstants.LOCAL_WORKER_MANAGEMENT) 
			WorkerManagementClient workerManagementClient,
			WorkerLoginResult loginResult) {
		if (!checkSender()) {
			return;
		}
		DAO.getInstance().setStatus(WorkerStatus.IDLE);
		getPeer().statusChanged(DAO.getInstance().getStatus());
	}

	public void workForPeer(String peerPubKey, List<String> usersDN,
			List<X509Certificate> caCertificates) {
		if (!checkSender()) {
			return;
		}
		ServiceID rwmId = Utils.deploy(serviceManager,
				WorkerConstants.REMOTE_WORKER_MANAGEMENT, 
				new RemoteWorkerManagementImpl(appContext, peerPubKey));
		DAO.getInstance().setStatus(WorkerStatus.ALLOCATED_FOR_PEER);
		getPeer().statusChangedAllocatedForPeer(rwmId, peerPubKey);
	}

	public void workForPeer(String peerPubKey) {
		if (!checkSender()) {
			return;
		}
		ServiceID rwmId = Utils.deploy(serviceManager,
				WorkerConstants.REMOTE_WORKER_MANAGEMENT, 
				new RemoteWorkerManagementImpl(appContext, peerPubKey));
		DAO.getInstance().setStatus(WorkerStatus.ALLOCATED_FOR_PEER);
		getPeer().statusChangedAllocatedForPeer(rwmId, peerPubKey);
	}

	public void workForBroker(DeploymentID brokerID) {
		if (!checkSender()) {
			return;
		}
		ServiceID wId = Utils.deploy(serviceManager,
				WorkerConstants.WORKER, 
				new WorkerImpl(appContext, brokerID.getPublicKey()));
		
		DAO.getInstance().setStatus(WorkerStatus.ALLOCATED_FOR_BROKER);
		getPeer().statusChangedAllocatedForBroker(wId, brokerID.getPublicKey());
	}

	public void stopWorking() {
		if (!checkSender()) {
			return;
		}
		DeploymentID workerClientId = DAO.getInstance().getWorkerClientId();
		if (workerClientId != null) {
			serviceManager.release(workerClientId.getServiceID());
			DAO.getInstance().setWorkerClientId(null);
		}
		Utils.undeploy(serviceManager, WorkerConstants.WORKER);
		
		DeploymentID rwmClientId = DAO.getInstance().getRwmClientId();
		if (rwmClientId != null) {
			serviceManager.release(rwmClientId.getServiceID());
			DAO.getInstance().setRwmClientId(null);
		}
		Utils.undeploy(serviceManager, WorkerConstants.REMOTE_WORKER_MANAGEMENT);
		
		DAO.getInstance().setStatus(WorkerStatus.IDLE);
		getPeer().statusChanged(DAO.getInstance().getStatus());
	}
	
	/**
	 * Informs that the master peer failed.
	 * @param monitorable 
	 * @param monitorableID
	 */
	@FailureNotification
	public void doNotifyFailure(WorkerManagementClient monitorable, DeploymentID monitorableID) {
		DAO.getInstance().setWmClientId(null);
	}

	private boolean checkSender() {
		String publicKey = DAO.getInstance().getWmClientId().getPublicKey();
		if (!serviceManager.getSenderPublicKey().equals(publicKey)) {
			LOGGER.warn("Not allocated for this peer.");
			return false;
		}
		return true;
	}
	
	@RecoveryNotification
	public void doNotifyRecovery(WorkerManagementClient monitorable, DeploymentID monitorableID) {
		WorkerSpecification workerSpecification = new WorkerSpecification();
		DeploymentID myDeploymentID = serviceManager.getMyDeploymentID();
		workerSpecification.putAttribute(OurGridSpecificationConstants.USERNAME, 
				myDeploymentID.getUserName());
		workerSpecification.putAttribute(OurGridSpecificationConstants.SERVERNAME, 
				myDeploymentID.getServerName());
		workerSpecification.putAttribute(WorkerSpecificationConstants.OS, "android");
		
		DAO.getInstance().setWmClientId(monitorableID);
		monitorable.workerLogin(getProxy(), workerSpecification);
	}

	private WorkerManagement getProxy() {
		ObjectDeployment objectDeployment = serviceManager.getObjectDeployment(
				WorkerConstants.LOCAL_WORKER_MANAGEMENT);
		WorkerManagement wm = (WorkerManagement) objectDeployment.getObject();
		return wm;
	}
	
	private WorkerManagementClient getPeer() {
		return serviceManager.getStub(DAO.getInstance().getWmClientId().getServiceID(), 
				WorkerManagementClient.class);
	}
}
