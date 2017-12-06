package my.company.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

//Base response pojo with a status code and message
public class ApiResponse {
	@ApiModelProperty(value="Response code. 0 for success.", example="4000")
	@JsonProperty
	private int code;
	
	@ApiModelProperty(value="Response message.", example="Invalid json content")
	@JsonProperty
	private String message;

	public ApiResponse() {};

	public ApiResponse(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	//generated
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ApiResponse that = (ApiResponse) o;

		if (code != that.code) return false;
		return message != null ? message.equals(that.message) : that.message == null;
	}

	@Override
	public int hashCode() {
		int result = code;
		result = 31 * result + (message != null ? message.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ApiResponse{" +
				"code=" + code +
				", message='" + message + '\'' +
				'}';
	}
}
