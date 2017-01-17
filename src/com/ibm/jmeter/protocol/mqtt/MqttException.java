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

public class MqttException extends Exception {
	private static final long serialVersionUID = 300L;

	public MqttException() {}

	public MqttException(String message) {
		super(message);
	}

	public MqttException(String message, Throwable t) {
		super(message, t);
	}

	public MqttException(Throwable t) {
		super(t);
	}

}
