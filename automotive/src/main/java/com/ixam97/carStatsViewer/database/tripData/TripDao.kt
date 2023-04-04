package com.mbuehler.carStatsViewer.database.tripData

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mbuehler.carStatsViewer.plot.objects.PlotLineItem

@Dao
interface TripDao {
    @Upsert
    fun addDrivingPoint(drivingPoint: DrivingPoint)

    @Query("SELECT * FROM DrivingPoints")
    fun getAllDrivingPoints(): List<DrivingPoint>

}