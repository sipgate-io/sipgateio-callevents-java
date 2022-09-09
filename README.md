<img src="https://www.sipgatedesign.com/wp-content/uploads/wort-bildmarke_positiv_2x.jpg" alt="sipgate logo" title="sipgate" align="right" height="112" width="200"/>

# sipgate.io Java call events example
This example demonstrates how to receive and process webhooks from [sipgate.io](https://developer.sipgate.io/).

For further information regarding the push functionalities of sipgate.io please visit https://developer.sipgate.io/push-api/api-reference/

- [Prerequisites](#prerequisites)
- [Enabling sipgate.io for your sipgate account](#enabling-sipgateio-for-your-sipgate-account)
- [How sipgate.io webhooks work](#how-sipgateio-webhooks-work)
- [Configure webhooks for sipgate.io](#configure-webhooks-for-sipgateio)
- [A word on security](#a-word-on-security)
- [Making your computer accessible from the internet](#making-your-computer-accessible-from-the-internet)
- [Execution](#execution)
- [How It Works](#how-it-works)
- [Common Issues](#common-issues)
- [Contact Us](#contact-us)
- [License](#license)


## Prerequisites
- JDK 8

## Enabling sipgate.io for your sipgate account
In order to use sipgate.io, you need to book the corresponding package in your sipgate account. The most basic package is the free **sipgate.io S** package.

If you use [sipgate basic](https://app.sipgatebasic.de/feature-store) or [simquadrat](https://app.simquadrat.de/feature-store) you can book packages in your product's feature store.
If you are a _sipgate team_ user logged in with an admin account you can find the option under **Account Administration**&nbsp;>&nbsp;**Plans & Packages**.


## How sipgate.io webhooks work
### What is a webhook?
A webhook is a POST request that sipgate.io makes to a predefined URL when a certain event occurs.
These requests contain information about the event that occurred in `application/x-www-form-urlencoded` format. You can find more information on this format in the pertinent documentation.

This is an example payload converted from `application/x-www-form-urlencoded` to JSON:
```json
{
  "event": "newCall",
  "direction": "in",
  "from": "492111234567",
  "to": "4915791234567",
  "callId":"12345678",
  "origCallId":"12345678",
  "user": [ "Alice" ],
  "xcid": "123abc456def789",
  "diversion": "1a2b3d4e5f"
}
```


### sipgate.io webhook events
sipgate.io offers webhooks for the following events:

- **newCall:** is triggered when a new incoming or outgoing call occurs 
- **onAnswer:** is triggered when a call is answered – either by a person or an automatic voicemail
- **onHangup:** is triggered when a call is hung up
- **dtmf:** is triggered when a user makes an entry of digits during a call

**Note:** Per default, sipgate.io only sends webhooks for **newCall** events.
To subscribe to other event types you can reply to the **newCall** event with an XML response.
This response includes the event types you would like to receive webhooks for as well as the respective URL they should be directed to.
You can find more information about the XML response here:
https://developer.sipgate.io/push-api/api-reference/#the-xml-response


## Making your computer accessible from the internet
There are many possibilities to obtain an externally accessible address for your computer.
In this example we use the service [localhost.run](localhost.run) which sets up a reverse ssh tunnel that forwards traffic from a public URL to your localhost.
The following command creates a subdomain at localhost.run and sets up a tunnel between the public port 80 on their server and your localhost:8080:

```bash
$ ssh -R 80:localhost:8080 ssh.localhost.run
```
If you run this example on a server which can already be reached from the internet, you do not need the forwarding.
In that case, the webhook URL needs to be adjusted accordingly.

## Configure webhooks for sipgate.io 
You can configure webhooks for sipgate.io as follows:

1. Navigate to [console.sipgate.com](https://console.sipgate.com/) and login with your sipgate account credentials.
2. Select the **Webhooks**&nbsp;>&nbsp;**URLs** tab in the left side menu
3. Click the gear icon of the **Incoming** or **Outgoing** entry
4. Fill in your webhook URL and click save. In this example we receive _newCall_ events on the route `/new-call`.\
  **Note:** your webhook URL has to be accessible from the internet. (See the section [Making your computer accessible from the internet](#making-your-computer-accessible-from-the-internet))\
  **Example:** Assuming your server's address was `example.localhost.run`, the address you'd need to set in the webhook console would be `https://example.localhost.run/new-call`.
5. In the **sources** section you can select what phonelines and groups should trigger webhooks.

## A word on security
Although sipgate.io can work with both HTTP and HTTPS connections, it is strongly discouraged to use plain HTTP as the webhooks contain sensitive information.
The service `localhost.run` also supports HTTPS, so for development you will be fine using that.
For production, it is important to note that sipgate.io does not accept self-signed SSL certificates.
If you need a certificate for your server, you can easily get one at _Let´s Encrypt_.

## Configuration 
Create the `.env` by copying the [`.env.example`](.env.example) and set the values according to the comment above each variable.
`WEBHOOK_URL` is the URL under which your server is accessible from the internet (i.e. the URL you set up in the webhooks console minus the "`/new-call`" portion).

## Execution
Navigate to the project's root directory.

Run the application:

```bash
./gradlew run
```


## How It Works
On the top level, the code is very simple: 
```java
SimpleHttpServer simpleHttpServer = new SimpleHttpServer(8080);
simpleHttpServer.addPostHandler("/new-call", App::handleNewCall);
simpleHttpServer.addPostHandler("/on-answer", App::handleOnAnswer);
simpleHttpServer.addPostHandler("/on-hangup", App::handleOnHangup);
simpleHttpServer.start();
```
A `SimpleHttpServer` is instantiated with a port it will be listening on. The `addPostHandler()` method registers a callback function that should be called when the server receives a POST request on the specified route. In this example we register POST-handlers for the following three routes, of which each is responsible for handling only one type of call event:
* `/new-call` 
* `/on-answer`
* `/on-hangup`

With that configuration done, all that's left to do is start the server.

For the sake of simplicity, we will not cover the inner implementation of the `SimpleHttpServer` class. It is simply a wrapper around the `HttpServer` class from `com.sun.net.httpserver` that abstracts the filtering of request methods.

```java
simpleHttpServer.addPostHandler("/new-call", App::handleNewCall);
```

The two arguments of the `addPostHandler()` method are a context string specifying the intended endpoint, and a callback function to be called in the event of a POST request received there. The callback function needs to be of the type `HttpHandler` as specified by a functional interface from the `httpserver` package. The interface requires a `void` method that takes a single input of type `HttpExchange`.

The `handleNewCall` function (referenced as `App::handleNewCall`) implements that interface: 

```java
private static void handleNewCall(HttpExchange httpExchange) throws IOException {
  Map<String, String> requestData = parseRequestBody(httpExchange);

  String caller = requestData.getOrDefault("from", "[unknown]");
  String calleeNumber = requestData.getOrDefault("to", "[unknown]");

  System.out.println(String.format("New call from %s to %s is ringing...", caller, calleeNumber));

  String xmlResponse = composeXmlResponse();
  sendResponse(httpExchange, xmlResponse);
}
```

It takes the `httpExchange`, parses its request body, and prints a message to the console. It then writes an XML response to the exchange.

The `handleOnAnswer` and `handleOnHangup` function work very similarly, although they do not send an XML response but rather a plain-text one. This is because only the responses to _newCall_ and _onData_ events are interpreted by sipgate.io, all others are discarded.

```java
private static Map<String, String> parseRequestBody(HttpExchange httpExchange) throws IOException {
  InputStream requestBody = httpExchange.getRequestBody();

  BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
  String urlEncodedContent = reader.readLine();

  return decodeUrlEncodedLine(urlEncodedContent);
}
```

The `parseRequestBody` function takes the `requestBody` from the `HttpExchange` object, initializes a `BufferedReader` to read from the resulting `InputStream`, and then reads the single line it contains. That line is the `application/x-www-form-urlencoded` webhook content, thus it has to be decoded in order to be readable.

```java
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
```

The `decodeUrlEndcodedLine` function handles that task. The line that is provided contains multiple key-value pairs, separated by the _ampersand_ (`"&"`) character. Each key-value pair, in turn, is separated by an _equals sign_ (`"="`). Also, some special characters are encoded in both keys and values, so they need to be decoded before they are added to the map output.

```java
private static void sendResponse(HttpExchange httpExchange, String response) throws IOException {
    Headers responseHeaders = httpExchange.getResponseHeaders();

    responseHeaders.set("Content-Type", "application/xml");
    httpExchange.sendResponseHeaders(200, response.length());

    OutputStream responseBody = httpExchange.getResponseBody();
    responseBody.write(response.getBytes());
    responseBody.close();
}
```

The `sendResponse` function gets the `responseHeaders` and `responseBody` from the `HttpExchange` object.
`responseHeaders` is a Map into which the HTTP response headers can be stored and which will be transmitted as part of the response.
`responseBody` is an output stream which the response body is written to.
In order to correctly terminate the exchange, the output stream needs to be closed.

```java
private static String composeXmlResponse() {
  return String.format("<Response onAnswer=\"%s\" onHangup=\"%s\" />",
      BASE_URL + "/on-answer",
      BASE_URL + "/on-hangup");
}
```

The `composeXmlResponse` function composes the XML response with the `onAnswer` and `onHangup` attributes with the destination URLs for the corresponding webhooks.

## Common Issues

### web app displays "Feature sipgate.io not booked."
Possible reasons are:
- the sipgate.io feature is not booked for your account

See the section [Enabling sipgate.io for your sipgate account](#enabling-sipgateio-for-your-sipgate-account) for instruction on how to book sipgate.io


### "java.net.BindException: Address already in use"

Possible reasons are:
- another instance of the application is running
- the port configured is used by another application.


### "java.net.SocketException: Permission denied"

Possible reasons are:
- you do not have the permission to bind to the specified port. This can happen if you use port 80, 443 or another well-known port which you can only bind to if you run the application with superuser privileges.


### Call happened but no webhook was received 
Possible reasons are:
- the configured webhook URL is incorrect
- the SSH tunnel connection was closed in the background
- webhooks are not enabled for the phoneline that received the call


## Contact Us
Please let us know how we can improve this example.
If you have a specific feature request or found a bug, please use **Issues** or fork this repository and send a **pull request** with your improvements.


## License
This project is licensed under **The Unlicense** (see [LICENSE file](./LICENSE)).

---

[sipgate.io](https://www.sipgate.io) | [@sipgateio](https://twitter.com/sipgateio) | [API-doc](https://api.sipgate.com/v2/doc)
