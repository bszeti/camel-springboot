package my.company.model;

import java.util.List;

public class CitiesResponse extends ApiResponse {
	private String country;
	private List<String> cities;

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public List<String> getCities() {
		return cities;
	}

	public void setCities(List<String> cities) {
		this.cities = cities;
	}

}
