package org.ourgrid.androidworker;

import org.ourgrid.androidworker.services.WorkerManagementImpl;
import org.ourgrid.common.interfaces.management.WorkerManagementClient;
import org.ourgrid.peer.PeerConstants;
import org.ourgrid.worker.WorkerConstants;

import android.content.Context;
import br.edu.ufcg.lsd.commune.ServerModule;
import br.edu.ufcg.lsd.commune.context.ModuleContext;
import br.edu.ufcg.lsd.commune.identification.ServiceID;
import br.edu.ufcg.lsd.commune.network.xmpp.CommuneNetworkException;
import br.edu.ufcg.lsd.commune.processor.ProcessorStartException;
import br.edu.ufcg.lsd.commune.processor.interest.InterestRequirements;

public class WorkerComponentAndroid extends ServerModule {

	private Context appContext;

	public WorkerComponentAndroid(Context appContext, ModuleContext context)
			throws CommuneNetworkException, ProcessorStartException {
		super(WorkerConstants.MODULE_NAME, context);
		this.appContext = appContext;
	}

	@Override
	protected void connectionCreated() {
		super.connectionCreated();
		start();
	}
	
	private void start() {
		deploy(WorkerConstants.LOCAL_WORKER_MANAGEMENT, new WorkerManagementImpl(appContext));
		ServiceID peerId = new ServiceID("lsd-voluntary-peer", "xmpp.ourgrid.org", 
				PeerConstants.MODULE_NAME, PeerConstants.WORKER_MANAGEMENT_CLIENT_OBJECT_NAME);
		registerInterest(WorkerConstants.LOCAL_WORKER_MANAGEMENT, WorkerManagementClient.class, 
				peerId, new InterestRequirements(getContext()));
	}
}
