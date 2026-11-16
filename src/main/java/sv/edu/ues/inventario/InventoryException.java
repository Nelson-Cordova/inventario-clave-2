package sv.edu.ues.inventario;

/** Error recuperable que puede mostrarse al usuario sin revelar credenciales. */
public class InventoryException extends RuntimeException {
    public InventoryException(String message) { super(message); }
    public InventoryException(String message, Throwable cause) { super(message, cause); }
}

