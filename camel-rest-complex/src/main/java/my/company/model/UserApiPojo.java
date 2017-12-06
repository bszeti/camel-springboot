package my.company.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;
import java.util.Date;

//Pojo for incoming object
public class UserApiPojo {
	private String name;
	private Integer age;

	@ApiModelProperty(readOnly=true)
	private Date createdDate = new Date();
	
	@ApiModelProperty(readOnly=true, example="2016-01-31 18:00:00")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createdLocalDate = LocalDateTime.now();

	//Constructors
	public UserApiPojo() {
	}

	public UserApiPojo(String name, Integer age) {
		this.name = name;
		this.age = age;
	}
	
	//Getters, setters
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public LocalDateTime getCreatedLocalDate() {
		return createdLocalDate;
	}

	public void setCreatedLocalDate(LocalDateTime createdLocalDate) {
		this.createdLocalDate = createdLocalDate;
	}

	//hasCode, equals. date fields are not included
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((age == null) ? 0 : age.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserApiPojo other = (UserApiPojo) obj;
		if (age == null) {
			if (other.age != null)
				return false;
		} else if (!age.equals(other.age))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	//Used in log for example
	@Override
	public String toString() {
		return "UserApiPojo{" +
				"name='" + name + '\'' +
				", age=" + age +
				", createdDate=" + createdDate +
				", createdLocalDate=" + createdLocalDate +
				'}';
	}
}
