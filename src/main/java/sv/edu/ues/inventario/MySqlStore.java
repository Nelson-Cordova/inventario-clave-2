package sv.edu.ues.inventario;

import java.sql.*;
import java.util.*;
import java.util.Locale;

/** Repositorio JDBC. PreparedStatement evita interpretar las entradas como SQL.
 * Cada escritura es una sentencia atómica confirmada por autoCommit. */
public final class MySqlStore implements ProductRepository {
    private final String url, username, password;
    public MySqlStore() {
        url = env("MYSQL_URL", "jdbc:mysql://localhost:3306/inventario_ues?sslMode=REQUIRED&connectTimeout=5000&socketTimeout=10000&connectionTimeZone=UTC");
        username = env("MYSQL_USER", "inventario_app");
        password = env("MYSQL_PASSWORD", "");
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch(ClassNotFoundException e) { throw new InventoryException("Falta MySQL Connector/J. Ejecute Maven para descargar las dependencias.",e); }
        database(() -> {
            try(Connection c=connection(); Statement s=c.createStatement()) {
                s.executeQuery("SELECT code, version FROM productos LIMIT 0").close();
                s.executeQuery("SELECT username FROM usuarios LIMIT 0").close();
            }
            return null;
        });
    }
    private static String env(String key,String fallback) {
        String value=System.getenv(key); return value==null||value.trim().isEmpty()?fallback:value;
    }
    private Connection connection() throws SQLException { return DriverManager.getConnection(url,username,password); }
    private interface SqlOperation<T> { T run() throws SQLException; }
    private <T> T database(SqlOperation<T> operation) {
        try { return operation.run(); }
        catch(SQLException e) {
            if(e.getErrorCode()==1062) throw new InventoryException("El código o usuario ya está registrado.",e);
            if(e.getErrorCode()==1146 || e.getErrorCode()==1049)
                throw new InventoryException("No existe la estructura de datos. Ejecute sql/01_schema.sql en MySQL.",e);
            if(e.getErrorCode()==1045)
                throw new InventoryException("MySQL rechazó las credenciales. Revise MYSQL_USER y MYSQL_PASSWORD.",e);
            throw new InventoryException("Error de conexión, lectura o escritura en MySQL. Actualice los datos antes de repetir una operación.",e);
        }
    }
    private Product product(ResultSet r) throws SQLException {
        try {
            String description=r.getString("description");
            int quantity=r.getInt("quantity");
            if(r.wasNull()) throw new InventoryException("Cantidad nula.");
            long version=r.getLong("version");
            if(r.wasNull()) throw new InventoryException("Versión nula.");
            return new Product(r.getString("code"),r.getString("name"),description,r.getString("category"),
                r.getBigDecimal("price"),quantity,version);
        } catch(InventoryException e) {
            throw new InventoryException("Hay un producto almacenado con datos inválidos. Revise la tabla productos; el registro no se modificó.",e);
        }
    }
    public List<Product> list() {
        return database(() -> {
            List<Product> result=new ArrayList<>();
            try(Connection c=connection(); PreparedStatement s=c.prepareStatement("SELECT code,name,description,category,price,quantity,version FROM productos ORDER BY code");
                ResultSet r=s.executeQuery()) {
                while(r.next()) result.add(product(r));
            }
            return result;
        });
    }
    public void insert(Product p) {
        database(() -> {
            try(Connection c=connection(); PreparedStatement s=c.prepareStatement(
                "INSERT INTO productos(code,name,description,category,price,quantity,version) VALUES(?,?,?,?,?,?,0)")) {
                s.setString(1,p.code); bind(s,p,2); s.executeUpdate();
            } return null;
        });
    }
    private void bind(PreparedStatement s,Product p,int start) throws SQLException {
        s.setString(start,p.name); s.setString(start+1,p.description); s.setString(start+2,p.category);
        s.setBigDecimal(start+3,p.price); s.setInt(start+4,p.quantity);
    }
    public void update(Product old,Product p) {
        database(() -> {
            try(Connection c=connection(); PreparedStatement s=c.prepareStatement(
                "UPDATE productos SET name=?,description=?,category=?,price=?,quantity=?,version=version+1 WHERE code=? AND version=?")) {
                bind(s,p,1); s.setString(6,old.code); s.setLong(7,old.version); changed(s.executeUpdate());
            } return null;
        });
    }
    public void delete(Product p) {
        database(() -> {
            try(Connection c=connection(); PreparedStatement s=c.prepareStatement("DELETE FROM productos WHERE code=? AND version=?")) {
                s.setString(1,p.code); s.setLong(2,p.version); changed(s.executeUpdate());
            } return null;
        });
    }
    public void move(Product p,int units,boolean entry) {
        if(units<=0) throw new InventoryException("Las unidades deben ser mayores que cero.");
        database(() -> {
            // El predicado y la actualización se evalúan en una sola sentencia.
            String sql=entry
                ? "UPDATE productos SET quantity=quantity+?,version=version+1 WHERE code=? AND version=? AND quantity<=?"
                : "UPDATE productos SET quantity=quantity-?,version=version+1 WHERE code=? AND version=? AND quantity>=?";
            try(Connection c=connection(); PreparedStatement s=c.prepareStatement(sql)) {
                s.setInt(1,units); s.setString(2,p.code); s.setLong(3,p.version);
                s.setInt(4,entry?Integer.MAX_VALUE-units:units); changed(s.executeUpdate());
            } return null;
        });
    }
    private void changed(int count) {
        if(count!=1) throw new InventoryException("El producto no existe, cambió en otra sesión o las existencias no permiten el movimiento. Actualice la tabla.");
    }
    public boolean needsSetup() {
        return database(() -> {
            try(Connection c=connection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT COUNT(*) FROM usuarios")) {
                r.next(); return r.getLong(1)==0;
            }
        });
    }
    public void setup(String name,char[] secret) {
        String normalized=Product.required(name,"Usuario",50).toLowerCase(Locale.ROOT);
        if(!normalized.matches("[a-z0-9._-]+")) throw new InventoryException("Usuario: use letras, números, punto, guion o guion bajo.");
        if(secret.length<10) throw new InventoryException("La contraseña debe tener al menos 10 caracteres.");
        String salt=Passwords.salt(), hash=Passwords.hash(secret,salt);
        database(() -> {
            // ID fijo y CHECK id=1 garantizan una sola configuración inicial incluso en concurrencia.
            try(Connection c=connection(); PreparedStatement s=c.prepareStatement("INSERT INTO usuarios(id,username,salt,password_hash) VALUES(1,?,?,?)")) {
                s.setString(1,normalized); s.setString(2,salt); s.setString(3,hash); s.executeUpdate();
            } return null;
        });
    }
    public boolean login(String name,char[] secret) {
        return database(() -> {
            try(Connection c=connection(); PreparedStatement s=c.prepareStatement("SELECT salt,password_hash FROM usuarios WHERE username=?")) {
                s.setString(1,name.trim().toLowerCase(Locale.ROOT));
                try(ResultSet r=s.executeQuery()) {
                    if(!r.next()) return false;
                    try { return Passwords.verify(secret,r.getString("salt"),r.getString("password_hash")); }
                    catch(RuntimeException e) { throw new InventoryException("No se puede interpretar la cuenta almacenada.",e); }
                }
            }
        });
    }
    /** Las conexiones se cierran en cada operación mediante try-with-resources. */
    public void close() {}
}

