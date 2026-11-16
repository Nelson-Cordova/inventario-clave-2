package sv.edu.ues.inventario;
import java.math.BigDecimal;
import java.util.*;
/** Ejecutable sin dependencias: falla con AssertionError si una regla se incumple. */
public final class DomainTest {
    private static int count;
    private static void check(boolean ok) { count++; if(!ok) throw new AssertionError("Caso "+count); }
    private static void reject(Runnable test) {
        count++; try { test.run(); } catch(InventoryException expected){ return; }
        throw new AssertionError("Se aceptaron datos inválidos: "+count);
    }
    private static Product p(String code,int qty) { return new Product(code,"Arroz","","Alimentos",new BigDecimal("2.50"),qty,0); }
    public static void main(String[] args) {
        check(p("a-1",3).code.equals("A-1"));
        check(p("A",3).value().compareTo(new BigDecimal("7.50"))==0);
        reject(() -> p("",0)); reject(() -> p("A",-1));
        reject(() -> new Product("A","","","C",BigDecimal.ZERO,0,0));
        reject(() -> new Product("A","N","","",BigDecimal.ZERO,0,0));
        reject(() -> new Product("A","N","","C",new BigDecimal("-1"),0,0));
        reject(() -> new Product("A","N","","C",new BigDecimal("1.001"),0,0));
        reject(() -> Product.integer("1.5","Cantidad",false));
        reject(() -> Product.integer("-1","Cantidad",false));
        reject(() -> Product.integer("2147483648","Cantidad",false));
        reject(() -> Product.integer("0","Movimiento",true));
        check(Product.integer("0","Límite",false)==0);
        check(Product.money("1,25").compareTo(new BigDecimal("1.25"))==0);
        reject(() -> Product.money("NaN")); reject(() -> Product.money("1e3"));
        check(InventoryService.total(Arrays.asList(p("A",2),p("B",3))).compareTo(new BigDecimal("12.50"))==0);
        check(InventoryService.total(Collections.emptyList()).signum()==0);
        ProductRepository fake=new ProductRepository(){
            public List<Product> list(){return Collections.emptyList();}
            public void insert(Product p){}
            public void update(Product a,Product b){}
            public void delete(Product p){}
            public void move(Product p,int u,boolean e){}
            public void close(){}
        };
        InventoryService service=new InventoryService(fake);
        reject(() -> service.move(p("A",2),3,false));
        reject(() -> service.move(p("A",2),0,true));
        reject(() -> service.move(p("A",Integer.MAX_VALUE),1,true));
        reject(() -> service.move(null,1,true));
        reject(() -> service.update(p("A",1),p("B",1)));
        reject(() -> service.delete(null));
        String salt=Passwords.salt();
        check(Passwords.verify("prueba12345".toCharArray(),salt,Passwords.hash("prueba12345".toCharArray(),salt)));
        check(!Passwords.verify("incorrecta".toCharArray(),salt,Passwords.hash("prueba12345".toCharArray(),salt)));
        System.out.println("OK: "+count+" comprobaciones de dominio y contraseñas.");
    }
}

