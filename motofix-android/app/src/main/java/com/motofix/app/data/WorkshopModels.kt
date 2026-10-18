package com.motofix.app.data

import kotlinx.serialization.Serializable

/** Herramientas de la caja del taller virtual. */
@Serializable
enum class Tool(val label: String) {
    SCANNER("Escáner OBD"),
    WRENCH_8("Llave 8mm"),
    WRENCH_12("Llave 12mm"),
    WRENCH_17("Llave 17mm"),
    WRENCH_24("Llave 24mm"),
    SCREWDRIVER("Destornillador"),
    MULTIMETER("Multímetro digital"),
    SPARE_FUSE("Fusible de repuesto"),
    SPARK_WRENCH("Llave de bujías"),
    FEELER_GAUGE("Galgas"),
    NEW_SPARK_PLUG("Bujía nueva"),
    FILTER_WRENCH("Llave de filtro"),
    NEW_OIL("Aceite 10W-40"),
    ALLEN_KEY("Llave Allen"),
    BRAKE_PADS("Pastillas nuevas"),
    BRAKE_FLUID("Líquido de frenos"),
    CHAIN_LUBE("Lubricante de cadena"),
    TORQUE_WRENCH("Torquímetro"),
    PLIERS("Alicate"),
}

/**
 * Componentes de la moto que se pueden tocar en el diagrama.
 * (x, y) es la posición en el dibujo, medida en fracciones del ANCHO (el alto es 0.72 del ancho).
 */
@Serializable
enum class Part(val label: String, val shortLabel: String, val x: Float, val y: Float) {
    ECU("Unidad CDI / ECU", "CDI/ECU", 0.20f, 0.36f),
    BATTERY("Batería", "Batería", 0.30f, 0.44f),
    FUSE_BOX("Caja de fusibles", "Fusibles", 0.41f, 0.38f),
    FAIRING("Carenado lateral", "Carenado", 0.70f, 0.40f),
    HEADLIGHT("Farola", "Farola", 0.88f, 0.30f),
    SPARK_PLUG("Bujía", "Bujía", 0.54f, 0.44f),
    ENGINE_COVER("Tapa del motor", "Motor", 0.55f, 0.58f),
    OIL_FILLER("Tapa de llenado de aceite", "Llenado", 0.42f, 0.54f),
    OIL_DRAIN("Tapón de cárter", "Cárter", 0.46f, 0.68f),
    OIL_FILTER("Filtro de aceite", "Filtro", 0.63f, 0.66f),
    BRAKE_CALIPER("Mordaza de freno", "Mordaza", 0.76f, 0.60f),
    BRAKE_BLEEDER("Purgador de freno", "Purgador", 0.88f, 0.50f),
    CHAIN("Cadena", "Cadena", 0.32f, 0.66f),
    REAR_AXLE("Eje trasero", "Eje trasero", 0.19f, 0.58f),
    CHAIN_ADJUSTER("Tensor de cadena", "Tensor", 0.08f, 0.50f),
}

data class RepairStep(
    /** Lo que se le pide al jugador (sin decir la herramienta). */
    val instruction: String,
    val tool: Tool,
    val part: Part,
    /** Cómo queda escrito en el certificado cuando se completa. */
    val doneTitle: String,
    val doneNote: String,
)

data class RepairScenario(
    val id: String,
    val number: Int,
    val title: String,
    val category: String,
    val faultCode: String,
    val briefing: String,
    val virtualMoto: String,
    val timeLimitSec: Int,
    val steps: List<RepairStep>,
    /** Componentes visibles en el diagrama (los de los pasos + algunos de distracción). */
    val parts: List<Part>,
    /** Herramientas en la caja (las correctas + algunas de distracción). */
    val toolbox: List<Tool>,
    val rewardXp: Int,
    val rewardCoins: Int,
    val recommendedLevel: Int,
)

@Serializable
data class LevelProgress(
    val completions: Int = 0,
    val bestStars: Int = 0,
    val bestTimeSec: Int? = null,
)

@Serializable
data class CertificateStep(val title: String, val tool: String, val note: String)

/** Certificado técnico de una reparación completada (RF10). */
@Serializable
data class Certificate(
    val id: String,
    val scenarioId: String,
    val title: String,
    val category: String,
    val virtualMoto: String,
    val faultCode: String,
    val mechanicName: String,
    val dateEpochDay: Long,
    val stars: Int,
    val timeUsedSec: Int,
    val mistakes: Int,
    val steps: List<CertificateStep>,
)

/** Lo que se envía al repositorio cuando el jugador termina una reparación. */
data class RepairOutcome(
    val scenario: RepairScenario,
    val stars: Int,
    val timeUsedSec: Int,
    val mistakes: Int,
)

data class RepairReward(
    val reward: RewardResult,
    val certificateId: String,
    val firstCompletion: Boolean,
    val improved: Boolean,
)
