package stocks.poc;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Entity implements Serializable {

	private static final long serialVersionUID = 1L;
	private String username;
	private String key;
	private ArrayList<Transaction> transactions;
	private HashMap<String, Integer> stocks;
	private double value;
	private double cash;
	
	public static final int ACTION_PENDING = -1;
	public static final int ACTION_ALLOWED = 0;
	public static final int ACTION_INVALID_STOCK = 1;
	public static final int ACTION_INSUFFICIENT_FUNDS = 2;
	public static final int ACTION_INVALID_MULTIPLE = 3;
	public static final int ACTION_LIMIT_SHORT = 4;
	public static final int ACTION_ILLEGAL_AMOUNT = 5;
	
	public Entity(String username) {
		this.username = username;
		this.key = Long.toHexString((long) (new Random().nextDouble()*0xFFFFFFFFL));
		stocks = new HashMap<>();
		stocks.put("CASH", 200000);
		value = 200000.0;
		cash = 200000.0;
		transactions = new ArrayList<>();
		log("Created new entity");
	}
	
	private void log(String message) {
		System.out.printf("[%s] %20s | %s\n", LocalDateTime.now(), username, message);
	}
	
	public void addTransaction(Transaction transaction) {
		transactions.add(transaction);
		log("Added transaction: "+transaction.toString());
	}
	
	public void executePendingTransactions() {
		transactions.forEach(trans -> {
			if (trans.getStatus() >= 0) return;
			if (trans.getDateTime().isAfter(LocalDate.now().atTime(LocalTime.of(16, 30)))) return;
			log("Executing "+trans.toString());
			int code;
			if (trans.isBuy()) {
				code = actionBuyStock(trans.getTicker(), trans.getAmount());
			} else {
				code = actionSellStock(trans.getTicker(), trans.getAmount());
			}
			trans.setStatus(code);
		});
		update();
	}
	
	public int actionBuyStock(String ticker, int amount) {
		if (!Util.isAllowedTicker(ticker)) {
			return ACTION_INVALID_STOCK;
		}
		if (amount % 100 != 0) return ACTION_INVALID_MULTIPLE;
		if (amount <= 0) return ACTION_ILLEGAL_AMOUNT;
		
		//MONEY NEEDED
		double price = Util.getPrice(ticker);
		log("Price: "+price);
		
		// brokerage fees
		double fees;
		if (price <= 5.0) {
			fees = 15.0 + (double)amount/100.0;
		}
		else if (price < 20.0) {
			fees = 15.0 + 3.0 * (double)amount/100.0;
		}
		else {
			fees = 15.0 + 5.0 * (double)amount/100.0;
		}
		fees = Math.min(fees, 250.0);
		
		double need = price * (double)amount + fees;
		
		// usable liquid cash
		double minus = stocks.keySet().stream().parallel()
				.mapToDouble(stock -> stocks.get(stock) < 0 ? Util.getPrice(stock) * stocks.get(stock) : 0)
				.sum();
		double usable = cash + minus;
		
		if (usable < need) {
			return ACTION_INSUFFICIENT_FUNDS;
		} else {
			cash -= need;
			stocks.put("CASH", (int)cash);
		}
		
		int amount_new = stocks.containsKey(ticker) ? stocks.get(ticker) + amount : amount;
		if (amount_new == 0) {
			stocks.remove(ticker);
		} else {
			stocks.put(ticker, amount_new);
		}
		return ACTION_ALLOWED;
	}
	
	public int actionSellStock(String ticker, int amount) {
		if (!Util.isAllowedTicker(ticker)) {
			return ACTION_INVALID_STOCK;
		}
		if (amount % 100 != 0) return ACTION_INVALID_MULTIPLE;
		if (amount <= 0) return ACTION_ILLEGAL_AMOUNT;
		
		//MONEY NEEDED
		double price = Util.getPrice(ticker);
		log("Price: "+price);
		
		// brokerage fees
		double fees;
		if (price <= 5.0) {
			fees = 15.0 + (double)amount/100.0;
		}
		else if (price < 20.0) {
			fees = 15.0 + 3.0 * (double)amount/100.0;
		}
		else {
			fees = 15.0 + 5.0 * (double)amount/100.0;
		}
		fees = Math.min(fees, 250.0);
		
		double value = price * (double) amount;
		double gains = value - fees;
		
		// shortable amount
		double minus = stocks.keySet().stream().parallel()
				.mapToDouble(stock -> stocks.get(stock) < 0 ? Util.getPrice(stock) * stocks.get(stock) : 0)
				.sum();
		update();
		double worth = this.value - stocks.get("CASH") + cash;
		boolean shortable = worth - fees >= Math.abs(minus) + value;
		
		if (!shortable) {
			return ACTION_LIMIT_SHORT;
		} else {
			cash += gains;
			stocks.put("CASH", (int)cash);
		}
		
		int amount_new = stocks.containsKey(ticker) ? stocks.get(ticker) - amount : -amount;
		if (amount_new == 0) {
			stocks.remove(ticker);
		} else {
			stocks.put(ticker, amount_new);
		}
		return ACTION_ALLOWED;
	}
	
	public void update() {
		value = stocks.keySet().stream().parallel().mapToDouble(ticker -> Util.getPrice(ticker) * stocks.get(ticker)).sum();
		value = Math.round((value - stocks.get("CASH") + cash) * 100.0) / 100.0;
		log("Value updated to "+value);
	}
	
	public double getValue() {
		return value;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getUsername() {
		return username;
	}
	
	public HashMap<String, Integer> getStocks() {
		return stocks;
	}
	
	public ArrayList<Transaction> getTransactions() {
		return transactions;
	}
	
}
