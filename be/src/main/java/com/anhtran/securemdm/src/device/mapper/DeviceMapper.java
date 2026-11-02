package com.anhtran.securemdm.src.device.mapper;

import com.anhtran.securemdm.src.device.dto.response.DeviceResponse;
import com.anhtran.securemdm.src.device.repository.projection.DeviceView;
import com.anhtran.securemdm.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceMapper {
  private final ModelMapper modelMapper;

  public DeviceResponse toResponse(DeviceView view) {
    DeviceResponse response = modelMapper.map(view, DeviceResponse.class);
    response.setInventory(JsonUtils.readObject(view.getInventoryJson()));
    return response;
  }
}
