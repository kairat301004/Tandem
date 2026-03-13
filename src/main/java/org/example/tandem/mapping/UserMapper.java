package org.example.tandem.mapping;

import org.example.tandem.dto.auth.AuthResponse;
import org.example.tandem.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    AuthResponse toAuthResponse(User user);
}
