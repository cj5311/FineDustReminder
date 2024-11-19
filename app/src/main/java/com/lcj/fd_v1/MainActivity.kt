package com.lcj.fd_v1

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.lcj.fd_v1.databinding.ActivityMainBinding

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
//    lateinit var locationProvider: LocationProvider


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAllPermissions() //1.권한허용처리
//        updateUI()
//        setRefreshButton()
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
//                updateUI() // UI없데이트 함수 실행
            } else { //결과값이 false 이면, 토스트메세지 실행하고, 앱 종료.
                Toast.makeText(this@MainActivity, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show()
                finish() // 앱종료
            }
        }
    }

    //==============================================================================================================


    private fun setRefreshButton() {
        binding.btnRefresh.setOnClickListener {
//            updateUI()
        }
    }

    //==============================================================================================================

//    private fun updateUI() {
//        locationProvider = LocationProvider(this@MainActivity)
//
//        //위도와 경도 정보를 가져옵니다.
//        val latitude: Double = locationProvider.getLocationLatitude()
//        val longitude: Double = locationProvider.getLocationLongitude()
//
//        if (latitude != 0.0 || longitude != 0.0) {
//
//            //1. 현재 위치를 가져오고 UI 업데이트
//            //현재 위치를 가져오기
//            val address = getCurrentAddress(latitude, longitude) //주소가 null 이 아닐 경우 UI 업데이트
//            address?.let {
//                binding.tvLocationTitle.text = "${it.thoroughfare}" // 예시: 역삼 1동
//                binding.tvLocationSubtitle.text = "${it.countryName} ${it.adminArea}" // 예시 : 대한민국 서울특별시
//            }
//
//            //2. 현재 미세먼지 농도 가져오고 UI 업데이트
//            getAirQualityData(latitude, longitude)
//
//        } else {
//            Toast.makeText(this@MainActivity, "위도, 경도 정보를 가져올 수 없었습니다. 새로고침을 눌러주세요.", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    /**
//     * @desc 레트로핏 클래스를 이용하여 미세먼지 오염 정보를 가져옵니다.
//     * */
//    private fun getAirQualityData(latitude: Double, longitude: Double) { // 레트로핏 객체를 이용하면 AirQualityService 인터페이스 구현체를 가져올 수 있습니다.
//        val retrofitAPI = RetrofitConnection.getInstance().create(AirQualityService::class.java)
//
//        retrofitAPI.getAirQualityData(latitude.toString(), longitude.toString(), "f8f5a711-7da9-4118-a875-304ffded8cb8")
//            .enqueue(object : Callback<AirQualityResponse> {
//                override fun onResponse(
//                    call: Call<AirQualityResponse>,
//                    response: Response<AirQualityResponse>,
//                ) { //정상적인 Response가 왔다면 UI 업데이트
//                    if (response.isSuccessful) {
//                        Toast.makeText(this@MainActivity, "최신 정보 업데이트 완료!", Toast.LENGTH_SHORT).show() //만약 response.body()가 null 이 아니라면 updateAirUI()
//                        response.body()?.let { updateAirUI(it) }
//                    } else {
//                        Toast.makeText(this@MainActivity, "업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//                override fun onFailure(call: Call<AirQualityResponse>, t: Throwable) {
//                    t.printStackTrace()
//                }
//            })
//    }
//
//    /**
//     * @desc 가져온 데이터 정보를 바탕으로 화면을 업데이트한다.
//     * */
//    private fun updateAirUI(airQualityData: AirQualityResponse) {
//        val pollutionData = airQualityData.data.current.pollution
//
//        //수치 지정 (가운데 숫자)
//        binding.tvCount.text = pollutionData.aqius.toString()
//
//        //측정된 날짜 지정
//        //"2021-09-04T14:00:00.000Z" 형식을  "2021-09-04 23:00"로 수정
//        val dateTime = ZonedDateTime.parse(pollutionData.ts).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime()
//        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
//
//        binding.tvCheckTime.text = dateTime.format(dateFormatter).toString()
//
//        when (pollutionData.aqius) {
//            in 0..50 -> {
//                binding.tvTitle.text = "좋음"
//                binding.imgBg.setImageResource(R.drawable.bg_good)
//            }
//
//            in 51..150 -> {
//                binding.tvTitle.text = "보통"
//                binding.imgBg.setImageResource(R.drawable.bg_soso)
//            }
//
//            in 151..200 -> {
//                binding.tvTitle.text = "나쁨"
//                binding.imgBg.setImageResource(R.drawable.bg_bad)
//            }
//
//            else -> {
//                binding.tvTitle.text = "매우 나쁨"
//                binding.imgBg.setImageResource(R.drawable.bg_worst)
//            }
//        }
//    }
//
//    /**
//     * @desc 위도와 경도를 기준으로 실제 주소를 가져온다.
//     * */
//    fun getCurrentAddress(latitude: Double, longitude: Double): Address? {
//        val geocoder = Geocoder(this, Locale.getDefault()) // Address 객체는 주소와 관련된 여러 정보를 가지고 있습니다. android.location.Address 패키지 참고.
//        val addresses: List<Address>?
//
//        addresses = try { //Geocoder 객체를 이용하여 위도와 경도로부터 리스트를 가져옵니다.
//            geocoder.getFromLocation(latitude, longitude, 7)
//        } catch (ioException: IOException) {
//            Toast.makeText(this, "지오코더 서비스 사용불가합니다.", Toast.LENGTH_LONG).show()
//            return null
//        } catch (illegalArgumentException: IllegalArgumentException) {
//            Toast.makeText(this, "잘못된 위도, 경도 입니다.", Toast.LENGTH_LONG).show()
//            return null
//        }
//
//        //에러는 아니지만 주소가 발견되지 않은 경우
//        if (addresses == null || addresses.size == 0) {
//            Toast.makeText(this, "주소가 발견되지 않았습니다.", Toast.LENGTH_LONG).show()
//            return null
//        }
//
//        val address: Address = addresses[0]
//
//        return address
//    }
//


}