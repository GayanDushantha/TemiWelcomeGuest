package lk.psi.temiwelcomeguest

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.json.JSONObject


class MQTTClient {

    private lateinit var mainActivity: MainActivity

    constructor(mainActivity: MainActivity){
        this.mainActivity = mainActivity
    }

    private lateinit var mqttClient: MqttAndroidClient

    // TAG
    companion object {
        const val TAG = "AndroidMqttClient"
    }

    fun connect(context: Context) {
//        val serverURI = "tcp://broker.emqx.io:1883"
        val serverURI = "tcp://18.119.41.100:1883"
        this.mqttClient = MqttAndroidClient(context, serverURI, "kotlin_client")
        this.mqttClient.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d(TAG, "Receive message: ${message.toString()} from topic: $topic")

                try {
                    val msg: JSONObject = JSONObject(message.toString())
                    val intent = msg.get("intent").toString()
                    val method = msg.get("method").toString()
                    val value = msg.get("value").toString()

                    if(intent == "start"){
                        if(method == "speak"){
                            mainActivity.speak(value)
                        }else if(method == "navigate"){
                            mainActivity.goTo(value)
                        }
                    }else if(intent == "stop"){
                        if(method == "speak"){
                            mainActivity.cancelAllTts()
                        }
                    }


                }catch (e: Exception ){
                    Log.e("MQTTActivity","invalid message format ${message.toString()}")
                }

            }

            override fun connectionLost(cause: Throwable?) {
                Log.d(TAG, "Connection lost ${cause.toString()}")
                mainActivity.showAlert("connection lost to MQTT server")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }
        })
        val options = MqttConnectOptions()
        options.setUserName("psi_user");
        options.setPassword("pass#word1".toCharArray());
        try {
            Log.i("MQTTActivity","Trying to connect.")
            this.mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")

                    Log.d(TAG, "starting subscribe")
                    subscribe(mainActivity.topic)
                    Log.d(TAG, "subscribe done")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Connection failure ${exception}")
                    mainActivity.showAlert("connection failed to MQTT server")
                }
            })
        } catch (e: MqttException) {
            Log.i("MqttClient","== error in connecting mqtt")
            e.printStackTrace()
        }

    }

    fun subscribe(topic: String, qos: Int = 1) {
        try {
            this.mqttClient.subscribe(topic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Subscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to subscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun unsubscribe(topic: String) {
        try {
            mqttClient.unsubscribe(topic, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Unsubscribed to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to unsubscribe $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, msg: String, qos: Int = 1, retained: Boolean = false) {
        try {
            val message = MqttMessage()
            message.payload = msg.toByteArray()
            message.qos = qos
            message.isRetained = retained
            mqttClient.publish(topic, message, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "$msg published to $topic")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to publish $msg to $topic")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

}