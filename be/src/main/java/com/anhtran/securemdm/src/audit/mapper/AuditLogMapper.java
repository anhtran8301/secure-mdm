package com.anhtran.securemdm.src.audit.mapper;

import com.anhtran.securemdm.src.audit.dto.response.AuditLogResponse;
import com.anhtran.securemdm.src.audit.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogMapper {
  private final ModelMapper modelMapper;

  public AuditLogResponse toResponse(AuditLog log) {
    return modelMapper.map(log, AuditLogResponse.class);
  }
}
