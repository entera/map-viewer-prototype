package de.entera.prototype.mapviewer

import com.vividsolutions.jts.geom.Geometry
import javafx.application.Application
import javafx.collections.FXCollections.observableArrayList
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.stage.Window
import org.geotools.data.FileDataStore
import org.geotools.data.FileDataStoreFinder
import java.io.File

fun main(args: Array<String>) {
    Application.launch(MainApp::class.java)
}

class MainApp : Application() {

    lateinit var rootDirectory: File
    lateinit var shapefiles: List<File>

    override fun init() {
        rootDirectory = File("contrib/TM_WORLD_BORDERS_SIMPL")
        shapefiles = rootDirectory.listFiles { file ->
            file.name.endsWith(".shp")
        }.toList()
    }

    override fun start(stage: Stage) {
        val rootPane = StackPane()
        stage.scene = Scene(rootPane, 300.0 + 600.0, 600.0)
        stage.title = javaClass.canonicalName
        registerKeyboardHandlers(stage.scene)
        stage.setOnShown { event -> initRootPane(rootPane) }
        stage.show()
    }

    private fun initRootPane(rootPane: Pane) {
        val borderPane = BorderPane()
        rootPane.children += borderPane

        val layerPane = buildLayerPane()
        borderPane.left = layerPane
        val mapPane = buildMapPane()
        borderPane.center = mapPane
    }

    private fun buildLayer(dataStore: FileDataStore): Group {
        val features = dataStore.featureSource.features.features()
        val bounds = dataStore.featureSource.bounds

        val layer = Group()
        val renderer = ShapeRendererImpl()

        features.use {
            while (features.hasNext()) {
                val feature = features.next()
                val geometry = feature.defaultGeometry as Geometry
                val shape = renderer.geometry(geometry)
                layer.children += shape
            }
        }

        return layer
    }

    private fun buildLayerPane(): ListView<Any> {
        val listView = ListView<Any>().apply {
            prefWidth = 300.0
        }
        listView.items = observableArrayList(shapefiles.toList())
        listView.setCellFactory {
            createListCell {
                val item = item
                text = when (item) {
                    is File -> item.name
                    is Any -> item.toString()
                    else -> null
                }
            }
        }
        return listView
    }

    private fun buildMapPane(): MapPane {
        val dataStore = FileDataStoreFinder.getDataStore(shapefiles.first())
        return MapPane().apply {
            layers += buildLayer(dataStore)
        }
    }

    private fun registerKeyboardHandlers(scene: Scene) {
        scene.setOnKeyReleased {
            when (it.code) {
                KeyCode.ESCAPE -> closeWindow(scene.window)
                else -> Unit
            }
        }
    }

    private fun closeWindow(window: Window) = window.hide()

    private fun <T> createListCell(updateItem: (ListCell<T>.() -> Unit)): ListCell<T> {
        return object : ListCell<T>() {
            override fun updateItem(item: T?, empty: Boolean) {
                super.updateItem(item, empty)
                updateItem(this)
            }
        }
    }

}
