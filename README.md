# mqttForJmeter
a mqtt plugin for jmeter testing

JMeter MQTT Sampler plugin

1. Requirements
 * JVM 6 or higher
 * JMeter 2.9 or higher
 * Paho 0.4.0 or higher

2. Install
Copy mqtt-sampler-1.0.jar and paho-0.4.0.jar into <JMeter install directory>/lib/ext.

3. Use
Choose MQTT Sampler from Edit -> Add -> Sampler when focusing a Thread Group.

----------------------------------------------------------------------------------------
当用maven打包时，它的结构是：
1，src和pom.xml在同一个目录下
2，src里面有main-->java & resource,  java--->com.....


