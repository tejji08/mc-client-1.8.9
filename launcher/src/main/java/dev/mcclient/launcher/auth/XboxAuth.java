package dev.mcclient.launcher.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Xbox Live + XSTS token exchange, the step between an MS token and a Minecraft token. */
public final class XboxAuth {

    public record Result(String token, String userHashSalt) {}

    private final HttpClient http;

    public XboxAuth(HttpClient http) {
        this.http = http;
    }

    public Result authenticateWithXboxLive(String msAccessToken) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + msAccessToken);
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "http://auth.xboxlive.com");
        body.addProperty("TokenType", "JWT");

        JsonObject response = postJson("https://user.auth.xboxlive.com/user/authenticate", body);
        return extractTokenAndUhs(response);
    }

    public Result authenticateWithXsts(String xboxLiveToken) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        var userTokens = new com.google.gson.JsonArray();
        userTokens.add(xboxLiveToken);
        properties.add("UserTokens", userTokens);
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        body.addProperty("TokenType", "JWT");

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (response.statusCode() == 401 && json.has("XErr")) {
            throw new IOException(describeXstsError(json.get("XErr").getAsLong()));
        }
        return extractTokenAndUhs(json);
    }

    private String describeXstsError(long code) {
        if (code == 2148916233L) return "This Microsoft account has no Xbox Live profile. Sign in to xbox.com once to create one.";
        if (code == 2148916235L) return "Xbox Live is not available in this account's region.";
        if (code == 2148916236L || code == 2148916237L) return "This account needs adult verification (South Korea age check).";
        if (code == 2148916238L) return "This is a child account. An adult needs to add it to a Microsoft family group.";
        return "XSTS authorization failed (XErr " + code + ")";
    }

    private Result extractTokenAndUhs(JsonObject response) {
        String token = response.get("Token").getAsString();
        String uhs = response.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui").get(0).getAsJsonObject()
                .get("uhs").getAsString();
        return new Result(token, uhs);
    }

    private JsonObject postJson(String url, JsonObject body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }
}
