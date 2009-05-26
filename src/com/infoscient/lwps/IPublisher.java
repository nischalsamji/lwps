package com.infoscient.lwps;

import java.util.Properties;

public interface IPublisher {
	public void connect(String host, String code, Properties props)
			throws Exception;

	public void connect(String host, int port, String code, Properties props)
			throws Exception;

	public int getID();

	public String getHost();

	public int getPort();

	public String getCode();

	public Properties getProps();

	public void sendMessage(String message) throws Exception;
}
