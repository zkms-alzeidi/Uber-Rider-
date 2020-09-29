package com.zkmsz.uberriderremake.Utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.zkmsz.uberriderremake.Common.Common
import com.zkmsz.uberriderremake.Model.TokenModel

object UserUtils {

    //to update user details
    fun updateUserDetails(context: Context, updateData: HashMap<String,Any>)
    {
        FirebaseDatabase.getInstance().getReference(Common.RIDER_INFO_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener{
                Toast.makeText(context,it.message!!, Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Toast.makeText(context,"Update information success", Toast.LENGTH_LONG).show()
            }
    }

    //to update token when register user and set on the database
    fun updateToken(context: Context, token: String)
    {
        val tokenModel= TokenModel()
        tokenModel.token= token

        FirebaseDatabase.getInstance().getReference(Common.TOKEN_REFERENCE)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(tokenModel)
            .addOnFailureListener { e->
                Toast.makeText(context,e.message +"1",Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Log.d("token: ", token)
                Toast.makeText(context,"done token",Toast.LENGTH_LONG).show()
            }
    }
}