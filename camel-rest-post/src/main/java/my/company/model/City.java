package my.company.model;

import java.util.List;

public class City {
	private String name;
	private List<String> zips;
	private String error;

	public City() {
	}

	public City(String cityName) {
		this.name = cityName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String> getZips() {
		return zips;
	}

	public void setZips(List<String> zips) {
		this.zips = zips;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
