package com.skdev.ytlivevideo.ui.createBroadcast

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.CreateLiveEvent
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
import kotlinx.android.synthetic.main.activity_create_broadcast.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class CreateNewBroadcast: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_broadcast)
    }

    fun onCreateBroadcast(view: View) {
        val name = broadcast_name.text.toString()
        val description = broadcast_description.text.toString()
        createNewBroadcast(name, description) {
            finish()
        }
    }

    /**
     * Create a new Broadcast
     */
    private fun createNewBroadcast(name: String, description: String, completion: () -> Unit) {
        Log.d(TAG, "createEvent")
        val date = Date().toString()
        val progressDialog = ProgressDialog.create(this, R.string.creatingEvent)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CreateLiveEvent.runAsync(name, description).await()
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    completion()
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    completion()
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    val context = application.applicationContext
                    Utils.showError(context, e.localizedMessage)
                    completion()
                }
            }
        }
    }

    companion object {
        private val TAG = CreateNewBroadcast::class.java.name
    }
}
