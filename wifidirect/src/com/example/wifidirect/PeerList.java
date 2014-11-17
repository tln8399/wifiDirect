package com.example.wifidirect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;

public class PeerList {

	int numberOfPeers;
	public static List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
	public static HashMap<String, Boolean> peersMap = new HashMap<String, Boolean>();
	public static DeviceListFragment deviceListObject = new DeviceListFragment();
	
	public PeerList() {
		numberOfPeers = 0;
	}
	
	public static List<WifiP2pDevice> getPeerList() {
		peers =  deviceListObject.getPeers();
		return peers;
	}

	public static void setPeerList(List<WifiP2pDevice> peerList) {
		peers.clear();
		peers = peerList;
		addAllPeersToMap();
	}
	
	private static void addAllPeersToMap() {
		getPeerList();
		for(WifiP2pDevice device : peers) {
			peersMap.put(device.deviceName, false);
		}
	}

	public static int getPeerListSize() {
		getPeerList();
		return peers.size();
	}
	
	public static Boolean areAllPeersVisited() {
		
		for(WifiP2pDevice device : peers) {
			if(!peersMap.get(device.deviceName)) {
				return false;
			}
		}
		return true;
	}
	
	public static void markDeviceAsVisited(String deviceName) {
		
		if(peersMap.containsKey(deviceName)) {
			peersMap.put(deviceName, true);
		}
		else {
			Log.d(WiFiDirectActivity.TAG, "Device not present in device list");
		}
	}
}
