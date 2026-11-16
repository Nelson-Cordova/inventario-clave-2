# Correspondencia con Clave 2 y RF-02

| Requisito | Implementación / evidencia |
|---|---|
| RF-02 Registrar | Nuevo producto → InventoryService.register → INSERT |
| RF-02 Consultar y visualizar | Tabla, Detalle, Consultar / actualizar |
| RF-02 Modificar | Modificar; código inmutable, ajuste confirmado |
| RF-02 Eliminar | Selección y confirmación; DELETE condicionado por versión |
| RF-02 Actualizar vistas | Después de escritura confirmada se vuelve a leer MySQL |
| RF-02 Informar resultados | Diálogo de éxito o error y barra de estado |
| 1.1–1.6 Gestión | Formulario completo, búsqueda, tabla, edición, eliminación y detalle |
| 2.1–2.5 Existencias | Cantidad visible, Entrada +, Salida −, validación y UPDATE atómico |
| 3.1 Código | Coincidencia exacta, sin distinguir mayúsculas |
| 3.2 Nombre | Coincidencia parcial |
| 3.3 Categoría | Selector y búsqueda textual |
| 3.4 Inventario | Tabla con cantidades, precios y valor |
| 3.5 Ordenación | Encabezados por código, nombre, precio y cantidad |
| 3.6 Existencias bajas | Cantidad menor o igual al límite ingresado |
| 4.1 General | Reporte Inventario general |
| 4.2 Sin existencias | Reporte cantidad cero |
| 4.3 Existencias bajas | Límite entero no negativo |
| 4.4 Valor | Suma BigDecimal de precio × cantidad desde datos leídos |
| 5.1 Guardado | Cada INSERT, UPDATE o DELETE se confirma en MySQL |
| 5.2 Recuperación | SELECT al iniciar sesión y actualizar |
| 5.3 Persistencia | Prueba JDBC de reapertura y protocolo manual de reinicio |
| 5.4 Resultados | Mensajes comprensibles y excepciones controladas |
| Login de generalidades | Cuenta inicial, verificación PBKDF2 y cierre de sesión |
| Tecnología de persistencia | MySQL 8.0.46 por cambio del usuario; difiere de MongoDB exigido originalmente |

## Situaciones de error

| Situación | Respuesta |
|---|---|
| Código duplicado | Clave primaria + código normalizado; mensaje de duplicado |
| Producto inexistente | Se rechaza actualización, movimiento o eliminación sin fila coincidente |
| Campos vacíos | Mensaje con nombre del campo |
| Precio inválido | Rechazo de texto, negativos, más de dos decimales y valores fuera de rango |
| Cantidad inválida | Rechazo de negativos, fracciones y desbordamiento |
| Movimiento cero | Se exige entero positivo |
| Salida superior a stock | Validación en servicio y predicado SQL |
| Producto agotado | Se muestra cero y se impiden salidas |
| Base vacía | Tabla vacía y totales cero |
| Error SQL/red | Mensaje de error sin borrar datos ni simular éxito |
| Registro inválido | Error de lectura controlado; no sobrescribir automáticamente |
| Cambios simultáneos | Comprobación de versión y solicitud de actualizar |

