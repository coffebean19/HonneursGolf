package com.example.testingmaps


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
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

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    var hole: Int = 0

    //Views for the animation
    private lateinit var chooseHoleView: View
    private lateinit var loadingView: View
    private lateinit var detectView: View
    private lateinit var showBallView: View
    private var mediumAnimationDuration: Int = 0

    //Variables for receiving the co-ordinates of ball, the default co-ordinates are those of
    //Potchefstroom
    var latitudeOfBall: String = "-27.138"
    var longitureOfBall: String = "26.833"


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chooseHoleView = findViewById(R.id.chooseHoleLayout)
        loadingView = findViewById(R.id.loadingLayout)
        detectView = findViewById(R.id.detectLayout)
        showBallView = findViewById(R.id.showBallLayout)

        //Hide loader
        loadingView.visibility = View.GONE

        //Retrieves system's short animation time default for loader animation
        mediumAnimationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)

        //Populate spinner with string array of the golf holes
        val spinner: Spinner = findViewById(R.id.holes_spinner)

        ArrayAdapter.createFromResource(
            this,
            R.array.holes_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = this

        //The assorted buttons and their functions that they need to execute

        //Umm, chooses the hole haha
        val btnChooseHole: Button = findViewById(R.id.btnChoose)
        btnChooseHole.setOnClickListener { holeChosen() }

        //Starts the detecting
        val btnDetect: Button = findViewById(R.id.btnDetect)
        btnDetect.setOnClickListener { detectBall() }

        //Starts the tracking of the ball
        val btnTrack: Button = findViewById(R.id.btnTrack)
        btnTrack.setOnClickListener { trackBall() }

        //Test things
        val btnCancel: Button = findViewById(R.id.btnCancel)
        btnCancel.setOnClickListener { reverseLoadingView(detectView) }

        //Showing the ball on the map
        val btnShow: Button = findViewById(R.id.btnShow)
        btnShow.setOnClickListener { showBallOnMap() }

        //Back button haha
        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { goBackToChooseView() }

        //This listener looks to the connection of the channel.
        pubnub.addListener(object: SubscribeCallback() {
            override fun status(pubNub: PubNub, status: PNStatus) {
                when (status.category) {
                    PNStatusCategory.PNConnectedCategory -> {
                        //Connect event, we can do stuff like publish apparently
                    }
                    PNStatusCategory.PNReconnectedCategory -> {
                        //This happens when signal is lost and regained. Apparently happens on a regular basis
                    }
                    PNStatusCategory.PNUnexpectedDisconnectCategory -> {
                        //When signal is completely lost
                    }
                }
            }

            //What do to if a message has been received
            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {

                //This is what happens if a message is received on the subscribed channel
                if (pnMessageResult.channel == myChannel) {

                    //We have a json Object which containts the messages that will be received
                    val jsonObject: JsonObject = pnMessageResult.message.asJsonObject
                        try {
                            /*
                                There is only events that respond to messages we intend to receive:
                                JetDetect : Success or Fail
                                JetTrack : Success or Fail
                                Any other messages received are ignored
                             */
                            if (jsonObject.has("JetDetect")) {
                                if (jsonObject.get("JetDetect").toString()
                                        .trim('\"') == "Success"
                                ) {
                                    runOnUiThread() { // because the reverseLoadingCard works with an animation,
                                        reverseLoadingView(detectView) // it needs to be run on the UI thread, or else it will crash.

                                        val cancel: Button = findViewById(R.id.btnCancel)
                                        cancel.setOnClickListener { stopTrackRequest() }
                                        loadingCard(detectView, "Tracking")
                                    }
                                }
                            }
                            if ((jsonObject.has("JetTrack"))) {

                                if (jsonObject.get("JetTrack").toString().trim('\"') == "Success") {
                                    runOnUiThread() {
                                        reverseLoadingView(showBallView)
                                    }
                                }
                            }
                            if ((jsonObject.has("lat"))) {
                                latitudeOfBall = jsonObject.get("lat").toString().trim('\"')
                                longitureOfBall = jsonObject.get("long").toString().trim('\"')
                            }
                        } catch (e: Exception) { }
                    }
                }
            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
                //handle the presence??
            }
        })

        pubnub.subscribe( channels = listOf(myChannel) )
        //---PubNub Things stop
    }

    private fun goBackToChooseView() {
        chooseHoleView.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(mediumAnimationDuration.toLong())
                .setListener(null)
        }
        showBallView.animate()
            .alpha(0f)
            .setDuration(mediumAnimationDuration.toLong())
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    chooseHoleView.visibility = View.GONE
                }
            })
    }

    //Stop tracking request
    private fun stopTrackRequest() {
        publishMessage("AppTrack", "Cancel")
        reverseLoadingView(chooseHoleView)
    }


    //Show the ball button view
    private fun showBallView() {
        showBallView.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(mediumAnimationDuration.toLong())
                .setListener(null)
        }
        chooseHoleView.animate()
            .alpha(0f)
            .setDuration(mediumAnimationDuration.toLong())
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    chooseHoleView.visibility = View.GONE
                }
            })
    }

    //The code for what the detect button need to do
    private fun detectBall() {
        val cancel: Button = findViewById(R.id.btnCancel)
        cancel.setOnClickListener { stopDetectRequest() }
        loadingCard(detectView, "Detecting")
        publishMessage("AppDetect", "Start")
    }

    //Sends the tracking signal
    private fun trackBall() {
        //loadingCard("Tracking")
        publishMessage("AppTrack", "Start")
    }

    //Spinner for golf hole selector functions(onItemSelected and onNothingSelected
    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
        hole = pos
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        val toast = Toast.makeText(applicationContext, "Nothing selected", Toast.LENGTH_SHORT)
        toast.show()
    }

    //Self explanatory, goes to the activity that displays the detect ball and show ball etc.
    private fun goToBallActivity(hole: Int)
    {
        val currentHole: String = (hole+1).toString()
        val intent = Intent(this, ShowBall::class.java).putExtra("CURRENT_HOLE", currentHole)

        startActivity(intent)
    }

    //executes when choose button is pressed
    private fun holeChosen() {
        val btnDetect: Button = findViewById(R.id.btnDetect)
        btnDetect.isEnabled = true
        displayDetectView()
    }

    //Displays detectView
    private fun displayDetectView() {
        detectView.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(mediumAnimationDuration.toLong())
                .setListener(null)
        }
        chooseHoleView.animate()
            .alpha(0f)
            .setDuration(mediumAnimationDuration.toLong())
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    chooseHoleView.visibility = View.GONE
                }
            })
    }

    //Send stop request to Jet to cancel detecting
    private fun stopDetectRequest() {
        publishMessage("AppDetect", "Cancel")
        reverseLoadingView(detectView)
    }


    fun onClickRequestPermission(view: android.view.View) {}

    /*
        Brings up the loading view and hides the previous view
        The parameters: view is the view from which it is called,
        the string is what the text under the loader should display
     */
    private fun loadingCard(view: View, process: String) {
        val txvCardLoader: TextView = findViewById(R.id.txvCardLoader)
        txvCardLoader.text = process
        loadingView.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(mediumAnimationDuration.toLong())
                .setListener(null)
        }
        view.animate()
            .alpha(0f)
            .setDuration(mediumAnimationDuration.toLong())
            .setListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                }
            })
    }

    //Reverse of loadingCard(), with the view to go back to
    private fun reverseLoadingView(view: View) {
        view.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(mediumAnimationDuration.toLong())
                .setListener(null)
        }

        loadingView.apply {
            animate()
                .alpha(0f)
                .setDuration(mediumAnimationDuration.toLong())
                .setListener(object: AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        loadingView.visibility = View.GONE
                    }
                })
        }
    }

    //Function to publish to pubnub channel
    private fun publishMessage(property: String, value: String) {
        val myMessage = JsonObject().apply {
            addProperty(property,value)
        }
        try {
            pubnub.publish(
                channel = myChannel,
                message = myMessage
            ).async { result, status ->
                if (!status.error) {
                    //TODO Error handling
                } else {
                    status.exception?.printStackTrace()
                }
            }
        } catch (e: Exception) {

        }
    }

    //Testing using toasting
    private fun testUsingToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //function for going to maps and throwing the pin where the co-ordinates of the ball is
    fun showBallOnMap() {

        val intent = Intent(this, MapsActivity::class.java).putExtra("BALL_LAT", latitudeOfBall)
        intent.putExtra("BALL_LONG", longitureOfBall)

        startActivity(intent)
    }

    //Receive device permission
    private fun getPermission() {

    }

}



















