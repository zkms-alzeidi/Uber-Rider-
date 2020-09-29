package com.zkmsz.uberriderremake.Callback

import com.zkmsz.uberriderremake.Model.DriverGeoModel

interface FirebaseDriverInfoListener {
    fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?)
}