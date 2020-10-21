import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Properties;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import com.pi4j.io.gpio.*;

public class SlotValidation {
	private static GpioPinDigitalOutput sensorTriggerPin;
	private static GpioPinDigitalInput sensorEchoPin;
	final static GpioController gpio = GpioFactory.getInstance();
	public static String trigger;
	public static String echo;
	public static String RFID;
	public static String red;
	public static String green;
	public static String ParkingSlot;
	public static GpioPinDigitalOutput redLED;
	public static GpioPinDigitalOutput greenLED;
	String GateStatus;
	public static void main(String[] args) throws IOException {
		new SlotValidation().SensorConnection();

	}
	public void SensorConnection() throws IOException {
		Properties p = new Properties();
		InputStream is = new FileInputStream("/home/pi/IOT-NEW/configuration1.txt");
		p.load(is);
		ParkingSlot = p.getProperty("ParkingSlot");
		sensorTriggerPin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_15);
		sensorEchoPin = gpio.provisionDigitalInputPin(RaspiPin.GPIO_16);
		redLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_23,PinState.LOW);
		greenLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_22,PinState.LOW);
		int counter=1;
		String line;
		while (true) {
			try {
				Thread.sleep(500);
				sensorTriggerPin.high(); // Make trigger pin HIGH
				Thread.sleep((long) 0.01);// Delay for 10 microseconds
				sensorTriggerPin.low(); // Make trigger pin LOW

				while (sensorEchoPin.isLow()) { // Wait until the ECHO pin gets HIGH

				}
				int startTime = (int) System.nanoTime(); // Store the current time to calculate ECHO pin HIGH time.
				while (sensorEchoPin.isHigh()) { // Wait until the ECHO pin gets LOW

				}
				int endTime = (int) System.nanoTime(); // Store the echo pin HIGH end time to calculate ECHO pin HIGH
														// time.
				float distance = (float) ((((endTime - startTime) / 1e3) / 2) / 29.1);
				if(distance >= 1 && distance <= 8 && counter==1) {
					System.out.println("Place your card to Read");
					Process pr = Runtime.getRuntime().exec("python Read_1.py");
					BufferedReader bri = new BufferedReader(new InputStreamReader(pr.getInputStream()));					
					while ((line = bri.readLine()) != null) {
						System.out.print(line);
						System.out.println();
						break;
					}
					bri.close();
					new SlotValidation().RfidValidation(line); // calling API functionality
					counter++;
					Thread.sleep(5000);
					System.out.println("Sensing the vehicle");
					float distance1=0;
					while (distance1<8){		
						sensorTriggerPin.high(); // Make trigger pin HIGH
						Thread.sleep((long) 0.01);// Delay for 10 microseconds
						sensorTriggerPin.low(); // Make trigger pin LOW

						while (sensorEchoPin.isLow()) { // Wait until the ECHO pin gets HIGH

						}
						int startTime1 = (int) System.nanoTime(); // Store the current time to calculate ECHO pin HIGH time.
						while (sensorEchoPin.isHigh()) { // Wait until the ECHO pin gets LOW

						}
						int endTime1 = (int) System.nanoTime(); // Store the echo pin HIGH end time to calculate ECHO pin HIGH
																// time.
						 distance1 = (float) ((((endTime1 - startTime1) / 1e3) / 2) / 29.1);
						 System.out.println(distance1);
						 Thread.sleep(5000);
					}
					new SlotValidation().LEDChange();
					
				}
				else {
					counter=1;
				}		
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	public String RfidValidation(String rfid)
	{
		String clientEndpoint = "ad54yvkwpe4b6-ats.iot.us-east-2.amazonaws.com";       
		String clientId = "slotvalidation";   		
		String certificateFile = "//home//pi//IOT-NEW//b3d877e439-certificate.pem.crt";                      
		String privateKeyFile = "//home//pi//IOT-NEW//b3d877e439-private.pem.key";                       
		KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
		try {
			AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
			client.connect();	
			//subscribing to topic
			class MyTopic extends AWSIotTopic {
			    public MyTopic(String topic, AWSIotQos qos) {
			        super(topic, qos);
			    }
			    @Override
			    public void onMessage(AWSIotMessage message) {
			    	String received=new String(message.getPayload());
			    	JSONParser parser = new JSONParser();
			    	try {
						JSONObject json = (JSONObject) parser.parse(received);						
					    GateStatus=(String) json.get("status");				    
					    new SlotValidation().LEDConnection(GateStatus);
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}    	  				    	
			    }
			}
			String topicName = "smartparking/slotstatus";
			AWSIotQos qos = AWSIotQos.QOS1;
			MyTopic topicc = new MyTopic(topicName, qos);
			client.subscribe(topicc);
			//Publishing to topic
			String topic = "smartparking/slotvalidation";
			JSONObject jo=new JSONObject();
			jo.put("id",rfid);
			jo.put("slot",ParkingSlot);
			String payload=jo.toJSONString();	
			System.out.println("Payload is "+payload);
			client.publish(topic, AWSIotQos.QOS1, payload.getBytes());	
			}
		catch (AWSIotException e) {			
			 e.printStackTrace();
		}
		
		return GateStatus;
			}
	public void LEDConnection(String result) throws InterruptedException {
		if (result.equalsIgnoreCase("correct")) {
			greenLED.high();
			redLED.low();		
		} else {			
			greenLED.low();
			redLED.high();
		}

	}
	public void LEDChange() {
		greenLED.low();
		redLED.low();
	}
}
