# GlobalAir: radar aéreo en directo para Android

Un mapa en tiempo real con **todo lo que vuela ahora mismo**: aviones comerciales, avionetas, reactores, helicópteros, planeadores, globos y dirigibles, drones, paracaidistas, vehículos espaciales y aeronaves militares.

## 1. Cómo funciona

```
 Receptores ADS-B/MLAT voluntarios (miles, en todo el mundo)
                 │
                 ▼
   adsb.lol  ◄──fuente de reserva──►  adsb.fi            (API REST, sin clave)
                 │  GET .../lat/{lat}/lon/{lon}/dist/{nm}   cada 5 s
                 ▼
 ┌──────────────────────── App Android ────────────────────────┐
 │ AdsbApi (OkHttp + kotlinx.serialization)                    │
 │    └─ clasifica cada aeronave → VehicleType                 │
 │ RadarViewModel                                              │
 │    ├─ bucle de consulta: zona visible del mapa              │
 │    └─ bucle de fotogramas (cada 0,5 s): navegación a estima │
 │         (posición + velocidad + rumbo) → movimiento fluido  │
 │ AircraftMap (MapLibre, GPU)                                 │
 │    └─ 1 GeoJSON + 1 SymbolLayer: iconos rotados por rumbo,  │
 │       teñidos por tipo, etiquetas a partir de zoom 8        │
 │ RadarScreen (Jetpack Compose): búsqueda, filtros, ficha     │
 └─────────────────────────────────────────────────────────────┘
```

**Por qué parece "al instante":** los datos llegan cada 5 s, pero cada medio segundo la app recalcula dónde está cada aeronave con su velocidad y rumbo (`Aircraft.extrapolatedTo`). Así los iconos avanzan de forma continua en lugar de dar saltos. La extrapolación tiene un tope de 30 s para no inventar trayectorias si se pierde la señal.

## 2. Tipos de vehículo

Se detectan a partir de la categoría de emisor ADS-B (DO-260B), con la bandera militar de la base de datos como prioridad y pistas del modelo de aeronave como respaldo:

| Tipo | Categoría ADS-B | Color |
|---|---|---|
| Comercial | A3, A4, A5 | azul cielo |
| Avioneta / ultraligero | A1, A2, B4 | verde |
| Alta performance | A6 | rosa |
| Helicóptero | A7 | amarillo |
| Planeador | B1 | violeta |
| Globo / dirigible | B2 | naranja |
| Paracaidista | B3 | magenta |
| Dron / UAV | B6 | turquesa |
| Vehículo espacial | B7 | blanco |
| Militar | `dbFlags & 1` | rojo |
| Emergencia (squawk 7500/7600/7700) | cualquiera | rojo intenso |

## 3. Pantallas y funciones

- **Mapa a pantalla completa** con un estilo oscuro tipo radar (OpenFreeMap, sin clave).
- **Indicador EN DIRECTO** con el número de aeronaves en vuelo. Se vuelve ámbar si pasan más de 15 s sin datos.
- **Chips de filtro** por tipo, cada uno con su contador.
- **Búsqueda** por indicativo, matrícula, modelo, código ICAO u operador.
- **Ficha de la aeronave** al tocarla: altitud (FL/ft/m), velocidad (kt y km/h), rumbo, velocidad vertical, matrícula, operador, squawk y aviso de emergencia.
- **Modo seguir:** la cámara acompaña a la aeronave seleccionada.
- **Mi ubicación**, para ver lo que vuela sobre ti.
- **Menú de capas:** mostrar u ocultar aeronaves en tierra y elegir la fuente de datos.

## 4. Estructura

```
app/src/main/java/com/globalair/app/
├── MainActivity.kt            Tema oscuro + edge-to-edge
├── data/
│   ├── Aircraft.kt            Modelo, VehicleType, navegación a estima
│   └── AdsbApi.kt             Cliente readsb v2 + clasificación
└── ui/
    ├── RadarViewModel.kt      Consulta, filtrado y fotogramas
    ├── AircraftMap.kt         MapLibre + capa de símbolos GeoJSON
    └── RadarScreen.kt         UI Compose (barra, filtros, ficha)
app/src/main/res/drawable/     Iconos vectoriales por tipo (SDF, teñibles)
```

## 5. Compilar

Necesitas Android Studio (Ladybug o posterior) o el SDK de Android 35 con JDK 17+.

```bash
./gradlew assembleDebug      # APK en app/build/outputs/apk/debug/
./gradlew test               # tests unitarios (clasificación y extrapolación)
```

## 6. Límites y licencias

- **adsb.lol** publica sus datos con licencia **ODbL** (hay que atribuirlo, y la app lo hace en pantalla). **adsb.fi** es gratuito para uso personal y no comercial. Ambos piden un máximo de unas 1 petición/s, y la app hace 1 cada 5 s como máximo, con espera extra si el servidor responde 429.
- El radio máximo por consulta es de 250 NM. Si alejas el zoom más allá de eso, solo se cargan las aeronaves en un radio de 250 NM alrededor del centro del mapa.
- **Drones:** solo aparecen los que emiten ADS-B (B6). La mayoría de los drones de consumo usan Remote ID por Bluetooth/Wi-Fi, que no llega a estas redes.
- **Satélites y la ISS** no transmiten ADS-B. Están en la hoja de ruta.
- Para uso comercial hacen falta datos con licencia (por ejemplo, la API de pago de ADS-B Exchange, OpenSky con acuerdo comercial o FlightAware Firehose).

## 7. Hoja de ruta

1. **Satélites/ISS:** propagar TLE de CelesTrak con SGP4 y añadirlos como otra capa.
2. **Remote ID local:** escuchar por Bluetooth LE / Wi-Fi NaN los drones cercanos (ASTM F3411).
3. **Estela de trayectoria:** historial de posiciones como LineLayer con degradado de altitud.
4. **Vista AR:** apuntar la cámara al cielo y superponer las etiquetas usando la brújula y el giroscopio.
5. **Alertas:** aviso cuando pase un militar, un globo o una emergencia cerca de ti (WorkManager + notificaciones).
6. **Rutas y fotos:** origen y destino desde adsbdb.com, fotos desde planespotters.net.
7. **WebSocket propio:** un backend que agregue y empuje deltas para ahorrar batería y datos.
