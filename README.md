# Sistema de inventario · Clave 2 / RF-02

Aplicación de escritorio Java Swing con persistencia en **MySQL Community Server 8.0.46**, solicitada en sustitución de MongoDB. La investigación previa orienta el dominio y el manejo de excepciones. Esta entrega aplica el nuevo enunciado de inventario.

## Funciones

- Registro, detalle, modificación y eliminación confirmada de productos.
- Código único normalizado a mayúsculas e inmutable; nombre, categoría y precio obligatorios. Descripción opcional.
- Tabla con ordenación por encabezados; búsqueda por código exacto, nombre parcial y categoría; filtro por categoría.
- Botones **Entrada +** y **Salida −**, con unidades enteras positivas y sin permitir stock negativo.
- Ajuste directo de cantidad desde Modificar, previa confirmación si cambia.
- Reportes general, sin existencias y existencias bajas con límite entero no negativo.
- Valor mostrado y valor total calculados con BigDecimal a partir de productos leídos de MySQL.
- Inicio de sesión, creación inicial de administrador y contraseñas PBKDF2 con sal aleatoria.
- Guardado inmediato en cada escritura, mensajes y actualización de tabla.
- Control de concurrencia por versión y actualización condicional: una sesión no sobrescribe silenciosamente los cambios de otra.

## Requisitos

1. MySQL Community Server **8.0.46**, iniciado.
2. JDK 17 o 21 recomendado. El código fuente se compila también con Java 8; no se recomienda usar una instalación antigua sin actualizaciones.
3. Maven 3.9 o NetBeans con Maven y conexión a Maven Central para la primera compilación.
4. MySQL Connector/J 9.6.0 se descarga automáticamente mediante pom.xml. La versión del conector no debe coincidir con la del servidor.

## Preparar la base

En MySQL Workbench o mysql, conectado como administrador:

1. Ejecutar `sql/01_schema.sql`.
2. Editar la contraseña del ejemplo `sql/02_usuario_ejemplo.sql` y ejecutarlo.
3. No subir el archivo con una contraseña real a GitHub; ejecutar la versión editada fuera del repositorio.

La cuenta de MySQL conecta la aplicación con la base. La cuenta que se crea al abrir Swing sirve para iniciar sesión en la aplicación; son cuentas distintas.

## Ejecutar en PowerShell

Desde la carpeta que contiene pom.xml:

```powershell
$env:MYSQL_USER = 'inventario_app'
$env:MYSQL_PASSWORD = 'TU_CLAVE_DE_MYSQL'
$env:MYSQL_URL = 'jdbc:mysql://localhost:3306/inventario_ues?sslMode=REQUIRED&connectTimeout=5000&socketTimeout=10000&connectionTimeZone=UTC'
mvn clean verify
mvn exec:java
```

La URL predeterminada usa localhost, puerto 3306 y TLS obligatorio. MySQL debe tener TLS habilitado. Para servidores remotos utilice sslMode=VERIFY_IDENTITY con el certificado de confianza correspondiente. Las credenciales se leen del entorno, nunca del código.

En NetBeans: abrir la carpeta como proyecto Maven. Iniciar NetBeans desde una consola con esas variables configuradas, seleccionar el JDK y ejecutar la clase `sv.edu.ues.inventario.App`.

Para crear el JAR con las dependencias:

```powershell
mvn clean package
java -jar target/inventario-1.0.0.jar
```

El primer inicio permite crear un administrador (contraseña de al menos diez caracteres). Hacer esta configuración antes de compartir el acceso a la aplicación. El alcance incluye una cuenta administradora; no incluye gestión de roles ni recuperación de contraseñas.

## Demostración

1. Crear e iniciar sesión.
2. Registrar A001, Arroz, Alimentos, precio 1.25 y cantidad 10.
3. Intentar registrar a001: debe rechazarse como duplicado.
4. Registrar entrada de 5: queda 15.
5. Registrar salida de 4: queda 11.
6. Intentar salida de 12: debe rechazarse.
7. Modificar nombre/precio, consultar detalle y ordenar la tabla.
8. Aplicar búsqueda y los tres reportes; cambiar el límite a 11.
9. Cerrar y abrir: el producto debe conservar sus datos.
10. Cancelar una eliminación y luego confirmarla.

## Verificación

`mvn verify` compila el proyecto y ejecuta DomainTest mediante el plugin exec. Para las pruebas reales de MySQL, seguir docs/PRUEBAS.md. GitHub Actions tiene un servicio MySQL 8.0.46 y ejecuta ambas suites.

En el entorno de preparación se compiló todo el código Java con javac y se ejecutaron 26 comprobaciones de dominio y contraseñas. **No se ejecutaron las pruebas de MySQL ni el empaquetado Maven**: el entorno no tiene MySQL/Maven y no puede descargar dependencias. La inclusión del flujo de CI no implica que ya se haya ejecutado.

## Límites y decisiones

- La eliminación requiere confirmación y admite productos con existencias: el enunciado no define una restricción adicional. El diálogo muestra la cantidad que se eliminará.
- Una consulta vacía se muestra como tal; un fallo de lectura no se presenta como un inventario vacío. La tabla anterior puede permanecer visible con el mensaje de error.
- Los reportes muestran una instantánea de la última lectura. Usar Consultar / actualizar para leer cambios de otras sesiones.
- Los filtros se aplican en memoria sobre datos persistidos. Apropiado para una pequeña empresa; para grandes volúmenes se requerirían paginación y consultas filtradas en SQL.
- No se incluyen facturación, contabilidad, historial permanente de movimientos ni exportaciones.
- Usar un esquema nuevo creado por el script. CREATE TABLE IF NOT EXISTS no migra una tabla antigua con otra estructura.
- La guía original pide MongoDB; MySQL corresponde al cambio solicitado por el usuario y debe concordar con lo aceptado por la cátedra.

Ver docs/ARQUITECTURA.md, docs/TRAZABILIDAD.md, docs/PRUEBAS.md y docs/GITHUB.md.

