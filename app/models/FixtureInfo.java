package models;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import play.db.jpa.GenericModel;

@Entity(name="SALESFORCE_FIXTURES")
public class FixtureInfo extends GenericModel {
	
	@Id
	@GeneratedValue
	@Column(name="ID")
	public long id;
	
	@Column(name="USERNAME")
	public String username;
	
	@Column(name="NAME")
	public String name;
	
	@Column(name="YAML")
	public String yaml;
	
	@Column(name="INSERT_DATE")
	public Timestamp insertDate;
	@Column(name="UPDATE_DATE")
	public Timestamp updateDate;
	
	public static List<FixtureInfo> findByUser(String username) {
		return FixtureInfo.find("byUSERNAME", username).fetch();
	}
	
	public static void create(String username, String name, String yaml) {
		FixtureInfo info = new FixtureInfo();
		info.username = username;
		info.name = name;
		info.yaml = yaml;
		info.insertDate = new Timestamp(System.currentTimeMillis());
		info.updateDate = info.insertDate;
		info.save();
	}
}
