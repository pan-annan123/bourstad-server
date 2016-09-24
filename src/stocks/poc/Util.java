package stocks.poc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Util {
	
	private static List<String> tickers;
	
	static {
		String[] s = {
				"ABX   ",
				"AC    ",
				"AGF.B ",
				"AGU   ",
				"ALA   ",
				"ATD.B ",
				"BB    ",
				"BBD.B ",
				"BCE   ",
				"BLD   ",
				"BMO   ",
				"BNS   ",
				"BPY.UN",
				"CAE   ",
				"CAM   ",
				"CAS   ",
				"CCA   ",
				"CCO   ",
				"CM    ",
				"CNQ   ",
				"CNR   ",
				"CP    ",
				"CPG   ",
				"CS    ",
				"CTC.A ",
				"CUF.UN",
				"DOL   ",
				"DSG   ",
				"ECA   ",
				"EMA   ",
				"EMP.A ",
				"ENB   ",
				"FSZ   ",
				"FTS   ",
				"G     ",
				"GIB.A ",
				"GIL   ",
				"HGD   ",
				"HGU   ",
				"HND   ",
				"HNU   ",
				"HOD   ",
				"HOU   ",
				"HRX   ",
				"HSD   ",
				"HSE   ",
				"HSU   ",
				"HXD   ",
				"HXU   ",
				"IAG   ",
				"IMO   ",
				"INE   ",
				"IRG   ",
				"JNX   ",
				"L     ",
				"LB    ",
				"MFC   ",
				"MFI   ",
				"MG    ",
				"MNT   ",
				"MNW   ",
				"MRU   ",
				"MSL   ",
				"NA    ",
				"OCX   ",
				"OR    ",
				"ORL   ",
				"ORT   ",
				"OSB   ",
				"OTC   ",
				"OVI.A ",
				"PD    ",
				"PEY   ",
				"PJC.A ",
				"PLI   ",
				"POT   ",
				"POW   ",
				"PWF   ",
				"QBR.B ",
				"QSR   ",
				"RCH   ",
				"RCI.B ",
				"RON   ",
				"RUS   ",
				"RX    ",
				"RY    ",
				"SAP   ",
				"SJ    ",
				"SLF   ",
				"SNC   ",
				"SU    ",
				"T     ",
				"TCK.B ",
				"TCL.A ",
				"TD    ",
				"TFI   ",
				"TMB   ",
				"TPX.B ",
				"TRP   ",
				"TRZ   ",
				"UFS   ",
				"VNR   ",
				"VRX   ",
				"WJA   ",
				"WSP   ",
				"X     ",
				"Y     "
		};
		tickers = new ArrayList<>();
		for (String str: s) {
			tickers.add(str.trim());
		}
	}
	
	public static String getStringFromParams(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry: params.entrySet()) {
			try {
				sb.append("&").append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return sb.length() == 0 ? "" : sb.substring(1);
	}
	
	public static boolean isAllowedTicker(String ticker) {
		return tickers.contains(ticker) || ticker.equals("CASH");
	}
	
	public static double getPrice(String ticker) {
		if (ticker.equals("CASH")) return 1.0;
		Document doc;
		try {
			doc = Jsoup.connect("https://www.google.com/finance/info?q=TSE%3A"+ticker).get();
			JSONArray array = new JSONArray(doc.getElementsByTag("body").text().substring(3).trim());
			JSONObject vars = array.getJSONObject(0);
			double price = Double.parseDouble(vars.getString("l"));
			return price;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0.0;
	}
	
}
