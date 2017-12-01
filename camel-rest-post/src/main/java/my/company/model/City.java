package my.company.model;

import java.util.List;
import java.util.Objects;

public class City {
	private String name;
	private List<String> zips;
	private String error;

	public City() {
	}

	public City(String cityName) {
		this.name = cityName;
	}

	public City(String cityName, List<String> zips) {
		this.name = cityName;
		this.zips = zips;
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

	//generated
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		City city = (City) o;

		if (name != null ? !name.equals(city.name) : city.name != null) return false;
		if (zips != null ? !zips.equals(city.zips) : city.zips != null) return false;
		return error != null ? error.equals(city.error) : city.error == null;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (zips != null ? zips.hashCode() : 0);
		result = 31 * result + (error != null ? error.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "City{" +
				"name='" + name + '\'' +
				", zips=" + zips +
				", error='" + error + '\'' +
				'}';
	}
}
