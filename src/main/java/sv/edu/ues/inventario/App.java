package sv.edu.ues.inventario;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Presentación Swing. El acceso a MySQL nunca bloquea el hilo gráfico. */
public final class App extends JFrame {
    private MySqlStore store;
    private InventoryService service;
    private final JPanel root = new JPanel(new BorderLayout(12,12));
    private final JLabel status = new JLabel("Conectando con MySQL...");
    private final List<JButton> buttons = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Código","Nombre","Categoría","Precio ($)","Cantidad","Valor ($)"},0) {
        public boolean isCellEditable(int row,int col) { return false; }
        public Class<?> getColumnClass(int col) { return col==4 ? Integer.class : col==3 || col==5 ? BigDecimal.class : String.class; }
    };
    private final JTable table = new JTable(model);
    private final JTextField search = new JTextField(16), limit = new JTextField("5",4);
    private final JComboBox<String> searchBy = new JComboBox<>(new String[]{"Todos","Código","Nombre","Categoría"});
    private final JComboBox<String> category = new JComboBox<>();
    private final JComboBox<String> report = new JComboBox<>(new String[]{"Inventario general","Sin existencias","Existencias bajas"});
    private final JLabel totals = new JLabel(" ");
    private List<Product> all = new ArrayList<>(), visible = new ArrayList<>();
    private boolean busy;

    private App() {
        super("Inventario | Clave 2 · UES");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (busy) { JOptionPane.showMessageDialog(App.this,"Espere a que termine la operación."); return; }
                if (store != null) store.close();
                dispose();
            }
        });
        root.setBorder(BorderFactory.createEmptyBorder(20,20,16,20));
        root.add(new JLabel("INVENTARIO · Gestión de productos", SwingConstants.CENTER),BorderLayout.NORTH);
        root.add(status,BorderLayout.SOUTH); setContentPane(root);
        setMinimumSize(new Dimension(1000,650)); setSize(1160,740); setLocationRelativeTo(null);
        setVisible(true); connect();
    }
    private JButton button(String title, Runnable action) {
        JButton b = new JButton(title); b.addActionListener(e -> action.run()); buttons.add(b); return b;
    }
    private <T> void task(String message, Callable<T> action, Consumer<T> success) {
        if (busy) return;
        busy=true; status.setText(message); buttons.forEach(b -> b.setEnabled(false));
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception { return action.call(); }
            protected void done() {
                busy=false; buttons.forEach(b -> b.setEnabled(true));
                try { success.accept(get()); }
                catch (Exception e) {
                    Throwable cause=e;
                    while (cause.getCause()!=null && !(cause instanceof InventoryException)) cause=cause.getCause();
                    String text=cause instanceof InventoryException ? cause.getMessage() : "Ocurrió un error inesperado. La operación no pudo confirmarse.";
                    status.setText(text);
                    JOptionPane.showMessageDialog(App.this,text,"Operación no completada",JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    private void connect() {
        JPanel panel=new JPanel(); panel.add(button("Reintentar conexión",this::connect));
        root.add(panel,BorderLayout.CENTER); root.revalidate();
        task("Conectando con MySQL...", () -> new MySqlStore(), connected -> {
            store=connected; service=new InventoryService(store);
            task("Verificando configuración...",store::needsSetup,this::loginPanel);
        });
    }
    private void loginPanel(boolean setup) {
        buttons.clear(); JPanel form=new JPanel(new GridBagLayout());
        JPanel fields=new JPanel(new GridLayout(0,1,8,10));
        JTextField username=new JTextField(24); JPasswordField password=new JPasswordField(24);
        fields.add(new JLabel(setup ? "Crear cuenta del administrador" : "Iniciar sesión"));
        fields.add(new JLabel("Usuario")); fields.add(username);
        fields.add(new JLabel(setup ? "Contraseña (mínimo 10 caracteres)" : "Contraseña")); fields.add(password);
        if(setup) fields.add(new JLabel("Configuración inicial: realícela en un entorno de confianza."));
        JButton enter=button(setup ? "Crear cuenta" : "Ingresar", () -> {
            String name=username.getText(); char[] secret=password.getPassword(); password.setText("");
            task(setup ? "Creando cuenta..." : "Verificando credenciales...", () -> {
                try {
                    Product.required(name,"Usuario",50);
                    if(setup) { store.setup(name,secret); return true; }
                    return store.login(name,secret);
                } finally { Arrays.fill(secret,'\0'); }
            }, valid -> {
                if(!valid) { status.setText("Usuario o contraseña incorrectos."); JOptionPane.showMessageDialog(this,status.getText()); }
                else if(setup) { loginPanel(false); status.setText("Cuenta creada. Inicie sesión."); }
                else { inventoryPanel(); refresh("Sesión iniciada."); }
            });
        });
        fields.add(enter); form.add(fields);
        replaceCenter(form); getRootPane().setDefaultButton(enter);
        status.setText(setup ? "MySQL conectado. Configure la primera cuenta." : "MySQL conectado. Ingrese sus credenciales.");
    }
    private void replaceCenter(Component panel) {
        BorderLayout layout=(BorderLayout)root.getLayout();
        Component old=layout.getLayoutComponent(BorderLayout.CENTER);
        if(old!=null) root.remove(old);
        root.add(panel,BorderLayout.CENTER); root.revalidate(); root.repaint();
    }
    private void inventoryPanel() {
        buttons.clear(); getRootPane().setDefaultButton(null);
        JPanel content=new JPanel(new BorderLayout(8,12));
        JPanel top=new JPanel(new GridLayout(0,1,6,6));
        JPanel filters=new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Buscar")); filters.add(search); filters.add(searchBy);
        filters.add(new JLabel("Categoría")); category.setPreferredSize(new Dimension(150,26)); filters.add(category);
        filters.add(button("Aplicar filtros",() -> guarded(this::applyFilters)));
        top.add(filters);
        JPanel reports=new JPanel(new FlowLayout(FlowLayout.LEFT));
        reports.add(new JLabel("Reporte")); reports.add(report); reports.add(new JLabel("Límite bajo")); reports.add(limit);
        reports.add(button("Consultar / actualizar",() -> refresh("Datos actualizados desde MySQL.")));
        reports.add(button("Ver todos",() -> {
            search.setText(""); searchBy.setSelectedIndex(0); report.setSelectedIndex(0);
            if(category.getItemCount()>0) category.setSelectedIndex(0);
            refresh("Inventario general actualizado.");
        }));
        top.add(reports); content.add(top,BorderLayout.NORTH);
        table.setAutoCreateRowSorter(true); table.setRowHeight(30); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){if(e.getClickCount()==2 && !busy) guarded(() -> detail(selected()));}});
        content.add(new JScrollPane(table),BorderLayout.CENTER);
        JPanel bottom=new JPanel(new BorderLayout(8,8));
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(button("Nuevo producto",() -> guarded(() -> edit(null))));
        actions.add(button("Modificar",() -> guarded(() -> edit(selected()))));
        actions.add(button("Detalle",() -> guarded(() -> detail(selected()))));
        actions.add(button("Eliminar",() -> guarded(this::delete)));
        actions.add(button("Entrada +",() -> guarded(() -> movement(true))));
        actions.add(button("Salida −",() -> guarded(() -> movement(false))));
        actions.add(button("Cerrar sesión",() -> { all.clear(); visible.clear(); model.setRowCount(0); loginPanel(false); }));
        bottom.add(actions,BorderLayout.NORTH); bottom.add(totals,BorderLayout.SOUTH);
        content.add(bottom,BorderLayout.SOUTH); replaceCenter(content);
    }
    private void guarded(Runnable action) {
        try { action.run(); } catch(InventoryException e) { JOptionPane.showMessageDialog(this,e.getMessage(),"Revise los datos",JOptionPane.WARNING_MESSAGE); }
    }
    private void refresh(String message) {
        // Validar antes de consultar: un límite inválido nunca genera un reporte.
        try { Product.integer(limit.getText(),"Límite",false); }
        catch(InventoryException e) { JOptionPane.showMessageDialog(this,e.getMessage()); return; }
        task("Consultando productos almacenados...",service::list, list -> {
            all=list;
            Object previous=category.getSelectedItem();
            category.removeAllItems(); category.addItem("Todas");
            TreeSet<String> values=new TreeSet<>();
            for(Product p:all) values.add(p.category);
            for(String value:values) category.addItem(value);
            if(previous!=null && values.contains(previous.toString())) category.setSelectedItem(previous);
            applyFilters(); status.setText(message);
        });
    }
    private void applyFilters() {
        int threshold=Product.integer(limit.getText(),"Límite",false);
        String text=search.getText().trim().toLowerCase(Locale.ROOT);
        String cat=(String)category.getSelectedItem();
        int mode=searchBy.getSelectedIndex(), reportMode=report.getSelectedIndex();
        visible=new ArrayList<>();
        for(Product p:all) {
            boolean match=text.isEmpty()
                || ((mode==0||mode==1) && p.code.equalsIgnoreCase(text))
                || ((mode==0||mode==2) && p.name.toLowerCase(Locale.ROOT).contains(text))
                || ((mode==0||mode==3) && p.category.toLowerCase(Locale.ROOT).contains(text));
            if(match && (cat==null||cat.equals("Todas")||cat.equals(p.category))
                && (reportMode!=1||p.quantity==0) && (reportMode!=2||p.quantity<=threshold)) visible.add(p);
        }
        model.setRowCount(0);
        for(Product p:visible) model.addRow(new Object[]{p.code,p.name,p.category,p.price,p.quantity,p.value()});
        totals.setText("Mostrados: "+visible.size()+" / "+all.size()+"    |    Valor mostrado: $"+InventoryService.total(visible)
            +"    |    Valor total del inventario: $"+InventoryService.total(all));
        status.setText(visible.isEmpty() ? "Sin productos para esta consulta." : "Consulta lista. Pulse un encabezado para ordenar.");
    }
    private Product selected() {
        int row=table.getSelectedRow();
        if(row<0) throw new InventoryException("Seleccione un producto de la tabla.");
        return visible.get(table.convertRowIndexToModel(row));
    }
    private void detail(Product p) {
        JTextArea area=new JTextArea("Código: "+p.code+"\nNombre: "+p.name+"\nDescripción: "+p.description+
            "\nCategoría: "+p.category+"\nPrecio: $"+p.price+"\nCantidad: "+p.quantity+"\nValor: $"+p.value(),12,45);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setEditable(false);
        JOptionPane.showMessageDialog(this,new JScrollPane(area),"Detalle del producto",JOptionPane.INFORMATION_MESSAGE);
    }
    private void edit(Product old) {
        JTextField code=new JTextField(old==null?"":old.code), name=new JTextField(old==null?"":old.name);
        JTextField description=new JTextField(old==null?"":old.description), cat=new JTextField(old==null?"":old.category);
        JTextField price=new JTextField(old==null?"":old.price.toPlainString()), quantity=new JTextField(old==null?"0":String.valueOf(old.quantity));
        code.setEditable(old==null);
        JPanel fields=new JPanel(new GridLayout(0,2,10,10));
        String[] labels={"Código *","Nombre *","Descripción","Categoría *","Precio unitario ($) *",old==null?"Cantidad inicial *":"Cantidad (ajuste) *"};
        JTextField[] inputs={code,name,description,cat,price,quantity};
        for(int i=0;i<labels.length;i++){fields.add(new JLabel(labels[i]));fields.add(inputs[i]);}
        while(JOptionPane.showConfirmDialog(this,fields,old==null?"Registrar producto":"Modificar producto",
            JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                Product p=new Product(code.getText(),name.getText(),description.getText(),cat.getText(),
                    Product.money(price.getText()),Product.integer(quantity.getText(),"Cantidad",false),old==null?0:old.version);
                if(old!=null && old.quantity!=p.quantity && JOptionPane.showConfirmDialog(this,
                    "Ajustar existencias de "+old.quantity+" a "+p.quantity+" unidades?",
                    "Confirmar ajuste",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) continue;
                mutate(old==null?"Producto registrado.":"Producto modificado.",() -> {
                    if(old==null) service.register(p); else service.update(old,p);
                }); return;
            } catch(InventoryException e) { JOptionPane.showMessageDialog(this,e.getMessage(),"Revise los datos",JOptionPane.WARNING_MESSAGE); }
        }
    }
    private void delete() {
        Product p=selected();
        if(JOptionPane.showConfirmDialog(this,"Eliminar "+p.code+" · "+p.name+"?\nExistencias: "+p.quantity+
            "\nLa eliminación es permanente.","Confirmar eliminación",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)==JOptionPane.YES_OPTION)
            mutate("Producto eliminado.",() -> service.delete(p));
    }
    private void movement(boolean entry) {
        Product p=selected();
        String text=JOptionPane.showInputDialog(this,(entry?"Entrada":"Salida")+" para "+p.code+"\nDisponibles: "+p.quantity+"\nUnidades:");
        if(text==null) return;
        int units=Product.integer(text,"Unidades",true);
        mutate(entry?"Entrada registrada.":"Salida registrada.",() -> service.move(p,units,entry));
    }
    private void mutate(String message,Runnable action) {
        task("Guardando cambios en MySQL...",() -> { action.run(); return true; }, ok -> {
            status.setText(message);
            JOptionPane.showMessageDialog(this,message,"Operación completada",JOptionPane.INFORMATION_MESSAGE);
            refresh(message);
        });
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ignored) {}
            UIManager.put("Label.font",new Font("Segoe UI",Font.PLAIN,14));
            UIManager.put("Button.font",new Font("Segoe UI",Font.PLAIN,13));
            new App();
        });
    }
}

