package com.example.testingmaps

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNLogVerbosity
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult

class ShowBall : AppCompatActivity() {

    //pubnub keys
    val pubNubConfig = PNConfiguration().apply {
        subscribeKey = "sub-c-932eacac-0c13-11ec-a08a-86a5cec709a9"
        publishKey = "pub-c-a74ae824-271e-4412-8de8-8c22048aa29f"
        uuid = "the_maps_app"
        logVerbosity = PNLogVerbosity.BODY
    }

    //The pubnub object, you do all the things with it
    val pubnub: PubNub =  PubNub(pubNubConfig)

    //The channel we communicate with
    val myChannel = "theChosenChannel"

    //Variables for receiving the co-ordinates of ball, the default co-ordinates are those of
    //Potchefstroom
    var latitudeOfBall: String = "-27.138"
    var longitureOfBall: String = "26.833"

    //Test variable for messages
    var theReceived: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_ball)

        val currentHole = intent.getStringExtra("CURRENT_HOLE")
        val detectBallHeader: TextView = findViewById(R.id.detectBallHeader)
        detectBallHeader.text = "You are at hole ${currentHole}"

        //+++PubNub things start

        //This listener looks to the connection of the channel.
        pubnub.addListener(object: SubscribeCallback() {
            override fun status(pubNub: PubNub, status: PNStatus) {
                when (status.category) {
                    PNStatusCategory.PNConnectedCategory -> {
                        //Connect event, we can do stuff like publish apparently
                        addToDebugText("Status: Connected")
                    }
                    PNStatusCategory.PNReconnectedCategory -> {
                        //This happens when signal is lost and regained. Apparently happens on a regular basis
                        addToDebugText("Status: reconnected")
                    }
                    PNStatusCategory.PNUnexpectedDisconnectCategory -> {
                        //When signal is completely lost
                        addToDebugText("Status: Signal lost")
                    }
                }
            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                if (pnMessageResult.channel == myChannel) {
                    val jsonObject: JsonObject = pnMessageResult.message.asJsonObject
                    theReceived = jsonObject.get("msg").toString()
                    latitudeOfBall = jsonObject.get("lat").toString().trim('\"')
                    longitureOfBall = jsonObject.get("long").toString().trim('\"')
                }
            }


            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
                //handle the presence??
            }
        })

        pubnub.subscribe( channels = listOf(myChannel) )
        addToDebugText(listOf(myChannel).toString())

        //---PubNub Things stop


        val showBall: Button = findViewById(R.id.btnShowBall)
        showBall.setOnClickListener { showBallOnMap() }

        val showMessageButton: Button = findViewById(R.id.button)
        showMessageButton.setOnClickListener { showTheMessage() }
    }

    private fun showTheMessage() {
        Toast.makeText(this, "$longitureOfBall, $latitudeOfBall", Toast.LENGTH_SHORT).show()
    }

    private fun testPublishFunction() {
        addToDebugText("detect function start")
        val myMessage = JsonObject().apply {
            addProperty("msg","Please gib work")
        }
        addToDebugText("publish start")
        try {
            pubnub.publish(
                channel = myChannel,
                message = myMessage
            ).async { result, status ->
                addToDebugText("status:  ${status.toString()}")
                if (!status.error) {
                    addToDebugText("Message sent, timetoken: ${result!!.timetoken}")
                } else {
                    addToDebugText("Error while publishing")
                    status.exception?.printStackTrace()
                }

            }
        } catch (e: Exception) {
            addToDebugText(e.localizedMessage)
        }
    }

    //function for going to maps and throwing the pin where the co-ordinates of the ball is
    fun showBallOnMap() {

        val intent = Intent(this, MapsActivity::class.java).putExtra("BALL_LAT", latitudeOfBall)
        intent.putExtra("BALL_LONG", longitureOfBall)

        startActivity(intent)
    }

    fun addToDebugText(message: String)
    {
        val tvDebug: TextView = findViewById(R.id.txtDebug)
        tvDebug.text = "${tvDebug.text}\n${message}"
    }
}