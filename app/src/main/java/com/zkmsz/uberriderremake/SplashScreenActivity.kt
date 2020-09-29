package com.zkmsz.uberriderremake

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.zkmsz.uberriderremake.Common.Common
import com.zkmsz.uberriderremake.Model.RiderModel
import com.zkmsz.uberriderremake.Utils.UserUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_splash_screen.*
import java.util.*
import java.util.concurrent.TimeUnit


class SplashScreenActivity : AppCompatActivity() {

    companion object
    {
        private val LOGIN_REQUEST_CODE= 7171
    }

    lateinit var provider: List<AuthUI.IdpConfig>
    lateinit var firebaseAuth: FirebaseAuth
    lateinit var listener:FirebaseAuth.AuthStateListener

    private lateinit var database:FirebaseDatabase
    private lateinit var riderInfoRef:DatabaseReference


    override fun onStart() {
        super.onStart()
        //show the splash screen
        delaySplashScreen()
    }

    private fun delaySplashScreen()
    {
        Completable.timer(3,TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
                firebaseAuth.addAuthStateListener(listener)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        init()
    }

    private fun init() {
        //root for the database
        database= FirebaseDatabase.getInstance()
        //reference for the riders
        riderInfoRef = database.getReference(Common.RIDER_INFO_REFERENCE)

        //the providers of sign
        provider = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        firebaseAuth = FirebaseAuth.getInstance()
        listener = FirebaseAuth.AuthStateListener {myFirebaseAuth->
            val user = myFirebaseAuth.currentUser
            if(user != null)
            {
                //update the token for the devise
                FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnFailureListener {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    }
                    .addOnSuccessListener {InstanceIdResult->
                        //set the token for this fun
                        UserUtils.updateToken(this,InstanceIdResult.token)
                    }

                checkUserFromFirebase()
            }
            else
            {
                showLoginLayout()
            }
        }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAuthMethodPickerLayout(authMethodPickerLayout)
            .setTheme(R.style.LoginTheme).setAvailableProviders(provider).setIsSmartLockEnabled(false).build(), LOGIN_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == LOGIN_REQUEST_CODE)
        {
            val response= IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK)
            {
                val user = firebaseAuth.currentUser

            }
            else
            {
                Toast.makeText(this, response!!.error!!.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkUserFromFirebase() {
        riderInfoRef.child(firebaseAuth.currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener
            {
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message, Toast.LENGTH_LONG).show()
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists())
                    {
                        val model = snapshot.getValue(RiderModel::class.java)
                        goToHomeActivity(model)
                    }
                    else
                    {
                        showRegisterLayout()
                    }
                }

            })
    }

    private fun showRegisterLayout() {
        val builder= AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView= LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val edt_first_name= itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name= itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number= itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val btn_continue=  itemView.findViewById<View>(R.id.btn_register) as Button

        //set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null
            && TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
        {
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        }

        builder.setView(itemView)
        val dialog = builder.create()
        dialog.show()

        //Event
        btn_continue.setOnClickListener {
            if(TextUtils.isDigitsOnly(edt_first_name.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter First Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else if(TextUtils.isDigitsOnly(edt_last_name.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter Last Name",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else if(TextUtils.isDigitsOnly(edt_phone_number.text.toString()))
            {
                Toast.makeText(this@SplashScreenActivity,"Please enter Phone Number",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            else
            {
                val model= RiderModel()
                model.firstName= edt_first_name.text.toString()
                model.lastName= edt_last_name.text.toString()
                model.phoneNumber= edt_phone_number.text.toString()


                riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{
                        Toast.makeText(this,it.message,Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        progress_bar.visibility= View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this,"Register successfully",Toast.LENGTH_LONG).show()
                        dialog.dismiss()

                        goToHomeActivity(model)

                        progress_bar.visibility= View.GONE
                    }
            }
        }
    }

    private fun goToHomeActivity(model: RiderModel?) {
        Common.currentRider= model
        startActivity(Intent(this,HomeActivity::class.java))
        finish()
    }

    override fun onStop() {
        super.onStop()
        if(firebaseAuth != null && listener != null)
        {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }
}