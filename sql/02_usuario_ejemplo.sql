-- EDITAR la contraseña antes de ejecutar con el administrador de MySQL.
-- Esta cuenta es de conexión a MySQL; no es la cuenta del inicio de sesión de Swing.
CREATE USER 'inventario_app'@'localhost' IDENTIFIED BY 'CAMBIAR_POR_UNA_CLAVE_PROPIA';
GRANT SELECT, INSERT, UPDATE, DELETE ON inventario_ues.productos TO 'inventario_app'@'localhost';
GRANT SELECT, INSERT ON inventario_ues.usuarios TO 'inventario_app'@'localhost';
-- Para otro equipo, configurar el host, permisos de red y certificados TLS apropiados.

