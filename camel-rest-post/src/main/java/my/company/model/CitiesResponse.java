package my.company.model;

import java.util.List;

public class CitiesResponse extends ApiResponse {
	private String country;
	private List<City> cities;

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public List<City> getCities() {
		return cities;
	}

	public void setCities(List<City> cities) {
		this.cities = cities;
	}

}
