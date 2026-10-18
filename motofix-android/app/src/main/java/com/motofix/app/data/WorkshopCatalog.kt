package com.motofix.app.data

/** Niveles del taller virtual (RF09). Más adelante el administrador podrá crearlos desde el servidor. */
object WorkshopCatalog {

    val scenarios: List<RepairScenario> = listOf(
        RepairScenario(
            id = "oil_change",
            number = 1,
            title = "Cambio de aceite",
            category = "Mantenimiento preventivo",
            faultCode = "ACEITE DEGRADADO",
            briefing = "El aceite de esta moto superó su vida útil y perdió viscosidad. Drena el aceite viejo, " +
                "cambia el filtro y llena con aceite nuevo.",
            virtualMoto = "Honda CB 190R",
            timeLimitSec = 150,
            steps = listOf(
                RepairStep(
                    "Haz un escaneo inicial del motor",
                    Tool.SCANNER, Part.ENGINE_COVER,
                    "Escaneo inicial con escáner OBD", "Aceite con 3.400 km de uso. Viscosidad fuera de rango.",
                ),
                RepairStep(
                    "Drena el aceite viejo del cárter",
                    Tool.WRENCH_17, Part.OIL_DRAIN,
                    "Drenado del cárter", "Tapón de cárter retirado; aceite usado recolectado.",
                ),
                RepairStep(
                    "Retira el filtro de aceite usado",
                    Tool.FILTER_WRENCH, Part.OIL_FILTER,
                    "Cambio de filtro", "Filtro usado reemplazado por uno nuevo.",
                ),
                RepairStep(
                    "Llena el motor con aceite nuevo",
                    Tool.NEW_OIL, Part.OIL_FILLER,
                    "Llenado de aceite", "1,1 L de aceite 10W-40 hasta la marca máxima.",
                ),
            ),
            parts = listOf(Part.ENGINE_COVER, Part.OIL_DRAIN, Part.OIL_FILTER, Part.OIL_FILLER, Part.BATTERY, Part.CHAIN, Part.SPARK_PLUG),
            toolbox = listOf(Tool.SCANNER, Tool.WRENCH_17, Tool.FILTER_WRENCH, Tool.NEW_OIL, Tool.SCREWDRIVER, Tool.CHAIN_LUBE),
            rewardXp = 80,
            rewardCoins = 100,
            recommendedLevel = 1,
        ),
        RepairScenario(
            id = "spark_plug",
            number = 2,
            title = "Bujía desgastada",
            category = "Encendido",
            faultCode = "FALLO DE ENCENDIDO: CILINDRO 1",
            briefing = "El motor tiembla en ralentí y cuesta encenderlo. El escáner sugiere un fallo de encendido. " +
                "Revisa la bujía y reemplázala si es necesario.",
            virtualMoto = "Yamaha FZ 2.0",
            timeLimitSec = 140,
            steps = listOf(
                RepairStep(
                    "Confirma el fallo con un escaneo",
                    Tool.SCANNER, Part.ECU,
                    "Escaneo de la unidad CDI", "Código de fallo de encendido intermitente en cilindro 1.",
                ),
                RepairStep(
                    "Desmonta la bujía del cilindro",
                    Tool.SPARK_WRENCH, Part.SPARK_PLUG,
                    "Extracción de bujía", "Electrodo con carbonilla y desgaste excesivo.",
                ),
                RepairStep(
                    "Calibra la separación del electrodo de la bujía nueva",
                    Tool.FEELER_GAUGE, Part.SPARK_PLUG,
                    "Calibración con galgas", "Separación ajustada a 0,8 mm.",
                ),
                RepairStep(
                    "Instala la bujía nueva",
                    Tool.NEW_SPARK_PLUG, Part.SPARK_PLUG,
                    "Instalación de bujía nueva", "Bujía apretada a mano + 1/4 de vuelta.",
                ),
            ),
            parts = listOf(Part.ECU, Part.SPARK_PLUG, Part.BATTERY, Part.FUSE_BOX, Part.ENGINE_COVER, Part.OIL_FILLER),
            toolbox = listOf(Tool.SCANNER, Tool.SPARK_WRENCH, Tool.FEELER_GAUGE, Tool.NEW_SPARK_PLUG, Tool.WRENCH_12, Tool.MULTIMETER),
            rewardXp = 100,
            rewardCoins = 150,
            recommendedLevel = 1,
        ),
        RepairScenario(
            id = "brakes",
            number = 3,
            title = "Frenos delanteros",
            category = "Seguridad",
            faultCode = "DESGASTE DE PASTILLAS: 90%",
            briefing = "La palanca de freno se siente esponjosa y las pastillas chillan al frenar. Cambia las pastillas " +
                "delanteras y purga el sistema.",
            virtualMoto = "Suzuki Gixxer 150",
            timeLimitSec = 160,
            steps = listOf(
                RepairStep(
                    "Afloja los pernos de la mordaza",
                    Tool.ALLEN_KEY, Part.BRAKE_CALIPER,
                    "Desmontaje de mordaza", "Pernos de la mordaza retirados con llave Allen.",
                ),
                RepairStep(
                    "Coloca las pastillas nuevas",
                    Tool.BRAKE_PADS, Part.BRAKE_CALIPER,
                    "Cambio de pastillas", "Pastillas con 1 mm de material reemplazadas por nuevas.",
                ),
                RepairStep(
                    "Abre el purgador para sacar el aire",
                    Tool.WRENCH_8, Part.BRAKE_BLEEDER,
                    "Apertura del purgador", "Purgador abierto 1/4 de vuelta.",
                ),
                RepairStep(
                    "Rellena con líquido de frenos nuevo",
                    Tool.BRAKE_FLUID, Part.BRAKE_BLEEDER,
                    "Purga del sistema", "Líquido DOT 4 nuevo, sin burbujas en la línea.",
                ),
                RepairStep(
                    "Aprieta la mordaza al torque correcto",
                    Tool.TORQUE_WRENCH, Part.BRAKE_CALIPER,
                    "Torque final", "Mordaza apretada a 25 N·m. Freno firme.",
                ),
            ),
            parts = listOf(Part.BRAKE_CALIPER, Part.BRAKE_BLEEDER, Part.HEADLIGHT, Part.FAIRING, Part.OIL_FILTER, Part.REAR_AXLE),
            toolbox = listOf(Tool.ALLEN_KEY, Tool.BRAKE_PADS, Tool.WRENCH_8, Tool.BRAKE_FLUID, Tool.TORQUE_WRENCH, Tool.PLIERS, Tool.NEW_OIL),
            rewardXp = 120,
            rewardCoins = 200,
            recommendedLevel = 2,
        ),
        RepairScenario(
            id = "electrical_a12",
            number = 4,
            title = "Reparación eléctrica",
            category = "Sistema eléctrico",
            faultCode = "FALLO DETECTADO: CIRCUITO A12",
            briefing = "Las luces y el tablero se apagaron de repente. El escáner marca el circuito A12. " +
                "Encuentra el componente quemado y restablece el sistema.",
            virtualMoto = "Yamaha MT-07",
            timeLimitSec = 165,
            steps = listOf(
                RepairStep(
                    "Escanea el sistema electrónico",
                    Tool.SCANNER, Part.ECU,
                    "Escaneo inicial con escáner OBD virtual", "Código A12: caída de tensión en el circuito de luces.",
                ),
                RepairStep(
                    "Retira el carenado izquierdo",
                    Tool.SCREWDRIVER, Part.FAIRING,
                    "Desmontaje de carenado izquierdo", "Destornillador de estrella, ajuste de presión manual.",
                ),
                RepairStep(
                    "Abre la tapa de acceso a la caja de fusibles",
                    Tool.WRENCH_12, Part.FUSE_BOX,
                    "Apertura de tapa de acceso", "Tornillo de 12 mm retirado, torque verificado.",
                ),
                RepairStep(
                    "Mide la continuidad para encontrar el fusible quemado",
                    Tool.MULTIMETER, Part.FUSE_BOX,
                    "Diagnóstico de continuidad", "Fusible de 15 A del circuito de luces sin continuidad.",
                ),
                RepairStep(
                    "Reemplaza el componente crítico",
                    Tool.SPARE_FUSE, Part.FUSE_BOX,
                    "Sustitución de componente crítico", "Fusible de alta intensidad quemado por sobrecarga reemplazado.",
                ),
                RepairStep(
                    "Prueba que las luces funcionen",
                    Tool.MULTIMETER, Part.HEADLIGHT,
                    "Prueba de banco y calibración", "Sistema eléctrico restablecido: luces y tablero operativos.",
                ),
            ),
            parts = listOf(Part.ECU, Part.FAIRING, Part.FUSE_BOX, Part.HEADLIGHT, Part.BATTERY, Part.SPARK_PLUG, Part.ENGINE_COVER),
            toolbox = listOf(Tool.SCANNER, Tool.SCREWDRIVER, Tool.WRENCH_12, Tool.MULTIMETER, Tool.SPARE_FUSE, Tool.PLIERS, Tool.SPARK_WRENCH),
            rewardXp = 150,
            rewardCoins = 450,
            recommendedLevel = 3,
        ),
        RepairScenario(
            id = "chain",
            number = 5,
            title = "Ajuste de cadena",
            category = "Transmisión",
            faultCode = "CADENA DESTENSADA: 45 MM",
            briefing = "La cadena golpea el basculante y hace ruido en los cambios. Ajusta la tensión, lubrícala y " +
                "deja el eje trasero con el torque correcto.",
            virtualMoto = "KTM Duke 390",
            timeLimitSec = 150,
            steps = listOf(
                RepairStep(
                    "Afloja la tuerca del eje trasero",
                    Tool.WRENCH_24, Part.REAR_AXLE,
                    "Liberación del eje trasero", "Tuerca del eje aflojada con llave de 24 mm.",
                ),
                RepairStep(
                    "Ajusta los tensores de la cadena",
                    Tool.WRENCH_12, Part.CHAIN_ADJUSTER,
                    "Ajuste de tensores", "Holgura llevada a 30 mm, rueda alineada en ambos lados.",
                ),
                RepairStep(
                    "Aprieta el eje trasero al torque correcto",
                    Tool.TORQUE_WRENCH, Part.REAR_AXLE,
                    "Torque del eje", "Eje trasero apretado a 100 N·m.",
                ),
                RepairStep(
                    "Lubrica la cadena",
                    Tool.CHAIN_LUBE, Part.CHAIN,
                    "Lubricación de cadena", "Lubricante aplicado en la cara interna de los eslabones.",
                ),
            ),
            parts = listOf(Part.REAR_AXLE, Part.CHAIN_ADJUSTER, Part.CHAIN, Part.OIL_DRAIN, Part.BATTERY, Part.BRAKE_CALIPER),
            toolbox = listOf(Tool.WRENCH_24, Tool.WRENCH_12, Tool.TORQUE_WRENCH, Tool.CHAIN_LUBE, Tool.WRENCH_17, Tool.NEW_OIL),
            rewardXp = 130,
            rewardCoins = 300,
            recommendedLevel = 3,
        ),
    )

    fun find(id: String): RepairScenario? = scenarios.firstOrNull { it.id == id }

    fun next(id: String): RepairScenario? {
        val index = scenarios.indexOfFirst { it.id == id }
        return scenarios.getOrNull(index + 1)
    }
}
