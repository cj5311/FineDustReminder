package com.lcj.fd_v2

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lcj.fd_v2.databinding.ActivityMainBinding
import com.lcj.fd_v2.retrofit.AirQualityResponse
import com.lcj.fd_v2.retrofit.AirQualityService
import com.lcj.fd_v2.retrofit.RetrofitConnection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var binding : ActivityMainBinding

    // 런타임 권한 요청시 필요한 코드
    private val PERMISSIONS_REQUEST_CODE = 100

    // 요청할 권한 리스트 정의
    var REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // ActivityResultLauncher<Intent> : 결과값을 반환하는 인텐트를 정의
    lateinit var getGPSPermissionLauncher: ActivityResultLauncher<Intent>

    // 위도와 경도를 가져올 때 사용할 변수
    lateinit var locationProvider: LocationProvider

    // 위도와 경도 업데이트 하기 위해 변수 선언
    var latitude: Double? = 0.0
    var longitude: Double? = 0.0

    // registerForActivityResult : 페이지 이동시, 데이터정보를 콜백에 저장하여 공유할수 있게 하는 함수
    val startMapActivityResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult>{
                override fun onActivityResult(result: ActivityResult) {
                    if (result?.resultCode ?: Activity.RESULT_CANCELED == Activity.RESULT_OK) {
                        latitude = result?.data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                        longitude = result?.data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                        updateUI()
                    }
                }
            }
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions() //권한허용처리
        updateUI() //위도,경도값을 바탕으로 현재 주소와 현재 정보 업데이트
        setRefreshButton() // 새로고침 버튼 정의
        setFab()//지도보기 버튼 정의

    }

//==============================================================================================================

    //GPS 설정 판단로직.
    private fun checkAllPermissions() {
        if (!isLocationServicesAvailable()) { //GPS가 꺼져있을떄
            showDialogForLocationServiceSetting(); // GPS 설정창 오픈--> GPS 설정 --> isRuTimePermissionsGranted() 앱동작 제어
        } else {  //GPS 켜져있일떄
            isRunTimePermissionsGranted(); //GPS권한허용 후 동작제어.
        }
    }
//-----------------------------------------------------------------------------------------------------------------------------
// GPS설정값 불러오기.
    fun isLocationServicesAvailable(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager // 시스템의 위치서비스를 가져와 로케이션메니저 타입으로 맞춰준다.
        // 위성신호와 기지국으로부터 받은 위치정보 활성화 여부
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

//--------------------------------------------------------------------------------------------------------------------------
// GPS 설정창 열기 : 설정값을 다음페이지에 넘겨줘야함(런쳐객체사용)
private fun showDialogForLocationServiceSetting() {
    //런처생성 : registerForActivityResult(어떤계약조건인지 인수 설정){콜백함수}
    getGPSPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { 
        //result값을 매개변수로 받아 함수 실행
        result -> if (result.resultCode == Activity.RESULT_OK) { // 작업의 상태코드 == 작업성공여부
                    if (isLocationServicesAvailable()) { // GPS설정값이 true 이면
                        isRunTimePermissionsGranted() // 권한허용함수 실행
                    } else { //GPS설정이 false이면
                        Toast.makeText(this@MainActivity, "위치 서비스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
                        finish() // 앱종료
                    }
                }
    }

    //GPS 꺼져있을때 : 다이얼로그알람 빌더 정의
    val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
    builder.setTitle("위치 서비스 비활성화")
    builder.setMessage("위치 서비스가 꺼져있습니다. 설정해야 앱을 사용할 수 있습니다.")
    
    builder.setCancelable(true) // 다이얼로그창 바깥영역을 눌렀을떄 창이 꺼지도록 설정
    
    //설정버튼 생성 : 버튼 클릭시 인텐트 실행하고, 런쳐 실행
    builder.setPositiveButton("설정", DialogInterface.OnClickListener { dialog, id ->
        val callGPSSettingIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        getGPSPermissionLauncher.launch(callGPSSettingIntent)
    })
    // 취소버튼 생성 : 버튼 클릭시, 다이얼로그알람창 종료 --> 토스트메세지 출력 --> 앱 종료
    builder.setNegativeButton("취소", DialogInterface.OnClickListener { dialog, id ->
        dialog.cancel()
        Toast.makeText(this@MainActivity, "기기에서 위치서비스(GPS) 설정 후 사용해주세요.", Toast.LENGTH_SHORT).show()
        finish()
    })
    // 다이얼로그알람 생성 및 화면출력
    builder.create().show()
}

//------------------------------------------------------------------------------------------------------------------------
    //GPS사용권한 상태값 확인후, 권한혀용 선택에 따라 동작제어(권한허용 요청 및 앱 종료 실행)
    fun isRunTimePermissionsGranted() {
        //권한허용 상태값 불러오기
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        // PackageManager.PERMISSION_GRANTED : 권한허용상태
        if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED || hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED) { // 권한이 한 개라도 없다면 퍼미션 요청을 합니다.
            //requestPermissions : 권한요청실행 (요청할 리스트, 요청코드 )
            ActivityCompat.requestPermissions(this@MainActivity, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE)
        }
    }
    // 권한요청에 대한 응답값 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // 권한요청한값 일치여부 확인.
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.size == REQUIRED_PERMISSIONS.size) {
            var checkResult = true

            // 권한요청 결과값들을 담은 어레이에서 각 결과값 확인
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) { //권한허용상태와 일치하지 않으면,
                    checkResult = false // 결과값 false 반환
                    break
                }
            }
            if (checkResult) { //결과값이 true이면,
                updateUI() // UI없데이트 함수 실행
            } else { //결과값이 false 이면, 토스트메세지 실행하고, 앱 종료.
                Toast.makeText(this@MainActivity, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show()
                finish() // 앱종료
            }
        }
    }

    //==============================================================================================================

    // 위도,경도값을 바탕으로 데이터 업데이트
    private fun updateUI() {

        locationProvider = LocationProvider(this@MainActivity)

        if (latitude == 0.0 && longitude == 0.0) {
            latitude = locationProvider.getLocationLatitude()
            longitude = locationProvider.getLocationLongitude()
        }

        // 위도와 경도가 0값이 아닐경우
        if (latitude != null && longitude != null) {

            //위도, 경도로 주소정보 반환하는 함수 정의
            val address = getCurrentAddress(latitude!!, longitude!!)

            //주소가 null 이 아닐 경우 UI 업데이트
            address?.let {
                binding.tvLocationTitle.text = "${it.thoroughfare}" // 예시: 역삼 1동
                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}" // 예시 : 대한민국 서울특별시
            }

            //미세먼지 API 데이터 연결 및 ui업데이트
            getAirQualityData(latitude!!, longitude!!)

        } else {
            Toast.makeText(this@MainActivity, "위도, 경도 정보를 가져올 수 없었습니다. 새로고침을 눌러주세요.", Toast.LENGTH_LONG).show()
        }
    }

    //---------------------------------------------------------------------------------------------
    //위도경도 데이터로 주소값 변환하는 함수
    fun getCurrentAddress(latitude: Double, longitude: Double): Address? {// Address : 주소정보 담고있는 객체
        // Geocoder : 위도, 경도값과 주소값 매칭해줌.
//        val geocoder = Geocoder(this, Locale.getDefault()) // 현재 기기의 언어세팅값 가져옴.
        val geocoder = Geocoder(this, Locale.KOREA) // 한국어 설정
        val addresses: List<Address>?

        //Geocoder에서 주소리스트 받아옴.
        addresses = try {
        geocoder.getFromLocation(latitude, longitude, 7)
        } catch (ioException: IOException) {
            Toast.makeText(this, "지오코더 서비스 사용불가합니다.", Toast.LENGTH_LONG).show()
            return null
        } catch (illegalArgumentException: IllegalArgumentException) {
            Toast.makeText(this, "잘못된 위도, 경도 입니다.", Toast.LENGTH_LONG).show()
            return null
        }

        //에러는 아니지만 주소가 발견되지 않은 경우
        if (addresses == null || addresses.size == 0) {
            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
            return null
        }
        
        //주소리스트에서 첫번째값 반환
        val address: Address = addresses[0]
        return address
    }
//--------------------------------------------------------------------------------------------------------------
    // 레트로핏을 통해 API 데이터 가져오기
    private fun getAirQualityData(latitude: Double, longitude: Double) {

        //정의한 레트로핏 커넥션클래스에 인터페이스 함수를 지정하여, 레트로핏객체 생성
        val retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)

        //정의한 데이터변환객체에 접근하여, 위도, 경도, api키를 입력해 원하는 정보 가져옴.
        retrofitAPI.getAirQualityData(latitude.toString(), longitude.toString(), getString(R.string.air_api))
            .enqueue(object : Callback<AirQualityResponse>{ //excute()(동기실행) vs enqueue(비동기실행-백그라운드실행후 응답오면 실행)
                override fun onResponse(
                    call: Call<AirQualityResponse>,
                    response: Response<AirQualityResponse>,
                ) {
                    if (response.isSuccessful) {//응답성공시
                        Toast.makeText(this@MainActivity, "최신 정보 업데이트 완료!", Toast.LENGTH_SHORT).show()
                        response.body()?.let { updateAirUI(it) } 
                    } else {//응답실패시
                        Log.v("getString(R.string.air_api)",getString(R.string.air_api))
                        Toast.makeText(this@MainActivity, "업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                // 응답실패시 에러 출력
                override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

//---------------------------------------------------------------------------------------------------------------
    private fun updateAirUI(airQualityData: AirQualityResponse) {
        //데이터 클레스에서 해당데이터 가져오기
        val pollutionData = airQualityData.data.current.pollution

        //현재 미세먼지 농도값 업데이트
        binding.tvCount.text = pollutionData.aqius.toString()

        //현재날짜 , 데이터 타입 지정
        val dateTime = ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        
        // 측정시간 텍스트 업데이트
        binding.tvCheckTime.text = dateTime.format(dateFormatter).toString()
        
        // 미세먼지 농도값에 따라 텍스트, 이미지 제어
        when (pollutionData.aqius) {
            in 0..50 -> {
                binding.tvTitle.text = "좋음"
                binding.imgBg.setImageResource(R.drawable.bg_good)
            }

            in 51..150 -> {
                binding.tvTitle.text = "보통"
                binding.imgBg.setImageResource(R.drawable.bg_soso)
            }

            in 151..200 -> {
                binding.tvTitle.text = "나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_bad)
            }

            else -> {
                binding.tvTitle.text = "매우 나쁨"
                binding.imgBg.setImageResource(R.drawable.bg_worst)
            }
        }
    }

    //==============================================================================================================
    //새로고침 버튼 눌렀을때 업데이트
    private fun setRefreshButton() {
        binding.btnRefresh.setOnClickListener {
            updateUI()
        }
    }
    //===========================================================================================================
    // 지도보기 버튼 클릭시
    private fun setFab(){
        binding.fab.setOnClickListener{
            // 페이지 이동 인텐트객체 생성
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("currentLat", latitude) // 데이터 저장 
            intent.putExtra("currentLng", longitude)
            startMapActivityResult.launch(intent) // 인텐트객체에 담긴 정보로 정의한 런쳐 실행
        }
    }
    
}