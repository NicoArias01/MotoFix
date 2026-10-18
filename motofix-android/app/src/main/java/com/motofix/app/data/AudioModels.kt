package com.motofix.app.data

import kotlinx.serialization.Serializable

@Serializable
enum class Severity(val label: String) {
    OK("Sin falla"),
    LOW("Leve"),
    MEDIUM("Moderada"),
    CRITICAL("Crítica"),
}

/** Fallas que puede sugerir el análisis acústico. */
@Serializable
enum class DiagnosisType(
    val title: String,
    val severity: Severity,
    val explanation: String,
    val recommendation: String,
    val estimatedCost: String,
    /** Nivel del taller para practicar la reparación, si existe. */
    val practiceScenarioId: String?,
    /** Tipo de mantenimiento a registrar si se repara. */
    val maintenanceType: MaintenanceType,
) {
    HEALTHY(
        title = "Motor en buen estado",
        severity = Severity.OK,
        explanation = "El sonido es estable y regular, sin golpes ni chasquidos fuera de lo normal.",
        recommendation = "Sigue con el mantenimiento preventivo: aceite cada 3.000 km y revisión general cada 6 meses.",
        estimatedCost = "Sin costo",
        practiceScenarioId = "oil_change",
        maintenanceType = MaintenanceType.GENERAL,
    ),
    VALVE_CLEARANCE(
        title = "Holgura en válvulas / balancines",
        severity = Severity.MEDIUM,
        explanation = "Se detectó un chasquido metálico rápido y agudo, típico de válvulas o balancines con " +
            "holgura excesiva en la culata.",
        recommendation = "Calibra las válvulas y revisa la lubricación del tren de válvulas superior.",
        estimatedCost = "$80.000 – $150.000",
        practiceScenarioId = null,
        maintenanceType = MaintenanceType.GENERAL,
    ),
    IGNITION_MISFIRE(
        title = "Fallo de encendido",
        severity = Severity.MEDIUM,
        explanation = "El ritmo del motor es irregular: algunas explosiones son más débiles o faltan. Suele " +
            "deberse a una bujía gastada, un cable de bujía en mal estado o un inyector sucio.",
        recommendation = "Revisa y cambia la bujía. Si persiste, revisa el capuchón, la bobina y el inyector.",
        estimatedCost = "$25.000 – $90.000",
        practiceScenarioId = "spark_plug",
        maintenanceType = MaintenanceType.ELECTRICAL,
    ),
    ENGINE_KNOCK(
        title = "Golpeteo interno (biela / cascabeleo)",
        severity = Severity.CRITICAL,
        explanation = "Hay golpes graves y fuertes que se repiten con el giro del motor. Puede ser desgaste en " +
            "la biela, cojinetes o detonación por mala gasolina.",
        recommendation = "No fuerces el motor. Revisa el nivel y estado del aceite y llévala a un mecánico pronto.",
        estimatedCost = "$300.000 – $1.200.000",
        practiceScenarioId = "oil_change",
        maintenanceType = MaintenanceType.GENERAL,
    ),
    EXHAUST_LEAK(
        title = "Posible fuga en escape o admisión",
        severity = Severity.LOW,
        explanation = "Se escucha un siseo agudo constante, típico de una fuga de gases en el empaque del " +
            "escape o una toma de aire en la admisión.",
        recommendation = "Revisa el empaque del múltiple de escape y las abrazaderas de la admisión.",
        estimatedCost = "$30.000 – $120.000",
        practiceScenarioId = null,
        maintenanceType = MaintenanceType.OTHER,
    ),
    LOOSE_CHAIN(
        title = "Cadena de transmisión destensada",
        severity = Severity.LOW,
        explanation = "Hay un traqueteo metálico de frecuencia media y ritmo constante, compatible con una " +
            "cadena floja que golpea el basculante.",
        recommendation = "Ajusta la tensión de la cadena (25–35 mm de holgura) y lubrícala.",
        estimatedCost = "$15.000 – $60.000",
        practiceScenarioId = "chain",
        maintenanceType = MaintenanceType.CHAIN,
    ),
}

/** Diagnóstico guardado en el historial de Audio IA. */
@Serializable
data class AudioDiagnosisRecord(
    val id: String,
    val createdAtMillis: Long,
    val motorcycleId: String? = null,
    val motoName: String,
    val type: DiagnosisType,
    /** Qué tan seguro está el análisis, de 0 a 100. */
    val confidence: Int,
    val dominantHz: Int,
    val loudnessDb: Int,
    val irregularity: Int,
    val durationSec: Float,
    val isDemo: Boolean,
    /** Forma de onda reducida para dibujarla (valores 0..1). */
    val waveform: List<Float>,
    /** Energía por banda de frecuencia (graves → agudos), valores 0..1. */
    val spectrum: List<Float>,
)
