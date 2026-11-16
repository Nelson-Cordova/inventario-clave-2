# Plan de pruebas y estado

## Ejecutado en el entorno de preparación

- Compilación completa del código fuente y pruebas mediante javac.
- DomainTest: 26 comprobaciones de validación, límites, cálculo monetario y contraseñas.
- No ejecutado: MySQLIntegrationTest, interfaz con conexión real, Maven package ni GitHub Actions.

## Ejecución automática

`mvn clean verify` ejecuta DomainTest. La prueba de MySQL requiere una base exclusiva llamada inventario_test: copiar el script de esquema y sustituir inventario_ues por inventario_test antes de ejecutarlo. No usar una base con datos reales.

```powershell
$env:MYSQL_TEST_MODE = 'true'
$env:MYSQL_URL = 'jdbc:mysql://localhost:3306/inventario_test?sslMode=REQUIRED&connectTimeout=5000&socketTimeout=10000'
mvn test-compile exec:java "-Dexec.mainClass=sv.edu.ues.inventario.MySqlIntegrationTest" "-Dexec.classpathScope=test"
```

Usar credenciales autorizadas para esa base. La prueba genera un código aleatorio y elimina solo ese producto al terminar. Verifica registro, duplicado, entrada, salida, stock insuficiente, versión obsoleta, modificación, total exacto, reapertura de conexión, eliminación y producto inexistente.

## Pruebas manuales pendientes

| Caso | Pasos | Resultado esperado |
|---|---|---|
| Login | Crear cuenta, cerrar sesión, clave incorrecta/correcta | Rechazo y acceso según credenciales |
| Cancelación | Abrir alta/modificación y cancelar | Sin cambios |
| Eliminación | Cancelar y luego confirmar | Solo elimina al confirmar |
| Tabla ordenada | Ordenar por precio, seleccionar, modificar | Modifica el producto elegido |
| Filtros | Código, nombre parcial, categoría | Coincidencias correctas |
| Límite | 0, 5, negativo y texto | Incluye <= límite, rechaza inválidos |
| Reportes | Precio 1.25 × 10 y precio 2.50 × 4 | Total 22.50 |
| Reinicio real | Guardar, cerrar proceso y volver a abrir | Datos conservados |
| Concurrencia | Dos ventanas leen el mismo producto; ambas retiran | Una operación obsoleta se rechaza; no hay stock negativo |
| Servicio caído | Detener MySQL y consultar/guardar | Error visible, sin éxito falso |
| Esquema ausente | Base sin tablas | Indica ejecutar el esquema |
| Datos incompatibles | En una base descartable alterar un registro saltándose controles | Error controlado, sin sobrescritura |
| Base vacía | Abrir sin productos | Tabla vacía y valor cero |

No adjuntar capturas inventadas. Recoger evidencia de estos casos al ejecutar la aplicación con el servidor configurado.

