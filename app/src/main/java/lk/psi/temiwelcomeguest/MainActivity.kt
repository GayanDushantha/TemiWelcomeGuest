package lk.psi.temiwelcomeguest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button

import com.robotemi.sdk.Robot
import com.robotemi.sdk.*
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.TtsRequest.Companion.create
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.navigation.model.SpeedLevel

class MainActivity : AppCompatActivity(), Robot.TtsListener,
    OnGoToLocationStatusChangedListener {

    private lateinit var mq: MQTTClient
    private lateinit var robot: Robot
    var topic: String = "temi/welcome"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Temi
        robot = Robot.getInstance()

        mq = MQTTClient(this)
        mq.connect(this)

//        mq.subscribe(topic)

        var btn = findViewById<Button>(R.id.button1);
        btn.setOnClickListener{

            Log.i("MainActivity","mq ${mq}")
            mq.subscribe(topic)

        }
    }



    override fun onStart() {
        super.onStart()
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addTtsListener(this)
    }

    override fun onStop() {
        super.onStop()
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeTtsListener(this)
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        Log.i("NavigationActivity", "onTtsStatusChonTtsStatusChangedanged ${TtsRequest}")
        mq.publish(topic,"TTS Status changed ${TtsRequest}")
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String
    ) {
        val text: String = "location ${location} - status ${status} - descriptionId ${descriptionId} - description ${description}"
        Log.i("NavigationActivity",text)
        mq.publish(topic,text)
    }

    fun goTo(place: String){
        var isExist: Boolean = false;
        for (location in robot.locations) {
            if (location == place) {
                isExist = true
                robot.goTo(
                    place,
                    backwards = false,
                    noBypass = false,
                    speedLevel = SpeedLevel.MEDIUM
                )
            }
        }

        if(!isExist)
            Log.e("NavigationActivity","No Location Found")
    }

    fun speak(text: String){
        val ttsRequest = create(
            text, language = TtsRequest.Language.SYSTEM,
            showAnimationOnly = false, isShowOnConversationLayer = false
        )
        robot.speak(ttsRequest)
    }

    fun cancelAllTts(){
        robot.cancelAllTtsRequests()
    }
}