package sv.edu.ues.inventario;
import java.math.BigDecimal;
import java.util.*;
/** Solo se ejecuta contra la base exclusiva inventario_test y con opt-in explícito. */
public final class MySqlIntegrationTest {
    private static int checks;
    private static void check(boolean ok) { checks++; if(!ok) throw new AssertionError("Integración: caso "+checks); }
    private static void reject(Runnable action) {
        checks++; try {action.run();} catch(InventoryException expected) {return;}
        throw new AssertionError("Se esperaba rechazo en caso "+checks);
    }
    private static Product get(MySqlStore db,String code) {
        for(Product p:db.list()) if(p.code.equals(code)) return p;
        throw new AssertionError("Producto ausente");
    }
    public static void main(String[] args) {
        String url=System.getenv("MYSQL_URL");
        if(!"true".equals(System.getenv("MYSQL_TEST_MODE")) || url==null || !url.matches("jdbc:mysql://[^/]+/inventario_test(\\?.*)?"))
            throw new IllegalStateException("Se requiere MYSQL_TEST_MODE=true y MYSQL_URL apuntando a inventario_test.");
        String code="TEST_"+UUID.randomUUID().toString().replace("-","").substring(0,20).toUpperCase(Locale.ROOT);
        MySqlStore db=new MySqlStore(); InventoryService service=new InventoryService(db);
        try {
            Product p=new Product(code,"Producto de prueba","Descripción","Pruebas",new BigDecimal("1.25"),5,0);
            service.register(p); check(get(db,code).quantity==5);
            reject(() -> service.register(p));
            service.move(get(db,code),3,true); check(get(db,code).quantity==8);
            service.move(get(db,code),8,false); check(get(db,code).quantity==0);
            reject(() -> service.move(get(db,code),1,false));
            Product stale=get(db,code);
            service.move(stale,2,true);
            reject(() -> db.move(stale,1,true));
            check(get(db,code).quantity==2);
            reject(() -> db.move(get(db,code),3,false));
            check(get(db,code).quantity==2);
            Product current=get(db,code);
            service.update(current,new Product(code,"Actualizado","Texto con ' comillas","Otra",new BigDecimal("2.50"),3,current.version));
            check(get(db,code).name.equals("Actualizado"));
            check(get(db,code).value().compareTo(new BigDecimal("7.50"))==0);
            try(MySqlStore reopened=new MySqlStore()) {check(get(reopened,code).quantity==3);}
            reject(() -> service.delete(stale));
            Product last=get(db,code); service.delete(last);
            check(db.list().stream().noneMatch(x -> x.code.equals(code)));
            reject(() -> service.delete(last));
            reject(() -> db.update(last,last));
            System.out.println("OK: "+checks+" comprobaciones JDBC/MySQL.");
        } finally {
            for(Product p:db.list()) if(p.code.equals(code)) db.delete(p);
            db.close();
        }
    }
}

