package controllers;

import play.mvc.Controller;
import play.mvc.Catch;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import models.SalesforceManager;
import models.FixtureInfo;

import jp.co.flect.net.OAuth2;
import jp.co.flect.net.OAuthResponse;
import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.UserInfo;
import jp.co.flect.salesforce.fixtures.Fixture;
import jp.co.flect.salesforce.fixtures.FixtureLoader;
import jp.co.flect.salesforce.fixtures.FixtureRunner;

import java.util.List;

public class Application extends Controller {
	
	static {
		utils.CommonSetting.setup();
	}
	
	private static final String AUTH_URL = "https://login.salesforce.com/services/oauth2/authorize";
	private static final String TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";
	
	private static final String SALESFORCE_APPID = System.getenv("SALESFORCE_APPID");
	private static final String SALESFORCE_SECRET = System.getenv("SALESFORCE_SECRET");
	
	private static OAuth2 createOAuth2(String baseUrl, boolean useSandbox) {
		if (baseUrl.indexOf("localhost") == -1 && baseUrl.startsWith("http://")) {
			baseUrl = "https://" + baseUrl.substring(7);
		}
		String redirectUrl = baseUrl + "/login";
		String authUrl = AUTH_URL;
		String tokenUrl = TOKEN_URL;
		if (useSandbox) {
			authUrl = authUrl.replace("login", "test");
			tokenUrl = tokenUrl.replace("login", "test");
		}
		return new OAuth2(
			authUrl,
			tokenUrl,
			SALESFORCE_APPID,
			SALESFORCE_SECRET,
			redirectUrl
		);
	}
	
	public static void index() {
		render();
	}
	
	public static void loginRedirect(boolean useSandbox) {
		new SalesforceManager(session.getId()).setUseSandbox(useSandbox);
		
		OAuth2 oauth = createOAuth2(request.getBase(), useSandbox);
		redirect(oauth.getLoginUrl());
	}
	
	public static void login(String code) throws Exception {
		if (code == null) {
			badRequest();
		}
		SalesforceManager sm = new SalesforceManager(session.getId());
		OAuth2 oauth = createOAuth2(request.getBase(), sm.isUseSandbox());
		OAuthResponse res = oauth.authenticate(code);
		SalesforceClient client = new SalesforceClient(SalesforceManager.getWSDL());
		UserInfo user = client.login(res);
		sm.setLoginInfo(user.getUserId(), client.getSessionId(), client.getEndpoint());
		main();
	}
	
	private static SalesforceManager checkLogin() {
		SalesforceManager sm = new SalesforceManager(session.getId());
		String username = sm.getUserName();
		if (username == null) {
			index();
		}
		return sm;
	}
		
	public static void main() {
		SalesforceManager sm = checkLogin();
		List<FixtureInfo> list = sm.getList();
		render(list);
	}
	
	public static void create(String name, String yaml) {
		SalesforceManager sm = checkLogin();
		if (name == null || yaml == null || name.length() == 0 || yaml.length() == 0) {
			badRequest();
		}
		try {
			parseYaml(yaml);
			FixtureInfo.create(sm.getUserName(), name, yaml);
			renderText("OK");
		} catch (Exception e) {
			renderText(e.getMessage());
		}
	}
	
	public static void update(long id, String name, String yaml) throws Exception {
		SalesforceManager sm = checkLogin();
		if (name == null || yaml == null || name.length() == 0 || yaml.length() == 0) {
			badRequest();
		}
		FixtureInfo info = sm.getInfo(id);
		if (info == null) {
			main();
		}
		try {
			parseYaml(yaml);
			info.name = name;
			info.yaml = yaml;
			info.save();
			renderText("OK");
		} catch (Exception e) {
			renderText(e.getMessage());
		}
	}
	
	public static void delete(long id) {
		SalesforceManager sm = checkLogin();
		FixtureInfo info = sm.getInfo(id);
		if (info != null) {
			info.delete();
		}
		main();
	}
	
	public static void edit(long id) {
		SalesforceManager sm = checkLogin();
		FixtureInfo info = sm.getInfo(id);
		if (info == null) {
			main();
		}
		List<FixtureInfo> list = sm.getList();
		renderTemplate("@main", info, list);
	}
	
	public static void fixtureByName(String name) throws Exception {
		SalesforceManager sm = checkLogin();
		FixtureInfo info = sm.getInfo(name);
		if (info == null) {
			main();
		}
		fixture(info.id);
	}
	
	public static void fixture(long id) throws Exception {
		SalesforceManager sm = checkLogin();
		List<FixtureInfo> list = sm.getList();
		FixtureInfo info = sm.getInfo(id);
		if (info == null) {
			main();
		}
		List<Fixture> fxList = parseYaml(info.yaml);
		render(info, list, fxList);
	}
	
	public static void runUpdate(long id, String name) throws Exception {
		SalesforceManager sm = checkLogin();
		Fixture fx = sm.getFixture(id, name);
		if (fx == null) {
			renderText("Fixtureが見つかりません: " + name);
		}
		SalesforceClient client = sm.getSalesforceClient();
		boolean ret = new FixtureRunner(client).update(fx);
		renderText(ret ? "OK" : "NG");
	}
	
	public static void runDelete(long id, String name) throws Exception {
		SalesforceManager sm = checkLogin();
		Fixture fx = sm.getFixture(id, name);
		if (fx == null) {
			renderText("Fixtureが見つかりません: " + name);
		}
		SalesforceClient client = sm.getSalesforceClient();
		boolean ret = new FixtureRunner(client).delete(fx);
		renderText(ret ? "OK" : "NG");
	}
	
	private static List<Fixture> parseYaml(String yaml) throws IOException {
		try {
			ByteArrayInputStream is = new ByteArrayInputStream(yaml.getBytes("utf-8"));
			return new FixtureLoader().load(is);
		} catch (IOException e) {
			throw new IOException("YAMLのパースに失敗しました: " + e.getMessage(), e);
		}
	}
	
	@Catch
	static void handleException(Exception e) {
		e.printStackTrace();
		renderText(e.toString());
	}
	
}