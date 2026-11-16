package sv.edu.ues.inventario;
import java.util.List;
public interface ProductRepository extends AutoCloseable {
    List<Product> list();
    void insert(Product product);
    void update(Product original, Product replacement);
    void delete(Product original);
    void move(Product original, int units, boolean entry);
    void close();
}

