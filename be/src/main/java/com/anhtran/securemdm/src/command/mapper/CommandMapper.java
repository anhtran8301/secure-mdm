package com.anhtran.securemdm.src.command.mapper;

import com.anhtran.securemdm.src.command.dto.response.CommandResponse;
import com.anhtran.securemdm.src.command.entity.DeviceCommand;
import com.anhtran.securemdm.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/** Request hash, idempotency key and signed JWS stay server-side; they are not mapped. */
@Component
@RequiredArgsConstructor
public class CommandMapper {
  private final ModelMapper modelMapper;

  public CommandResponse toResponse(DeviceCommand command) {
    CommandResponse response = modelMapper.map(command, CommandResponse.class);
    response.setPayload(JsonUtils.readObject(command.getPayloadJson()));
    response.setResult(JsonUtils.readObject(command.getResultJson()));
    return response;
  }
}
