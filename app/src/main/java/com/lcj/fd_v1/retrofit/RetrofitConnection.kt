package com.lcj.fd_v1.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitConnection {
    //자원절약을 위해 싱글톤 패턴 설정
    companion object {
        private const val BASE_URL = "https://api.airvisual.com/v2/"
        private var INSTANCE: Retrofit? = null
        
        //응답 인스턴스를 반환하는 함수 정의
        fun getInstance(): Retrofit {
            if (INSTANCE == null) {
                INSTANCE = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())//GsonConverterFactory : 응답을 데이터클레스로 자동변환
                    .build()
            }
            return INSTANCE!!
        }
    }


}