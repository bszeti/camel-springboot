package my.company.model;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;


public class HeaderValidationsPojo {
	@Max(value=99)
	@NotNull(message="is required")
	private Long id;
	
	@Length(min=16, max=48)
	private String businessId;
	

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}
	
	
	
}
