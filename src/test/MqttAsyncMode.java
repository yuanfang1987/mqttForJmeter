package test;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.TimerPingSender;

public class MqttAsyncMode implements MqttCallback{
	
	private MqttAsyncClient clientAsync;
	private MqttConnectOptions connectOpt;
	private MemoryPersistence per;
	private MqttMessage msg;
	
	public MqttAsyncMode(String type) throws KeyManagementException, NoSuchAlgorithmException{
		this.connectOpt = new MqttConnectOptions();
		connectOpt.setKeepAliveInterval(30);
		this.per = new MemoryPersistence();
		msg = new MqttMessage();
		
		if(type.equalsIgnoreCase("us")){
			try {
				connectOpt.setSocketFactory(SslUtil.getSocketFactory(".\\auth\\ca.crt", ".\\auth\\cert.crt", ".\\auth\\private.key", ""));
				System.out.println("set socket factory success.");
			} catch (Exception e) {
				System.out.println("no way..");
				e.printStackTrace();
			}
		}else{
			System.setProperty("javax.net.ssl.trustStore", "D:\\temp\\GDCA.keystore");
		}
		
	}
	
	public void connectToBroker(String broker){
		String clientid = MqttAsyncClient.generateClientId();
		try {
			clientAsync = new MqttAsyncClient(broker, clientid, per, new TimerPingSender());
			clientAsync.setCallback(this);
			clientAsync.connect(connectOpt).waitForCompletion(10000);
			System.out.println("async mode, connect to broker: "+broker);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void subscribeTopic(String topic, int qos){
		try {
			clientAsync.subscribe(topic, qos);
			System.out.println("subscribe to topic: "+topic+" with qos: "+qos);
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	public void publishTotopic(String t, String message){
		msg.clearPayload();
		msg.setPayload(message.getBytes());
		msg.setQos(1);
		try {
			clientAsync.publish(t, msg).waitForCompletion(3000);
			System.out.println("publish message: "+message);
		} catch (MqttPersistenceException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable arg0) {
		System.out.println("async, connection lost");
		clientAsync.setCallback(this);
		try {
			clientAsync.connect(connectOpt);
			System.out.println("async, re-connect success.");
		} catch (MqttSecurityException e) {
			e.printStackTrace();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {
		System.out.println("delevery complete: "+arg0.getMessageId());
	}

	@Override
	public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
		System.out.println("received topic: "+arg0);
	}

}
