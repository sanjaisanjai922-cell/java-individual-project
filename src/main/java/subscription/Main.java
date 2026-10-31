package subscription;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Main {
    static int uid; static String role,name;
    static final List<Tab> tabs=new ArrayList<>();
    static class Tab extends JPanel {
        JTable grid=new JTable(); JPanel bar=new JPanel(new FlowLayout(FlowLayout.LEFT));
        String sql; Object[] args;
        Tab(String title,String sql,Object...args){
            super(new BorderLayout(8,8));this.sql=sql;this.args=args;
            setBorder(BorderFactory.createTitledBorder(title));
            grid.setAutoCreateRowSorter(true);add(bar,BorderLayout.NORTH);add(new JScrollPane(grid),BorderLayout.CENTER);
            tabs.add(this);refresh();
        }
        void refresh(){grid.setModel(Db.table(sql,args));}
        Integer selected(){
            int r=grid.getSelectedRow();if(r<0){msg("Select a row first.");return null;}
            r=grid.convertRowIndexToModel(r);return Integer.parseInt(grid.getValueAt(r,0).toString());
        }
        Tab btn(String label,Runnable action){
            JButton b=new JButton(label);bar.add(b);b.addActionListener(e->{try{action.run();refreshAll();}catch(Exception x){err(x);}});return this;
        }
    }
    static void refreshAll(){for(Tab t:tabs)try{t.refresh();}catch(Exception ignored){}}
    static void msg(String s){JOptionPane.showMessageDialog(null,s);}
    static void err(Exception e){Throwable c=e;while(c.getCause()!=null)c=c.getCause();msg("Error: "+c.getMessage());}
    static String[] form(String title,String... labels){
        JPanel p=new JPanel(new GridLayout(0,2,8,8));JComponent[] fields=new JComponent[labels.length];
        for(int i=0;i<labels.length;i++){p.add(new JLabel(labels[i]));fields[i]=new JTextField(20);p.add(fields[i]);}
        if(JOptionPane.showConfirmDialog(null,p,title,JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return null;
        String[] a=new String[labels.length];for(int i=0;i<a.length;i++)a[i]=((JTextField)fields[i]).getText().trim();return a;
    }
    static void status(String table,String value,Tab t){
        Integer id=t.selected();if(id!=null)Db.exec("UPDATE "+table+" SET status=? WHERE id=?",value,id);
    }
    public static void main(String[] a){
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());Db.init();}
        catch(Exception e){msg("Cannot connect to MySQL. Start MySQL in XAMPP and check Db.java settings.\n"+e.getMessage());return;}
        SwingUtilities.invokeLater(Main::login);
    }
    static void login(){
        JTextField email=new JTextField("admin@subscription.com",20);JPasswordField pass=new JPasswordField(20);
        JPanel p=new JPanel(new GridLayout(0,2,8,8));p.setBorder(new EmptyBorder(12,12,12,12));
        p.add(new JLabel("Email"));p.add(email);p.add(new JLabel("Password"));p.add(pass);
        int x=JOptionPane.showConfirmDialog(null,p,"Subscription Billing Management System - Login",JOptionPane.OK_CANCEL_OPTION);
        if(x!=JOptionPane.OK_OPTION)return;
        try{
            String[] u=Db.row("SELECT id,name,role FROM users WHERE email=? AND password_hash=?",email.getText().trim(),Db.hash(new String(pass.getPassword())));
            if(u==null){msg("Invalid email or password.");login();return;}
            uid=Integer.parseInt(u[0]);name=u[1];role=u[2];dashboard();
        }catch(Exception e){err(e);login();}
    }
    static void dashboard(){
        tabs.clear();JFrame f=new JFrame("Subscription Billing Management System");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);f.setSize(1100,680);f.setLocationRelativeTo(null);
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(new EmptyBorder(10,10,10,10));
        JPanel top=new JPanel(new BorderLayout());JLabel heading=new JLabel("  SUBSCRIPTION BILLING MANAGEMENT SYSTEM");
        heading.setFont(new Font("SansSerif",Font.BOLD,19));top.add(heading,BorderLayout.WEST);
        JButton logout=new JButton("Logout  ("+name+" / "+role+")");top.add(logout,BorderLayout.EAST);logout.addActionListener(e->{f.dispose();login();});
        root.add(top,BorderLayout.NORTH);JTabbedPane tp=new JTabbedPane();
        boolean admin=role.equals("ADMIN"),billing=role.equals("BILLING"),customer=role.equals("CUSTOMER");
        if(admin||billing){
            Tab customers=new Tab("Customer Accounts","SELECT id,name,email,phone,created_at FROM users WHERE role='CUSTOMER' ORDER BY id DESC");
            if(admin)customers.btn("Add Customer",()->{String[]v=form("New Customer","Name","Email","Password","Phone");if(v!=null)Db.exec("INSERT INTO users(name,email,password_hash,role,phone) VALUES(?,?,?,'CUSTOMER',?)",v[0],v[1],Db.hash(v[2]),v[3]);});
            tp.addTab("Customers",customers);
            Tab plans=new Tab("Subscription Plans","SELECT id,name,description,price,billing_cycle,status FROM plans ORDER BY id");
            if(admin){
                plans.btn("Add Plan",()->{String[]v=form("New Plan","Plan Name","Description","Price","Billing Cycle (MONTHLY/QUARTERLY/YEARLY)");if(v!=null)Db.exec("INSERT INTO plans(name,description,price,billing_cycle) VALUES(?,?,?,?,?)".replace("?,?,?,?,?","?,?,?,?"),v[0],v[1],v[2],v[3]);});
                plans.btn("Activate",()->status("plans","ACTIVE",plans));plans.btn("Deactivate",()->status("plans","INACTIVE",plans));
            }tp.addTab("Plans",plans);
            Tab subs=new Tab("All Subscriptions","SELECT s.id,u.name customer,p.name plan,p.price,p.billing_cycle,s.start_date,s.next_billing_date,s.status FROM subscriptions s JOIN users u ON u.id=s.user_id JOIN plans p ON p.id=s.plan_id ORDER BY s.id DESC");
            if(admin||billing)subs.btn("Cancel Subscription",()->status("subscriptions","CANCELLED",subs));
            tp.addTab("Subscriptions",subs);
            Tab invoices=new Tab("Invoices","SELECT i.id,u.name customer,p.name plan,i.invoice_date,i.due_date,i.amount,i.status FROM invoices i JOIN subscriptions s ON s.id=i.subscription_id JOIN users u ON u.id=s.user_id JOIN plans p ON p.id=s.plan_id ORDER BY i.id DESC");
            invoices.btn("Mark Paid",()->{Integer id=invoices.selected();if(id==null)return;String[]v=form("Record Payment","Method (CASH/CARD/UPI/BANK_TRANSFER)","Transaction Reference");if(v==null)return;String[]a=Db.row("SELECT amount,status FROM invoices WHERE id=?",id);if(a==null||a[1].equals("PAID")){msg("Invoice is already paid.");return;}Db.exec("INSERT INTO payments(invoice_id,amount,payment_method,transaction_ref) VALUES(?,?,?,?)",id,a[0],v[0].toUpperCase(),v[1]);Db.exec("UPDATE invoices SET status='PAID' WHERE id=?",id);});
            invoices.btn("Generate Due Invoices",()->generateInvoices());
            tp.addTab("Invoices",invoices);
            Tab payments=new Tab("Payment History","SELECT p.id,u.name customer,p.invoice_id,p.amount,p.payment_method,p.transaction_ref,p.payment_date,p.status FROM payments p JOIN invoices i ON i.id=p.invoice_id JOIN subscriptions s ON s.id=i.subscription_id JOIN users u ON u.id=s.user_id ORDER BY p.id DESC");
            tp.addTab("Payments",payments);
            Tab reports=new Tab("Billing Summary","SELECT (SELECT COUNT(*) FROM users WHERE role='CUSTOMER') customers,(SELECT COUNT(*) FROM subscriptions WHERE status='ACTIVE') active_subscriptions,(SELECT COUNT(*) FROM invoices WHERE status='UNPAID') unpaid_invoices,(SELECT COALESCE(SUM(amount),0) FROM payments WHERE status='SUCCESS') total_collected,(SELECT COALESCE(SUM(amount),0) FROM invoices WHERE status<>'PAID') outstanding_amount");
            tp.addTab("Reports",reports);
        }
        if(customer){
            Tab plans=new Tab("Available Plans","SELECT id,name,description,price,billing_cycle FROM plans WHERE status='ACTIVE'");
            plans.btn("Subscribe",()->{Integer id=plans.selected();if(id==null)return;String[]p=Db.row("SELECT price,billing_cycle FROM plans WHERE id=? AND status='ACTIVE'",id);if(p==null)return;
                LocalDate today=LocalDate.now();LocalDate next=cycle(today,p[1]);
                Db.exec("INSERT INTO subscriptions(user_id,plan_id,start_date,next_billing_date) VALUES(?,?,?,?)",uid,id,today.toString(),next.toString());
                String[]s=Db.row("SELECT id FROM subscriptions WHERE user_id=? ORDER BY id DESC LIMIT 1",uid);
                Db.exec("INSERT INTO invoices(subscription_id,invoice_date,due_date,amount) VALUES(?,?,?,?)",s[0],today.toString(),today.plusDays(7).toString(),p[0]);msg("Subscription created. Your first invoice is ready.");
            });tp.addTab("Plans",plans);
            Tab subs=new Tab("My Subscriptions","SELECT s.id,p.name plan,p.price,p.billing_cycle,s.start_date,s.next_billing_date,s.status FROM subscriptions s JOIN plans p ON p.id=s.plan_id WHERE s.user_id=? ORDER BY s.id DESC",uid);
            subs.btn("Cancel Selected",()->status("subscriptions","CANCELLED",subs));tp.addTab("My Subscriptions",subs);
            Tab invoices=new Tab("My Invoices","SELECT i.id,i.invoice_date,i.due_date,i.amount,i.status FROM invoices i JOIN subscriptions s ON s.id=i.subscription_id WHERE s.user_id=? ORDER BY i.id DESC",uid);
            invoices.btn("Pay Invoice",()->{Integer id=invoices.selected();if(id==null)return;String[]a=Db.row("SELECT i.amount,i.status FROM invoices i JOIN subscriptions s ON s.id=i.subscription_id WHERE i.id=? AND s.user_id=?",id,uid);if(a==null||a[1].equals("PAID")){msg("Invoice cannot be paid.");return;}String[]v=form("Payment","Method (CASH/CARD/UPI/BANK_TRANSFER)","Transaction Reference");if(v==null)return;Db.exec("INSERT INTO payments(invoice_id,amount,payment_method,transaction_ref) VALUES(?,?,?,?)",id,a[0],v[0].toUpperCase(),v[1]);Db.exec("UPDATE invoices SET status='PAID' WHERE id=?",id);msg("Payment recorded successfully.");});
            tp.addTab("Invoices",invoices);
            Tab payments=new Tab("My Payments","SELECT p.id,p.invoice_id,p.amount,p.payment_method,p.transaction_ref,p.payment_date,p.status FROM payments p JOIN invoices i ON i.id=p.invoice_id JOIN subscriptions s ON s.id=i.subscription_id WHERE s.user_id=? ORDER BY p.id DESC",uid);tp.addTab("Payments",payments);
        }
        root.add(tp,BorderLayout.CENTER);f.setContentPane(root);f.setVisible(true);
    }
    static LocalDate cycle(LocalDate d,String c){return switch(c){case "QUARTERLY"->d.plusMonths(3);case "YEARLY"->d.plusYears(1);default->d.plusMonths(1);};}
    static void generateInvoices(){
        String[] due=Db.row("SELECT COUNT(*) FROM subscriptions WHERE status='ACTIVE' AND next_billing_date<=CURDATE()");
        int n=Integer.parseInt(due[0]);
        Db.exec("INSERT INTO invoices(subscription_id,invoice_date,due_date,amount) SELECT s.id,CURDATE(),DATE_ADD(CURDATE(),INTERVAL 7 DAY),p.price FROM subscriptions s JOIN plans p ON p.id=s.plan_id WHERE s.status='ACTIVE' AND s.next_billing_date<=CURDATE()");
        Db.exec("UPDATE subscriptions s JOIN plans p ON p.id=s.plan_id SET s.next_billing_date=CASE p.billing_cycle WHEN 'YEARLY' THEN DATE_ADD(s.next_billing_date,INTERVAL 1 YEAR) WHEN 'QUARTERLY' THEN DATE_ADD(s.next_billing_date,INTERVAL 3 MONTH) ELSE DATE_ADD(s.next_billing_date,INTERVAL 1 MONTH) END WHERE s.status='ACTIVE' AND s.next_billing_date<=CURDATE()");
        msg(n+" due subscription invoice(s) generated.");
    }
}
