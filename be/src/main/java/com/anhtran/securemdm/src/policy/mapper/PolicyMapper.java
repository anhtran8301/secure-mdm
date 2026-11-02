package com.anhtran.securemdm.src.policy.mapper;

import com.anhtran.securemdm.src.policy.dto.model.PolicyConfig;
import com.anhtran.securemdm.src.policy.dto.response.PolicyResponse;
import com.anhtran.securemdm.src.policy.dto.response.PolicyVersionResponse;
import com.anhtran.securemdm.src.policy.entity.PolicyVersion;
import com.anhtran.securemdm.src.policy.repository.projection.AssignedVersionView;
import com.anhtran.securemdm.src.policy.repository.projection.PolicyView;
import com.anhtran.securemdm.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PolicyMapper {
  private final ModelMapper modelMapper;

  public PolicyResponse toResponse(PolicyView view) {
    PolicyResponse response = modelMapper.map(view, PolicyResponse.class);
    response.setConfig(config(view.getConfigJson()));
    return response;
  }

  public PolicyVersionResponse toVersionResponse(PolicyVersion version) {
    PolicyVersionResponse response = modelMapper.map(version, PolicyVersionResponse.class);
    response.setConfig(config(version.getConfigJson()));
    return response;
  }

  public PolicyVersionResponse toVersionResponse(AssignedVersionView version) {
    PolicyVersionResponse response = modelMapper.map(version, PolicyVersionResponse.class);
    response.setConfig(config(version.getConfigJson()));
    return response;
  }

  public PolicyConfig config(String json) {
    return JsonUtils.read(json, PolicyConfig.class);
  }
}
