package my.company.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Length;


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
