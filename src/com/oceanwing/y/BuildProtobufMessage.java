package com.oceanwing.y;

import org.apache.commons.codec.binary.Base64;

import com.google.protobuf.ByteString;
import com.oceanwing.y.MqttProtobufMsgNew.BATTERY_BORROW_RETURN_CODE;
import com.oceanwing.y.MqttProtobufMsgNew.Battery;
import com.oceanwing.y.MqttProtobufMsgNew.CMD;
import com.oceanwing.y.MqttProtobufMsgNew.CMsg;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceAck;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceOpen;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceStatus;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgBodyDeviceUpdate;
import com.oceanwing.y.MqttProtobufMsgNew.CMsgHead;
import com.oceanwing.y.MqttProtobufMsgNew.DEVACTION;
import com.oceanwing.y.MqttProtobufMsgNew.Device;
import com.oceanwing.y.MqttProtobufMsgNew.ENABLE_BORROW_STATUS;
import com.oceanwing.y.MqttProtobufMsgNew.HEART_BEAT_BATTERY_CODE;
import com.oceanwing.y.MqttProtobufMsgNew.HEART_BEAT_DEVICE_CODE;
import com.oceanwing.y.MqttProtobufMsgNew.HEART_BEAT_SLOT_CODE;
import com.oceanwing.y.MqttProtobufMsgNew.Slot;

public class BuildProtobufMessage {
	
	private CMsg.Builder cmsgBuilder;
	private CMsgHead.Builder cmheadBuilder;
	private String[] msgs;
	private String tranid;
	
	//2017.1.16 added.
	private byte[] payload;
	
	//for device heart beat, 2017.1.16 added.
	private String[] devParameters;
	private String[] batterys;
	private String[] slots;
	
	private CMsgBodyDeviceStatus.Builder devStatus;
	private Device.Builder dev;
	private HEART_BEAT_DEVICE_CODE heartStatus;
	
	private Battery.Builder bats;
	private String[] each_bat_param;
	
	private Slot.Builder slot;
	private String[] slotParam;
	
	private HEART_BEAT_SLOT_CODE slot_state;
	
	
	public BuildProtobufMessage(){
		this.cmsgBuilder = CMsg.newBuilder();
		this.cmheadBuilder = CMsgHead.newBuilder();
		//this.tranid = transaid;
	}
	
	public void setTransid(String tranid){
		this.tranid = tranid;
	}
	
	public byte[] serializeData(String message){
		//byte[] payload = null;
		
		msgs = message.split(";");
		
		//String cmdType = msgs[0];
		switch(msgs[0]){
		case "OPENDEV":
			payload = openDev();
			break;
		case "UPDATEDEVICEPARAMS":
			payload = updateDev();
			break;
		case "STATUSRES":
			payload = devHeartBeat();
			break;
		}
		msgs = null;
		return payload;
	}
	
	/**
	 * open device, borrow or return battery.
	 * @return
	 */
	private byte[] openDev(){
		byte[] re = null;
		
		// header
		String version = msgs[1];
		//String transid = msgs[2];
		String groupid = msgs[2];
		cmheadBuilder.setCmd(CMD.OPENDEV);
		cmheadBuilder.setVersion(version);
		cmheadBuilder.setTranid(tranid);
		cmheadBuilder.setGroupid(Integer.parseInt(groupid));
		
		// body
		String action =  msgs[3];
		String slotNo = msgs[4];
		String password = msgs[5];
		
		int slotNum = Integer.parseInt(slotNo);
		
		
		CMsgBodyDeviceOpen.Builder devOpenBuilder = CMsgBodyDeviceOpen.newBuilder();
		
		// set action type, borrow or return.
		if(action.equalsIgnoreCase("borrow")){
			devOpenBuilder.setAction(DEVACTION.BORROW);
		}else{
			devOpenBuilder.setAction(DEVACTION.RETURN);
		}
		
		// set slot number
		devOpenBuilder.setNum(slotNum);
		
		// set password if needed.
		if(password != "" && password != null){
			byte[] pwd = Base64.decodeBase64(password);
			devOpenBuilder.setPasswordBytes(ByteString.copyFrom(pwd));
		}else{
			devOpenBuilder.setPasswordBytes(null);
		}
		
		// build and serialize
		cmsgBuilder.setMsgHead(cmheadBuilder);
		cmsgBuilder.setOpenDeviceBody(devOpenBuilder);
		re = cmsgBuilder.build().toByteArray();
		return re;
	}
	
	/**
	 * update dev parameters
	 * @return
	 */
	private byte[] updateDev(){
		String para = msgs[1];
		
		// build head
		cmheadBuilder.setCmd(CMD.UPDATEDEVICEPARAMS);
		cmheadBuilder.setGroupid(1);
		
		// body
		CMsgBodyDeviceUpdate.Builder upDev = CMsgBodyDeviceUpdate.newBuilder();
		upDev.setCmd(para);
		
		// cmsg
		cmsgBuilder.setMsgHead(cmheadBuilder);
		cmsgBuilder.setUpdateDeviceBody(upDev);
		
		// build
		return cmsgBuilder.build().toByteArray();
	}
	
	private byte[] devHeartBeat(){
		devParameters = msgs[1].split("#");
		batterys = msgs[2].split("/");
		slots = msgs[3].split("/");
		
		
		// head
		cmheadBuilder.setCmd(CMD.STATUSRES);
		
		// body
		devStatus = CMsgBodyDeviceStatus.newBuilder();
		
		// body->Device
		dev = Device.newBuilder();
		dev.setDeviceid(devParameters[0]);	// device id
		dev.setArmTemprature(Float.parseFloat(devParameters[1]));	// arm temprature
		dev.setEnvironmentTemprature(Float.parseFloat(devParameters[2])); // env temp
		dev.setSoftwareVersion(devParameters[3]); // software version
		dev.setIpAddr(devParameters[4]); // ip address
		
		switch(devParameters[5]){
		case "0":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_STATUS_OK;
			break;
		case "1":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_TEMPERATURE_ERROR;
			break;
		case "2":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_WIFI_ERROR;
			break;
		case "3":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_BLUE_ERROR;
			break;
		case "4":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_IBEACON_ERROR;
			break;
		case "5":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_CONFIG_FILE_ERROR;
			break;
		case "6":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_SYSTEM_ERROR;
			break;
		case "7":
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_OTHER_ERROR;
			break;
		default:
			heartStatus = HEART_BEAT_DEVICE_CODE.HBD_STATUS_OK;
			break;
		}
		dev.setStatus(heartStatus);
		devStatus.setDevice(dev);
		
		// body->Battery
		//System.out.println("battery number is: "+batterys.length);
		for(int i = 0; i < batterys.length; i++){
			bats = Battery.newBuilder();
			each_bat_param = batterys[i].split("#");
			
			bats.setBatterysn(each_bat_param[0]); // battery sn
			bats.setTemprature(Float.parseFloat(each_bat_param[1]));// temprature
			bats.setVoltage(Integer.parseInt(each_bat_param[2]));// voltage
			bats.setFullChargeCapacity(Integer.parseInt(each_bat_param[3])); //fullChargeCapacity
			bats.setRemainingCapacity(Integer.parseInt(each_bat_param[4])); //remainingCapacity
			bats.setAverageCurrent(Integer.parseInt(each_bat_param[5]));  // averageCurrent
			bats.setCycleCount(Integer.parseInt(each_bat_param[6])); // cycleCount
			bats.setBmsSafetyStatus(Integer.parseInt(each_bat_param[7])); // bmsSafetyStatus
			bats.setBmsFlags(Integer.parseInt(each_bat_param[8])); //bmsFlags
			// HEART_BEAT_BATTERY_CODE
			if(each_bat_param[9].equalsIgnoreCase("0")){
				bats.setStatus(HEART_BEAT_BATTERY_CODE.HBB_STATUS_OK);
			}else{
				bats.setStatus(HEART_BEAT_BATTERY_CODE.HBB_OTHER_ERROR);
			}
			//ENABLE_BORROW_STATUS
			if(each_bat_param[10].equalsIgnoreCase("0")){
				bats.setEnableStatus(ENABLE_BORROW_STATUS.ENABLE_STATUS);
			}else{
				bats.setEnableStatus(ENABLE_BORROW_STATUS.DISABLE_STATUS);
			}
			
			//devStatus.setBattery(i+1, bats);
			devStatus.addBattery(bats);
		}
		
		// body->slot
		//System.out.println("slot numbers: "+slots.length);
		for(int i = 0; i < slots.length; i++){
			slot = Slot.newBuilder();
			slotParam = slots[i].split("#");
			
			slot.setNum(Integer.parseInt(slotParam[0])); // slot number
			// HEART_BEAT_SLOT_CODE
			switch(slotParam[1]){
			case "0":
				slot_state = HEART_BEAT_SLOT_CODE.HBS_STATUS_OK;
				break;
			case "1":
				slot_state = HEART_BEAT_SLOT_CODE.HBS_TEMPERATURE_ERROR;
				break;
			case "2":
				slot_state = HEART_BEAT_SLOT_CODE.HBS_VOLTAGE_ERROR;
				break;
			case "3":
				slot_state = HEART_BEAT_SLOT_CODE.HBS_CURRENT_ERROR;
				break;
			case "4":
				slot_state = HEART_BEAT_SLOT_CODE.HBS_OTHER_ERROR;
				break;
			case "5":
				slot_state = HEART_BEAT_SLOT_CODE.HBS_CYCLECOUNT_ERROR;
				break;
			default:
				slot_state = HEART_BEAT_SLOT_CODE.HBS_STATUS_OK;
				break;
			}
			slot.setStatus(slot_state);
			
			if(slotParam.length > 2){
				slot.setBatterysn(slotParam[2]);
			}else{
				slot.setBatterysn("");
			}
			
			//devStatus.setSlot(i+1, slot);
			devStatus.addSlot(slot);
		}
		
		cmsgBuilder.setMsgHead(cmheadBuilder);
		cmsgBuilder.setDeviceStatusBody(devStatus);
		
		return cmsgBuilder.build().toByteArray();
	}
	
	/**
	 * @param cmsg:  the mqtt message sent from the server
	 * @param batterySN: when return battery, need SN;
	 * @return
	 */
	public byte[] buildDeviceAckMessage(CMsg cm, String msgBody, String status_code){
		byte[] re = null;
		String[] arrTemp;
		String devid = "";
		String batterySN = "";
		String[] bts = null;
		
		
		/*** the msgBody should contain 3 elements: 
		 *   1, device id, which is a string.
		 *   2, battery sn array, which is a string like this: btn1/btn2/btn3.
		 *   3, the slot number that battery was borrowed from, which is a integer.
		 *   
		 *   its structure looks like this:
		 *   eea6e77c-f403-4a5a-aa23-52505b60828d;btsn01/btsn02/btsn03;2
		 *  *****/
		arrTemp = msgBody.split(";");
		// get device id
		devid = arrTemp[0];
		if(arrTemp.length > 2){
			// get battery sn array
			bts = arrTemp[1].split("/");
			batterySN = bts[Integer.parseInt(arrTemp[2])-1];
		}else{
			batterySN = "";
		}
		
		BATTERY_BORROW_RETURN_CODE s_code;
		
		// head
		String version = null;
		String transid = null;
						
		// open device ack body
		DEVACTION act = null;
		int slotNo = 0;
		
		// decode protobuf msg
		if(cm.hasMsgHead()){
			CMsgHead head = cm.getMsgHead();
			version = head.getVersion();
			transid = head.getTranid();
			
		}
		
		if(cm.hasOpenDeviceBody()){
			CMsgBodyDeviceOpen openDev = cm.getOpenDeviceBody();
			act = openDev.getAction();
			slotNo = openDev.getNum();
		}
		
		// build message, head
		cmheadBuilder.setCmd(CMD.OPENACK);
		cmheadBuilder.setVersion(version);
		cmheadBuilder.setTranid(transid);
		cmheadBuilder.setGroupid(0);
		//cmheadBuilder.setErrcode(0); need error code?
		
		// build message, body
		CMsgBodyDeviceAck.Builder devAck = CMsgBodyDeviceAck.newBuilder();
		devAck.setAction(act);
		devAck.setDeviceid(devid);
		devAck.setNum(slotNo);
		
		/**   
		 **  if return battery, need battery sn, if borrow, set the battery sn as null.
		 ***/
		if(act.toString() == "RETURN"){
			devAck.setBatterysn(batterySN);
		}else{
			devAck.setBatterysn("");
		}
		
		switch(status_code){
		case "0":
			s_code = BATTERY_BORROW_RETURN_CODE.BBR_STATUS_OK;
			break;
		default:
			s_code = BATTERY_BORROW_RETURN_CODE.BBR_STATUS_OK;
			break;
		}
		
		devAck.setState(s_code);
		
		// build and serialize
		cmsgBuilder.setMsgHead(cmheadBuilder);
		cmsgBuilder.setOpenDeviceAckBody(devAck);
		re = cmsgBuilder.build().toByteArray();
		
		return re;
	}
	
}
