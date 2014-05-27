package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import com.avaje.ebean.Ebean;

import play.db.ebean.*;

@Entity

public class Foodtruckinfo extends Model {

	@Id
	public String id;
	
	public String applicant;
	@Lob
	public String fooditems;
	public String address;
	public double latitude;
	public double longitude;
	public String status;
	public long fetchtime;
	
	public static synchronized void updateinfo(List<Foodtruckinfo> new_foodtruck_info){
		List<Foodtruckinfo> old_foodtrucks = new Model.Finder<String, Foodtruckinfo>(String.class, Foodtruckinfo.class).all();
		Ebean.delete(old_foodtrucks);
		for (Foodtruckinfo f : new_foodtruck_info)
		{
			f.save();
		}
	}
	
}