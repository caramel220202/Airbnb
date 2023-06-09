package com.example.airbnb

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.airbnb.databinding.ActivityMainBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
import com.naver.maps.map.widget.LocationButtonView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity(), OnMapReadyCallback,Overlay.OnClickListener {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var naverMap: NaverMap
    private val mapview: MapView by lazy {
        binding.mapView
    }
    private lateinit var locationSource: FusedLocationSource
    private val viewPager:ViewPager2 by lazy {
        binding.houseViewPager
    }
    private val currentLocationBtn :LocationButtonView by lazy {
        binding.currentLocationBtn
    }
    private val viewPagerAdapter= HouseViewPagerAdapter(itemClicked = {
        val intent = Intent()
            .apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT,"[지금 이가격에 예약하세요!!] ${it.title} ${it.price}  ${it.imageUrl} ")
                type = "text/plain"
            }
        startActivity(Intent.createChooser(intent,null))
    })
    private val viewPagerRecyclerview :RecyclerView by lazy {
        findViewById(R.id.houseListRecyclerview)
    }
    private val houseListAdapter = HouseListAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        mapview.onCreate(savedInstanceState)

        mapview.getMapAsync(this)

        viewPager.adapter = viewPagerAdapter

        viewPagerRecyclerview.adapter = houseListAdapter
        viewPagerRecyclerview.layoutManager = LinearLayoutManager(this)

        viewPager.registerOnPageChangeCallback(object :ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val viewPagerModel = viewPagerAdapter.currentList[position]
                val cameraUpdate = CameraUpdate.scrollTo(LatLng(viewPagerModel.lat,viewPagerModel.lng))
                    .animate(CameraAnimation.Easing)

                naverMap.moveCamera(cameraUpdate)
            }
        })
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        naverMap.maxZoom = 18.0
        naverMap.minZoom = 10.0

        val uiSetting = naverMap.uiSettings
        uiSetting.isLocationButtonEnabled = false

        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.497816, 127.027235))
        naverMap.moveCamera(cameraUpdate)

        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource

        getHouseListFromAPI()

        currentLocationBtn.map = naverMap
    }

    private fun getHouseListFromAPI() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(HouseService::class.java).also {
            it.getHouseList().enqueue(object : Callback<HouseDto> {
                override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) {
                    if (response.isSuccessful.not()) {
                        return
                    }

                    response.body()?.let { dto ->
                        updateMarker(dto.items)
                        viewPagerAdapter.submitList(dto.items)
                        houseListAdapter.submitList(dto.items)
                    }
                }

                override fun onFailure(call: Call<HouseDto>, t: Throwable) {
                    Toast.makeText(this@MainActivity, t.message.toString(), Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }

    }
    private fun updateMarker(house:List<HouseModel>){
        house.forEach { house ->
            val marker = Marker()
            marker.position = LatLng(house.lat, house.lng)
            marker.onClickListener = this
            marker.map = naverMap
            marker.tag = house.id
            marker.icon = MarkerIcons.BLACK
            marker.iconTintColor = Color.RED
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }

        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated) {
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        mapview.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapview.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapview.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapview.onLowMemory()
    }

    override fun onClick(overlay: Overlay): Boolean {
     val selectedModel = viewPagerAdapter.currentList.firstOrNull(){
         it.id == overlay.tag
     }
        selectedModel?.let {
            val position = viewPagerAdapter.currentList.indexOf(it)
            viewPager.currentItem = position
        }
        return true
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}
