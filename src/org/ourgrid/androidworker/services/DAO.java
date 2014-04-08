package org.ourgrid.androidworker.services;

import org.ourgrid.common.interfaces.to.WorkerStatus;

import br.edu.ufcg.lsd.commune.identification.DeploymentID;

public class DAO {

	private static DAO instance = null;
	
	private DeploymentID workerClientId;
	private DeploymentID rwmClientId;
	private DeploymentID wmClientId;
	private WorkerStatus status;
	
	public static DAO getInstance() {
		if (instance == null) {
			instance = new DAO();
		}
		return instance;
	}
	
	public WorkerStatus getStatus() {
		return status;
	}

	public void setStatus(WorkerStatus status) {
		this.status = status;
	}



	public DeploymentID getWorkerClientId() {
		return workerClientId;
	}

	public void setWorkerClientId(DeploymentID workerClientId) {
		this.workerClientId = workerClientId;
	}

	public DeploymentID getRwmClientId() {
		return rwmClientId;
	}

	public void setRwmClientId(DeploymentID rwmClientId) {
		this.rwmClientId = rwmClientId;
	}

	public DeploymentID getWmClientId() {
		return wmClientId;
	}

	public void setWmClientId(DeploymentID wmClientId) {
		this.wmClientId = wmClientId;
	}
	
}
