package com.example.myapplication2.interfacepackage

import android.content.Context
import com.example.myapplication2.model.Faq

interface InfoView {
    fun showFaqList (faqlist: List<Faq>)
    fun getContext(): Context
}