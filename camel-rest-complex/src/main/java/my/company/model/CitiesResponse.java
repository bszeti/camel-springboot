package my.company.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

//Response pojo with country and a list of cities
public class CitiesResponse extends ApiResponse {
	@ApiModelProperty(value="Country name requested", example="France")
	@JsonProperty
	private String country;

	@ApiModelProperty(value="List of some cities in the country")
	@JsonProperty
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

	//generated
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		CitiesResponse that = (CitiesResponse) o;

		if (country != null ? !country.equals(that.country) : that.country != null) return false;
		return cities != null ? cities.equals(that.cities) : that.cities == null;
	}

	@Override
	public int hashCode() {
		int result = country != null ? country.hashCode() : 0;
		result = 31 * result + (cities != null ? cities.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "CitiesResponse{" +
				"country='" + country + '\'' +
				", cities=" + cities +
				'}';
	}
}
