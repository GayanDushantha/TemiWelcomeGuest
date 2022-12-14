package lk.psi.temiwelcomeguest

import android.content.ContentValues
import android.content.DialogInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AlertDialog

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


        val videoPath = "android.resource://" + packageName + "/" + R.raw.temiscreen
        val uri = Uri.parse(videoPath)
        val videoView = findViewById<VideoView>(R.id.videoView)
        videoView.setVideoURI(uri)
        videoView.requestFocus()

        videoView.setOnCompletionListener {
            Toast.makeText(this@MainActivity, "thanks for watching", Toast.LENGTH_SHORT).show()
        }
        videoView.setOnErrorListener { mp, what, extra ->
            Toast.makeText(this@MainActivity, "thanks for watching", Toast.LENGTH_SHORT).show()
            false
        }
        videoView.setOnPreparedListener{ mp ->
            videoView.start()
            mp!!.isLooping = true;
            Log.i(ContentValues.TAG, "Video Started");
        }

        // Temi
        robot = Robot.getInstance()

        mq = MQTTClient(this)
        mq.connect(this)

//        mq.subscribe(topic)

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
        mq.publish(topic,"TTS Status changed $ttsRequest")
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
                    speedLevel = SpeedLevel.SLOW
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

    fun showAlert(message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Error Occurred")
        builder.setMessage(message)
        builder.setIcon(R.drawable.ic_launcher_background)
        builder.setPositiveButton("ok", DialogInterface.OnClickListener{ dialog, which ->
            dialog.dismiss()
        })
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }
}