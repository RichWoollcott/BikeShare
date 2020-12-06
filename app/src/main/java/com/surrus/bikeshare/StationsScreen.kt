package com.surrus.bikeshare

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.surrus.bikeshare.ui.highAvailabilityColor
import com.surrus.bikeshare.ui.lowAvailabilityColor
import com.surrus.bikeshare.ui.viewmodel.BikeShareViewModel
import com.surrus.common.remote.Station
import com.surrus.common.remote.freeBikes
import com.surrus.common.remote.slots
import org.koin.androidx.compose.getViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


enum class Tabs(val title: String) {
    Map("Map"),
    List("List"),
}

class TabContent(val tab: Tabs, val content: @Composable (innerPadding: PaddingValues) -> Unit)


@Composable
fun StationsScreen(networkId: String, latitude: String, longitude: String, popBack: () -> Unit) {
    val bikeShareViewModel = getViewModel<BikeShareViewModel>()
    val stations by bikeShareViewModel.stations.observeAsState(emptyList())

    bikeShareViewModel.setCity(networkId)

    val mapTab = TabContent(Tabs.Map) {
        val map = MapView(AmbientContext.current)

        Text("hi")
        Column {

            AndroidView({ map }, modifier = Modifier.fillMaxHeight()) { map ->
                map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
                map.setMultiTouchControls(true)

                val mapController = map.controller
                mapController.setZoom(15.0)
                val startPoint = GeoPoint(latitude.toDouble(), longitude.toDouble())
                mapController.setCenter(startPoint)

                for (station in stations) {
                    val stationMarker = Marker(map)
                    stationMarker.position = GeoPoint(station.latitude, station.longitude)
                    stationMarker.title = station.name
                    map.overlays.add(stationMarker)
                }
            }

        }
    }

    val listTab = TabContent(Tabs.List) {
        LazyColumn {
            items(items = stations, itemContent = { station ->
                StationView(station)
            })
        }
    }

    val tabContent = listOf(listTab, mapTab)
    val (currentTab, updateTab) = savedInstanceState { tabContent.first().tab }
    val selectedTabIndex = tabContent.indexOfFirst { it.tab == currentTab }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BikeShare - $networkId") },
                navigationIcon = {
                    IconButton(onClick = { popBack() }) { Icon(Icons.Filled.ArrowBack) }
                }
            )
        },
        bodyContent = { innerPadding ->

            Column {
                TabRow(
                    selectedTabIndex = selectedTabIndex
                ) {
                    tabContent.forEachIndexed { index, tabContent ->
                        Tab(
                            text = { Text(tabContent.tab.title) },
                            selected = selectedTabIndex == index,
                            onClick = { updateTab(tabContent.tab) }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    // display the current tab content which is a @Composable () -> Unit
                    tabContent[selectedTabIndex].content(innerPadding)
                }
            }
        }
    )
}


@Composable
fun StationView(station: Station) {
    Card(elevation = 12.dp, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {

        Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {

            Image(vectorResource(R.drawable.ic_bike),
                colorFilter = ColorFilter.tint(if (station.freeBikes() < 5) lowAvailabilityColor else highAvailabilityColor),
                modifier = Modifier.preferredSize(32.dp))

            Spacer(modifier = Modifier.preferredSize(16.dp))

            Column {
                Text(text = station.name, style = MaterialTheme.typography.h6)

                val textStyle = MaterialTheme.typography.body2
                Row {
                    Text("Free:", style = textStyle, textAlign = TextAlign.Justify, modifier = Modifier.width(48.dp))
                    Text(text = station.freeBikes().toString(), style = textStyle)
                }
                Row {
                    Text("Slots:", style = textStyle, textAlign = TextAlign.Justify, modifier = Modifier.width(48.dp), )
                    Text(text = station.slots().toString(), style = textStyle)
                }
            }
        }
    }
}

