package controllers;

import static play.data.Form.form;

import java.util.*;

import models.Citylocation;
import models.Foodtruckinfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import models.InputForm;
import play.*;
import play.data.Form;
import play.db.ebean.Model;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.*;
import views.html.*;

public class Application extends Controller {
	public static Result index() {
		List<Foodtruckinfo> foodtrucks=new ArrayList<Foodtruckinfo>();;
		Citylocation location = get_zip_latlng("94114");
		return ok(home.render(location, foodtrucks));
	}

	public static Result show() {
		//get userinput from the form
		Form<InputForm> input = Form.form(InputForm.class).bindFromRequest();
		String zipCode = input.get().zipcode;
		String foodType = input.get().foodType;
		String distanceString = input.get().distance;
		
		double distance;
		Citylocation location = get_zip_latlng(zipCode);
		
		List<Foodtruckinfo> foodtrucks;
		
		//check to see if the zip code is in San Francisco or not
		if (!"San Francisco".equals(location.city_name)) {
			foodtrucks = new ArrayList<Foodtruckinfo>();
			Citylocation sanFrancisco = get_zip_latlng("94114");
			location.latitude=sanFrancisco.latitude;
			location.longitude=sanFrancisco.longitude;
			location.city_name="otherCity";
			return ok(index.render(input, location, foodtrucks));
		}
        
		// check the foodType; if it is empty return all food trucks.
		if (foodType == "") {
			foodType = "any";
		}

		// check the distance; if it is not a number show the food trucks within 1 mile
		if (distanceString == "") {
			distance = 1.0;
		} else {
			try{
			distance = Double.parseDouble(distanceString);}
			catch(Exception e)
			{
				distance = 1.0;
			}
		}
		
		//get the food trucks
		foodtrucks = getFoodTrucks(zipCode, foodType, distance);
		return ok(index.render(input, location, foodtrucks));

	}


	public static List<Foodtruckinfo> getFoodTrucks(String zipcode,
			String type, double distance) {
		synchronized (Application.class) {
			long current_time = System.currentTimeMillis();
			List<Foodtruckinfo> last_foodtrucks = new Model.Finder<String, Foodtruckinfo>(
					String.class, Foodtruckinfo.class).all();
			// update model if current model is empty or last update time was
			// done over 24 hours
			if (last_foodtrucks.size() == 0
					|| ((current_time - last_foodtrucks.get(0).fetchtime) / (1000 * 60 * 60 * 24)) > 1) {
				update_foodtruck_model();
			}
		}
		
		Citylocation citylocation;
		citylocation = get_zip_latlng(zipcode);

		if (!"San Francisco".equals(citylocation.city_name)) {
			return new ArrayList<Foodtruckinfo>();
		}

		List<Foodtruckinfo> foodtrucks;
		if ("any".equals(type)) {
			foodtrucks = new Model.Finder<String, Foodtruckinfo>(String.class,
					Foodtruckinfo.class).where().ne("latitude", 0.0).findList();
		} else {
			foodtrucks = new Model.Finder<String, Foodtruckinfo>(String.class,
					Foodtruckinfo.class).where()
					.like("fooditems", "%" + type + "%").where()
					.ne("latitude", 0.0).findList();
		}

		List<Foodtruckinfo> foodtrucks_in_distance = distance_filter(
				foodtrucks, distance, citylocation.latitude,
				citylocation.longitude);

		System.out.println(foodtrucks.size());
		System.out.println(foodtrucks_in_distance.size());

		if (foodtrucks_in_distance.size() != 0) {
			return foodtrucks_in_distance;
		}
		return new ArrayList<Foodtruckinfo>();
	}

	@SuppressWarnings("deprecation")
	public static synchronized List<Foodtruckinfo> update_foodtruck_model() {
		String food_data_url = "http://data.sfgov.org/resource/rqzj-sfat.json";
		final Promise<List<Foodtruckinfo>> resultPromise = WS
				.url(food_data_url).setQueryParameter("status", "APPROVED")
				.get().map(new Function<WS.Response, List<Foodtruckinfo>>() {
					public List<Foodtruckinfo> apply(WS.Response response) {
						JsonNode json = response.asJson();
						ArrayNode results = (ArrayNode) json;
						List<Foodtruckinfo> foodtrucks = new ArrayList<Foodtruckinfo>();
						Iterator<JsonNode> it = results.iterator();

						while (it.hasNext()) {
							JsonNode node = it.next();
							Foodtruckinfo foodtruck = new Foodtruckinfo();
							if (node.get("applicant") != null)
								foodtruck.applicant = node.get("applicant")
										.asText();
							if (node.get("fooditems") != null)
								foodtruck.fooditems = node.get("fooditems")
										.asText();
							if (node.get("address") != null)
								foodtruck.address = node.get("address")
										.asText();
							if (node.get("status") != null)
								foodtruck.status = node.get("status").asText();
							if (node.get("longitude") != null)
								foodtruck.longitude = node.get("longitude")
										.asDouble();
							if (node.get("latitude") != null)
								foodtruck.latitude = node.get("latitude")
										.asDouble();
							foodtruck.fetchtime = System.currentTimeMillis();
							foodtrucks.add(foodtruck);
						}
						return foodtrucks;
					}
				});
		List<Foodtruckinfo> new_foodtruck_info = resultPromise.get();
		Foodtruckinfo.updateinfo(new_foodtruck_info);
		return new_foodtruck_info;
	}

	@SuppressWarnings("deprecation")
	public static Citylocation get_zip_latlng(String zipcode) {
		String get_zip_latlng_url = "https://maps.googleapis.com/maps/api/geocode/json";
		List<Citylocation> citylocations = new Model.Finder<String, Citylocation>(
				String.class, Citylocation.class).where()
				.eq("zipcode", zipcode).findList();
		if (citylocations.size() > 0) {
			return citylocations.get(0);
		}
		final Promise<Citylocation> resultPromise = WS.url(get_zip_latlng_url)
				.setQueryParameter("address", zipcode).get()
				.map(new Function<WS.Response, Citylocation>() {
					public Citylocation apply(WS.Response response) {
						JsonNode json = response.asJson();
						Citylocation citylocation = new Citylocation();
						if ("OK".equals(json.get("status").asText())) {
							JsonNode address_components = json.get("results")
									.get(0).get("address_components");
							ArrayNode results = (ArrayNode) address_components;
							Iterator<JsonNode> it = results.iterator();
							while (it.hasNext()) {
								JsonNode node = it.next();
								String type = node.get("types").get(0).asText();
								if ("postal_code".equals(type)
										&& node.get("long_name") != null) {
									citylocation.zipcode = node
											.get("long_name").asText();
									continue;
								}
								if ("locality".equals(type)
										&& node.get("long_name") != null) {
									citylocation.city_name = node.get(
											"long_name").asText();
									continue;
								}
								if ("administrative_area_level_1".equals(type)
										&& node.get("long_name") != null) {
									citylocation.state = node.get("long_name")
											.asText();
									continue;
								}
							}
							if (json.get("results").get(0).get("geometry")
									.get("location").get("lat") != null)
								citylocation.latitude = json.get("results")
										.get(0).get("geometry").get("location")
										.get("lat").asDouble();
							if (json.get("results").get(0).get("geometry")
									.get("location").get("lng") != null)
								citylocation.longitude = json.get("results")
										.get(0).get("geometry").get("location")
										.get("lng").asDouble();
							citylocation.fetchtime = System.currentTimeMillis();
						}
						return citylocation;
					}
				});
		Citylocation new_citylocation_info = resultPromise.get();
		Citylocation.addinfo(new_citylocation_info);
		return new_citylocation_info;
	}

	private static List<Foodtruckinfo> distance_filter(
			List<Foodtruckinfo> foodtrucks, double distance, double latitude,
			double longitude) {
		List<Foodtruckinfo> foodtrucks_in_distance = new ArrayList<Foodtruckinfo>();
		for (Foodtruckinfo f : foodtrucks) {
			if (distance_check(latitude, longitude, f.latitude, f.longitude,
					distance)) {
				foodtrucks_in_distance.add(f);
			}
		}
		return foodtrucks_in_distance;
	}

	private static boolean distance_check(double lat1, double lon1,
			double lat2, double lon2, double distance) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
				+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2))
				* Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		return dist <= distance;
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts decimal degrees to radians : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	/* :: This function converts radians to decimal degrees : */
	/* ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: */
	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}
}
