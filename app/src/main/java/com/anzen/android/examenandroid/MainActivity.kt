package com.anzen.android.examenandroid

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.widget.*
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.ButterKnife
import com.anzen.android.examenandroid.adapters.StationListAdapter
import com.anzen.android.examenandroid.helpers.JsonReaderHelper
import com.anzen.android.examenandroid.models.Station
import com.anzen.android.examenandroid.utils.ResponseListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.math.RoundingMode

enum class OrderBy{
    ID, DISTANCE, BIKES, SLOTS
}

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapLoadedCallback,
    ResponseListener<Any> {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvStations: TextView
    private lateinit var fabFilter: FloatingActionButton
    private lateinit var mMap: GoogleMap
    private var nearbyRadius: Int = 1000
    private val mockLocation: Location = Location("")
    private val completeStationList: MutableList<Station> = mutableListOf()
    private var nearbyStationList: MutableList<Station> = mutableListOf()
    private var orderBy : OrderBy = OrderBy.DISTANCE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        ButterKnife.bind(this)

        mockLocation.latitude = 19.4329043
        mockLocation.longitude = -99.1355819

        tvStations = findViewById(R.id.tvStations)
        fabFilter = findViewById(R.id.fabFilter)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    override fun onSuccess(responseObject: Any) {
        Log.d("onSuccess[Response]", this.getString(R.string.success_message))
        makeText(this, this.getString(R.string.success_message), Toast.LENGTH_LONG).show()

        for (item in (responseObject as List<*>)) {
            val station: Station = item as Station

            val stationLocation = Location("")
            stationLocation.latitude = station.lat
            stationLocation.longitude = station.lon

            val distanceInMeters = mockLocation.distanceTo(stationLocation)
            station.distance = distanceInMeters

            completeStationList.add(station)
        }

        filterNearbyList(completeStationList)

    }

    override fun onError(error: String) {
        Log.d("onError[Response]", error)
        makeText(this, this.getString(R.string.error_message), Toast.LENGTH_LONG).show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLoadedCallback(this)
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.setPadding(0, 0, 0, 310)
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        mMap.clear()
        val mexicoCity = CameraPosition.builder()
            .target(LatLng(19.4329043, -99.1355819))
            .zoom(15f)
            .bearing(0f)
            .tilt(0f)
            .build()
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mexicoCity))
    }

    override fun onMapLoaded() {
        val jsonReader = JsonReaderHelper(this)
        jsonReader.getInfoBikes(this)
    }

    private fun filterNearbyList(stationList: List<Station>) {

        ///Create a new list in nearby radius
        for (station in stationList) {
            if (station.distance <= nearbyRadius) {
                nearbyStationList.add(station)
            }
        }

        ///order nearby station list
        when(orderBy){
            OrderBy.ID -> nearbyStationList.sortBy { it.id } //default
            OrderBy.DISTANCE -> nearbyStationList.sortBy { it.distance } //most nearby
            OrderBy.BIKES -> nearbyStationList.sortByDescending { it.bikes } //major to minor
            OrderBy.SLOTS -> nearbyStationList.sortBy { it.slots } //minor to major
        }

        ///create markers and add to the map
        addMarkersToMap(nearbyStationList)
        tvStations.text = "${nearbyStationList.size} ${getString(R.string.nearbyStationList)}"
        Log.i("items in list", nearbyStationList.size.toString())

        ///create list adapter
        val mAdapter = StationListAdapter(this, nearbyStationList)
        recyclerView = findViewById(R.id.station_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = mAdapter

        ///add on click listener when nearbyStationList is ready
        fabFilter.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun addMarkersToMap(stationList: List<Station>) {

        for (station in stationList) {
            val distanceInKilometers = (station.distance / 1000).toBigDecimal().setScale(2, RoundingMode.CEILING).toFloat()
            mMap.addMarker(
                MarkerOptions()
                    .position(LatLng(station.lat, station.lon))
                    .title(station.name)
                    .snippet("${this.getString(R.string.distanceLabel)}: $distanceInKilometers km")
                    .icon(bitmapDescriptorFromVector(this, R.drawable.ic_bike))
            )
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        val vectorWidth = vectorDrawable!!.intrinsicWidth + 15
        val vectorHeight = vectorDrawable.intrinsicHeight + 15
        val vectorPadding = 10

        vectorDrawable.setBounds(
            vectorPadding,
            vectorPadding,
            vectorWidth - vectorPadding,
            vectorHeight - vectorPadding
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0xFF6600FF.toInt()
        paint.style = Paint.Style.FILL

        val bitmap = Bitmap.createBitmap(
            vectorWidth,
            vectorHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        canvas.drawCircle(
            (vectorWidth.toFloat() / 2),
            (vectorHeight.toFloat() / 2),
            (vectorWidth.toFloat() / 2),
            paint
        )

        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun bitmapDescriptorFromImage(imageResId: Int): BitmapDescriptor? {
        val icon = BitmapFactory.decodeResource(this.resources, imageResId)
        return BitmapDescriptorFactory.fromBitmap(icon)
    }

    private fun showFilterDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.filter_dialog)

        val btnApplyFilter : Button = dialog.findViewById(R.id.btnApplyFilter)
        val seekBar : SeekBar = dialog.findViewById(R.id.seekBar)
        val tvSeekBarResult : TextView = dialog.findViewById(R.id.tvSeekBarResult)
        val btnId : RadioButton = dialog.findViewById(R.id.btnId)
        val btnDistance : RadioButton = dialog.findViewById(R.id.btnDistance)
        val btnBikes : RadioButton = dialog.findViewById(R.id.btnBikes)
        val btnSlots : RadioButton = dialog.findViewById(R.id.btnSlots)

        ///set values in controls
        tvSeekBarResult.text = "${this.getString(R.string.distanceLabel)}: ${(nearbyRadius / 1000)} km"
        seekBar.progress = (nearbyRadius / 1000).toInt()

        when(orderBy){
            OrderBy.ID -> btnId.isChecked = true
            OrderBy.DISTANCE -> btnDistance.isChecked = true
            OrderBy.BIKES -> btnBikes.isChecked = true
            OrderBy.SLOTS -> btnSlots.isChecked = true
        }

        ///set listeners of each control in dialog
        btnId.setOnClickListener{
            orderBy = OrderBy.ID
        }
        btnDistance.setOnClickListener{
            orderBy = OrderBy.DISTANCE
        }
        btnBikes.setOnClickListener{
            orderBy = OrderBy.BIKES
        }
        btnSlots.setOnClickListener{
            orderBy = OrderBy.SLOTS
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (i == 0){
                    nearbyRadius = 1 * 1000
                    seekBar.progress = 1
                } else {
                    nearbyRadius = i * 1000
                }

                tvSeekBarResult.text = "${applicationContext.getString(R.string.distanceLabel)}: ${(nearbyRadius / 1000)} km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })

        btnApplyFilter.setOnClickListener {
            mMap.clear()
            recyclerView.adapter =  null
            nearbyStationList.clear()
            filterNearbyList(completeStationList)
            dialog.dismiss()
        }

        dialog .show()

    }
}
