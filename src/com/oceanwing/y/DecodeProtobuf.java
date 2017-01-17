package com.oceanwing.y;

import java.util.List;

import com.oceanwing.y.MqttProtobufMsgNew.Battery;
import com.oceanwing.y.MqttProtobufMsgNew.CMsg;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgHead;
import com.oceanwing.y.MqttProtobufMsgNew.Device;
import com.oceanwing.y.MqttProtobufMsgNew.Slot;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceAck;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceOpen;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceStatus;

public class DecodeProtobuf {
	
	public static String getDecodeProtobufMsg(CMsg cmsg){
		if(cmsg == null){
			return "The protobuf message body is empty.";
		}
		
		String result = "The decode message: \r\n";
		
		// decode head
		if(cmsg.hasMsgHead()){
			CMsgHead head = cmsg.getMsgHead();
			result = result + "Command Type: " + head.getCmd() + "\r\n";
			result = result + "Group id: "+ head.getGroupid() + "\r\n";
			result = result + "trans id: "+ head.getTranid() + "\r\n";
		}
		
		// decode openAck body.
		if(cmsg.hasOpenDeviceAckBody()){
			CMsgBodyDeviceAck openAck = cmsg.getOpenDeviceAckBody();
			result = result + "Action Type: " + openAck.getAction() + "\r\n";
			result = result + "Device id: " + openAck.getDeviceid() + "\r\n";
			result = result + "Slot Number: " + openAck.getNum() + "\r\n";
			result = result + "Battery SN: " + openAck.getBatterysn() + "\r\n";
			result = result + "State: " + openAck.getState() + "\r\n";
		}
		
		// decode device status body
		if(cmsg.hasDeviceStatusBody()){
			CMsgBodyDeviceStatus devStstusBody = cmsg.getDeviceStatusBody();
			// device
			Device dev = devStstusBody.getDevice();
			result = result + "Device id: " + dev.getDeviceid() + "\r\n";
			result = result + "armTemprature: " + dev.getArmTemprature() + "\r\n";
			result = result + "envtemp: " + dev.getEnvironmentTemprature() + "\r\n";
			result = result + "Software version: " + dev.getSoftwareVersion() + "\r\n";
			result = result + "id address: " + dev.getIpAddr() + "\r\n";
			result = result + "heart beat code: " + dev.getStatus() + "\r\n";
			// battery
			List<Battery> li = devStstusBody.getBatteryList();
			for(int i = 0; i < li.size(); i++){
				Battery bt = li.get(i);
				result = result + "No.[" + i + "] battery info: " + "\r\n";
				result = result + "batterysn: " + bt.getBatterysn() + "\r\n";
				result = result + "temprature: " + bt.getTemprature() + "\r\n";
				result = result + "voltage: " + bt.getVoltage() + "\r\n";
				result = result + "fullChargeCapacity: " + bt.getFullChargeCapacity() + "\r\n";
				result = result + "remainingCapacity: " + bt.getRemainingCapacity() + "\r\n";
				result = result + "averageCurrent: " + bt.getAverageCurrent() + "\r\n";
				result = result + "cycleCount: " + bt.getCycleCount() + "\r\n";
				result = result + "bmsSafetyStatus: " + bt.getBmsSafetyStatus() + "\r\n";
				result = result + "bmsFlags: " + bt.getBmsFlags() + "\r\n";
				result = result + "status: " + bt.getStatus() + "\r\n";
				result = result + "enableStatus: " + bt.getEnableStatus() + "\r\n";
			}
			// slot
			List<Slot> sl = devStstusBody.getSlotList();
			for(int i = 0; i < sl.size(); i++){
				Slot slot = sl.get(i);
				result = result + "No.[" + i + "] slot info: " + "\r\n";
				result = result + "slot number: " + slot.getNum() + "\r\n";
				result = result + "status: " + slot.getStatus() + "\r\n";
				result = result + "batterysn: " + slot.getBatterysn() + "\r\n";
			}
		}
		
		// decode open dev body
		if(cmsg.hasOpenDeviceBody()){
			CMsgBodyDeviceOpen opendev = cmsg.getOpenDeviceBody();
			result = result + "Action Type: " + opendev.getAction().toString() + "\r\n";
			result = result + "Slot Number: " + opendev.getNum() + "\r\n";
			result = result + "Password: " + opendev.getPassword();
		}
		
		// decode the 
		return result;
	}
	
	public static byte[] formatOpenDevBodyToJson(CMsg cmsg){
		String actionType = null;
		int slotNum = 0;
		String result = "";
		if(cmsg.hasOpenDeviceBody()){
			CMsgBodyDeviceOpen opendev = cmsg.getOpenDeviceBody();
			actionType = opendev.getAction().toString();
			slotNum = opendev.getNum();
			result = "{\"action\":\""+actionType+"\",\"num\":"+slotNum+"}";
		}
		
		if(cmsg.hasOpenDeviceAckBody()){
			CMsgBodyDeviceAck openAck = cmsg.getOpenDeviceAckBody();
			actionType = openAck.getAction().toString();
			String devid = openAck.getDeviceid();
			slotNum = openAck.getNum();
			String state = openAck.getState().toString();
			result = "{\"action\":\""+actionType+"\",\"device_id\":\""+devid+"\",\"num\":"+slotNum+",\"state\":\""+state+"\"}";
		}
		return result.getBytes();
	}

}
