package org.ourgrid.androidworker.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.ourgrid.broker.communication.actions.ErrorOcurredMessageHandle;
import org.ourgrid.broker.communication.actions.HereIsFileInfoMessageHandle;
import org.ourgrid.broker.communication.actions.HereIsGridProcessResultMessageHandle;
import org.ourgrid.broker.communication.actions.WorkerIsReadyMessageHandle;
import org.ourgrid.common.FileTransferInfo;
import org.ourgrid.common.executor.ExecutorResult;
import org.ourgrid.common.filemanager.FileInfo;
import org.ourgrid.common.interfaces.Worker;
import org.ourgrid.common.interfaces.WorkerClient;
import org.ourgrid.common.interfaces.to.GridProcessErrorTypes;
import org.ourgrid.common.interfaces.to.GridProcessHandle;
import org.ourgrid.common.interfaces.to.MessageHandle;
import org.ourgrid.worker.WorkerConstants;
import org.ourgrid.worker.business.controller.GridProcessError;
import org.ourgrid.worker.communication.processors.handle.GetFileInfoMessageHandle;
import org.ourgrid.worker.communication.processors.handle.GetFilesMessageHandle;
import org.ourgrid.worker.communication.processors.handle.RemoteExecuteMessageHandle;

import android.content.Context;
import br.edu.ufcg.lsd.commune.api.FailureNotification;
import br.edu.ufcg.lsd.commune.api.InvokeOnDeploy;
import br.edu.ufcg.lsd.commune.api.MonitoredBy;
import br.edu.ufcg.lsd.commune.api.RecoveryNotification;
import br.edu.ufcg.lsd.commune.container.ObjectDeployment;
import br.edu.ufcg.lsd.commune.container.servicemanager.ServiceManager;
import br.edu.ufcg.lsd.commune.processor.filetransfer.IncomingTransferHandle;
import br.edu.ufcg.lsd.commune.processor.filetransfer.OutgoingTransferHandle;
import br.edu.ufcg.lsd.commune.processor.filetransfer.TransferProgress;

public class WorkerImpl implements Worker {

	private static final Logger LOGGER = Logger.getLogger(WorkerImpl.class);
	private ServiceManager serviceManager;
	private String brokerPubKey;
	private Context appContext;
	private File playpen;
	
	public WorkerImpl(Context appContext, String brokerPubKey) {
		this.appContext = appContext;
		this.brokerPubKey = brokerPubKey;
	}

	@InvokeOnDeploy
	public void init(ServiceManager serviceManager) {
		this.serviceManager = serviceManager;
	}
	
	@Override
	public void shutdown(boolean force) {
		// TODO Auto-generated method stub
	}

	@Override
	public void transferRejected(OutgoingTransferHandle handle) {
		LOGGER.warn("Transfer rejected " + handle);
	}

	@Override
	public void outgoingTransferFailed(OutgoingTransferHandle handle,
			Exception failCause, long amountWritten) {
		LOGGER.warn(failCause);
		getWorkerClient().sendMessage(new ErrorOcurredMessageHandle(
				new GridProcessError(GridProcessErrorTypes.IO_ERROR)));
	}

	@Override
	public void outgoingTransferCancelled(OutgoingTransferHandle handle,
			long amountWritten) {
		LOGGER.warn("Outgoing transfer cancelled " + handle);	
	}

	@Override
	public void outgoingTransferCompleted(OutgoingTransferHandle handle,
			long amountWritten) {
		LOGGER.debug("Outgoing transfer completed " + handle);		
	}

	@Override
	public void updateTransferProgress(TransferProgress transferProgress) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void transferRequestReceived(IncomingTransferHandle handle) {
		LOGGER.debug("Transfer request received " + handle);
		serviceManager.acceptTransfer(handle, getProxy(), 
				new File(playpen, handle.getLogicalFileName()));
	}

	@Override
	public void incomingTransferFailed(IncomingTransferHandle handle,
			Exception failCause, long amountWritten) {
		LOGGER.warn(failCause);
		getWorkerClient().sendMessage(new ErrorOcurredMessageHandle(
				new GridProcessError(GridProcessErrorTypes.IO_ERROR)));
	}

	@Override
	public void incomingTransferCompleted(IncomingTransferHandle handle,
			long amountWritten) {
		LOGGER.debug("Incoming transfer completed " + handle);
	}

	@Override
	public void startWork(@MonitoredBy(WorkerConstants.WORKER) WorkerClient workerClient, 
			long requestID, GridProcessHandle replicaHandle) {
		if (!checkSender()) {
			return;
		}
		DAO.getInstance().setWorkerClientId(
				serviceManager.getStubDeploymentID(workerClient));
		long playpenId = Math.abs(new Random().nextLong());
		playpen = new File(appContext.getCacheDir(), "playpen-" + playpenId);
		playpen.mkdirs();
		getWorkerClient().sendMessage(new WorkerIsReadyMessageHandle());
	}

	@Override
	public void sendMessage(MessageHandle handle) {
		if (!checkSender()) {
			return;
		}
		
		String actionName = handle.getActionName();
		if (actionName.equals(WorkerConstants.REMOTE_EXECUTE_ACTION_NAME)) {
			execute((RemoteExecuteMessageHandle) handle);
		} else if (actionName.equals(WorkerConstants.GET_FILE_INFO_ACTION_NAME)) {
			getFileInfo((GetFileInfoMessageHandle) handle);
		} else if (actionName.equals(WorkerConstants.GET_FILES_ACTION_NAME)) {
			getFiles((GetFilesMessageHandle) handle);
		}
	}
	
	private void getFiles(GetFilesMessageHandle handle) {
		for (FileTransferInfo fti : handle.getFiles()) {
			File file = new File(playpen, fti.getFilePath());
			OutgoingTransferHandle oth = new OutgoingTransferHandle(
					fti.getTransferHandleID(), file.getName(), file, "GET", 
					DAO.getInstance().getWorkerClientId());
			serviceManager.startTransfer(oth, getProxy());
		}
	}

	private void getFileInfo(GetFileInfoMessageHandle handle) {
		getWorkerClient().sendMessage(new HereIsFileInfoMessageHandle(handle.getHandleId(), 
				new FileInfo()));
	}

	private void execute(RemoteExecuteMessageHandle handle) {
		try {
			ExecutorResult executorResult = exec(handle.getCommand());
			getWorkerClient().sendMessage(new HereIsGridProcessResultMessageHandle(
					executorResult));
		} catch (Exception e) {
			getWorkerClient().sendMessage(new ErrorOcurredMessageHandle(
					new GridProcessError(e, GridProcessErrorTypes.APPLICATION_ERROR)));
		}
	}
	
	private ExecutorResult exec(String command) throws Exception {
		installBusyBox();
		
		File execFile = new File(playpen, "exec-" + Math.abs(new Random().nextLong()));
		IOUtils.write(command, new FileOutputStream(execFile));
		
		ProcessBuilder chmodPB = new ProcessBuilder("chmod", "755", "busybox");
		chmodPB.directory(playpen);
		int chmodExitValue = chmodPB.start().waitFor();
		
		if (chmodExitValue != 0) {
			throw new Exception("Could not chmod busybox.");
		}
		
		ProcessBuilder pb = new ProcessBuilder("./busybox", "ash", execFile.getName());
		pb.directory(playpen);
		Process process = pb.start();
		
		int exitValue = process.waitFor();
		
		String stdout = IOUtils.toString(process.getInputStream());
		String stderr = IOUtils.toString(process.getErrorStream());
		
		return new ExecutorResult(exitValue, stdout, stderr);
	}
	
	private void installBusyBox() {
		try {
			InputStream in = appContext.getAssets().open("native/busybox");
			File busyboxFile = new File(playpen, "busybox");
			FileOutputStream out = new FileOutputStream(busyboxFile);
			IOUtils.copy(in, out);
			in.close();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private WorkerClient getWorkerClient() {
		return (WorkerClient) serviceManager.getStub(
				DAO.getInstance().getWorkerClientId().getServiceID(), 
				WorkerClient.class);
	}

	/**
	 * Informs that the master peer failed.
	 * @param monitorable 
	 * @param monitorableID
	 */
	@FailureNotification
	public void doNotifyFailure(WorkerClient monitorable) {
	}

	@RecoveryNotification
	public void doNotifyRecovery(WorkerClient monitorable) {
		
	}

	private boolean checkSender() {
		if (!serviceManager.getSenderPublicKey().equals(brokerPubKey)) {
			LOGGER.warn("Not allocated for this broker.");
			return false;
		}
		return true;
	}
	
	private Worker getProxy() {
		ObjectDeployment objectDeployment = serviceManager.getObjectDeployment(
				WorkerConstants.WORKER);
		Worker w = (Worker) objectDeployment.getObject();
		return w;
	}
}
