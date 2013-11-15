package models;

import play.cache.Cache;
import java.io.Serializable;
import java.io.IOException;
import java.io.ByteArrayInputStream;

import jp.co.flect.salesforce.SalesforceClient;
import jp.co.flect.salesforce.fixtures.Fixture;
import jp.co.flect.salesforce.fixtures.FixtureLoader;
import jp.co.flect.soap.WSDL;
import java.io.File;
import java.util.List;

public class SalesforceManager {
	
	private static final WSDL wsdl;
	
	static {
		try {
			wsdl = new WSDL(new File("conf/partner.wsdl"));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static WSDL getWSDL() { return wsdl;}
	
	private String sessionId;
	private List<FixtureInfo> list;
	
	public SalesforceManager(String sessionId) {
		this.sessionId = sessionId;
	}
	
	private String key(String name) {
		return sessionId + "-" + name;
	}
	
	public boolean isUseSandbox() { 
		Boolean b = (Boolean)Cache.get(key("useSandbox"));
		return b != null && b;
	}
	
	public void setUseSandbox(boolean b) { Cache.set(key("useSandbox"), b, "1h");}
	
	public void setLoginInfo(String username, String session, String endpoint) {
		Cache.set(key("login"), new LoginInfo(username, session, endpoint));
	}
	
	public String getUserName() {
		LoginInfo info = (LoginInfo)Cache.get(key("login"));
		return info == null ? null : info.username;
	}
	
	public SalesforceClient getSalesforceClient() {
		LoginInfo info = (LoginInfo)Cache.get(key("login"));
		if (info == null) {
			return null;
		}
		SalesforceClient client = new SalesforceClient(wsdl);
		client.setSessionId(info.session);
		client.setEndpoint(info.endpoint);
		return client;
	}
	
	public List<FixtureInfo> getList() {
		return this.list != null ? this.list : FixtureInfo.findByUser(getUserName());
	}
	
	public FixtureInfo getInfo(long id) {
		List<FixtureInfo> list = getList();
		for (FixtureInfo fx : list) {
			if (id == fx.id) {
				return fx;
			}
		}
		return null;
	}
	
	public FixtureInfo getInfo(String name) {
		List<FixtureInfo> list = getList();
		for (FixtureInfo fx : list) {
			if (name.equals(fx.name)) {
				return fx;
			}
		}
		return null;
	}
	
	public Fixture getFixture(long id, String name) throws IOException {
		FixtureInfo info = getInfo(id);
		if (info == null) {
			return null;
		}
		ByteArrayInputStream is = new ByteArrayInputStream(info.yaml.getBytes("utf-8"));
		List<Fixture> list = new FixtureLoader().load(is);
		for (Fixture fx : list) {
			if (name.equals(fx.getName())) {
				return fx;
			}
		}
		return null;
	}
	
	public static class LoginInfo implements Serializable {
		
		public String username;
		public String session;
		public String endpoint;
		
		public LoginInfo(String u, String s, String e) {
			this.username = u;
			this.session = s;
			this.endpoint = e;
		}
	}
}