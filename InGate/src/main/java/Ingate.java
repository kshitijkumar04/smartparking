import com.amazonaws.services.iot.client.sample.sampleUtil.*;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.pi4j.io.gpio.*;

public class Ingate{
	AWSIotMqttClient mqttClient;
	final static GpioController gpio = GpioFactory.getInstance();
	public static String RFID;
	public static String red;
	public static String green;
	public static GpioPinDigitalOutput redLED;
	public static GpioPinDigitalOutput greenLED;
	String GateStatus;
	public void SensorConnection() throws IOException {
		redLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03,PinState.HIGH);
		greenLED = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04,PinState.LOW);

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
				new Ingate().RfidValidation(line); 				
				Thread.sleep(1000);
			}
		    catch (InterruptedException e) {
				e.printStackTrace();
			}
	}		

}

	public String RfidValidation(String rfid) throws InterruptedException
	{
		String clientEndpoint = "ad54yvkwpe4b6-ats.iot.us-east-2.amazonaws.com";       
		String clientId = "kshitijingate";   		
		String certificateFile = "//home//pi//IOT-NEW//b3d877e439-certificate.pem.crt";                      
		String privateKeyFile = "//home//pi//IOT-NEW//b3d877e439-private.pem.key";                            
		KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile);
		try {			
			AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
			client.connect();
			System.out.println("Connection Successful!!");
			//subscribing to topic
			class MyTopic extends AWSIotTopic {
			    public MyTopic(String topic, AWSIotQos qos) {
			        super(topic, qos);
			    }
			    @Override
			    public void onMessage(AWSIotMessage message) {
			    	System.out.println("inside onmessage");
			    	String received=new String(message.getPayload());
			    	System.out.println(received);
			    	JSONParser parser = new JSONParser();
			    	try {
						JSONObject json = (JSONObject) parser.parse(received);						
					    GateStatus=(String) json.get("status");	
					    System.out.println(GateStatus);				   
					if (GateStatus.equalsIgnoreCase("open")) {
							new Ingate().LEDConnection();
						}
					else if(GateStatus.equalsIgnoreCase("close")) {
						System.out.println("Not Registered");
					}
					} catch (ParseException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}    	  				    	
			    }
			}
			Thread.sleep(3000);
			String topicName = "smartparking/ingatestatus";
			AWSIotQos qos = AWSIotQos.QOS1;
			MyTopic topicc = new MyTopic(topicName, qos);
			client.subscribe(topicc);
			System.out.println("subscribed");
			//Publishing to topic
			String topic = "smartparking/rfidvalidation";
			JSONObject jo=new JSONObject();
			jo.put("id",rfid);
			jo.put("locationID","DXC-Park1");
			String payload=jo.toJSONString();	
			System.out.println("Payload is "+payload);
			client.publish(topic, AWSIotQos.QOS1, payload.getBytes());	
			}
		catch (AWSIotException e) {			
			 e.printStackTrace();
		}
		
		return GateStatus;
			}
	public static void main( String[] args ) throws IOException
    {
		Ingate ingate=new Ingate();		
		ingate.SensorConnection();
		
    }
	public void LEDConnection() throws InterruptedException {
		greenLED.high();
		redLED.low();
		Thread.sleep(5000);
		greenLED.low();
		redLED.high();
	}
	
}
