package my.company.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;


public class HeadersPojo {
	@Max(value=99)
	@NotNull(message="is required")
	private Long id;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
}
