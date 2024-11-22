package com.lcj.fd_v2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.lcj.fd_v2.databinding.ActivityMapBinding

// OnMapReadyCallback 지도가 준비되었을떄 실행되는 콜백함수 인터페이스 구현
class MapActivity : AppCompatActivity() ,OnMapReadyCallback {

    lateinit var binding: ActivityMapBinding //레이아웃 데이터 연결

    private var mMap: GoogleMap? = null // 구글맵 사용위한 변수

    var currentLat: Double = 0.0 // 메인엑티비티 인텐트에 전달된 데이터 변수
    var currentLng: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 바인딩 세팅
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /// 메인엑티비티 인텐트에 전달된 데이터 변수 가져오기
        currentLat = intent.getDoubleExtra("currentLat", 0.0)
        currentLng = intent.getDoubleExtra("currentLng", 0.0)

        // 지도객체 변수 생성
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this) // 지도 요청 : 지도가 준비되면 지도실행을 요청해주는 비동기 함수

        setButton() // 버튼클릭함수 정의 (미세먼지농도 보기 + 현재위치보기)

    }
//===============================================================================================
    //지도가 준비되었을 때 실행되는 콜백
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap // 구글맵 객체 저장

        mMap?.let{
            val currentLocation = LatLng(currentLat, currentLng)//위도경도를 지도상에 현재위치로 찾아줌.
            it.setMaxZoomPreference(20.0f) //줌 최대값 설정
            it.setMinZoomPreference(12.0f) //줌 최솟값 설정
            it.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
        }

        setMarker() // 현재위치에 마커 출력해주는 함수
    }
//--------------------------------------------------------------------------------------------
    //마커 설정하는 함수
    private fun setMarker(){
        mMap?.let{
            it.clear() //지도에 있는 마커를 먼저 삭제
            val markerOptions = MarkerOptions() // 마커 옵션 설정
            markerOptions.position(it.cameraPosition.target) //마커의 위치 설정
            markerOptions.title("마커 위치") //마커의 이름 설정
            val marker = it.addMarker(markerOptions) //마커 객체 생성후 변수화 : 카메라 변동에 따라 추척하기 위해
            it.setOnCameraMoveListener { // 카메라 이동될때
                marker?.let { marker ->
                    marker.setPosition(it.cameraPosition.target)
                }
            }
        }
    }

    //---------------------------------------------------------------------------------------
    private fun setButton(){
        binding.btnCheckHere.setOnClickListener {
            mMap?.let { // mMap객체가 null이 아닐때 함수 실행
                val intent = Intent() // 인텐트 객체 생성 : 종료만 시켜줘도 메인엑티비티로 이동됨.
                intent.putExtra("latitude", it.cameraPosition.target.latitude) // 데이터 저장
                intent.putExtra("longitude", it.cameraPosition.target.longitude)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
        binding.fabCurrentLocation.setOnClickListener{
            val locationProvider = LocationProvider(this@MapActivity)
            val latitude = locationProvider.getLocationLatitude()
            val longitude = locationProvider.getLocationLongitude()
            // 현재위치로 카메라 이동 후 마커 표출
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude!!,longitude!!), 16f))
            setMarker()
        }
    }

}