package voss.core.server.builder;

import naef.dto.NaefDto;

import java.util.List;

public interface ComplementBuilder extends CommandBuilder {

    void addTargetName(String absoluteName);

    void addTargetNames(List<String> names);

    void addTargetDto(NaefDto dto);

    void addTargetDtos(List<NaefDto> dtos);
}