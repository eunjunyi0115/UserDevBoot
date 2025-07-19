package user.dev.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import user.dev.dto.model.UserTest;

//@Mapper
public interface UserTestMapper {

	List<UserTest> findAll();
}
