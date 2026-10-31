package subscription;

import java.security.MessageDigest;
import java.sql.*;
import java.util.HexFormat;
import javax.swing.table.DefaultTableModel;

public class Db {
    static final String HOST="jdbc:mysql://localhost:3306/";
    static final String OPTS="?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    static final String USER="root", PASS="";
    static Connection get() throws SQLException {
        return DriverManager.getConnection(HOST+"subscription_db"+OPTS,USER,PASS);
    }
    static String hash(String s) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"))); }
        catch(Exception e){ throw new RuntimeException(e); }
    }
    static void exec(String sql,Object... p) {
        try(Connection c=get(); PreparedStatement s=c.prepareStatement(sql)){
            for(int i=0;i<p.length;i++) s.setObject(i+1,p[i]); s.executeUpdate();
        } catch(Exception e){ throw new RuntimeException(e); }
    }
    static String[] row(String sql,Object... p) {
        try(Connection c=get(); PreparedStatement s=c.prepareStatement(sql)){
            for(int i=0;i<p.length;i++) s.setObject(i+1,p[i]);
            try(ResultSet r=s.executeQuery()){
                if(!r.next()) return null; ResultSetMetaData m=r.getMetaData(); String[] a=new String[m.getColumnCount()];
                for(int i=0;i<a.length;i++) a[i]=r.getString(i+1); return a;
            }
        } catch(Exception e){ throw new RuntimeException(e); }
    }
    static DefaultTableModel table(String sql,Object... p) {
        try(Connection c=get(); PreparedStatement s=c.prepareStatement(sql)){
            for(int i=0;i<p.length;i++) s.setObject(i+1,p[i]);
            try(ResultSet r=s.executeQuery()){
                ResultSetMetaData m=r.getMetaData(); int n=m.getColumnCount();
                String[] cols=new String[n]; for(int i=0;i<n;i++) cols[i]=m.getColumnLabel(i+1);
                DefaultTableModel model=new DefaultTableModel(cols,0){ public boolean isCellEditable(int r,int c){return false;} };
                while(r.next()){ Object[] a=new Object[n]; for(int i=0;i<n;i++) a[i]=r.getObject(i+1); model.addRow(a); }
                return model;
            }
        } catch(Exception e){ throw new RuntimeException(e); }
    }
    static void init() throws Exception {
        try(Connection c=DriverManager.getConnection(HOST+OPTS,USER,PASS); Statement s=c.createStatement()){s.execute("CREATE DATABASE IF NOT EXISTS subscription_db");}
        String[] ddl={
          "CREATE TABLE IF NOT EXISTS users(id INT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100) NOT NULL,email VARCHAR(150) UNIQUE NOT NULL,password_hash CHAR(64) NOT NULL,role ENUM('ADMIN','BILLING','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',phone VARCHAR(25),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
          "CREATE TABLE IF NOT EXISTS plans(id INT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100) NOT NULL,description TEXT,price DECIMAL(10,2) NOT NULL, billing_cycle ENUM('MONTHLY','QUARTERLY','YEARLY') NOT NULL, status ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE')",
          "CREATE TABLE IF NOT EXISTS subscriptions(id INT AUTO_INCREMENT PRIMARY KEY,user_id INT NOT NULL,plan_id INT NOT NULL,start_date DATE NOT NULL,next_billing_date DATE NOT NULL,status ENUM('ACTIVE','CANCELLED','EXPIRED') DEFAULT 'ACTIVE',FOREIGN KEY(user_id) REFERENCES users(id),FOREIGN KEY(plan_id) REFERENCES plans(id))",
          "CREATE TABLE IF NOT EXISTS invoices(id INT AUTO_INCREMENT PRIMARY KEY,subscription_id INT NOT NULL,invoice_date DATE NOT NULL,due_date DATE NOT NULL,amount DECIMAL(10,2) NOT NULL,status ENUM('UNPAID','PAID','OVERDUE') DEFAULT 'UNPAID',FOREIGN KEY(subscription_id) REFERENCES subscriptions(id))",
          "CREATE TABLE IF NOT EXISTS payments(id INT AUTO_INCREMENT PRIMARY KEY,invoice_id INT NOT NULL,amount DECIMAL(10,2) NOT NULL,payment_method ENUM('CASH','CARD','UPI','BANK_TRANSFER') NOT NULL,transaction_ref VARCHAR(100),payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,status ENUM('SUCCESS','FAILED') DEFAULT 'SUCCESS',FOREIGN KEY(invoice_id) REFERENCES invoices(id))"
        };
        try(Connection c=get();Statement s=c.createStatement()){for(String q:ddl)s.execute(q);}
        if(row("SELECT id FROM users WHERE email='admin@subscription.com'")==null)
            exec("INSERT INTO users(name,email,password_hash,role,phone) VALUES(?,?,?,'ADMIN',?)","System Admin","admin@subscription.com",hash("admin123"),"9999999999");
        if(row("SELECT id FROM users WHERE email='billing@subscription.com'")==null)
            exec("INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,'BILLING')","Billing Staff","billing@subscription.com",hash("bill123"));
        if(row("SELECT id FROM users WHERE email='customer@example.com'")==null)
            exec("INSERT INTO users(name,email,password_hash,role,phone) VALUES(?,?,?,'CUSTOMER',?)","Demo Customer","customer@example.com",hash("cust123"),"9876543210");
        if(row("SELECT id FROM plans LIMIT 1")==null){
            exec("INSERT INTO plans(name,description,price,billing_cycle) VALUES('Basic','Essential features for individuals',199,'MONTHLY')");
            exec("INSERT INTO plans(name,description,price,billing_cycle) VALUES('Professional','Advanced tools for growing teams',499,'MONTHLY')");
            exec("INSERT INTO plans(name,description,price,billing_cycle) VALUES('Business Annual','Complete business package billed yearly',4999,'YEARLY')");
        }
    }
}
