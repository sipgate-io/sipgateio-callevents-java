package sipgateio.callevents;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class App {

	private static final String BASE_URL = "[YOUR_SERVERS_ADDRESS]";

	public static void main(String[] args) throws IOException {
		SimpleHttpServer simpleHttpServer = new SimpleHttpServer(8080);
		simpleHttpServer.addPostHandler("/new-call", App::handleNewCall);
		simpleHttpServer.addPostHandler("/on-answer", App::handleOnAnswer);
		simpleHttpServer.addPostHandler("/on-hangup", App::handleOnHangup);
		simpleHttpServer.start();
	}

	private static void handleNewCall(HttpExchange httpExchange) throws IOException {
		Map<String, String> requestData = parseRequestBody(httpExchange);

		String caller = requestData.getOrDefault("from", "[unknown]");
		String calleeNumber = requestData.getOrDefault("to", "[unknown]");

		System.out.println(String.format("New call from %s to %s is ringing...", caller, calleeNumber));

		String xmlResponse = composeXmlResponse();
		sendResponse(httpExchange, xmlResponse);
	}

	private static void handleOnAnswer(HttpExchange httpExchange) throws IOException {
		Map<String, String> requestData = parseRequestBody(httpExchange);

		String caller = requestData.getOrDefault("from", "[unknown]");
		String calleeName = requestData.getOrDefault("to", "[unknown]");

		System.out.println(String.format("%s answered call from %s", calleeName, caller));

		String response = "This response will be discarded.";
		sendResponse(httpExchange, response);
	}

	private static void handleOnHangup(HttpExchange httpExchange) throws IOException {
		System.out.println("The call has been hung up");

		String response = "This response will be discarded.";
		sendResponse(httpExchange, response);
	}

	private static Map<String, String> parseRequestBody(HttpExchange httpExchange) throws IOException {
		InputStream requestBody = httpExchange.getRequestBody();

		BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
		String urlEncodedContent = reader.readLine();

		return decodeUrlEncodedLine(urlEncodedContent);
	}

	private static Map<String, String> decodeUrlEncodedLine(String line) throws UnsupportedEncodingException {
		Map<String, String> keyValuePairs = new HashMap<>();

		String[] pairs = line.split("&");
		for (String pair : pairs) {
			String[] fields = pair.split("=");
			String key = URLDecoder.decode(fields[0], "UTF-8");
			String value = URLDecoder.decode(fields[1], "UTF-8");

			keyValuePairs.put(key, value);
		}

		return keyValuePairs;
	}

	private static void sendResponse(HttpExchange httpExchange, String response) throws IOException {
		Headers responseHeaders = httpExchange.getResponseHeaders();

		responseHeaders.set("Content-Type", "application/xml");
		httpExchange.sendResponseHeaders(200, response.length());

		OutputStream responseBody = httpExchange.getResponseBody();
		responseBody.write(response.getBytes());
		responseBody.close();
	}

	private static String composeXmlResponse() {
		return String.format("<Response onAnswer=\"%s\" onHangup=\"%s\" />",
				BASE_URL + "/on-answer",
				BASE_URL + "/on-hangup");
	}


}
