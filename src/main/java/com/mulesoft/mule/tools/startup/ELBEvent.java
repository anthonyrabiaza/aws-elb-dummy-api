package com.mulesoft.mule.tools.startup;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mule.api.MuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ELBEvent class with manage the communication with AWS ELB System API
 * @author anthony.rabiaza@mulesoft.com
 */
public class ELBEvent {

	private static Logger log = LoggerFactory.getLogger(ELBEvent.class);

	public enum State {
		start,
		stop
	}

	private List<String> getInterfaces() throws SocketException {
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
		List<String> interfaces = new ArrayList<String>();
		for (NetworkInterface netint : Collections.list(nets)) {
			interfaces.addAll(getInterface(netint));
		}
		return interfaces;
	}

	private List<String> getInterface(NetworkInterface netint) throws SocketException {
		List<String> interfaces = new ArrayList<String>();
		Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
		for (InetAddress inetAddress : Collections.list(inetAddresses)) {
			interfaces.add(inetAddress.getHostAddress());
		}
		return interfaces;
	}

	private void onEvent(MuleContext muleContext, State state) throws Exception{
		Map<String, String> parameters = new HashMap<>();
		parameters.put("apiName", ((org.mule.module.launcher.MuleApplicationClassLoader)this.getClass().getClassLoader()).getAppName());
		parameters.put("ipAddress", String.join(",", getInterfaces()));
		parameters.put("state", state.name());

		StringBuffer parametersAsURL = new StringBuffer("?");
		int i = 0;
		for (Map.Entry<String, String> entry : parameters.entrySet()) {
			parametersAsURL.append(entry.getKey());
			parametersAsURL.append("=");
			parametersAsURL.append(entry.getValue());
			if(i<parameters.size()-1){
				parametersAsURL.append("&");
			}
			i++;
		}

		URL url = new URL(muleContext.getRegistry().get("aws-elb-system-api.url") + parametersAsURL.toString());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");

		log.info("Calling AWS ELB System API ("+url+")");
		int responseCode = connection.getResponseCode();
		log.info("Response code:" + responseCode);
	}

	public void onStartup(MuleContext muleContext) throws Exception{
		onEvent(muleContext, State.start);
	}

	public void onShutdown(MuleContext muleContext) throws Exception{
		onEvent(muleContext, State.stop);
	}
}
