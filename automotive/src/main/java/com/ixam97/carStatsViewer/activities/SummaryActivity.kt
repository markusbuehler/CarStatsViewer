package com.mbuehler.carStatsViewer.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.mbuehler.carStatsViewer.R
import com.mbuehler.carStatsViewer.objects.DataHolder
import kotlinx.android.synthetic.main.activity_summary.*

class SummaryActivity: Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)

        summary_button_back.setOnClickListener {
            finish()
        }

        summary_button_reset.setOnClickListener {
            DataHolder.resetDataHolder()
            sendBroadcast(Intent(getString(R.string.save_trip_data_broadcast)))
        }
    }
}