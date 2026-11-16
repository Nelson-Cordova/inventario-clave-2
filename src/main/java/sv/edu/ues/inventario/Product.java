package sv.edu.ues.inventario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Entidad inmutable: los valores inválidos nunca forman un producto válido. */
public final class Product {
    public final String code, name, description, category;
    public final BigDecimal price;
    public final int quantity;
    public final long version;

    public Product(String code, String name, String description, String category,
                   BigDecimal price, int quantity, long version) {
        this.code = required(code, "Código", 40).toUpperCase(Locale.ROOT);
        if (!this.code.matches("[A-Z0-9_-]+"))
            throw new InventoryException("Código: use letras, números, guion o guion bajo.");
        this.name = required(name, "Nombre", 120);
        this.category = required(category, "Categoría", 80);
        this.description = description == null ? "" : description.trim();
        if (this.description.length() > 1000) throw new InventoryException("Descripción: máximo 1000 caracteres.");
        if (price == null || price.signum() < 0 || price.compareTo(new BigDecimal("999999999.99")) > 0)
            throw new InventoryException("Precio: debe estar entre 0 y 999999999.99.");
        try { this.price = price.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new InventoryException("Precio: máximo dos decimales."); }
        if (quantity < 0) throw new InventoryException("Cantidad: debe ser un entero no negativo.");
        if (version < 0) throw new InventoryException("Versión de producto inválida.");
        this.quantity = quantity;
        this.version = version;
    }
    public static String required(String value, String field, int max) {
        if (value == null || value.trim().isEmpty()) throw new InventoryException(field + ": es obligatorio.");
        String text = value.trim();
        if (text.length() > max) throw new InventoryException(field + ": máximo " + max + " caracteres.");
        return text;
    }
    public static int integer(String text, String field, boolean positive) {
        try {
            if (text == null || !text.trim().matches("[0-9]+")) throw new NumberFormatException();
            int value = Integer.parseInt(text.trim());
            if (positive && value == 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new InventoryException(field + ": ingrese un entero " + (positive ? "mayor que cero." : "no negativo."));
        }
    }
    public static BigDecimal money(String text) {
        try {
            if (text == null || !text.trim().matches("[0-9]+([.,][0-9]{1,2})?")) throw new NumberFormatException();
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) { throw new InventoryException("Precio: ingrese un número no negativo con máximo dos decimales."); }
    }
    public BigDecimal value() { return price.multiply(BigDecimal.valueOf(quantity)); }
}

