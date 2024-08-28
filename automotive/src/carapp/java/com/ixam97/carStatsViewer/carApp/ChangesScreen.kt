package com.mbuehler.carStatsViewer.carApp

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.mbuehler.carStatsViewer.BuildConfig
import com.mbuehler.carStatsViewer.CarStatsViewer
import com.mbuehler.carStatsViewer.R
import com.mbuehler.carStatsViewer.utils.ChangeLogCreator

class ChangesScreen(carContext: CarContext): Screen(carContext) {
    override fun onGetTemplate(): Template {
        return PaneTemplate.Builder(Pane.Builder().apply {
            addRow(messageRow())

            createVersionNoticesRows().forEach {row ->
                addRow(row)
            }

            addAction(Action.Builder().apply {
                setTitle("OK")
                setFlags(Action.FLAG_PRIMARY)
                setOnClickListener {
                    CarStatsViewer.appPreferences.versionString = BuildConfig.VERSION_NAME
                    screenManager.pop()
                }
            }.build())
            setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_car_app_upgrade)).build())
        }.build()).apply {
            setHeader(Header.Builder().apply {
                setTitle(carContext.getString(R.string.dialog_changes_title_updated))
                setStartHeaderAction(Action.APP_ICON)
            }.build())
        }.build()
    }

    private fun createVersionNoticesRows(): List<Row> {
        val changesRowList = mutableListOf<Row>()
        ChangeLogCreator.createChangelog(carContext).forEach {
            changesRowList.add(Row.Builder().apply {
                setTitle(it.key.ifBlank { "THERE WAS AN ERROR CREATING CHANGELOG! Please get in touch with the developer via mbuehler@mbuehler.de" })
                addText(it.value.ifBlank { "THERE WAS AN ERROR CREATING CHANGELOG! Please get in touch with the developer via mbuehler@mbuehler.de" })
            }.build())
        }
        return changesRowList
    }

    private fun messageRow(): Row {
        val message = carContext.getString(
            R.string.dialog_changes_message,
            carContext.getString(R.string.app_name),
            BuildConfig.VERSION_NAME.dropLast(5)
        )
        return Row.Builder().apply {
            setTitle(message)
        }.build()
    }
}