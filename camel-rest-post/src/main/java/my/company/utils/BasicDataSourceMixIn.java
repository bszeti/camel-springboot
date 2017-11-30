package my.company.utils;

import org.apache.commons.dbcp2.BasicDataSource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility=Visibility.NONE, getterVisibility=Visibility.NONE, isGetterVisibility=Visibility.NONE)
public abstract class BasicDataSourceMixIn extends BasicDataSource{
	
	@Override
	@JsonProperty
	public abstract String getDriverClassName();

	@Override
	@JsonProperty
	public abstract int getMaxIdle();

	@Override
	@JsonProperty
	public abstract int getMaxTotal();

	
	@Override
	@JsonProperty
	public abstract int getMinIdle();
	
	@Override
	@JsonProperty
	public abstract String getUrl();
	
	@Override
	@JsonProperty
	public abstract String getUsername();
	
	@Override
	@JsonProperty
	public abstract String getPassword();
}
