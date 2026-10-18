package com.motofix.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.ElectricScooter
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Moped
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.SportsMotorsports
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.TireRepair
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.InvertColors
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import com.motofix.app.data.Tool
import com.motofix.app.data.BikeType
import com.motofix.app.data.MaintenanceType

val BikeType.icon: ImageVector
    get() = when (this) {
        BikeType.SCOOTER -> Icons.Outlined.ElectricScooter
        BikeType.NAKED -> Icons.Outlined.TwoWheeler
        BikeType.SPORTS -> Icons.Outlined.SportsMotorsports
        BikeType.TRAIL -> Icons.Outlined.Terrain
        BikeType.CLASSIC -> Icons.Outlined.Moped
    }

val MaintenanceType.icon: ImageVector
    get() = when (this) {
        MaintenanceType.OIL_CHANGE -> Icons.Outlined.WaterDrop
        MaintenanceType.BRAKES -> Icons.Outlined.RadioButtonChecked
        MaintenanceType.CHAIN -> Icons.Outlined.Link
        MaintenanceType.TIRES -> Icons.Outlined.TireRepair
        MaintenanceType.ELECTRICAL -> Icons.Outlined.ElectricBolt
        MaintenanceType.GENERAL -> Icons.Outlined.Build
        MaintenanceType.OTHER -> Icons.Outlined.MoreHoriz
    }

val Tool.icon: ImageVector
    get() = when (this) {
        Tool.SCANNER -> Icons.Outlined.Sensors
        Tool.WRENCH_8, Tool.WRENCH_12, Tool.WRENCH_17, Tool.WRENCH_24 -> Icons.Outlined.Build
        Tool.SCREWDRIVER -> Icons.Outlined.Hardware
        Tool.MULTIMETER -> Icons.Outlined.Speed
        Tool.SPARE_FUSE -> Icons.Outlined.ElectricalServices
        Tool.SPARK_WRENCH -> Icons.Outlined.Handyman
        Tool.FEELER_GAUGE -> Icons.Outlined.Straighten
        Tool.NEW_SPARK_PLUG -> Icons.Outlined.FlashOn
        Tool.FILTER_WRENCH -> Icons.Outlined.FilterAlt
        Tool.NEW_OIL -> Icons.Outlined.WaterDrop
        Tool.ALLEN_KEY -> Icons.Outlined.Construction
        Tool.BRAKE_PADS -> Icons.Outlined.Album
        Tool.BRAKE_FLUID -> Icons.Outlined.Opacity
        Tool.CHAIN_LUBE -> Icons.Outlined.InvertColors
        Tool.TORQUE_WRENCH -> Icons.Outlined.Tune
        Tool.PLIERS -> Icons.Outlined.ContentCut
    }
