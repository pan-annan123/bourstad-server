

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import pan.cache.loader.Cache;
import stocks.poc.Entity;
import stocks.poc.Transaction;
import stocks.poc.Util;

/**
 * Servlet implementation class Origin
 */
@WebServlet("/Origin")
public class Origin extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private HashMap<String, Entity> users = new HashMap<>();
	private long lastUpdate = 0;
	private static final long UPDATE_INTERVAL = 60*60*1000;
	private boolean active = false;
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Origin() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}
	
	private String getMessage(int response) {
		switch (response) {
		case Entity.ACTION_PENDING:
			return "Pending: this transaction is waiting for processing time";
		case Entity.ACTION_ALLOWED:
			return "Accepted: this transaction has been accepted";
		case Entity.ACTION_INVALID_STOCK:
			return "Refused: invalid stock";
		case Entity.ACTION_INSUFFICIENT_FUNDS:
			return "Refused: tried to execute a transaction with insufficient funds";
		case Entity.ACTION_INVALID_MULTIPLE:
			return "Refused: invalid multiple of 100";
		case Entity.ACTION_LIMIT_SHORT:
			return "Refused: limit exceeded for shorting";
		default:
			return "Transaction Error";
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		Cache<HashMap<String, Entity>> usercache = new Cache<>(new File("/tmp/bourstad"), "data");

		long time = System.currentTimeMillis();
		if (time - lastUpdate > UPDATE_INTERVAL) {
			Thread t = new Thread(() -> {
				users.values().stream().parallel().forEach(user -> {
					user.update();
				});
				lastUpdate = time;
//				usercache.saveCache(users);
			});
			t.start();
		}

		if (!active) {
//			if (usercache.exists()) {
//				users = usercache.loadCache();
//			}
			active = true;
			new Thread(() -> {
				while (active) {
					LocalTime schedule = LocalTime.of(16, 35);
					LocalDateTime next = LocalTime.now().isAfter(schedule) ? LocalDate.now().plusDays(1).atTime(schedule) : LocalDate.now().atTime(schedule);
					Duration waittime = Duration.between(LocalDateTime.now(), next);
					System.out.println(waittime);
					try {
						Thread.sleep(waittime.toMillis());
					} catch (InterruptedException e) {}
					System.out.println("Executing transactions..");
					users.values().stream().parallel().forEach(user -> user.executePendingTransactions());
//					usercache.saveCache(users);
				}
			}).start();
		}
		
		Map<String, String[]> params = request.getParameterMap();
		if (!params.containsKey("action")) end(response);

		Map<String, String> echo = new HashMap<>();
		String key;
		String ticker;
		int amount;
		Entity ent;
		switch (params.get("action")[0]) {
		case "auth":
			String username = params.get("username")[0];
			boolean duplicate = users.values().stream().anyMatch(user -> user.getUsername().equals(username));
			if (duplicate) {
				echo.put("status", "false");
				break;
			}
			do {
				ent = new Entity(username);
			} while (users.containsKey(ent.getKey()));
			users.put(ent.getKey(), ent);
			echo.put("status", "true");
			echo.put("key", ent.getKey());
			break;
		case "keyAuth":
			key = params.get("key")[0];
			echo.put("status", String.valueOf(users.containsKey(key)));
			if (users.containsKey(key)) {
				echo.put("username", users.get(key).getUsername());
			}
			break;
		case "buy":
			key = params.get("key")[0];
			if (!users.containsKey(key)) end(response);
			ticker = params.get("ticker")[0];
			try {
				amount = Integer.parseInt(params.get("amount")[0]);
				ent = users.get(key);
				ent.addTransaction(new Transaction(true, ticker, amount));
				echo.put("message", "Transaction added to queue");
			} catch (NumberFormatException e) {end(response);}
			break;
		case "sell":
			key = params.get("key")[0];
			if (!users.containsKey(key)) end(response);
			ticker = params.get("ticker")[0];
			try {
				amount = Integer.parseInt(params.get("amount")[0]);
				ent = users.get(key);
				ent.addTransaction(new Transaction(false, ticker, amount));
				echo.put("message", "Transaction added to queue");
			} catch (NumberFormatException e) {end(response);}
			break;
		case "transactions":
			key = params.get("key")[0];
			if (!users.containsKey(key)) end(response);
			ent = users.get(key);
			JSONArray transactions = new JSONArray();
			ent.getTransactions().forEach(trans -> {
				JSONObject obj = new JSONObject();
				obj.put("message", getMessage(trans.getStatus()));
				obj.put("buy", trans.isBuy());
				obj.put("ticker", trans.getTicker());
				obj.put("amount", trans.getAmount());
				transactions.put(obj);
			});
			echo.put("transactions", transactions.toString());
			break;
		case "list":
			key = params.get("key")[0];
			if (!users.containsKey(key)) end(response);
			ent = users.get(key);
			ent.getStocks().forEach((a,b) -> echo.put(a, String.valueOf(b)));
			break;
		case "rank":
			users.values().stream()
			.collect(Collectors.toMap(user -> user.getUsername(), user -> user.getValue()))
			.forEach((a,b) -> echo.put(a, String.valueOf(b)));
			break;
		default:
			end(response);
		}
		response.getWriter().append(Util.getStringFromParams(echo));
//		usercache.saveCache(users);
	}

	private void end(HttpServletResponse response) throws IOException {
		response.getWriter().append("Invalid Action");
	}
}
