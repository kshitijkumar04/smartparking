import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
public class OutGate {
	AWSIotMqttClient mqttClient;
	final static GpioController gpio = GpioFactory.getInstance();
	public static String RFID;
	public static String red;
	public static String green;
	public static GpioPinDigitalOutput redLED;
	public static GpioPinDigitalOutput greenLED;
	String GateStatus;
	public static void main(String[] args) throws IOException {
		new OutGate().SensorConnection();
	}
	public void SensorConnection() throws IOException {
		redLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_26,PinState.HIGH);
		greenLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_21,PinState.LOW);
		while (true) {
			try {
				Thread.sleep(2000);
		System.out.println("Place your card to Read");
		Process pr = Runtime.getRuntime().exec("python Read.py");
		BufferedReader bri = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line;
		while ((line = bri.readLine()) != null) {
			System.out.print(line);
			System.out.println();
			break;
		}
		bri.close();
		new OutGate().RfidValidation(line);
		Thread.sleep(1000);
				}
			catch (InterruptedException e) 
			{
						e.printStackTrace();
											}
					}			
		}		
	public String RfidValidation(String rfid)
	{
		String clientEndpoint = "ad54yvkwpe4b6-ats.iot.us-east-2.amazonaws.com";       
		String clientId = "kshitijoutgate";   		
		String certificateFile = "//home//pi//IOT-NEW//b3d877e439-certificate.pem.crt";                      
		String privateKeyFile = "//home//pi//IOT-NEW//b3d877e439-private.pem.key";                          
		KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
		try {
			System.out.println("Try Block");
			AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
			client.connect();
			System.out.println("Connection Successful!!");
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
					    if (GateStatus.equalsIgnoreCase("success")) {
							new OutGate().LEDConnection(); // making Green LED
						}
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}    	  				    	
			    }
			}
			String topicName = "smartparking/outgatestatus";
			AWSIotQos qos = AWSIotQos.QOS1;
			MyTopic topicc = new MyTopic(topicName, qos);
			client.subscribe(topicc);
			//Publishing to topic
			String topic = "smartparking/outgate";
			JSONObject jo=new JSONObject();
			jo.put("id",rfid);
			String payload=jo.toJSONString();	
			System.out.println("Payload is "+payload);
			client.publish(topic, AWSIotQos.QOS1, payload.getBytes());			
			
			
			}
		catch (AWSIotException e) {			
			 e.printStackTrace();
		}
		
		return GateStatus;
			}
	public void LEDConnection() throws InterruptedException {
		greenLED.high();
		redLED.low();
		Thread.sleep(5000);
		greenLED.low();
		redLED.high();

	}
}
