package com.zkmsz.uberriderremake.Services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zkmsz.uberriderremake.Common.Common
import com.zkmsz.uberriderremake.Utils.UserUtils
import kotlin.random.Random

class MyFirebaseMessagingServices : FirebaseMessagingService()
{
    //to take the token
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        //update token
        if(FirebaseAuth.getInstance().currentUser != null)
        {
            UserUtils.updateToken(this,token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data= remoteMessage.data
        if(data!=null)
        {
            Common.showNotification(this,
                Random.nextInt(),
                data[Common.NOTI_TITLE],
                data[Common.NOTI_BODY],
                null)
        }
    }
}