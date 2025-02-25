package org.simbrain.util.piccolo

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import com.thoughtworks.xstream.annotations.XStreamImplicit
import org.piccolo2d.nodes.PImage
import org.simbrain.util.component1
import org.simbrain.util.component2
import java.awt.Color
import java.awt.Point
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.util.*

/**
 * Java representation of a .tmx tilemap produced by the Tiled app
 * (https://doc.mapeditor.org/en/stable/)
 *
 * A tilemap contains a list of [TileMapLayer]s and a list of [TileSet]s.
 *
 * Each layer is basically a grid of tiles, each of which points to a
 * member of a tileset.
 *
 * To get a sense of this see a sample tmx file like `aris_world.tmx`.
 *
 * This object does not return a list of separate PImages, but rather one big image for each layer. See [#createImageList]
 * This image is rendered only when the map changes.
 *
 * The XStream annotations in this class allow XStream to read in a .tmx file in its default format.
 *
 */
@XStreamAlias("map")
class TileMap(width: Int, height: Int) {

    /**
     * The TMX format version. Was “1.0” so far, and will be incremented to match minor Tiled releases.
     */
    @XStreamAsAttribute
    private val version: String = "1.0"

    /**
     * The Tiled version used to save the file (since Tiled 1.0.1). May be a date (for snapshot builds).
     */
    @XStreamAsAttribute
    private val tiledversion: String = "1.0.1"

    /**
     * The map width in tiles.
     */
    @XStreamAsAttribute
    var width = width
        private set

    /**
     * The map height in tiles.
     */
    @XStreamAsAttribute
    var height = height
        private set

    /**
     * The width of a tile.
     */
    @XStreamAlias("tilewidth")
    @XStreamAsAttribute
    val tileWidth = 32

    /**
     * The height of a tile.
     */
    @XStreamAlias("tileheight")
    @XStreamAsAttribute
    val tileHeight = 32

    /**
     * Get the map height in pixels.
     */
    val mapHeight: Int
        get() = height * tileHeight

    /**
     * Get the map width in pixels.
     */
    val mapWidth: Int
        get() = width * tileWidth

    /**
     * The tile set this map uses
     */
    @XStreamImplicit
    @XStreamAlias("tileset")
    val tileSets: List<TileSet> = listOf(createDefaultTileSet())

    /**
     * Cache id ranges for each TileSet in [tileSets]
     */
    @Transient
    private var tileSetRanges = tileSets.map { it.firstgid..(it.tilecount + it.firstgid) to it }

    /**
     * Cache [Tile] objects by their ids
     */
    @Transient
    private var idTileMapping = HashMap<Int, Tile>()

    /**
     * The layers of this map.
     */
    @XStreamImplicit
    val layers = mutableListOf(TileMapLayer("Default Layer", width, height, true))

    /**
     * The background color of the map. (optional, may include alpha value since 0.15 in the form #AARRGGBB)
     * (Not used for now)
     */
    private val backgroundcolor: Color? = null

    private var guiEnabled = false

    /**
     * Support for property change events.
     */
    @Transient
    private var changeSupport = PropertyChangeSupport(this)

    /**
     * Get a list of images of each layer of this map.
     * @return the list of layer images
     */
    fun createImageList(): List<PImage> {
        guiEnabled = true
        return layers.map { it.renderImage(tileSets) }
    }

    fun renderLayer(layer: TileMapLayer) {
        layer.renderImage(tileSets)
    }

    /**
     * Edit tiles in first layer. Use with care; assumes just one layer.
     */
    fun setTile(x: Int, y: Int, tileID: Int) {
        layers.first().setTile(x, y, tileID)
    }

    fun setTile(x: Int, y: Int, tileStringID: String) {
        val tileID = tileSets.asSequence().map { it[tileStringID] }.first()?.id
        tileID?.let { layers.first().setTile(x, y, it + 1) } ?: println("warn: no tile string id [$tileStringID] found")
    }

    /**
     * Edit tile in a named layer
     */
    fun setTile(layerName: String, x: Int, y: Int, tileID: Int) {
        getLayer(layerName).setTile(x, y, tileID)
    }

    /**
     * Set all tiles on first layer to specified tile id.
     */
    fun fill(tileId: Int) {
        (0 until width).forEach { i ->
            (0 until height).forEach { j ->
                setTile(i, j, tileId)
            }
        }
    }

    fun fill(tileLabel: String) {
        fill(tileSets.getIdFromLabel(tileLabel))
    }

    // TODO: should not be able to edit tile map layers that don't belong to this map
    fun TileMapLayer.setTile(x: Int, y: Int, tileID: Int) {
        this[x, y] = tileID
        if (guiEnabled) {
            val oldRenderedImage = layerImage
            val newRenderedImage = renderImage(tileSets, true)
            changeSupport.firePropertyChange("layerImageChanged", oldRenderedImage, newRenderedImage)
        }
    }

    /**
     * Check if a tile with a given id exists at a specified location in
     * pixel coordinates.
     *
     * @param id the id of the tile to check
     * @param x the x pixel location
     * @param y the y pixel location
     * @return true if the given tile exists in the tile stack
     */
    fun hasTileIdAtPixel(id: Int, x: Double, y: Double) =
            getTileStackAtPixel(x, y).any { t: Tile -> t.id == id }

    fun getTile(gid: Int) = idTileMapping.getOrPut(gid) {

        tileSetRanges
            .firstOrNull { (range, _) -> gid in range }
            ?.let { (_, tileSet) -> tileSet[gid] } ?: zeroTile
    }

    fun tileImage(gid: Int) = tileSetRanges
            .firstOrNull { (range, _) -> gid in range }
            ?.let { (_, tileSet) -> tileSet.getTileImage(gid) } ?: transparentTexture(tileWidth, tileHeight)

    /**
     * Returns the "stack" of tiles at a given location as a list.
     *
     * @param x tile coordinate x
     * @param y tile coordinate y
     * @return a list of tiles at that location in the same order as in the xml file
     */
    fun getTileStackAt(x: Int, y: Int) = layers.map { getTile(it[x, y]) }

    fun getTileStackAt(gridCoordinate: GridCoordinate) =
        getTileStackAt(gridCoordinate.x.toInt(), gridCoordinate.y.toInt())

    /**
     * Check if a given tile location contains any tiles or layers that with the collision property.
     *
     * @param x x in tile coordinate
     * @param y y in tile coordinate
     * @return true if the given location has a collision tile
     */
    fun collidingAt(x: Int, y: Int) =
            layers.filter { it.collision }.map { getTile(it[x, y]) }.any { it.id != 0 } ||
                    layers.map{ getTile(it[x,y]) }.any{ it.collision }

    /**
     * Get a Rectangle2D region of a given tile
     *
     * @param x x in tile coordinate
     * @param y y in tile coordinate
     * @return a Rectangle2D region representing the area of the tile
     */
    fun getTileBound(x: Int, y: Int) = Rectangle2D.Double(
            (x * tileWidth).toDouble(),
            (y * tileHeight).toDouble(),
            tileWidth.toDouble(),
            tileHeight.toDouble()
    )

    /**
     * Returns the "stack" of tiles at a given location as a list.
     *
     * @param x pixel x location
     * @param y pixel y location
     * @return a list of tiles at that location in the same order as in the xml file
     */
    fun getTileStackAtPixel(x: Double, y: Double) = pixelToTileCoordinate(x, y).let { (tileX, tileY) ->
        getTileStackAt(tileX, tileY)
    }

    /**
     * Returns the "stack" of tiles at a given location as a list.
     *
     * @param p pixel location
     * @return a list of tiles at that location in the same order as in the xml file
     */
    fun getTileStackAtPixel(p: Point2D) = getTileStackAtPixel(p.x, p.y)

    /**
     * Converts pixel location to tile coordinate.
     *
     * @param x pixel x location
     * @param y pixel y location
     * @return the corresponding tile location
     */
    fun pixelToTileCoordinate(x: Double, y: Double) = Point((x / tileWidth).toInt(), (y / tileHeight).toInt())

    fun pixelToTileCoordinate(p: Point2D) = pixelToTileCoordinate(p.x, p.y)

    /**
     * Converts pixel location to tile coordinate.
     *
     * @param p pixel location
     * @return the corresponding tile location
     */
    fun Point2D.toTileCoordinate() = pixelToTileCoordinate(x, y)

    /**
     * Get layer by name.
     */
    fun getLayer(name: String) =
            layers.firstOrNull { name == it.name } ?: throw IllegalArgumentException("$name not a tile layer name")

    /**
     * Clear and resize the map to the specified size. Maintains current tilemap.
     *
     * TODO: consider creating empty map instead of using this to avoid mutability
     *
     * @param width the new width of the map
     * @param height the new height of the map
     */
    fun updateMapSize(width: Int, height: Int) {
        if (width < 0 || height < 0) return

        this.width = width
        this.height = height
        layers.forEach {
            it.clear(width, height)
            it.renderImage(tileSets, true)
        }
    }

    fun addPropertyChangeListener(listener: PropertyChangeListener?) {
        changeSupport.addPropertyChangeListener(listener)
    }

    /**
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private fun readResolve(): Any {
        changeSupport = PropertyChangeSupport(this)
        tileSetRanges = tileSets.map { it.firstgid..(it.tilecount + it.firstgid) to it }
        idTileMapping = HashMap()
        return this
    }

}

