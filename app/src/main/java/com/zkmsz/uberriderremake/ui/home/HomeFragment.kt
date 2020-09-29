package com.zkmsz.uberriderremake.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.database.Observable
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryDataEventListener
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.zkmsz.uberriderremake.Callback.FirebaseDriverInfoListener
import com.zkmsz.uberriderremake.Callback.FirebaseFailedListener
import com.zkmsz.uberriderremake.Common.Common
import com.zkmsz.uberriderremake.Model.DriverGeoModel
import com.zkmsz.uberriderremake.Model.DriverInfoModel
import com.zkmsz.uberriderremake.Model.GeoQueryModel
import com.zkmsz.uberriderremake.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class HomeFragment : Fragment(), OnMapReadyCallback, FirebaseDriverInfoListener {

    companion object
    {
        const val LOCATION_PERMESSION_REQUEST= 12
    }

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mapFragment : SupportMapFragment
    private lateinit var mMap: GoogleMap

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //load drivers
    private var distance = 1.0
    private val LIMIT_RANGE= 10.0
    private var previousLocation: Location? = null
    private var currentLocation:Location? = null

    //listener
    lateinit var iFirebaseDriverInfoListener: FirebaseDriverInfoListener
    lateinit var iFirebaseFailedLestener: FirebaseFailedListener

    var firstTime= true

    var cityName= ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)


        init()

        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root


    }

    private fun init() {

        iFirebaseDriverInfoListener= this

        //location
        locationRequest = LocationRequest()
        locationRequest.apply {
            this.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            this.setFastestInterval(3000)
            this.interval= 5000
            this.setSmallestDisplacement(10f)
        }

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)

                val newPos= LatLng(locationResult!!.lastLocation.latitude,locationResult.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                //if user has change location, load the drivers again
                if(firstTime)
                {
                    previousLocation= locationResult.lastLocation
                    currentLocation= locationResult.lastLocation

                    firstTime= false
                }
                else
                {
                    previousLocation= currentLocation
                    currentLocation= locationResult.lastLocation
                }

                if (previousLocation!!.distanceTo(currentLocation)/1000 <= LIMIT_RANGE)
                {
                    loadAvailableDrivers()
                }


            }
        }

        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMESSION_REQUEST )
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,
            Looper.myLooper())

        loadAvailableDrivers()


    }

    private fun loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //Snackbar.make(requireView(),getString(R.string.permission_require),Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener {location ->

                //take name of city for user location
                val geocoder= Geocoder(requireContext(), Locale.getDefault())
                val addressList: List<Address>

                try
                {
                    //take the city from location
                    addressList= geocoder.getFromLocation(location.latitude,location.longitude,1)
                    cityName= addressList[0].locality

                    //query
                    val driver_location_ref= FirebaseDatabase.getInstance().getReference(Common.DRIVER_LOCATION_REFERENCES)
                        .child(cityName)

                    val gf = GeoFire(driver_location_ref)
                    val geoQuery= gf.queryAtLocation(GeoLocation(location.latitude,location.longitude), distance)

                    geoQuery.removeAllListeners()

                    geoQuery.addGeoQueryDataEventListener(object : GeoQueryEventListener,
                        GeoQueryDataEventListener {

                        override fun onKeyEntered(key: String?, location: GeoLocation?) {

                            Common.driversFound.add(DriverGeoModel(key!!, location!!))

                        }

                        override fun onKeyExited(key: String?) {

                        }

                        override fun onKeyMoved(key: String?, location: GeoLocation?) {

                        }

                        override fun onDataEntered(
                            dataSnapshot: DataSnapshot?,
                            location: GeoLocation?
                        ) {

                        }

                        override fun onDataExited(dataSnapshot: DataSnapshot?) {

                        }

                        override fun onDataMoved(
                            dataSnapshot: DataSnapshot?,
                            location: GeoLocation?
                        ) {

                        }

                        override fun onDataChanged(
                            dataSnapshot: DataSnapshot?,
                            location: GeoLocation?
                        ) {
                        }

                        override fun onGeoQueryReady() {
                            if (distance <= LIMIT_RANGE)
                            {
                                distance++
                                loadAvailableDrivers()
                            }
                            else
                            {
                                distance= 0.0
                                addDriverMarker()
                            }
                        }

                        override fun onGeoQueryError(error: DatabaseError?) {
                            Snackbar.make(requireView(),error!!.message,Snackbar.LENGTH_LONG).show()
                        }

                    })

                    driver_location_ref.addChildEventListener(object : ChildEventListener {
                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                            //have new driver
                            val geoQueryModel= snapshot.getValue(GeoQueryModel::class.java)
                            val geoLocation= GeoLocation(geoQueryModel!!.l!![0],geoQueryModel.l!![1])
                            val driverGeoModel= DriverGeoModel(snapshot.key,geoLocation)
                            val newDriverLocation= Location("")
                            newDriverLocation.latitude= geoLocation.latitude
                            newDriverLocation.longitude= geoLocation.longitude
                            val newDistance= location.distanceTo(newDriverLocation)/1000
                            if (newDistance <= LIMIT_RANGE)
                            {
                                findDriverBykey(driverGeoModel)
                            }

                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {

                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Snackbar.make(requireView(),error.message,Snackbar.LENGTH_LONG).show()
                        }

                    })
                }

                catch (e:IOException)
                {
                    Snackbar.make(requireView(),e.message!!,Snackbar.LENGTH_LONG).show()
                }

            }
            .addOnFailureListener {error->
                Snackbar.make(requireView(),error.message!!,Snackbar.LENGTH_LONG).show()
            }
    }

    private fun addDriverMarker() {
        if(Common.driversFound.size > 0)
        {
            io.reactivex.Observable.fromIterable(Common.driversFound)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe (
                    {
                        findDriverBykey(it)
                    },
                    {t: Throwable? ->
                        Snackbar.make(requireView(),t!!.message!!,Snackbar.LENGTH_LONG).show()
                    }
                )
        }

        else
        {
            Snackbar.make(requireView(),getString(R.string.drivers_not_found),Snackbar.LENGTH_LONG).show()
        }
    }
    //get the information for the driver
    private fun findDriverBykey(driverGeoModel: DriverGeoModel?) {
        FirebaseDatabase.getInstance().getReference(Common.DRIVER_INFO_REFERENCES)
            .child(driverGeoModel!!.key!!)
            .addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists())
                    {
                        driverGeoModel.driverInfoModel= snapshot.getValue(DriverInfoModel::class.java)
                        iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel)
                    }
                    else
                    {
                        iFirebaseFailedLestener.onFirebaseFailed(getString(R.string.key_not_found)+driverGeoModel.key)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    iFirebaseFailedLestener.onFirebaseFailed(error.message)
                }

            })
    }

    override fun onMapReady(map: GoogleMap?) {
        mMap= map!!

        mMap.uiSettings.isZoomControlsEnabled= true

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMESSION_REQUEST )
            return
        }

        if (ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mMap.isMyLocationEnabled= true
            mMap.uiSettings.isMyLocationButtonEnabled =true

            mMap.setOnMyLocationButtonClickListener {

                fusedLocationProviderClient.lastLocation

                    .addOnFailureListener {
                        Toast.makeText(requireContext(),it.message, Toast.LENGTH_LONG).show()
                    }

                    .addOnSuccessListener {location ->
                        val newPos= LatLng(location.latitude,location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))
                    }


                true
            }
            val locationButton= (mapFragment.requireView().findViewById<View>("1".toInt()).parent!! as View).findViewById<View>("2".toInt())
            val params= locationButton.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
            params.addRule(RelativeLayout.ALIGN_PARENT_END,0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.marginStart= 15
            }
            params.bottomMargin = 250

            //set map style
            try {
                val success= map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(),R.raw.uber_maps_style))
                if (!success)
                {
                    Snackbar.make(requireView(),"load map style is failed", Snackbar.LENGTH_LONG).show()
                }
            }
            catch (e:Exception)
            {
                Snackbar.make(requireView(),e.message.toString(), Snackbar.LENGTH_LONG).show()
            }
        }

    }

    override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel?) {
        if(!Common.MarkerList.containsKey(driverGeoModel!!.key))
        {
            Common.MarkerList.put(driverGeoModel!!.key!!,mMap.addMarker(MarkerOptions()
                .position(
                    LatLng(driverGeoModel.geoLocation!!.latitude,
                        driverGeoModel.geoLocation!!.longitude))
                .flat(true)
                .title(Common
                    .buildName(
                        driverGeoModel.driverInfoModel!!.firstName,
                        driverGeoModel.driverInfoModel!!.lastName))
                .snippet(driverGeoModel.driverInfoModel!!.phoneNumber)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))))
        }

        if (!TextUtils.isEmpty(cityName))
        {
            val driverLocation= FirebaseDatabase.getInstance().getReference(Common.DRIVER_LOCATION_REFERENCES)
                .child(cityName)
                .child(driverGeoModel.key!!)

            driverLocation.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChildren())
                    {
                        if(Common.MarkerList[driverGeoModel.key!!] != null)
                        {
                            Common.MarkerList[driverGeoModel.key!!]!!.remove() //remove marker from map
                            Common.MarkerList.remove(driverGeoModel.key!!) //remove information from map
                            driverLocation.removeEventListener(this)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Snackbar.make(requireView(),error!!.message,Snackbar.LENGTH_LONG).show()
                }

            })
        }
    }
}