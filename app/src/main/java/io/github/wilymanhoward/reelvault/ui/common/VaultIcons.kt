package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** ReelVault's geometric icon set, shared across navigation and content actions. */
object VaultIcons {
    private fun line(name: String, data: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        .addPath(pathData = addPathNodes(data), stroke = SolidColor(Color.White), strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round).build()

    val Library = line("Library", "M4 4H9V10H4ZM15 4H20V10H15ZM4 16H9V20H4ZM15 14H20V20H15Z")
    val Collections = line("Collections", "M4 7H9L11 4H20V19H4ZM4 10H20")
    val Search = line("Search", "M16 16L21 21M18 10A8 8 0 1 1 2 10A8 8 0 1 1 18 10")
    val Settings = line("Settings", "M4 7H20M4 17H20M8 4V10M16 14V20")
    val Plus = line("Plus", "M12 4V20M4 12H20")
    val Check = line("Check", "M5 12L10 17L20 6")
    val CheckCircle = line("CheckCircle", "M21 12A9 9 0 1 1 12 3M7 12L11 16L21 5")
    val Close = line("Close", "M6 6L18 18M18 6L6 18")
    val Pin = line("Pin", "M8 3H16L15 9L19 14H13V21H11V14H5L9 9Z")
    val Star = line("Star", "M12 3L15 9L22 10L17 15L18 22L12 18L6 22L7 15L2 10L9 9Z")
    val User = line("User", "M16 7A4 4 0 1 1 8 7A4 4 0 1 1 16 7M4 21V18C4 12 20 12 20 18V21")
    val CloudOff = line("CloudOff", "M3 3L21 21M5 16A5 5 0 0 1 7 6A7 7 0 0 1 20 10A4 4 0 0 1 22 15M7 18H17")
    val Paste = line("Paste", "M9 5H5V21H19V5H15M9 3H15V7H9Z")
    val Trash = line("Trash", "M4 6H20M9 6V3H15V6M6 6L7 21H17L18 6M10 10V17M14 10V17")
    val Move = line("Move", "M3 6H9L11 9H21V20H3ZM9 14H17M14 11L17 14L14 17")
    val Edit = line("Edit", "M4 16V21H9L21 9L16 4ZM13 7L18 12")
    val Image = line("Image", "M3 3H21V21H3ZM3 17L9 11L14 16L17 13L21 17M16 7H16.01")
    val Info = line("Info", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12M12 11V17M12 7H12.01")
    val Keyboard = line("Keyboard", "M2 5H22V19H2ZM6 9H6.01M10 9H10.01M14 9H14.01M18 9H18.01M6 13H6.01M18 13H18.01M9 15H15")
    val ChevronDown = line("ChevronDown", "M5 9L12 16L19 9")
    val ChevronUp = line("ChevronUp", "M5 15L12 8L19 15")
    val ChevronLeft = line("ChevronLeft", "M15 5L8 12L15 19")
    val ChevronRight = line("ChevronRight", "M9 5L16 12L9 19")
    val Import = line("Import", "M3 7H8M16 7H21V21H3V7M12 2V15M8 11L12 15L16 11")
    val Link = line("Link", "M9 15L15 9M7 11L4 14A4 4 0 0 0 10 20L13 17M11 7L14 4A4 4 0 0 1 20 10L17 13")
    val LinkOff = line("LinkOff", "M3 3L21 21M8 12L4 16A3 3 0 0 0 8 20L12 16M12 8L16 4A3 3 0 0 1 20 8L16 12")
    val More = line("More", "M4 12H4.05M12 12H12.05M20 12H20.05")
    val MoreVertical = line("MoreVertical", "M12 4H12.05M12 12H12.05M12 20H12.05")
    val Refresh = line("Refresh", "M20 8A9 9 0 1 0 21 15M20 3V8H15")
    val MinusCircle = line("MinusCircle", "M21 12A9 9 0 1 1 3 12A9 9 0 1 1 21 12M7 12H17")
    val Tag = line("Tag", "M3 3H12L22 13L13 22L3 12ZM7 7H7.01")
    val Share = line("Share", "M12 16V2M7 7L12 2L17 7M5 12H3V22H21V12H19")
    val Sort = line("Sort", "M4 6H20M4 12H15M4 18H10")
    val List = line("List", "M9 5H21M9 12H21M9 19H21M3 5H4M3 12H4M3 19H4")
    val Swap = line("Swap", "M7 3V21M3 7L7 3L11 7M17 21V3M13 17L17 21L21 17")
    val Sync = line("Sync", "M4 9A8 8 0 0 1 18 5L21 8M21 3V8H16M20 15A8 8 0 0 1 6 19L3 16M3 21V16H8")
    val External = line("External", "M14 3H21V10M21 3L10 14M9 3H3V21H21V15")
    val AutoTag = line("AutoTag", "M3 3H12L22 13L13 22L3 12ZM7 7H7.01M11 12L14 15L18 11")

    // The launcher mark (design/reelvault-icon.svg) mapped onto the 24-unit icon grid.
    val Mark = ImageVector.Builder("ReelVault", 24.dp, 24.dp, 24f, 24f)
        .addPath(
            pathData = addPathNodes(
                "M9.6,2H18.8A1.6,1.6 0 0 1 20.4,3.6V22L14.2,17.8L8,22V3.6A1.6,1.6 0 0 1 9.6,2Z " +
                    "M12,7.2V14L17.6,10.6Z " +
                    "M4.8,3.6A1.2,1.2 0 0 1 6,4.8V19.2A1.2,1.2 0 0 1 3.6,19.2V4.8A1.2,1.2 0 0 1 4.8,3.6Z",
            ),
            fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd,
        ).build()
}
