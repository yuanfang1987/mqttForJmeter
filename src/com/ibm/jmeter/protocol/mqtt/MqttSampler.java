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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.oceanwing.y.BuildProtobufMessage;
import com.oceanwing.y.MqttProtobufMsgNew.CMsg;
import com.oceanwing.y.DecodeProtobuf;

import java.util.UUID;

public class MqttSampler extends AbstractSampler
		implements TestBean, TestStateListener {

	private static final long serialVersionUID = 300L;

	private static final Logger log = LoggingManager.getLoggerForClass();

	private static final ThreadLocal<Map<String, MqttOverTcpIpClient>> tlClientCache = new ThreadLocal<Map<String, MqttOverTcpIpClient>>();

	private static final List<MqttOverTcpIpClient> allCreatedClients = Collections.synchronizedList(new ArrayList<MqttOverTcpIpClient>());

	private String serverURI;
	private String clientId;
	private boolean reuseConnection;
	private boolean closeConnection;
	private String topicString;
	private int qos;
	private boolean retained;
	private String messageBody;
	private String needAuth;
	private String userName;
	private String passWord;
	private String subscribeTopic;
	private String devAckStatusCode;
	
	private String respString;
	
	private String trandid;
	
	//2015/12/7, added.
	private MqttOverTcpIpClient client = null;
	
	//2015.12.15, Matt add timeout config parameter
	private String timeout;
	private int ti;
	//2015.12.23, Matt added, for CA authorize
	private String caAddr;
	//2016.1.23, Matt added.
	private String brokerType;
	
	//2016.2.25, Matt added
	private boolean isOnlyReceived;
	private boolean sendDevAck;
	private String businessType;
	
	private Map<String,byte[]> temp;
	
	private Map<String, MqttOverTcpIpClient> clientCache;
	private String clientCacheKey;
	
	//2017.1.16 added
	private BuildProtobufMessage bpm;
	private SampleResult res;
	
	private CMsg cmsg = null;
	private byte[] by = null;
	private boolean isOK;
	
	public MqttSampler() {
		log.debug("created " + System.identityHashCode(this) + ", " + Thread.currentThread().getId());
		//2017.1.16 added.
		bpm = new BuildProtobufMessage();
	}

	@Override
	public SampleResult sample(Entry entry) {
		// 2017.1.16 modify
		res = new SampleResult();
		isOK = false;
		
		ti = Integer.parseInt(timeout);
		
		res.setSampleLabel(getName()); //"MQTT Sampler"
		
		//create a new MQTT client.
		try {
			buildMqttclient();
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
		
		client.setTimeout(ti);
		
		// start calculating load time.
		res.sampleStart();
		if(!isOnlyReceived){
			// send message
			try {
				send();
			} catch (Exception e) {
				log.error("An error was occurred during the message sending.", e);
				res.setResponseMessage(e.toString());
			}
		}
		
		// debug, get device id
//		String devid = "n";
//		if(messageBody != "" || messageBody != null){
//			if(!messageBody.split(";")[0].equalsIgnoreCase("OPENDEV")){
//				devid = messageBody.split(";")[0];
//				
//			}else{
//				devid = topicString.split("/")[1];
//				
//			}
//		}
		
		
		if(sendDevAck){
			by = getReceiveMsg();
		}else{
			by = client.getPayloads();
		}
		
		
		// end debug
		
		//by = getReceiveMsg(devid);
		// end calculating load time.
		res.sampleEnd();
		
		client.deletePayloadFromMap();
		//client.setPalyloadNull();	// in order to get the next message.
		if(by != null){
			switch(businessType){
			case "Heart Beat":
				break;
			case "Borrow Or Return Battery":
				// decode protobuf
				try {
					cmsg = CMsg.parseFrom(by);
				} catch (InvalidProtocolBufferException e) {
					log.error(e.getMessage());
				}
				// get the correct message
				if(cmsg != null){
					// this logic is use for performance testing.
					if(sendDevAck){
						client.publish(topicString, bpm.buildDeviceAckMessage(cmsg, messageBody, devAckStatusCode));
					}
					res.setResponseData(DecodeProtobuf.formatOpenDevBodyToJson(cmsg));
				}else{
					respString = "Fail to receive the open device ack message.";
					res.setResponseData(respString.getBytes());
				}
				break;
			}
			isOK = true;
			res.setResponseCodeOK();
		}else{
			if(ti == 1){
				isOK = true;
			}else{
				isOK = false;
				res.setResponseData("fail to get the mqtt message.".getBytes());
			}
		}
		client.setPalyloadNull();
		res.setSuccessful(isOK);
		return res;
	}

	private void send() throws Exception {
		trandid = UUID.randomUUID().toString();
		client.setTrandid(trandid);
//		if(businessType == "Heart Beat"){
//			//client.setCMD("STATUSRES");
//			// set device id
//			//client.setDeviceid(messageBody);
//		}
		client.publish(topicString, createMessageBody());
        //trandid = "";
	}
	
	private byte[] getReceiveMsg(){
		// debug, 2016.4.19
		int i = 0;
		temp = client.getMsgMap();
		String devid = "n";
		if(messageBody != "" || messageBody != null){
			devid = messageBody.split(";")[0];
//			if(!messageBody.split(";")[0].equalsIgnoreCase("OPENDEV")){
//				
//			}else{
//				devid = topicString.split("/")[1];
//			}
		}
		while(!temp.containsKey(devid)){
			try {
				Thread.sleep(10);
				i += 1;
			} catch (InterruptedException e) {
				log.error(e.getMessage());
			}
			temp = client.getMsgMap();
			if(i == ti){
				break;
			}
		}
		
		if(temp.containsKey(devid)){
			byte[] re = temp.get(devid);
			return Arrays.copyOf(re, re.length);
		}else{
			return null;
		}
	}
	
	//2015.12.22, Matt added.
	private void buildMqttclient() throws Exception{
		clientCache = tlClientCache.get();
		clientCacheKey = createCacheKey(clientId, serverURI);
		if (clientCache == null) {
			clientCache = new HashMap<String, MqttOverTcpIpClient>();
			tlClientCache.set(clientCache);
		} else {
			client = clientCache.get(clientCacheKey);
		}

		if (!reuseConnection || client == null || !client.isConnected()) {
			close(client);
			clientCache.remove(clientCacheKey);

			client = createClient();
			
			if (log.isDebugEnabled()) {
				log.debug("A client was created. (" + client.getClientId() + ":" + System.identityHashCode(client) + ")");
			}

			client.connect(brokerType, caAddr, needAuth, userName, passWord);
			// subscribe the topic in order to get the response message.
			client.subscribeToTopic(subscribeTopic, qos);
			
			if (log.isDebugEnabled()) {
				log.debug("A client has been connected. (" + client.getClientId() + ":" + System.identityHashCode(client) + ")");
			}

			clientCache.put(clientCacheKey, client);
			allCreatedClients.add(client);
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Got a client from the cache." + System.identityHashCode(client));
			}
		}
		// debug , added 2016/4/19
		//client.subscribeToTopic(subscribeTopic, qos);
	}

	protected String createCacheKey(String clientId, String serverURL) {
		return clientId + "@" + serverURI;
	}

	protected MqttOverTcpIpClient createClient() throws MqttException {
		//MqttOverTcpIpClient client = null;
		if (serverURI.startsWith("tcp:") || serverURI.startsWith("ssl:")) {
			return new MqttOverTcpIpClient(serverURI, clientId);
		} else {
			throw new MqttException("Unknown protocol: " + serverURI);
		}

//		if (log.isDebugEnabled()) {
//			log.debug("A client has been created. (" + client.getClientId() + ":" + System.identityHashCode(client) + ")");
//		}

		//return client;
	}

	private void close(MqttOverTcpIpClient client) {
		if (client != null && client.isConnected()) {
			try {
				client.close();
				if (log.isDebugEnabled()) {
					log.debug("A client has been closed. (" + client.getClientId() + ":" + System.identityHashCode(client) + ")");
				}
			} catch (MqttException e) {
				log.warn("Closing the connection failed.", e);
			}
		}
	}

	// update by Matt, 2015/12/5
	protected byte[] createMessageBody() {
		bpm.setTransid(trandid);
		return bpm.serializeData(messageBody);
	}

	@Override
	public void testEnded() {
		log.debug("testEnded " + System.identityHashCode(this) + ", " + Thread.currentThread().getId());

		synchronized (allCreatedClients) {
			for (MqttOverTcpIpClient c : allCreatedClients) {
				close(c);
			}
		}
		allCreatedClients.clear();
	}

	@Override
	public void testEnded(String arg0) {
		log.debug("testEnded (" + arg0 + ") " + System.identityHashCode(this) + ", " + Thread.currentThread().getId());
		testEnded();
	}

	@Override
	public void testStarted() {
		log.debug("testStarted " + System.identityHashCode(this) + ", " + Thread.currentThread().getId());
	}

	@Override
	public void testStarted(String arg0) {
		log.debug("testStarted (" + arg0 + ") " + System.identityHashCode(this) + ", " + Thread.currentThread().getId());
		testStarted();
	}

	@Override
	public String toString() {
		return "MqttSampler [serverURI=" + serverURI + ", " +
				"clientId=" + clientId + ", " +
				"reuseConnection=" + reuseConnection + ", " +
				"closeConnection=" + closeConnection + ", " +
				"topicString=" + topicString + ", " +
				"qos=" + qos + ", " +
				"retained=" + retained + ", " +
				"messageBody=" + messageBody + "]";
	}

	public String getServerURI() {
		return serverURI;
	}

	public void setServerURI(String serverURI) {
		this.serverURI = serverURI;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public boolean getReuseConnection() {
		return reuseConnection;
	}

	public void setReuseConnection(boolean reuseConnection) {
		this.reuseConnection = reuseConnection;
	}

	public boolean getCloseConnection() {
		return closeConnection;
	}

	public void setCloseConnection(boolean closeConnection) {
		this.closeConnection = closeConnection;
	}

	public String getTopicString() {
		return topicString;
	}

	public void setTopicString(String topicString) {
		this.topicString = topicString;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public boolean getRetained() {
		return retained;
	}

	public void setRetained(boolean retained) {
		this.retained = retained;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}
	
	public String getNeedAuth(){
		return needAuth;
	}
	
	public void setNeedAuth(String flag){
		this.needAuth = flag;
	}
	
	public String getUserName(){
		return userName;
	}
	
	public void setUserName(String name){
		this.userName = name;
	}
	
	public String getPassWord(){
		return passWord;
	}

	public void setPassWord(String pwd){
		this.passWord = pwd;
	}
	
	public String getSubscribeTopic(){
		return subscribeTopic;
	}
	
	public void setSubscribeTopic(String subTopic){
		this.subscribeTopic = subTopic;
	}
	
	public String getDevAckStatusCode(){
		return this.devAckStatusCode;
	}
	
	public void setDevAckStatusCode(String ack){
		this.devAckStatusCode = ack;
	}
	
	public String getTimeout(){
		return timeout;
	}
	
	public void setTimeout(String t){
		this.timeout = t;
	}
	
	//2015.12.23, added
	public String getCaAddr(){
		return this.caAddr;
	}
	
	public void setCaAddr(String ca){
		this.caAddr = ca;
	}
	
	//2016.1.23 added
	public String getBrokerType(){
		return this.brokerType;
	}
	
	public void setBrokerType(String type){
		this.brokerType = type;
	}
	
	//2016.2.25 Matt added.
	public boolean getIsOnlyReceived(){
		return this.isOnlyReceived;
	}
	
	public void setIsOnlyReceived(boolean flag){
		this.isOnlyReceived = flag;
	}
	
	public String getBusinessType(){
		return this.businessType;
	}
	
	public void setBusinessType(String btype){
		this.businessType = btype;
	}
	
	public boolean getSendDevAck(){
		return this.sendDevAck;
	}
	
	public void setSendDevAck(boolean flag){
		this.sendDevAck = flag;
	}
}
