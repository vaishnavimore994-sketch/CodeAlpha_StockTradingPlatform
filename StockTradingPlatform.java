import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class StockTradingPlatform {

    // ---------- OOP classes ----------
    static class Stock {
        String symbol, name;
        double price;
        Stock(String symbol, String name, double price) {
            this.symbol = symbol; this.name = name; this.price = price;
        }
    }

    static class Transaction {
        String type, symbol, time;
        int qty;
        double price;
        Transaction(String type, String symbol, int qty, double price, String time) {
            this.type = type; this.symbol = symbol; this.qty = qty; this.price = price; this.time = time;
        }
    }

    static class User {
        String name;
        double cash;
        Map<String, Integer> holdings = new LinkedHashMap<>();
        Map<String, Double> avgCost = new HashMap<>();
        List<Transaction> history = new ArrayList<>();
        User(String name, double cash) { this.name = name; this.cash = cash; }
    }

    static class Market {
        Map<String, Stock> stocks = new LinkedHashMap<>();
        Random rnd = new Random();
        Market() {
            add("TCS", "Tata Consultancy", 3900);
            add("INFY", "Infosys", 1550);
            add("RELI", "Reliance Ind.", 2900);
            add("HDFC", "HDFC Bank", 1650);
            add("WIPR", "Wipro", 480);
        }
        void add(String s, String n, double p) { stocks.put(s, new Stock(s, n, p)); }
        void tick() {
            for (Stock s : stocks.values()) {
                double change = (rnd.nextDouble() - 0.5) * 0.10; // +-5%
                s.price = Math.round(s.price * (1 + change) * 100.0) / 100.0;
            }
        }
        void display() {
            System.out.println("\n--------- MARKET DATA ---------");
            System.out.printf("%-6s %-18s %10s%n", "Sym", "Company", "Price");
            for (Stock s : stocks.values())
                System.out.printf("%-6s %-18s %10.2f%n", s.symbol, s.name, s.price);
        }
    }

    static final String FILE = "portfolio.txt";
    static Market market = new Market();
    static User user;
    static List<Double> valueHistory = new ArrayList<>();
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        load();
        valueHistory.add(totalValue());
        while (true) {
            System.out.println("\n===== STOCK TRADING PLATFORM =====");
            System.out.println("User: " + user.name + " | Cash: " + String.format("%.2f", user.cash));
            System.out.println("1. View market   2. Buy   3. Sell");
            System.out.println("4. Portfolio     5. Transactions   6. Next day (update prices)");
            System.out.println("7. Performance over time   8. Save & Exit");
            System.out.print("Choice: ");
            switch (sc.nextLine().trim()) {
                case "1": market.display(); break;
                case "2": trade(true); break;
                case "3": trade(false); break;
                case "4": portfolio(); break;
                case "5": transactions(); break;
                case "6": market.tick(); valueHistory.add(totalValue()); System.out.println("Prices updated."); break;
                case "7": performance(); break;
                case "8": save(); System.out.println("Saved. Bye!"); return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static double totalValue() {
        double v = user.cash;
        for (Map.Entry<String, Integer> e : user.holdings.entrySet())
            v += e.getValue() * market.stocks.get(e.getKey()).price;
        return v;
    }

    static void trade(boolean buy) {
        market.display();
        System.out.print("Symbol: ");
        String sym = sc.nextLine().trim().toUpperCase();
        Stock s = market.stocks.get(sym);
        if (s == null) { System.out.println("Unknown symbol."); return; }
        System.out.print("Quantity: ");
        int q;
        try { q = Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { System.out.println("Invalid quantity."); return; }
        if (q <= 0) { System.out.println("Quantity must be positive."); return; }

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        if (buy) {
            double cost = q * s.price;
            if (cost > user.cash) { System.out.println("Insufficient funds."); return; }
            int old = user.holdings.getOrDefault(sym, 0);
            double oldAvg = user.avgCost.getOrDefault(sym, 0.0);
            user.avgCost.put(sym, (old * oldAvg + cost) / (old + q));
            user.holdings.put(sym, old + q);
            user.cash -= cost;
            user.history.add(new Transaction("BUY", sym, q, s.price, time));
            System.out.printf("Bought %d %s @ %.2f%n", q, sym, s.price);
        } else {
            int have = user.holdings.getOrDefault(sym, 0);
            if (q > have) { System.out.println("You only hold " + have + " shares."); return; }
            user.cash += q * s.price;
            if (have == q) { user.holdings.remove(sym); user.avgCost.remove(sym); }
            else user.holdings.put(sym, have - q);
            user.history.add(new Transaction("SELL", sym, q, s.price, time));
            System.out.printf("Sold %d %s @ %.2f%n", q, sym, s.price);
        }
    }

    static void portfolio() {
        System.out.println("\n--------------- PORTFOLIO ---------------");
        if (user.holdings.isEmpty()) System.out.println("No holdings.");
        else {
            System.out.printf("%-6s %5s %10s %10s %10s%n", "Sym", "Qty", "AvgCost", "Price", "P/L");
            for (Map.Entry<String, Integer> e : user.holdings.entrySet()) {
                String sym = e.getKey(); int q = e.getValue();
                double cur = market.stocks.get(sym).price, avg = user.avgCost.get(sym);
                System.out.printf("%-6s %5d %10.2f %10.2f %10.2f%n", sym, q, avg, cur, (cur - avg) * q);
            }
        }
        System.out.printf("Cash: %.2f | Total value: %.2f%n", user.cash, totalValue());
    }

    static void transactions() {
        if (user.history.isEmpty()) { System.out.println("No transactions."); return; }
        for (Transaction t : user.history)
            System.out.printf("%s | %-4s %-5s x%d @ %.2f%n", t.time, t.type, t.symbol, t.qty, t.price);
    }

    static void performance() {
        System.out.println("\nPortfolio value over time:");
        for (int i = 0; i < valueHistory.size(); i++)
            System.out.printf("Day %2d : %.2f%n", i, valueHistory.get(i));
        double start = valueHistory.get(0), now = totalValue();
        System.out.printf("Change: %.2f (%.2f%%)%n", now - start, (now - start) / start * 100);
    }

    // ---------- File persistence ----------
    static void save() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println("USER," + user.name + "," + user.cash);
            for (Map.Entry<String, Integer> e : user.holdings.entrySet())
                pw.println("HOLD," + e.getKey() + "," + e.getValue() + "," + user.avgCost.get(e.getKey()));
            for (Transaction t : user.history)
                pw.println("TX," + t.type + "," + t.symbol + "," + t.qty + "," + t.price + "," + t.time);
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    static void load() {
        File f = new File(FILE);
        if (!f.exists()) {
            System.out.print("Enter your name: ");
            user = new User(sc.nextLine().trim(), 100000);
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                switch (p[0]) {
                    case "USER": user = new User(p[1], Double.parseDouble(p[2])); break;
                    case "HOLD":
                        user.holdings.put(p[1], Integer.parseInt(p[2]));
                        user.avgCost.put(p[1], Double.parseDouble(p[3])); break;
                    case "TX":
                        user.history.add(new Transaction(p[1], p[2], Integer.parseInt(p[3]),
                                Double.parseDouble(p[4]), p[5])); break;
                }
            }
            System.out.println("Portfolio loaded for " + user.name);
        } catch (Exception e) {
            System.out.println("Corrupt save file, starting fresh.");
            user = new User("Trader", 100000);
        }
    }
}