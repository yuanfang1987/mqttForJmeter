/*******************************************************************************
 * Copyright (c) 2013 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Takahiro Inaba - initial API and implementation and/or initial
 *                     documentation
 *******************************************************************************/
package com.ibm.jmeter.protocol.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
//import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//import org.eclipse.paho.client.mqttv3.TimerPingSender;
import org.eclipse.paho.client.mqttv3.MqttMessage;
// Matt added
import com.oceanwing.y.MqttProtobufMsgNew.CMsg;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceStatus;
//import com.oceanwing.y.MqttProtobufMsgNew.CMD;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgHead;
import com.oceanwing.y.MqttProtobufMsgNew.Device;

import java.util.Map;
import java.util.HashMap;


public class MqttOverTcpIpClient implements MqttCallback {
	private MqttClient client;
	
	// Matt added.
	private String trans_id;
	//private CMD cmd;
	private byte[] bys;
	private static final Logger log = LoggingManager.getLoggerForClass();
	private MqttConnectOptions opts;
	private int timeout;
	//private String devid;
	
	private String ackType;
	//2015.12.19, Matt added.
	private String[] sub_To_Topic;
	//private String sub_Topic;
	//private int sub_qos;
	private Map<String,byte[]> msgMap;
	
	//2017.1.16 added
	private MqttMessage msg;

	public MqttOverTcpIpClient(String serverURI, String clientId) throws MqttException {
		try {
			if(clientId == "" || clientId == null){
				clientId = MqttClient.generateClientId();
			}
			client = new MqttClient(serverURI, clientId, new MemoryPersistence());
			// debug, added on 2016/4/19
			client.setTimeToWait(30000);
			log.info("start a new MqttAsyncClient.");
		} catch (org.eclipse.paho.client.mqttv3.MqttException e) {
			log.error(e.getMessage());
		}
		opts = new MqttConnectOptions();
		opts.setKeepAliveInterval(30);
		//sub_Topic = "";
		sub_To_Topic = null;
		msgMap = new HashMap<String,byte[]>();
	}
	
	//2015.12.24, Matt added.
	public void setTimeout(int t){
		this.timeout = t;
	}

	public void connect(String type, String path, String ca, String cert, String key) throws MqttException {
		switch(type){
		case "CN":
			System.setProperty("javax.net.ssl.trustStore", path);
			break;
		case "US":
			try {
				opts.setSocketFactory(SslUtil.getSocketFactory(ca, cert, key, ""));
				log.info("set socket factory with: "+ca+"; "+cert+"; "+key);
			} catch (Exception e) {
				log.error(e.getMessage());
			}
			break;
		}
		
		try {
			client.setCallback(this);
			client.connect(opts);
			log.info("connect to broker success.");
		} catch (MqttSecurityException e) {
			log.error("connect to broker fail, level 1.");
			log.error(e.getMessage());
		} catch (org.eclipse.paho.client.mqttv3.MqttException e) {
			log.error("connect to broker fail, level 2.");
			log.error(e.getMessage());
		}
	}

	public void close() throws MqttException {
		if (client != null) {
			try {
				client.disconnect();
				client.close();
			} catch (org.eclipse.paho.client.mqttv3.MqttException e) {
				throw new MqttException(e);
			}
		}
	}

	public boolean isConnected() {
		return client.isConnected();
	}
	
	//2015.12.22, Matt added.
	public void publish(String topicString, byte[] payload){
		msg = new MqttMessage();
		msg.clearPayload();
		msg.setQos(1);
		msg.setPayload(payload);
		try {
			client.publish(topicString, msg);
		} catch (org.eclipse.paho.client.mqttv3.MqttException e) {
			log.error("publish message fail.");
			log.error(e.getMessage());
		}
	}
	
	/**
	 * if we got a new topic, then we need to unsubscribe the old topic and subscribe the new topic.
	 * @param subTopic
	 * @param qos
	 */
	public void subscribeToTopic(String subTopic, int qos){
		try {
			this.sub_To_Topic = subTopic.split(";");
			client.subscribe(sub_To_Topic);
			log.info("subscribe to topic: "+subTopic+", with qos level: "+qos);
		} catch (org.eclipse.paho.client.mqttv3.MqttException e) {
			log.error("Failed to subscribe to topic due to: "+e.getMessage());
		}
	}

	public String getClientId() {
		return client.getClientId();
	}

	@Override
	public void connectionLost(Throwable cause) {
		log.info("connection lost due to: "+cause.getMessage());
		log.info("Re-connect to broker.");
		// re-connect to broker and subscribe to the previous topic once the connect broken.
		try {
			client.setCallback(this);
			client.connect(opts);
			client.subscribe(sub_To_Topic);
			log.info("re-connect to broker success.");
		} catch (MqttSecurityException e) {
			log.error(e.getMessage());
		} catch (org.eclipse.paho.client.mqttv3.MqttException e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		this.bys = null;
	}

	@Override
	public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
		log.debug("the avvrived message topic is: "+topic);
		byte[] payload = message.getPayload();
		
		// || topic.startsWith("L/")
		if(topic.startsWith("SERTODEV/")){
			//this.bys = payload;
			String id = topic.split("/")[1];
			msgMap.put(id, payload);
		}else{
			String transactionid = null;
			CMsg cmsgTemp = CMsg.parseFrom(payload);
			if(cmsgTemp != null && cmsgTemp.hasMsgHead()){
				CMsgHead head = cmsgTemp.getMsgHead();
				transactionid = head.getTranid();
				log.debug("message arrived, command type is: "+head.getCmd());
				//log.debug("meesage arrived, transid is: "+transactionid);
				
				//debug
//				if(cmsgTemp.hasOpenDeviceAckBody()){
//					String devid = cmsgTemp.getOpenDeviceAckBody().getDeviceid();
//					msgMap.put(devid, payload);
//				}
				
				if(head.hasTranid() && transactionid.equalsIgnoreCase(trans_id)){
					//debug 4.29.2016, Matt.
//					String devid = cmsgTemp.getOpenDeviceAckBody().getDeviceid();
//					msgMap.put(devid, payload);
					this.bys = payload;
				}
				
				
				
				// end
				
				// this logic is used for borrow/return test.
//				if(head.hasTranid() && transactionid.equalsIgnoreCase(trans_id)){
//					//debug 4.29.2016, Matt.
//					String devid = cmsgTemp.getOpenDeviceAckBody().getDeviceid();
//					msgMap.put(devid, payload);
//					//this.bys = payload;
//				}else if(head.getCmd().toString().equalsIgnoreCase(ackType)){
//					// this logic is used for heart beat test.
//					if(cmsgTemp.hasDeviceStatusBody()){
//						CMsgBodyDeviceStatus devStatus = cmsgTemp.getDeviceStatusBody();
//						Device dev = devStatus.getDevice();
//						String id = dev.getDeviceid();
//						if(id.equalsIgnoreCase(this.devid)){
//							this.bys = payload;
//						}
//					}
//				}
			}
		}
	}
	
	// get message
	public byte[] getPayloads(){
		int i = 0;
		while(bys == null){
			try {
				Thread.sleep(500);
				i += 1;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(i == timeout){
				break;
			}
		}
		if(bys == null){
			log.debug("waiting "+i+" second for the message arrived, bug get null.");
		}
		return bys;
	}
	
	// get loyalty message
	public byte[] getLoyaltyPayload(){
		int i = 0;
		while(bys == null){
			try {
				Thread.sleep(10);
				i += 1;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(i == timeout){
				break;
			}
		}
		if(bys == null){
			log.debug("waiting "+i+" second for the message arrived, bug get null.");
		}
		return bys;
	}
	
	//debug 2016.3.9
	public byte[] getMsg(){
		return this.bys;
	}
	
	// debug, 2016.4.19
	public Map<String,byte[]> getMsgMap(){
		return this.msgMap;
	}
	
	public void deletePayloadFromMap(){
//		if(this.msgMap.containsKey(key)){
//			this.msgMap.remove(key);
//		}
		this.msgMap.clear();
	}
	
	//2015.12.22, Matt added.
	public void setPalyloadNull(){
		this.bys = null;
	}
	
	public void setTrandid(String tr){
		this.trans_id = tr;
		log.debug("set transaction id: "+tr);
	}
	
	public void setCMD(String c){
		this.ackType = c;
		log.debug("set ACK type is: "+this.ackType);
	}
	
	// this function is just used for device heart beat testing.
//	public void setDeviceid(String id){
//		String[] temp = id.split(";");
//		temp = temp[1].split("#");
//		this.devid = temp[0];
//	}
	
}
