package sv.edu.ues.inventario;
import java.math.BigDecimal;
import java.util.List;

/** Reglas independientes de Swing y del controlador de MongoDB. */
public final class InventoryService {
    private final ProductRepository repository;
    public InventoryService(ProductRepository repository) { this.repository = repository; }
    public List<Product> list() { return repository.list(); }
    public void register(Product product) { repository.insert(product); }
    public void update(Product original, Product replacement) {
        if (original == null) throw new InventoryException("Seleccione un producto existente.");
        if (!original.code.equals(replacement.code)) throw new InventoryException("El código no puede modificarse.");
        repository.update(original, replacement);
    }
    public void delete(Product original) {
        if (original == null) throw new InventoryException("Seleccione un producto existente.");
        repository.delete(original);
    }
    public void move(Product original, int units, boolean entry) {
        if (original == null) throw new InventoryException("Seleccione un producto existente.");
        if (units <= 0) throw new InventoryException("Las unidades deben ser mayores que cero.");
        if (entry && (long) original.quantity + units > Integer.MAX_VALUE)
            throw new InventoryException("La entrada excede la cantidad máxima permitida.");
        if (!entry && units > original.quantity) throw new InventoryException("Existencias insuficientes.");
        repository.move(original, units, entry);
    }
    public static BigDecimal total(List<Product> products) {
        BigDecimal sum = new BigDecimal("0.00");
        for (Product p : products) sum = sum.add(p.value());
        return sum;
    }
}

