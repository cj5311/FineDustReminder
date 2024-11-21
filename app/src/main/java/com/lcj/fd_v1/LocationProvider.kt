package com.lcj.fd_v1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

class LocationProvider(val context: Context) {

    //Location : 위치정보 담고있는 클래스
    private var location: Location? = null
    //Location Manager : 위치서비스 접근을 제공하는 클래스
    private var locationManager: LocationManager? = null

    // 초기화 함수 정의 : 초기 위치정보 불러오는 함수 실행
    init {
        getLocation();
    }
    // ---------------------------------------------------------------------------------------

    private fun getLocation(): Location? {
        try {
            //locationManager 호출
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // 데이터 담을 변수 정의
            var gpsLocation: Location? = null
            var networkLocation: Location? = null

            //GPS위치정보 활성화 여부 변수로 할당
            val isGPSEnabled: Boolean =
                locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled: Boolean =
                locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            //둘다 비활성화 일 경우, null 반환
            if (!isGPSEnabled && !isNetworkEnabled) {
                return null
            } else { // 둘중에 하나라도 활성화 되었다면,
                // 권한정보 가져옴.
                val hasFineLocationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                //둘중의 하나의 권한만 없어도 null값 반환
                if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED
                ) return null

                // 두개권한이 모두 있는경우
                // 네트워크정보 활성화된경우 위치정보 가져옴
                if (isNetworkEnabled) {
                    networkLocation =locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }

                //GPS정보 활성화된경우 위치정보 가져옴.
                if (isGPSEnabled) {
                    gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                }
                // 두개의 정보가 있을때 정확도높은것을 location에 할당.
                if (gpsLocation != null && networkLocation != null) {
                    if (gpsLocation.accuracy > networkLocation.accuracy) {
                        location = gpsLocation
                    } else {
//                        location = networkLocation
                        location = gpsLocation // 가상기기 테스트용
                    }
                } else { //위치정보가 하나만 존재할때 null 값이 아닌값을 location에 할당
                    if (gpsLocation != null) {
                        location = gpsLocation
                    }
                    if (networkLocation != null) {
                        location = networkLocation
                    }
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return location
    }

    //위도값 반환
    fun getLocationLatitude(): Double? {
        return location?.latitude
    }

    //경도값 반환
    fun getLocationLongitude(): Double? {
        return location?.longitude
    }

}