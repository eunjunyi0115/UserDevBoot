package other.dev.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import other.dev.dto.model.OtherTest;

@Mapper
public interface OtherTestMapper {

	List<OtherTest> findAll();
}
