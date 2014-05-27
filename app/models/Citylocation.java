package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import com.avaje.ebean.Ebean;

import play.db.ebean.*;

@Entity

public class Citylocation extends Model {

	@Id
	public String id;
	
	public String city_name;
	public String zipcode;
	public String state;
	public double latitude;
	public double longitude;
	public long fetchtime;
	
	public static void addinfo(Citylocation citylocation){
		citylocation.save();
	}
	
}

