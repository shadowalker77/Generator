package com.alirezabdn.generator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ir.ayantech.ayannetworking.api.AyanApi
import ir.ayantech.networking.callGetEndUserInquiryHistoryDetail
import ir.ayantech.networking.callMyApi
import ir.ayantech.networking.simpleCallGetEndUserInquiryHistoryDetail
import ir.ayantech.networking.simpleCallMyApi

class MainActivity : AppCompatActivity() {

    lateinit var ayanAPI: AyanApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ayanAPI.simpleCallMyApi(MyApi.Input("test")) { }
        ayanAPI.callMyApi(MyApi.Input("test")) {
            success { }
            failure { }
            changeStatusCallback { }
        }
        ayanAPI.simpleCallGetEndUserInquiryHistoryDetail(
            GetEndUserInquiryHistoryDetail.Input("Test")
        ) {

        }
        ayanAPI.callGetEndUserInquiryHistoryDetail(
            GetEndUserInquiryHistoryDetail.Input("Test")
        ) {
            success { }
            failure { }
            changeStatusCallback { }
        }
    }
}