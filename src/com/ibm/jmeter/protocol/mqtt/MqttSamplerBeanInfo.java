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

import java.beans.PropertyDescriptor;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testbeans.gui.BooleanPropertyEditor;
//import org.apache.jmeter.testbeans.gui.TextAreaEditor;

public class MqttSamplerBeanInfo extends BeanInfoSupport {

	public MqttSamplerBeanInfo() {
		this(MqttSampler.class);
	}

	protected MqttSamplerBeanInfo(Class<? extends TestBean> beanClass) {
		super(beanClass);

		createPropertyGroup("connectionInfo",
				new String[]{"serverURI", "clientId", "reuseConnection", "closeConnection", "isOnlyReceived", "businessType", "sendDevAck", "brokerType", 
						"needAuth", "userName", "passWord", "caAddr"});

		// modify by Matt,2015/12/5, delete "base64Encoded", "characterEncoding"
		createPropertyGroup("messageInfo",
				new String[] {"topicString", "subscribeTopic", "qos", "retained", "messageBody", "devAckStatusCode", "timeout"});

		PropertyDescriptor p = property("serverURI");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");

		p = property("clientId");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");

		p = property("reuseConnection");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.TRUE);
		p.setPropertyEditorClass(BooleanPropertyEditor.class);

		p = property("closeConnection");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.FALSE);
		p.setPropertyEditorClass(BooleanPropertyEditor.class);
		
		p = property("isOnlyReceived");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.FALSE);
		p.setPropertyEditorClass(BooleanPropertyEditor.class);
		
		p = property("businessType");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "Borrow Or Return Battery");
		p.setValue(NOT_EXPRESSION, Boolean.TRUE);
		p.setValue(TAGS, new String[] {"Heart Beat", "Borrow Or Return Battery"});
		
		p = property("sendDevAck");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.FALSE);
		p.setPropertyEditorClass(BooleanPropertyEditor.class);
		
		p = property("brokerType");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
		p = property("needAuth");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
		p = property("userName");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
		p = property("passWord");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
		//2015.12.23, Matt added.
		p = property("caAddr");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
		p = property("topicString");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
		p = property("subscribeTopic");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");

		p = property("qos");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, 0);
		p.setValue(NOT_EXPRESSION, Boolean.TRUE);
		p.setValue(TAGS, new String[] {"0", "1", "2", "3"});

		p = property("retained");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.FALSE);
		p.setPropertyEditorClass(BooleanPropertyEditor.class);

		p = property("messageBody");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		//p.setPropertyEditorClass(TextAreaEditor.class);
		
		p = property("devAckStatusCode");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
		p = property("timeout");
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		
	}

}
