package my.company.model;

import io.swagger.annotations.ApiModelProperty;

public class ApiResponse {
	@ApiModelProperty(value="Response code. 0 for success.", example="4000")
	private int code;
	
	@ApiModelProperty(value="Response message.", example="Invalid json content")
	private String message;

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

}
