package com.example.wifidirect;

import java.io.Serializable;
import java.net.InetAddress;

public class Device implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1430780053137656189L; // Automatic generated
	private String name;
	private InetAddress IpAddress;
	private int port;
	
	public Device (String name, InetAddress IpAddress, int port) {
		this.setName(name);
		this.setIpAddress(IpAddress);
		this.setPort(port);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public InetAddress getIpAddress() {
		return IpAddress;
	}

	public void setIpAddress(InetAddress ipAddress) {
		IpAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}	
	
}
