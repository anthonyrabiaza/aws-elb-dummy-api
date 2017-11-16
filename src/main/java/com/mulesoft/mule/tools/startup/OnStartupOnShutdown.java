package com.mulesoft.mule.tools.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.annotations.expressions.Lookup;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.Callable;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.Flow;
import org.mule.context.notification.MuleContextNotification;
import org.mule.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OnStartupOnShutdown class <br/>
 * This class requires the configuration in the XML (in between OnStartupOnShutdown and END OnStartupOnShutdown)
 * 
 * @author anthony.rabiaza@mulesoft.com
 *
 */
@SuppressWarnings("rawtypes")
public class OnStartupOnShutdown implements Callable, MuleContextNotificationListener, MuleContextAware {

	private static Logger log = LoggerFactory.getLogger(OnStartupOnShutdown.class);

	@Lookup 
	private MuleContext muleContext; 
	
	public void setMuleContext(MuleContext muleContext) { 
		this.muleContext = muleContext; 
	} 

	static OnStartupOnShutdown s_onStartup;
	static List<Flow> s_flowsToStart;

	public OnStartupOnShutdown() {
		s_onStartup = this;
		try {
			log.info("## Init of OnStartup");
			s_flowsToStart = new ArrayList<>();
		} catch (Exception e) {
			log.error("## Error OnStartup", e);
		}

	}

	public static OnStartupOnShutdown getInstance() {
		return s_onStartup;
	}

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception {
		log.info("## Receiving onCall");
		return null;
	}

	@Override
	public void onNotification(ServerNotification notification) {
		Collection<FlowConstruct> flows;
		//if the context has been started
		if(notification.getAction() == MuleContextNotification.CONTEXT_STARTED ){
			log.info("## RECEIVED START NOTIFICATION");

			//obtain the context
			MuleContext muleContext = ((MuleContextNotification)notification).getMuleContext();

			//get a hold of the Flows defined in the application
			flows = muleContext.getRegistry().lookupFlowConstructs();

			for(FlowConstruct flowConstruct : flows) {
				Flow flow = (Flow)flowConstruct;
				//if the initial state was stopped
				if (!flow.getName().startsWith("onShutdown") && StringUtils.equals(flow.getInitialState(), AbstractFlowConstruct.INITIAL_STATE_STOPPED)){
					//adding the flow
					s_flowsToStart.add(flow);
				}
			}

		} else if(notification.getAction() == MuleContextNotification.CONTEXT_STOPPING) {
			log.info("## RECEIVED STOP NOTIFICATION");
			beforeShutdown();
		}
	}

	public void onReady() {
		log.info("## IS READY ACTIVITIES");
		for (Iterator iterator = s_flowsToStart.iterator(); iterator.hasNext();) {
			Flow flow = (Flow) iterator.next();
			//start the flows
			try {
				log.info("## STARTING FLOW " + flow.getName());
				flow.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			new ELBEvent().onStartup(muleContext);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		log.info("## END OF IS READY ACTIVITIES");
	}

	public void beforeShutdown() {
		log.info("## SHUTDOWN ACTIVITIES");
		//Some activities
		try {
			new ELBEvent().onShutdown(muleContext);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
		log.info("## END OF SHUTDOWN ACTIVITIES");
	}

}
