# Entrega mediante GitHub

No se creó ni publicó un repositorio remoto: no se proporcionó cuenta/repositorio de destino.

1. Crear un repositorio privado vacío en la cuenta del equipo.
2. Desde la carpeta Inventario:

```powershell
git init
git add .
git commit -m "Implementar inventario Swing con persistencia MySQL"
git branch -M main
git remote add origin https://github.com/USUARIO/REPOSITORIO.git
git push -u origin main
```

Sustituir la URL por la real. Antes de subir, confirmar que ningún archivo contiene credenciales reales. El ejemplo de usuario SQL debe conservar su marcador de contraseña. La configuración real va en variables de entorno.

## Participación del equipo

Cada integrante debe configurar su nombre/correo, trabajar en una rama y realizar commits de sus aportes reales. Usar pull requests para revisión. No fabricar commits ni atribuir autoría a otra persona.

Propuesta de reparto para revisión y mejora:
- Dominio y validaciones.
- Interfaz Swing y pruebas de uso.
- Persistencia SQL y pruebas de concurrencia.
- Documentación, trazabilidad y verificación de entrega.

En Actions se incluye un flujo de compilación y pruebas con MySQL 8.0.46. El resultado solo queda confirmado cuando el flujo termine correctamente en GitHub. La contraseña del servicio CI es ficticia, exclusiva de la base efímera de pruebas; no es una credencial del sistema real.

